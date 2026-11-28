package li.gkd.app.data.snapshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import li.gkd.app.text.UiStrings
import li.gkd.app.data.ComplexSnapshot
import li.gkd.app.snapshot.SnapshotFileLayout
import li.gkd.app.snapshot.commitSnapshotDirectory
import li.gkd.db.Snapshot
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ZipUtils
import li.gkd.app.data.appinfo.AppInfoRepository
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.ExportFileNames
import li.gkd.app.util.json
import li.gkd.app.util.keepNullJson
import li.gkd.app.util.webpLossyCompressFormat
import li.gkd.db.Db
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.UUID

object SnapshotRepository : SnapshotStore(
    snapshotDao = Db.snapshotDao,
    snapshotRoot = FolderUtils.snapshotFolder,
)

data class SnapshotUploadArchive(
    val file: File,
    val screenshotModifiedAt: Long,
)

private const val MAX_RECENT_SNAPSHOT_IDS = 100

open class SnapshotStore(
    private val snapshotDao: Snapshot.SnapshotDao,
    snapshotRoot: File,
) {
    private val mutationMutex = Mutex()
    private val fileLayout = SnapshotFileLayout(snapshotRoot)

    fun snapshotFile(id: Long): File = fileLayout.committed(id).snapshotFile

    fun screenshotFile(id: Long): File = fileLayout.committed(id).screenshotFile

    fun snapshots(): Flow<List<Snapshot>> = snapshotDao.query()

    /**
     * Reconcile filesystem/Room publication windows left by process death. Run after Room opens
     * and before snapshot readers or writers start.
     */
    suspend fun recoverInterruptedMutations() = mutationMutex.withLock {
        withContext(NonCancellable + Dispatchers.IO) {
            val root = fileLayout.rootDirectory
            if (Files.notExists(root.toPath(), NOFOLLOW_LINKS)) {
                snapshotDao.queryIds().forEach { snapshotDao.deleteById(it) }
                return@withContext
            }
            val entries = root.listFiles() ?: throw IOException("Unable to list snapshot storage")
            val databaseIds = snapshotDao.queryIds().toMutableSet()
            val deletePattern = Regex("^\\.(\\d+)\\.delete-[0-9a-fA-F-]+$")
            val tempPattern = Regex("^\\.\\d+\\.tmp$")

            entries.filter { it.isDirectory }.forEach { entry ->
                when {
                    tempPattern.matches(entry.name) -> entry.deleteRecursivelyRequired()
                    else -> {
                        val match = deletePattern.matchEntire(entry.name) ?: return@forEach
                        val id = match.groupValues[1].toLongOrNull() ?: return@forEach
                        val published = fileLayout.committed(id)
                        val target = published.directory
                        if (id !in databaseIds) {
                            entry.deleteRecursivelyRequired()
                        } else if (Files.notExists(target.toPath(), NOFOLLOW_LINKS)) {
                            if (!entry.renameTo(target)) {
                                throw IOException("Unable to restore snapshot directory $id")
                            }
                        } else if (published.hasCompleteFiles) {
                            entry.deleteRecursivelyRequired()
                        } // Keep the staged copy if the published copy is incomplete or unreadable.
                    }
                }
            }

            databaseIds.toList().forEach { id ->
                val files = fileLayout.committed(id)
                try {
                    files.recoverInterruptedScreenshotReplacement { snapshotDao.deleteGithubAssetId(id) }
                } catch (error: IOException) {
                    LogUtils.d("暂时无法恢复快照截图", id, error::class.simpleName)
                }
                // An incomplete directory can still contain an irreplaceable screenshot or tree.
                if (Files.notExists(files.directory.toPath(), NOFOLLOW_LINKS)) {
                    snapshotDao.deleteById(id)
                    databaseIds.remove(id)
                }
            }

            (root.listFiles() ?: throw IOException("Unable to list snapshot storage"))
                .filter { it.isDirectory }
                .forEach { entry ->
                    val id = entry.name.toLongOrNull() ?: return@forEach
                    if (id !in databaseIds) entry.deleteRecursivelyRequired()
                }
        }
    }

    private fun File.deleteRecursivelyRequired() {
        if (exists() && !deleteRecursively()) {
            throw IOException("Unable to remove snapshot recovery directory $name")
        }
    }

    suspend fun markUploaded(
        snapshotId: Long,
        githubAssetId: Int,
        screenshotModifiedAt: Long,
    ): Boolean = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            val screenshot = fileLayout.committed(snapshotId).screenshotFile
            if (!screenshot.isFile || screenshot.lastModified() != screenshotModifiedAt) {
                return@withContext false
            }
            snapshotDao.markUploadedIfPending(snapshotId, githubAssetId) > 0
        }
    }

    suspend fun queryRecentSnapshotIds(maxCount: Int): List<Long> =
        snapshotDao.queryRecentIds(maxCount.coerceIn(0, MAX_RECENT_SNAPSHOT_IDS))

    suspend fun getMinSnapshot(id: Long): JsonObject = mutationMutex.withLock {
        requireNotNull(getMinSnapshotLocked(id, maxSourceBytes = null))
    }

    /** Returns null without reading when the source/cache would exceed [maxSourceBytes]. */
    suspend fun getMinSnapshot(id: Long, maxSourceBytes: Long): JsonObject? = mutationMutex.withLock {
        require(maxSourceBytes >= 0) { "maxSourceBytes must be nonnegative" }
        getMinSnapshotLocked(id, maxSourceBytes)
    }

    private suspend fun getMinSnapshotLocked(id: Long, maxSourceBytes: Long?): JsonObject? {
        val files = fileLayout.committed(id)
        val cachedText = withContext(Dispatchers.IO) {
            try {
                files.minSnapshotFile.takeIf {
                    it.isFile && it.length() > 0 && (maxSourceBytes == null || it.length() <= maxSourceBytes)
                }?.readText()
            } catch (_: IOException) {
                null // A cache read failure must not hide the source snapshot.
            }
        }
        if (cachedText != null) {
            val cachedSnapshot = withContext(Dispatchers.Default) {
                runCatching { json.decodeFromString<JsonObject>(cachedText) }.getOrNull()
            }
            if (cachedSnapshot != null) return cachedSnapshot
        }
        val text = withContext(Dispatchers.IO) {
            val source = files.snapshotFile
            if (maxSourceBytes != null && (!source.isFile || source.length() > maxSourceBytes)) {
                null
            } else {
                source.readText()
            }
        } ?: return null
        val snapshotJson = withContext(Dispatchers.Default) {
            // #1185
            json.decodeFromString<JsonObject>(text)
        }
        val minSnapshot = JsonObject(snapshotJson.toMutableMap().apply {
            this["nodes"] = JsonArray(emptyList())
        })
        withContext(Dispatchers.IO) {
            try {
                files.minSnapshotFile.writeText(keepNullJson.encodeToString(minSnapshot))
            } catch (_: IOException) {
                // A full disk or unusable cache path should not hide a successfully read snapshot.
            }
        }
        return minSnapshot
    }


    suspend fun delete(snapshot: Snapshot) {
        mutationMutex.withLock {
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable + Dispatchers.IO) {
                val directory = fileLayout.committed(snapshot.id).directory
                val staged = stageDeletion(directory)
                try {
                    snapshotDao.delete(snapshot)
                } catch (e: Throwable) {
                    rollbackDeletion(directory, staged, e)
                    throw e
                }
                finishDeletion(staged)
            }
        }
    }

    suspend fun replaceScreenshot(snapshot: Snapshot, newBytes: ByteArray): Boolean =
        mutationMutex.withLock {
            withContext(Dispatchers.IO) {
                val files = fileLayout.committed(snapshot.id)
                val oldBitmap = BitmapFactory.decodeFile(files.screenshotFile.absolutePath)
                    ?: return@withContext false
                val newBitmap = BitmapFactory.decodeByteArray(newBytes, 0, newBytes.size)
                if (newBitmap == null) {
                    oldBitmap.recycle()
                    return@withContext false
                }
                val sameSize = oldBitmap.width == newBitmap.width &&
                    oldBitmap.height == newBitmap.height
                oldBitmap.recycle()
                if (!sameSize) {
                    newBitmap.recycle()
                    return@withContext false
                }
                val tempFile = files.directory.resolve(
                    ".${files.webpFile.name}.${System.nanoTime()}.tmp"
                )
                try {
                    FileOutputStream(tempFile).use { stream ->
                        if (!newBitmap.compress(webpLossyCompressFormat, 85, stream)) {
                            throw IOException(UiStrings.screenshot_replacement_compress_failed)
                        }
                        stream.fd.sync()
                    }
                    currentCoroutineContext().ensureActive()
                    withContext(NonCancellable) {
                        // Clear the remote asset first. A crash can then only leave a re-uploadable image.
                        snapshotDao.deleteGithubAssetId(snapshot.id)
                        // Same-directory rename replaces the original without a missing-image window.
                        Os.rename(tempFile.absolutePath, files.webpFile.absolutePath)
                        val directorySynced = try {
                            syncDirectory(files.directory)
                            true
                        } catch (error: ErrnoException) {
                            LogUtils.d("暂时无法同步快照目录", snapshot.id, error.errno)
                            false
                        }
                        if (directorySynced && files.legacyPngFile.exists() && !files.legacyPngFile.delete()) {
                            LogUtils.d("无法删除旧快照截图", files.legacyPngFile.absolutePath)
                        }
                    }
                    true
                } finally {
                    newBitmap.recycle()
                    tempFile.delete()
                }
            }
        }

    suspend fun createUploadArchive(snapshotId: Long): SnapshotUploadArchive =
        mutationMutex.withLock {
            val screenshotModifiedAt = withContext(Dispatchers.IO) {
                fileLayout.committed(snapshotId).screenshotFile.lastModified()
            }
            SnapshotUploadArchive(
                file = createArchiveLocked(snapshotId),
                screenshotModifiedAt = screenshotModifiedAt,
            )
        }

    suspend fun createArchive(
        snapshotId: Long,
        appId: String? = null,
        activityId: String? = null,
    ): File =
        mutationMutex.withLock {
            createArchiveLocked(snapshotId, appId, activityId)
        }

    private suspend fun createArchiveLocked(
        snapshotId: Long,
        appId: String? = null,
        activityId: String? = null,
    ): File =
        withContext(Dispatchers.IO) {
            val filename = if (appId != null) {
                val appName = AppInfoRepository.appInfoMapFlow.value[appId]?.name
                    ?.filterNot { char -> char in "\\/:*?\"<>|" || char <= ' ' }
                val stem = if (activityId != null) {
                    "${(appName ?: appId).take(20)}_${
                        activityId.split('.').last().take(40)
                    }-${ExportFileNames.timestamp(snapshotId)}"
                } else {
                    "${(appName ?: appId).take(20)}-${ExportFileNames.timestamp(snapshotId)}"
                }
                ExportFileNames.availableName(stem, "zip") { name ->
                    FolderUtils.sharedDir.listFiles().orEmpty().any { directory ->
                        directory.isDirectory && directory.name.startsWith("snapshot-") &&
                            directory.resolve(name).exists()
                    }
                }
            } else {
                "${snapshotId}.zip"
            }
            require(File(filename).name == filename) { UiStrings.archive_name_invalid }
            val outputDirectory = FolderUtils.sharedDir.resolve(
                "snapshot-$snapshotId-${UUID.randomUUID()}"
            )
            if (!outputDirectory.mkdirs()) {
                throw IOException(UiStrings.snapshot_archive_directory_create_failed)
            }
            val outputFile = outputDirectory.resolve(filename)
            try {
                val files = fileLayout.committed(snapshotId)
                if (!files.hasCompleteFiles) {
                    throw IOException(UiStrings.snapshot_files_incomplete(snapshotId))
                }
                if (!ZipUtils.zipFiles(
                        listOf(files.snapshotFile, files.screenshotFile),
                        outputFile,
                    )
                ) {
                    throw IOException(UiStrings.snapshot_compress_failed)
                }
                outputFile
            } catch (e: Throwable) {
                if (!outputDirectory.deleteRecursively()) {
                    e.addSuppressed(IOException(UiStrings.snapshot_archive_directory_cleanup_failed))
                }
                throw e
            }
        }

    suspend fun deleteArchive(file: File) = withContext(NonCancellable + Dispatchers.IO) {
        val directory = file.parentFile ?: return@withContext
        if (directory.parentFile != FolderUtils.sharedDir || !directory.name.startsWith("snapshot-")) {
            return@withContext
        }
        if (directory.exists() && !directory.deleteRecursively()) {
            LogUtils.d("无法清理快照压缩目录", directory.absolutePath)
        }
    }

    suspend fun save(snapshot: ComplexSnapshot, bitmap: Bitmap): Unit = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            commitSnapshotDirectory(
                layout = fileLayout,
                id = snapshot.id,
                write = { files ->
                    files.webpFile.outputStream().use { stream ->
                        if (!bitmap.compress(webpLossyCompressFormat, 85, stream)) {
                            throw IOException(UiStrings.snapshot_screenshot_compress_failed)
                        }
                    }
                    files.snapshotFile.writeText(
                        keepNullJson.encodeToString(snapshot)
                    )
                    files.minSnapshotFile.writeText(
                        keepNullJson.encodeToString(snapshot.copy(nodes = emptyList()))
                    )
                },
                publish = {
                    snapshotDao.insert(snapshot.toSnapshot())
                },
            )
        }
    }

    private fun stageDeletion(target: File): File? {
        if (!target.exists()) return null
        val staged = requireNotNull(target.parentFile)
            .resolve(".${target.name}.delete-${UUID.randomUUID()}")
        if (!target.renameTo(staged)) {
            throw IOException(UiStrings.directory_stage_delete_failed(target.name))
        }
        return staged
    }

    private fun rollbackDeletion(target: File, staged: File?, cause: Throwable) {
        if (staged == null) return
        if (target.exists() && !target.deleteRecursively()) {
            cause.addSuppressed(IOException(UiStrings.directory_rollback_cleanup_failed(target.name)))
            return
        }
        if (!staged.renameTo(target)) {
            cause.addSuppressed(IOException(UiStrings.snapshot_directory_restore_failed(target.name)))
        }
    }

    private fun finishDeletion(staged: File?) {
        if (staged != null && staged.exists() && !staged.deleteRecursively()) {
            LogUtils.d("无法清理已删除快照目录", staged.absolutePath)
        }
    }

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

}
