package li.gkd.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import li.gkd.app.text.UiStrings
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.platform.lifecycle.RuntimeStateSynchronizer
import li.gkd.app.util.LogUtils
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.componentName
import li.gkd.app.util.runMainPost
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.ToastUtils.toast

class ExposeService : Service() {
    override fun onBind(intent: Intent?): Binder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appScope.launchUi {
            try {
                handleIntent(intent)
            } finally {
                runMainPost(1000) { stopSelf() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    suspend fun handleIntent(intent: Intent?) {
        // 这个服务是 exported 的（第三方主动调用快照/同步就是它的功能），所以调用方身份不能信：
        // 以前任何应用都能用 `am start-foreground-service` 触发截图和全局同步。现在必须带上
        // 只有本应用（或拿到过 expose.sh 的人）才知道的 token。
        if (!ExternalCallTokens.accepts(intent?.getStringExtra(TOKEN_EXTRA))) {
            LogUtils.d("ExposeService::handleIntent 拒绝：token 不匹配", intent?.getIntExtra("expose", 0))
            toast(UiStrings.external_call_denied, forced = true)
            return
        }
        val expose = intent?.getIntExtra("expose", 0) ?: 0
        val data = intent?.getStringExtra("data")
        LogUtils.d("ExposeService::handleIntent", expose, data)
        when (expose) {
            -1 -> StatusService.autoStart()
            0 -> SnapshotCapture.capture()
            1 -> {
                toast(UiStrings.execution_success, forced = true)
                RuntimeStateSynchronizer.requestSync()
            }

            else -> {
                toast(UiStrings.external_call_unknown(expose, data), forced = true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationCatalog.expose().startForeground()
    }

    companion object {
        const val TOKEN_EXTRA = "token"

        fun initCommandFile() {
            val commandText = template
                .replace("{service}", ExposeService::class.componentName.flattenToShortString())
                .replace("{token}", ExternalCallTokens.token)
            FolderUtils.shFolder.resolve("expose.sh").writeText(commandText)
        }

        fun exposeIntent(expose: Int, data: String? = null): Intent {
            return Intent(app, ExposeService::class.java).apply {
                putExtra("expose", expose)
                putExtra(TOKEN_EXTRA, ExternalCallTokens.token)
                if (data != null) {
                    putExtra("data", data)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

/**
 * 外部调用的共享密钥。放 noBackupFilesDir 而不是 storeFolder —— storeFolder 会被「导出日志」
 * 和备份带出去，token 落到别人手里等于这道门白开。
 *
 * expose.sh 里内嵌同一个 token 是可接受的：那个文件在应用自己的 filesDir/sh 下，能读到它的
 * 已经是 shell/root 级别权限，不在这道门要防的范围内（防的是设备上其它应用）。
 */
object ExternalCallTokens {
    private val tokenFile by lazy { app.noBackupFilesDir.resolve("expose_token.txt") }

    val token: String by lazy {
        runCatching { tokenFile.readText().trim() }.getOrNull()?.takeIf { it.length >= 16 } ?: run {
            val bytes = ByteArray(12).also { SecureRandom().nextBytes(it) }
            // android.util.Base64 没有 getUrlEncoder()（那是 java.util.Base64，要 API 26），
            // 这里用 Android 自己的 flags，避免抬高 minSdk。
            val generated = Base64.encodeToString(
                bytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            tokenFile.parentFile?.mkdirs()
            tokenFile.writeText(generated)
            generated
        }
    }

    fun accepts(candidate: String?): Boolean =
        candidate != null && MessageDigest.isEqual(token.toByteArray(), candidate.toByteArray())
}

private const val template = $$"""set -euo pipefail
echo '> start expose.sh'
p=''
if [ -n "${1:-}" ]; then
  p+=" --ei expose $1"
fi
if [ -n "${2:-}" ]; then
  p+=" --es data $2"
fi
am start-foreground-service -n {service} $p --es token {token}
echo '> expose.sh end'
"""
