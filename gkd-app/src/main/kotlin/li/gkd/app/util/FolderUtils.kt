package li.gkd.app.util

import android.text.format.DateUtils
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import li.gkd.app.META
import li.gkd.app.app
import li.gkd.app.data.appinfo.AppInfoRepository
import li.gkd.app.data.AppInfo
import li.gkd.app.data.UserInfo
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.currentUserId
import java.io.File

object FolderUtils {
    @Volatile private var storageRoot: File? = null

    val isStorageReady: Boolean get() = storageRoot != null

    @WorkerThread
    @Synchronized
    fun initializeStorage() {
        if (storageRoot != null) return
        @Suppress("DEPRECATION")
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
        storageRoot = li.gkd.app.data.backup.PrivateStorageMigration.migrate(
            app.filesDir, app.getExternalFilesDir(null),
            allowEmptyInitialization = packageInfo.firstInstallTime > 0 &&
                packageInfo.firstInstallTime == packageInfo.lastUpdateTime,
            syncDirectory = { directory ->
                val descriptor = android.system.Os.open(
                    directory.absolutePath, android.system.OsConstants.O_RDONLY, 0,
                )
                try { android.system.Os.fsync(descriptor) } finally { android.system.Os.close(descriptor) }
            },
        )
    }

    private val filesDir: File
        get() = checkNotNull(storageRoot) { "Private storage must be initialized before use" }

    val dbFolder: File
        get() = filesDir.resolve("db").autoMk()
    val shFolder: File
        get() = filesDir.resolve("sh").autoMk()
    val storeFolder: File
        get() = filesDir.resolve("store").autoMk()
    val subsFolder: File
        get() = filesDir.resolve("subscription").autoMk()
    val snapshotFolder: File
        get() = filesDir.resolve("snapshot").autoMk()
    val logFolder: File
        get() = filesDir.resolve("log").autoMk()
    val crashFolder: File
        get() = filesDir.resolve("crash").autoMk()
    val crashTempFolder: File
        get() = filesDir.resolve("crash/temp").autoMk()

    val privateStoreFolder: File
        get() = app.filesDir.resolve("private-store").autoMk()

    private val cacheDir by lazy { app.cacheDir }
    val coilCacheDir: File
        get() = cacheDir.resolve("coil").autoMk()
    val sharedDir: File
        get() = cacheDir.resolve("shared").autoMk()
    private val tempDir: File
        get() = cacheDir.resolve("temp").autoMk()

    fun createGkdTempDir(): File {
        return java.nio.file.Files.createTempDirectory(tempDir.toPath(), "gkd-").toFile()
    }

    private fun removeExpired(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > DateUtils.HOUR_IN_MILLIS) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    fun clearCache() {
        removeExpired(sharedDir)
        removeExpired(tempDir)
    }

    suspend fun deleteSharedFile(file: File) = withContext(NonCancellable + Dispatchers.IO) {
        if (file.parentFile == sharedDir && file.exists() && !file.delete()) {
            LogUtils.d("无法清理共享缓存文件", file.absolutePath)
        }
    }

    suspend fun <T> withTemporaryZip(
        create: suspend () -> File,
        delete: suspend (File) -> Unit,
        consume: suspend (File) -> T,
    ): T {
        val file = create()
        try {
            return consume(file)
        } finally {
            withContext(NonCancellable) { delete(file) }
        }
    }

    @Serializable
    private data class AppJsonData(
        val userId: Int = currentUserId,
        val apps: List<AppInfo> = AppInfoRepository.userAppInfoMapFlow.value.values.toList(),
        val otherUsers: List<UserInfo> = AppInfoRepository.otherUserMapFlow.value.values.toList(),
        val othersApps: List<AppInfo> = AppInfoRepository.otherUserAppInfoMapFlow.value.values.toList(),
    )

    fun redactSensitiveText(text: String): String = li.gkd.app.data.backup.DiagnosticFiles.redact(text)

    suspend fun buildLogFile(snapshotDays: Int = 0): File = withContext(Dispatchers.IO) {
        require(snapshotDays >= 0)
        val logStatus = LogUtils.flushStatus()
        check(logStatus.drained) { "日志仍在写入，请稍后重试导出" }
        val capturedAt = System.currentTimeMillis()
        val since = if (snapshotDays > 0) capturedAt - snapshotDays.toLong() * 86_400_000L else null
        val tempDir = createGkdTempDir()
        val budget = li.gkd.app.data.backup.DiagnosticBudget()
        try {
            li.gkd.app.data.backup.BackupManager.writeDiagnostics(tempDir, budget, since, capturedAt)
            listOf(logFolder, crashFolder, app.noBackupFilesDir.resolve("crash-mirror")).forEach { source ->
                li.gkd.app.data.backup.DiagnosticFiles.copyTree(source, tempDir.resolve(source.name), budget = budget)
            }
            // Screenshots are included only for the range chosen for this export.
            li.gkd.app.data.backup.DiagnosticFiles.copySnapshots(
                snapshotFolder, tempDir.resolve("snapshot"), since, capturedAt, budget,
            )
            budget.writeProducedText(tempDir.resolve("source-paths.txt")) { output ->
                app.assets.open("source-paths.txt").use { it.copyTo(output) }
            }
            budget.writeJson(tempDir.resolve("apps.json"), json, AppJsonData())
            budget.writeText(tempDir.resolve("permission.txt"), buildString {
                val granted = PermissionStates.all.filter { it.value }
                if (granted.isNotEmpty()) append("已授权\n" + granted.joinToString("\n") { it.name } + "\n\n")
                val denied = PermissionStates.all.filter { !it.value }
                if (denied.isNotEmpty()) append("未授权\n" + denied.joinToString("\n") { it.name } + "\n\n")
                if (AppInfoRepository.appListAuthAbnormalFlow.value) append("其它\n读取应用列表权限异常\n")
            })
            val formattedJson = Json(from = json) { prettyPrint = true }
            budget.writeJson(tempDir.resolve("gkd.json"), formattedJson, META)
            tempDir.resolve("diagnostic-export.txt").writeText(
                listOfNotNull(budget.summary(), logStatus.diagnosticWarning()).joinToString("\n"),
            )
            val logZipFile = ExportFileNames.reserve(
                sharedDir, "log-${ExportFileNames.timestamp(System.currentTimeMillis())}", "zip",
            )
            try {
                check(ZipUtils.zipFiles(tempDir.listFiles().orEmpty().toList(), logZipFile)) { "日志压缩失败" }
                logZipFile
            } catch (e: Throwable) {
                logZipFile.delete()
                throw e
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
