package li.gkd.app.priv

import li.gkd.app.util.LogUtils
import priv.kit.core.Privilege
import priv.kit.core.PrivilegeConfig

object PrivilegeOwnerLifecycle {
    private const val FOLLOW_DEATH_DELAY_MILLIS = 10 * 60 * 1000L
    private const val APP_RESTART_PASSIVE_TIMEOUT_MILLIS = 5_000L
    private var automatorEnabled = false
    private var updateInstallationActive = false

    @Synchronized
    fun configure(enableAutomator: Boolean) {
        automatorEnabled = enableAutomator
        applyConfiguration()
    }

    /** 更新安装期间置位（对齐上游 8225f46a）：装完只能由安装成功回调拉起应用，禁止 owner 死亡重连抢跑。 */
    @Synchronized
    fun setUpdateInstallationActive(active: Boolean) {
        updateInstallationActive = active
        applyConfiguration()
    }

    private fun applyConfiguration() {
        PrivilegeConfig.configure(
            followDeathDelayMillis =
                if (automatorEnabled || updateInstallationActive) FOLLOW_DEATH_DELAY_MILLIS else 0L,
            activeReconnectOnOwnerDeath = automatorEnabled && !updateInstallationActive,
        )
    }

    fun prepareAppRestart() {
        runCatching {
            Privilege.prepareOwnerRestart(APP_RESTART_PASSIVE_TIMEOUT_MILLIS)
        }.onFailure { error ->
            LogUtils.d("prepare owner restart failed", error)
        }
    }
}
