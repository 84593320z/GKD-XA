package li.gkd.app.priv

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.app.appScope
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged
import priv.kit.core.Privilege
import priv.kit.ui.PrivilegeUi

private const val RECOVERY_COOLDOWN_MILLIS = 10 * 60 * 1000L
private val recoveryMutex = Mutex()
private var recoveryAllowedAt = 0L

/**
 * 特权服务断开后（Shizuku 重启 / 服务被杀 / 用户切回前台）自动重连。
 * 只在用户仍想要特权（[PrivilegeUi.desiredEnabled]）且不在冷却期时尝试；
 * 连续失败进 10 分钟冷却，避免把 Shizuku 的拉起弹窗刷屏。
 * 例外：Shizuku binder 刚到达且上次正是用 Shizuku 方式启动的（上游 89fc47a 的
 * 精确触发），说明 Shizuku 刚活过来、用户意图明确，此时 ignoreCooldown 绕过冷却。
 * 上游同时把通用冷却砍到 3 秒，XA 不跟：保留 10 分钟防刷屏，只取精确触发这一半。
 */
fun requestPrivilegeRecovery() {
    appScope.launchLogged(Dispatchers.IO) { recoverPrivilege() }
}

suspend fun recoverPrivilege(ignoreCooldown: Boolean = false) = recoveryMutex.withLock {
    Privilege.serverState.value?.let { return@withLock }
    val now = SystemClock.elapsedRealtime()
    if (!PrivilegeUi.desiredEnabled.value) return@withLock
    if (!ignoreCooldown && now < recoveryAllowedAt) return@withLock
    LogUtils.d("requestPrivilegeRecovery: 尝试重新连接特权服务")
    val serverInfo = PrivilegeUi.startSilently(gkdPrivilegeUiConfig)
    if (serverInfo == null && PrivilegeUi.desiredEnabled.value) {
        recoveryAllowedAt = now + RECOVERY_COOLDOWN_MILLIS
        LogUtils.d("requestPrivilegeRecovery: 失败，${RECOVERY_COOLDOWN_MILLIS}ms 内不再尝试")
    }
}

suspend fun clearPrivilegeRecoveryCooldown() {
    recoveryMutex.withLock { recoveryAllowedAt = 0L }
}
