package li.gkd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import li.gkd.app.platform.lifecycle.RuntimeStateSynchronizer
import li.gkd.app.priv.CrashRecovery

/**
 * 恢复自本地构建 APK（仓库 main 里没有 receiver 这个包）。
 * debug 包行号：类声明 8，onReceive 10-13。
 * manifest 侧需要一并补回（本地包有、CI 包没有）：
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 *   <receiver android:name="li.gkd.app.receiver.BootReceiver" android:exported="false">
 *       <intent-filter><action android:name="android.intent.action.BOOT_COMPLETED" /></intent-filter>
 *   </receiver>
 */
class BootReceiver : BroadcastReceiver() {                                   // line 8
    override fun onReceive(context: Context, intent: Intent) {               // line 10
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && !CrashRecovery.isRecoveryBlocked) {
            // Application.onCreate now only begins migration. The conflated synchronizer
            // waits asynchronously; never read settings or hold this broadcast for file copies.
            RuntimeStateSynchronizer.requestSync(loc = "boot")
        }
    }                                                                        // line 13
}
