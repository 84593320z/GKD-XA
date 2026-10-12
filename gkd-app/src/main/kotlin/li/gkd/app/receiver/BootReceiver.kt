package li.gkd.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import li.gkd.app.platform.lifecycle.RuntimeStateSynchronizer
import li.gkd.app.service.StatusService

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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {                 // line 10
            // autoStart() 里的 needRestart 第一项就是「常驻通知开关」，关掉它就等于把开机兜底
            // 一起关掉 —— 而补开无障碍并不依赖常驻通知。广播既然已经把进程拉起来了（App.onCreate
            // 也随之跑完），这里就无条件补一次同步，起不起通知是 StatusService 自己的事。
            StatusService.autoStart()                                       // line 11
            RuntimeStateSynchronizer.requestSync(loc = "boot")
        }
    }                                                                        // line 13
}
