package li.gkd.app.snapshot

import java.io.File
import java.io.IOException
import java.nio.file.Files as NioFiles
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.ATOMIC_MOVE

class SnapshotFileLayout(
    val rootDirectory: File,
) {
    class Files(
        private val id: Long,
        val directory: File,
    ) {
        val snapshotFile: File
            get() = directory.resolve("$id.json")
        val minSnapshotFile: File
            get() = directory.resolve("$id.min.json")
        val webpFile: File
            get() = directory.resolve("$id.webp")
        val legacyPngFile: File
            get() = directory.resolve("$id.png")
        val screenshotFile: File
            get() = validScreenshotFile ?: webpFile.takeIf { it.exists() } ?: legacyPngFile
        val hasCompleteFiles: Boolean
            get() = snapshotFile.isFile && snapshotFile.length() > 0 &&
                validScreenshotFile != null

        private val validScreenshotFile: File?
            get() = when {
                webpFile.hasSupportedImageHeader() -> webpFile
                legacyPngFile.hasSupportedImageHeader() -> legacyPngFile
                else -> null
            }

        /** Recover originals staged by older versions before looking at the published image. */
        internal suspend fun recoverInterruptedScreenshotReplacement(invalidateUpload: suspend () -> Unit) {
            if (!directory.isDirectory) return
            val children = directory.listFiles() ?: throw IOException("Unable to list snapshot files")
            val replacements = children.filter { child ->
                child.name.startsWith(".$id.webp.replace-") &&
                    child.name.removePrefix(".$id.webp.replace-").matches(Regex("[0-9a-fA-F-]{36}")) &&
                    NioFiles.isRegularFile(child.toPath(), NOFOLLOW_LINKS)
            }
            if (replacements.isNotEmpty()) {
                invalidateUpload()
                if (!webpFile.hasSupportedImageHeader()) {
                    val original = replacements.filter { it.hasSupportedImageHeader() }.maxByOrNull { it.lastModified() }
                    if (original != null) NioFiles.move(original.toPath(), webpFile.toPath(), ATOMIC_MOVE)
                }
                // Keep any remaining original until we have a readable published WebP.
                if (webpFile.hasSupportedImageHeader()) replacements.forEach { it.delete() }
            }
            val tempName = Regex("\\.$id\\.webp\\.-?\\d+\\.tmp")
            children.filter { tempName.matches(it.name) &&
                NioFiles.isRegularFile(it.toPath(), NOFOLLOW_LINKS) }.forEach { it.delete() }
        }
    }

    fun committed(id: Long): Files {
        return Files(id, rootDirectory.resolve(id.toString()))
    }

    fun staging(id: Long): Files {
        return Files(id, rootDirectory.resolve(".$id.tmp"))
    }
}

private fun File.hasSupportedImageHeader(): Boolean {
    if (!isFile || length() < 2) return false
    return runCatching {
        val header = ByteArray(12)
        val size = inputStream().use { it.read(header) }
        fun matches(vararg bytes: Int): Boolean {
            return size >= bytes.size && bytes.indices.all { index ->
                header[index].toInt() and 0xff == bytes[index]
            }
        }
        matches(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) ||
            matches(0xff, 0xd8, 0xff) ||
            matches(0x52, 0x49, 0x46, 0x46) &&
            size >= 12 && header.copyOfRange(8, 12).contentEquals("WEBP".encodeToByteArray()) ||
            matches(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) ||
            matches(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) ||
            matches(0x42, 0x4d)
    }.getOrDefault(false)
}
