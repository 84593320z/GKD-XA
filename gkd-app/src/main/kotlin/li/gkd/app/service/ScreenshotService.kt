package li.gkd.app.service

import android.content.Intent
import coil3.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import li.gkd.app.text.UiStrings
import li.gkd.app.app
import li.gkd.app.appRuntimeReady
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.platform.lifecycle.ResourceSlot
import li.gkd.app.platform.screenshot.MediaProjectionScreenshotSession
import li.gkd.app.util.LogUtils
import li.gkd.app.util.componentName
import li.gkd.app.util.runMainPost
import li.gkd.app.util.IntentUtils
import kotlin.time.Duration.Companion.milliseconds

class ScreenshotService : LifecycleHookService() {
    private val captureSessionSlot = ResourceSlot<MediaProjectionScreenshotSession>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!runtimeInitialized || CrashRecovery.isRecoveryBlocked || intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        captureSessionSlot.replace { createCaptureSession(intent) }
        LogUtils.d("screenshot restart")
        // A projection grant cannot be replayed by a sticky service restart.
        return START_NOT_STICKY
    }

    private fun createCaptureSession(intent: Intent): MediaProjectionScreenshotSession {
        return MediaProjectionScreenshotSession(intent) { invalidatedSession ->
            runMainPost {
                if (captureSessionSlot.get() === invalidatedSession) {
                    stopSelf()
                }
            }
        }
    }

    init {
        useLogLifecycle()
        useServicePresence(
            stateFlow = isRunning,
            name = UiStrings.screenshot_service,
        )
        useStopServiceReceiver()
        onCreated {
            NotificationCatalog.screenshot().startForeground()
            instance = this@ScreenshotService
        }
        onDestroyed {
            if (instance === this@ScreenshotService) instance = null
            captureSessionSlot.close()
        }
    }

    companion object {
        private var instance: ScreenshotService? = null
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)
        suspend fun screenshot(): Bitmap? {
            if (!isRunning.value) return null
            return try {
                withTimeoutOrNull(5000.milliseconds) {
                    instance?.captureSessionSlot?.get()?.capture()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtils.d("截取屏幕失败", e)
                null
            }
        }

        fun start(intent: Intent) {
            if (!appRuntimeReady.isCompleted || CrashRecovery.isRecoveryBlocked) return
            intent.component = ScreenshotService::class.componentName
            app.startForegroundService(intent)
        }

        fun stop() = IntentUtils.stopServiceByClass(ScreenshotService::class)
    }
}
