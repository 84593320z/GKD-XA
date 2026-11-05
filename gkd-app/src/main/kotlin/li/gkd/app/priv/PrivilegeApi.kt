package li.gkd.app.priv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.platform.lifecycle.RuntimeStateSynchronizer
import li.gkd.app.service.ExposeService
import li.gkd.app.service.StatusService
import li.gkd.app.service.currentAppBlocked
import li.gkd.app.service.currentAppUseA11y
import li.gkd.app.service.updateTopTaskAppId
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged
import li.gkd.app.util.ToastUtils.toast
import priv.kit.core.Privilege
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.userservice.PrivilegeUserServiceSpec

val currentUserId by lazy { android.os.Process.myUserHandle().hashCode() }

val privilegeContextFlow: StateFlow<PrivilegeContext?>
    field = MutableStateFlow(null)

private val privilegeContextMutex = Mutex()

private val userServiceSpec = PrivilegeUserServiceSpec(
    serviceClassName = UserService::class.java.name,
    embedded = true,
)

private suspend fun clearPrivilegeContext(context: PrivilegeContext) {
    if (!privilegeContextFlow.compareAndSet(context, null)) return
    uiAutomationFlow.value?.shutdown(true)
    try {
        context.destroy()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogUtils.d("destroy PrivilegeContext failed", e)
    }
}

private suspend fun updatePrivilegeContext(serverInfo: PrivilegeServerInfo?) =
    withContext(Dispatchers.IO) {
        privilegeContextMutex.withLock { applyPrivilegeContext(serverInfo) }
    }

private suspend fun applyPrivilegeContext(serverInfo: PrivilegeServerInfo?) {
    // 自动恢复与 serverState 收集器可能同时进入：以 Privilege 的当前状态为准，
    // 丢弃过期的 null / 旧 serverInfo，避免把刚连上的 context 又清掉。
    if (Privilege.serverState.value != serverInfo) return
    val oldContext = privilegeContextFlow.value
    if (oldContext?.serverInfo == serverInfo) return

        if (serverInfo != null) {
            if (oldContext != null) {
                clearPrivilegeContext(oldContext)
            }

            if (!app.justStarted) {
                toast(UiStrings.privilege_service_connecting)
            }
            val userServiceConnection = Privilege.bindUserService(userServiceSpec)
            val privilegeContext = PrivilegeContext.create(serverInfo, userServiceConnection)
            privilegeContextFlow.value = privilegeContext
            privilegeContext.topCpn()?.let { cpn ->
                updateTopTaskAppId(cpn.packageName)
            }
            if (
                storeFlow.value.useAutomation &&
                !currentAppBlocked &&
                !currentAppUseA11y
            ) {
                AutomationService.tryConnect(true)
            }
            PermissionStates.refreshAll()
            // 特权通道就绪 == 「拿到 root」的时刻，这里补一次全局同步：RuntimeStateSynchronizer 先
            // grantSelf()（用特权进程给自己 pm grant WRITE_SECURE_SETTINGS）、再 refreshAll()，
            // 然后 armA11yService() 按工作模式把无障碍写进 secure settings 并校验重试
            // （fixRestartAutomatorService 那道门看的是 enableAutomator，它只有在无障碍已经跑起来之后
            // 才为 true，冷启动必然是 false，所以必须由不看那个标志位的 arm 分支来开）。
            // 缺这根线时，开机只有 App.onCreate 那一次同步，而它早于 root 就绪；之后就要等用户
            // 打开界面才补开无障碍 —— ColorOS 开机会关掉无障碍、临时 root 与 LSPosed 又普遍要
            // 软重启后才可用，正是这个时序。用事件驱动而不是轮询：Privilege.serverState 是
            // StateFlow，进程晚起或 root 先到都能立刻拿到当前状态。
            RuntimeStateSynchronizer.requestSync(loc = "privilege connected")
            if (StatusService.needRestart) {
                privilegeContext.startForegroundService(
                    ExposeService.exposeIntent(expose = -1),
                )
            }
            val delayMillis = if (app.justStarted) 1200L else 0L
            toast(UiStrings.privilege_service_connect_success, delayMillis = delayMillis)
        } else if (oldContext != null) {
            clearPrivilegeContext(oldContext)
            PermissionStates.refreshAll()
            toast(UiStrings.privilege_service_disconnected)
        }
}

fun initPrivilege() {
    appScope.launchLogged(Dispatchers.IO) {
        var wasConnected = false
        Privilege.serverState.collect { serverInfo ->
            val disconnected = wasConnected && serverInfo == null
            wasConnected = serverInfo != null
            LogUtils.d("Privilege.serverState", serverInfo)
            if (serverInfo != null) clearPrivilegeRecoveryCooldown()
            try {
                updatePrivilegeContext(serverInfo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                LogUtils.d("update PrivilegeContext failed", e)
                toast(UiStrings.privilege_service_state_update_failed(e.message))
            }
            if (disconnected) requestPrivilegeRecovery()
        }
    }
}
