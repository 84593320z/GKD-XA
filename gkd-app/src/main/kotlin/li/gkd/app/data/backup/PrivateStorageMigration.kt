package li.gkd.app.data.backup

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

/** Called before any database or writer opens: copy + verify + sync, then one atomic commit. */
internal object PrivateStorageMigration {
    private const val COMPLETE = ".migration-complete"
    private const val INVENTORY = ".migration-inventory"
    private val stagingName = Regex("[.]data-v2-migration-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private val directories = listOf("db", "sh", "store", "subscription", "snapshot", "log", "crash")

    fun migrate(
        internal: File,
        external: File?,
        allowEmptyInitialization: Boolean = false,
        syncDirectory: (File) -> Unit = {},
    ): File {
        check(internal.isDirectory && !Files.isSymbolicLink(internal.toPath())) { "Invalid private storage root" }
        val destination = internal.resolve("data-v2")
        if (destination.exists()) {
            check(destination.isDirectory && !Files.isSymbolicLink(destination.toPath()) &&
                destination.resolve(COMPLETE).readText() == "2") {
                "Private storage is incomplete; original data has been retained"
            }
            cleanupStaging(internal, external, destination)
            syncDirectory(internal)
            cleanupVerifiedSources(destination, internal, external)
            return destination
        }
        // A killed process cannot run finally; reclaim its private copies before allocating another.
        cleanupStaging(internal, external)
        val active = when {
            internal.resolve(".gkd").isFile -> internal
            external != null -> external
            allowEmptyInitialization -> internal
            else -> throw IOException("External storage unavailable; original data retained, retry when available")
        }
        val staging = internal.resolve(".data-v2-migration-${UUID.randomUUID()}")
        check(staging.mkdir()) { "Cannot create private storage staging directory" }
        try {
            val inventory = mutableListOf<String>()
            val directoryTimes = mutableListOf<Pair<File, Long>>()
            fun copyRoot(root: File, prefix: String) {
                check(root.isDirectory && !Files.isSymbolicLink(root.toPath())) { "Legacy storage unavailable" }
                directories.forEach { name ->
                    val source = root.resolve(name)
                    if (Files.notExists(source.toPath())) return@forEach
                    check(source.isDirectory && !Files.isSymbolicLink(source.toPath())) { "Unsafe legacy directory: $name" }
                    source.walkTopDown().onFail { _, error -> throw error }.forEach { file ->
                        check(!Files.isSymbolicLink(file.toPath())) { "Symbolic links are not supported in legacy storage" }
                        val relative = file.relativeTo(root).invariantSeparatorsPath
                        val targetRelative = prefix + relative
                        val target = staging.resolve(targetRelative)
                        if (file.isDirectory) {
                            check(target.isDirectory || target.mkdirs()) { "Cannot create migration directory" }
                            directoryTimes += target to file.lastModified()
                        } else {
                            check(file.isFile) { "Unsupported legacy storage entry" }
                            file.inputStream().use { input ->
                                target.outputStream().use { output ->
                                    input.copyTo(output)
                                    output.fd.sync()
                                }
                            }
                            val digest = hash(file)
                            check(digest == hash(target)) { "Legacy file changed while copying: $relative" }
                            target.setLastModified(file.lastModified())
                            inventory += listOf(encode(root.canonicalPath), encode(relative), encode(targetRelative), digest)
                                .joinToString("	")
                        }
                    }
                }
            }
            copyRoot(active, "")
            // Preserve inactive legacy trees separately; never overwrite the selected live root.
            listOfNotNull(internal, external).distinctBy { it.canonicalPath }
                .filter { it.canonicalFile != active.canonicalFile }
                .forEachIndexed { index, root -> copyRoot(root, "legacy-recovery/$index/") }
            // Apply in post-order: writing children must not make historical snapshot folders look new.
            directoryTimes.asReversed().forEach { (directory, modified) ->
                check(directory.setLastModified(modified)) { "Cannot preserve migration directory timestamp" }
            }
            syncText(staging.resolve(INVENTORY), inventory.joinToString("\n"))
            syncText(staging.resolve(COMPLETE), "2")
            staging.walkBottomUp().filter { it.isDirectory }.forEach(syncDirectory)
            Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            // Do not remove any original until the rename directory entry is durable.
            syncDirectory(internal)
        } finally {
            // A source may disappear/change during a failed copy. Apply the same proof as startup
            // recovery before deleting this attempt; it may now contain the only surviving bytes.
            runCatching { cleanupStaging(internal, external, destination.takeIf { it.isDirectory }) }
        }
        cleanupVerifiedSources(destination, internal, external)
        return destination
    }

    private fun cleanupStaging(internal: File, external: File?, committed: File? = null) {
        val candidates = internal.listFiles() ?: throw IOException("Cannot inspect private migration staging")
        val active = if (internal.resolve(".gkd").isFile) internal else external
        val inactive = listOfNotNull(internal, external).distinctBy { it.canonicalPath }
            .filter { it.canonicalFile != active?.canonicalFile }
        candidates.filter { stagingName.matches(it.name) && Files.isDirectory(it.toPath(), NOFOLLOW_LINKS) }
            .forEach { stage ->
                Files.walkFileTree(stage.toPath(), object : SimpleFileVisitor<Path>() {
                    override fun visitFile(path: Path, attributes: BasicFileAttributes): FileVisitResult {
                        if (attributes.isSymbolicLink) return FileVisitResult.CONTINUE
                        val relative = stage.toPath().relativize(path).toString().replace(File.separatorChar, '/')
                        if (relative == COMPLETE || relative == INVENTORY) return FileVisitResult.CONTINUE
                        val source = if (relative.startsWith("legacy-recovery/")) {
                            val parts = relative.split('/', limit = 3)
                            parts.getOrNull(1)?.toIntOrNull()?.let { inactive.getOrNull(it) }
                                ?.let { root -> parts.getOrNull(2)?.let { safeChild(root, it) } }
                        } else active?.let { safeChild(it, relative) }
                        val durableCopy = committed?.let { safeChild(it, relative) }
                        val file = path.toFile()
                        if (!attributes.isRegularFile ||
                            listOfNotNull(source, durableCopy).none { matchesCopiedPrefix(file, it) }) {
                            throw IOException("Interrupted migration contains unverified data; retained for recovery: "+ stage.name)
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
                removeStaging(internal, stage)
            }
    }

    private fun matchesCopiedPrefix(partial: File, original: File): Boolean {
        if (!original.isFile || partial.length() > original.length()) return false
        return partial.inputStream().buffered().use { copied ->
            original.inputStream().buffered().use { source ->
                while (true) {
                    val value = copied.read()
                    if (value < 0) break
                    if (value != source.read()) return false
                }
                true
            }
        }
    }

    private fun removeStaging(internal: File, staging: File) {
        // This namespace is reserved by this migrator. Never accept arbitrary sibling paths or follow links.
        check(stagingName.matches(staging.name) && staging.parentFile.canonicalFile == internal.canonicalFile)
        if (!Files.exists(staging.toPath(), NOFOLLOW_LINKS)) return
        check(Files.isDirectory(staging.toPath(), NOFOLLOW_LINKS)) { "Unsafe migration staging entry" }
        Files.walkFileTree(staging.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                Files.delete(file) // walkFileTree does not follow symbolic links without FOLLOW_LINKS.
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(directory: Path, error: IOException?): FileVisitResult {
                if (error != null) throw error
                Files.delete(directory)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun cleanupVerifiedSources(destination: File, internal: File, external: File?) {
        // Cleanup is retryable and cannot turn an already committed migration into startup failure.
        runCatching {
            val allowed = listOfNotNull(internal, external).map { it.canonicalFile }.toSet()
            val touched = mutableSetOf<File>()
            destination.resolve(INVENTORY).forEachLine { line ->
                runCatching {
                    val parts = line.split('	')
                    if (parts.size != 4) return@runCatching
                    val root = File(decode(parts[0])).canonicalFile
                    val relative = decode(parts[1])
                    if (root !in allowed || relative.substringBefore('/') !in directories) return@runCatching
                    val file = safeChild(root, relative) ?: return@runCatching
                    val copy = safeChild(destination, decode(parts[2])) ?: return@runCatching
                    // Revalidate both ends, including after a crash between commit and cleanup.
                    if (file.isFile && copy.isFile && hash(file) == parts[3] && hash(copy) == parts[3]) file.delete()
                    touched += root.resolve(relative.substringBefore('/'))
                }
            }
            touched.filter { it.isDirectory }.forEach { tree ->
                tree.walkBottomUp().filter { it.isDirectory && it.list()?.isEmpty() == true }.forEach { it.delete() }
            }
        }
    }

    private fun safeChild(root: File, relative: String): File? {
        if (File(relative).isAbsolute || relative.split('/', '\\').any { it == ".." }) return null
        val file = root.resolve(relative)
        var current: File? = file
        while (current != null && current != root) {
            if (Files.isSymbolicLink(current.toPath())) return null
            current = current.parentFile
        }
        return file.takeIf { it.canonicalPath.startsWith(root.canonicalPath + File.separator) }
    }

    private fun syncText(file: File, text: String) = file.outputStream().use {
        it.write(text.toByteArray(Charsets.UTF_8))
        it.fd.sync()
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun encode(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
    private fun decode(text: String) = String(Base64.getUrlDecoder().decode(text), Charsets.UTF_8)
}
