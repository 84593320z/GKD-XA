package li.gkd.app.priv

import android.util.Log
import li.gkd.app.app
import li.gkd.app.data.backup.DiagnosticFiles
import li.gkd.app.platform.lifecycle.RuntimeStateSynchronizer
import java.io.File
import java.io.FileOutputStream

/** Recovery state and emergency evidence must work before FolderUtils/AppStore/Db exist. */
object CrashRecovery {
    private var window: PersistedCrashWindow? = null
    @Volatile private var storageUnavailable = false
    val isRecoveryBlocked: Boolean get() = storageUnavailable || window?.isBlocked != false

    @Synchronized
    fun initialize() {
        if (window != null) return
        try {
            window = PersistedCrashWindow(File(app.noBackupFilesDir, "crash-recovery.state"))
        } catch (error: Exception) {
            storageUnavailable = true
            Log.e("GkdCrash", "Unable to initialize crash recovery", error)
        }
    }

    fun recordCrash(thread: Thread, error: Throwable) {
        initialize()
        try {
            requireNotNull(window).recordCrash(System.currentTimeMillis())
        } catch (failure: Exception) {
            storageUnavailable = true
            Log.e("GkdCrash", "Unable to persist crash recovery", failure)
        }
        // Apply the owner-death gate before slower crash/report/log operations.
        runCatching { PrivilegeOwnerLifecycle.refreshCrashRecovery() }
        saveEmergencyEvidence(thread, error)
    }

    fun saveEmergencyEvidence(thread: Thread, error: Throwable) {
        runCatching {
            val folder = File(app.noBackupFilesDir, "crash-mirror").apply { mkdirs() }
            val file = File(folder, "crash-" + System.currentTimeMillis() + "-" + thread.id + ".log")
            val text = DiagnosticFiles.redact("thread=" + thread.name + "\n" + Log.getStackTraceString(error).take(128 * 1024))
            FileOutputStream(file).use { output -> output.write(text.toByteArray()); output.fd.sync() }
            folder.listFiles().orEmpty().filter { it.isFile }.sortedByDescending { it.name }.drop(20).forEach { it.delete() }
        }.onFailure { Log.e("GkdCrash", "Unable to save emergency crash mirror", it) }
    }

    /** Explicit user action; never called by a timer or by a Shizuku reconnect. */
    @Synchronized
    fun resumeAutomaticRecovery() {
        if (window == null) window = PersistedCrashWindow(File(app.noBackupFilesDir, "crash-recovery.state"))
        requireNotNull(window).reset()
        storageUnavailable = false
        PrivilegeOwnerLifecycle.refreshCrashRecovery()
        requestPrivilegeRecovery()
        RuntimeStateSynchronizer.requestSync(loc = "user resumed crash recovery")
    }
}
