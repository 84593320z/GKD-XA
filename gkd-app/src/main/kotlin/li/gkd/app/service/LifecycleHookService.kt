package li.gkd.app.service

import android.content.Intent
import androidx.lifecycle.LifecycleService
import li.gkd.app.appRuntimeReady
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.platform.lifecycle.LifecycleHooks
import li.songe.codeorigin.CallSite

abstract class LifecycleHookService : LifecycleService() {
    private val lifecycleHooks = LifecycleHooks()
    protected var runtimeInitialized = false
        private set

    fun onCreated(callback: () -> Unit) = lifecycleHooks.onCreated(callback)

    fun onDestroyed(
        @CallSite loc: String = "",
        callback: () -> Unit,
    ) = lifecycleHooks.onDestroyed(loc = loc, callback = callback)

    fun useLogLifecycle(@CallSite loc: String = "") {
        lifecycleHooks.useLogLifecycle(owner = this, loc = loc)
    }

    final override fun onCreate() {
        super.onCreate()
        // A service started with startForegroundService must not wait for migration:
        // stopping now also clears the system foreground-service startup deadline.
        if (!appRuntimeReady.isCompleted || CrashRecovery.isRecoveryBlocked) {
            stopSelf()
            return
        }
        runtimeInitialized = true
        lifecycleHooks.dispatchCreated()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (!runtimeInitialized || CrashRecovery.isRecoveryBlocked) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return result
    }

    final override fun onDestroy() {
        if (runtimeInitialized) {
            lifecycleHooks.dispatchDestroyed()
            runtimeInitialized = false
        }
        super.onDestroy()
    }
}
