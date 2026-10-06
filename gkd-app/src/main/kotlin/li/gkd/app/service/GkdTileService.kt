package li.gkd.app.service

import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.a11y.systemRecentCn
import li.gkd.app.a11y.currentTopActivity
import li.gkd.app.ui.app.showAccessRestrictedSettingsDialog
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.platform.lifecycle.MainActivityVisibility
import li.gkd.app.priv.AutomationService
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.store.AppStore.actualA11yScopeAppList
import li.gkd.app.store.AppStore.actualBlockA11yAppList
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.LogUtils
import li.gkd.app.util.mapState
import li.gkd.app.util.runMainPost
import li.gkd.app.util.ToastUtils.toast
import li.songe.codeorigin.CallSite
import kotlin.time.Duration.Companion.milliseconds

class GkdTileService : BaseTileService() {
    override val activeFlow = combine(A11yService.isRunning, uiAutomationFlow) { a11y, automator ->
        a11y || automator != null
    }

    override fun onTileClick() = switchAutomatorService()
}

private val modifyA11yMutex = Mutex()
private const val A11Y_AWAIT_START_TIME = 2000L
private const val A11Y_AWAIT_FIX_TIME = 1000L

private fun modifyA11yRun(
    @CallSite loc: String = "",
    block: suspend () -> Unit,
) {
    appScope.launchUi(Dispatchers.IO, loc = loc) {
        if (!modifyA11yMutex.tryLock()) return@launchUi
        try {
            block()
        } finally {
            modifyA11yMutex.unlock()
        }
    }
}

/**
 * 把无障碍总开关与我们的服务写进 secure settings，并等待系统真正把服务拉起来。
 * 调用方必须持有 modifyA11yMutex。
 */
private suspend fun writeA11yServiceEnabled(): Boolean {
    val names = app.getSecureA11yServices()
    app.putSecureInt(Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    if (names.contains(A11yService.a11yCn)) { // 当前无障碍异常, 重启服务
        names.remove(A11yService.a11yCn)
        app.putSecureA11yServices(names)
        delay(A11Y_AWAIT_FIX_TIME.milliseconds)
    }
    names.add(A11yService.a11yCn)
    app.putSecureA11yServices(names)
    delay(A11Y_AWAIT_START_TIME.milliseconds)
    // https://github.com/orgs/gkd-kit/discussions/799
    return A11yService.isRunning.value
}

private suspend fun switchA11yService() {
    if (A11yService.isRunning.value) {
        // 用户在开关上主动关掉：一段时间内不再自动 arm，否则开机/回到前台会和他对着干
        a11yUserDisabledAt = System.currentTimeMillis()
        A11yService.instance?.disableSelf()
    } else {
        a11yUserDisabledAt = 0L
        if (!PermissionStates.writeSecureSettings.updateAndGet()) {
            if (!PermissionStates.writeSecureSettings.value) {
                toast(UiStrings.secure_settings_permission_required)
                return
            }
        }
        if (!writeA11yServiceEnabled()) {
            toast(UiStrings.a11y_enable_failed)
            showAccessRestrictedSettingsDialog()
            return
        }
    }
}

private fun switchAutomationService() {
    val newEnabled = uiAutomationFlow.value == null
    uiAutomationFlow.value?.shutdown()
    if (newEnabled && privilegeContextFlow.value != null) {
        AutomationService.tryConnect()
    }
}

fun switchAutomatorService(@CallSite loc: String = "") = modifyA11yRun(loc = loc) {
    if (currentAppUseA11y) {
        switchA11yService()
    } else {
        switchAutomationService()
    }
}

private fun skipBlockApp(): Boolean {
    if (storeFlow.value.enableBlockA11yAppList) {
        val topAppId = if (MainActivityVisibility.isVisible || app.justStarted) {
            META.appId
        } else {
            privilegeContextFlow.value?.run { topCpn()?.packageName }
        }
        if (topAppId != null && topAppId in actualBlockA11yAppList) {
            return true
        }
    }
    return false
}

private suspend fun fixA11yService() {
    if (!A11yService.isRunning.value && PermissionStates.writeSecureSettings.updateAndGet()) {
        if (skipBlockApp()) return
        val names = app.getSecureA11yServices()
        val a11yBroken = names.contains(A11yService.a11yCn)
        if (a11yBroken) {
            // 无障碍出现故障, 重启服务
            names.remove(A11yService.a11yCn)
            app.putSecureA11yServices(names)
            // 必须等待一段时间, 否则概率不会触发系统重启无障碍
            delay(A11Y_AWAIT_FIX_TIME.milliseconds)
            if (!currentAppUseA11y) return
        }
        names.add(A11yService.a11yCn)
        app.putSecureA11yServices(names)
        delay(A11Y_AWAIT_START_TIME.milliseconds)
        if (currentAppUseA11y && !A11yService.isRunning.value) {
            toast(UiStrings.a11y_restart_failed)
            showAccessRestrictedSettingsDialog()
        }
    }
}

private fun fixAutomationService() {
    if (uiAutomationFlow.value == null && privilegeContextFlow.value != null) {
        if (skipBlockApp()) return
        if (currentAppUseA11y) return
        AutomationService.tryConnect(true)
    }
}

fun fixRestartAutomatorService(@CallSite loc: String = "") = modifyA11yRun(loc = loc) {
    if (storeFlow.value.enableAutomator) {
        if (currentAppUseA11y) {
            fixA11yService()
        } else {
            fixAutomationService()
        }
    }
}

@Volatile
private var a11yUserDisabledAt = 0L

private const val A11Y_USER_DISABLED_SUPPRESS_MILLIS = 30 * 60 * 1000L

/** 第 1 次立即执行，之后按这里的间隔重试（对付开机时系统把刚拉起的无障碍又解绑的那几秒）。 */
private val A11Y_ARM_RETRY_MILLIS = longArrayOf(2_000L, 5_000L, 10_000L)

private val armA11yDriverMutex = Mutex()

private enum class ArmA11yOutcome {
    Running,
    WriteAgain,
    Stop,
}

private suspend fun armA11yOnce(loc: String, attempt: Int, total: Int): ArmA11yOutcome {
    if (A11yService.isRunning.value) {
        return ArmA11yOutcome.Running
    }
    if (!currentAppUseA11y) {
        LogUtils.d("$loc 工作模式不是无障碍，不自动开启", loc = "armA11yService")
        return ArmA11yOutcome.Stop
    }
    if (skipBlockApp()) {
        LogUtils.d("$loc 前台在禁用无障碍列表里，不自动开启", loc = "armA11yService")
        return ArmA11yOutcome.Stop
    }
    val disabledAt = a11yUserDisabledAt
    if (disabledAt != 0L) {
        val remaining = A11Y_USER_DISABLED_SUPPRESS_MILLIS - (System.currentTimeMillis() - disabledAt)
        if (remaining > 0) {
            LogUtils.d("$loc 用户刚主动关闭无障碍，${remaining}ms 内不自动开启", loc = "armA11yService")
            return ArmA11yOutcome.Stop
        }
        a11yUserDisabledAt = 0L
    }
    if (!PermissionStates.writeSecureSettings.updateAndGet()) {
        LogUtils.d("$loc 没有 WRITE_SECURE_SETTINGS（root/特权未就绪），不自动开启", loc = "armA11yService")
        return ArmA11yOutcome.Stop
    }
    var wrote = false
    val running = modifyA11yMutex.withLock {
        if (A11yService.isRunning.value) {
            true
        } else {
            wrote = true
            writeA11yServiceEnabled()
        }
    }
    if (wrote) {
        LogUtils.d(
            "$loc 自动开启无障碍第 $attempt/$total 次，写入后无障碍${if (running) "已运行" else "未运行"}",
            loc = "armA11yService",
        )
    }
    return if (running) ArmA11yOutcome.Running else ArmA11yOutcome.WriteAgain
}

/**
 * 冷启动补开无障碍。和 fixRestartAutomatorService 的区别是不看 storeFlow.enableAutomator：
 * 那个标志位只有无障碍已经 onCreate（或 uiAutomation 连上）之后才为 true，开机时必然是 false，
 * 用它当门等于「要等无障碍跑起来才会去跑起来」，冷启动永远进不去（自动化模式不受影响，
 * 因为 AutomationService.tryConnect 在 updatePrivilegeContext 里是无条件走的）。
 * 这里用 automatorMode 作为用户意图，并在写入后有限次校验重试；每个分支都打日志，
 * 方便下次直接从导出日志看出是哪一条门拦住了。
 */
fun armA11yService(@CallSite loc: String = "") {
    appScope.launchUi(Dispatchers.IO, loc = loc) {
        if (!armA11yDriverMutex.tryLock()) {
            LogUtils.d("$loc 已有自动开启任务在跑，跳过", loc = "armA11yService")
            return@launchUi
        }
        try {
            val total = A11Y_ARM_RETRY_MILLIS.size + 1
            var outcome = ArmA11yOutcome.Stop
            for (attempt in 1..total) {
                if (attempt > 1) {
                    delay(A11Y_ARM_RETRY_MILLIS[attempt - 2].milliseconds)
                }
                outcome = armA11yOnce(loc, attempt, total)
                if (outcome != ArmA11yOutcome.WriteAgain) break
            }
            if (outcome == ArmA11yOutcome.WriteAgain) {
                LogUtils.d("$loc 自动开启无障碍失败：写入 $total 次后无障碍仍未运行", loc = "armA11yService")
            }
        } finally {
            armA11yDriverMutex.unlock()
        }
    }
}

val currentAppUseA11y
    get() = storeFlow.value.useA11y || topAppIdFlow.value in actualA11yScopeAppList

val currentAppBlocked
    get() = storeFlow.value.enableBlockA11yAppList && topAppIdFlow.value in actualBlockA11yAppList

private fun innerForcedUpdateA11yService(disabled: Boolean) {
    if (!storeFlow.value.enableAutomator) {
        return
    }
    if (disabled) {
        A11yService.instance?.shutdown(true)
        uiAutomationFlow.value?.shutdown(true)
        return
    }
    if (currentAppUseA11y) {
        if (A11yService.isRunning.value) {
            return
        }
        if (!PermissionStates.writeSecureSettings.stateFlow.value) {
            return
        }
        val names = app.getSecureA11yServices()
        names.add(A11yService.a11yCn)
        app.putSecureA11yServices(names)
    } else {
        AutomationService.tryConnect(true)
    }
}

private fun forcedUpdateA11yService(
    disabled: Boolean,
    @CallSite loc: String = "",
) = modifyA11yRun(loc = loc) {
    innerForcedUpdateA11yService(disabled)
}

const val A11Y_WHITE_APP_AWAIT_TIME = 3000L

@Volatile
private var lastAppIdChangeTime = 0L
val topAppIdFlow: StateFlow<String>
    field = MutableStateFlow("")
val a11yPartDisabledFlow by lazy {
    topAppIdFlow.mapState(appScope) {
        actualBlockA11yAppList.contains(it)
    }
}

fun updateTopTaskAppId(value: String) {
    if (storeFlow.value.enableBlockA11yAppList || actualA11yScopeAppList.isNotEmpty()) {
        topAppIdFlow.value = value
    }
}

fun initA11yWhiteAppList() {
    val actualFlow = topAppIdFlow.drop(1)
    appScope.launch(Dispatchers.Main) {
        actualFlow.collect {
            lastAppIdChangeTime = System.currentTimeMillis()
            if (!currentAppBlocked) {
                if (currentTopActivity.sameAs(systemRecentCn) && currentAppUseA11y) {
                    // 切换无障碍会造成卡顿，在最近任务界面时，延迟这个卡顿
                    val tempTime = lastAppIdChangeTime
                    runMainPost(A11Y_WHITE_APP_AWAIT_TIME) {
                        if (tempTime == lastAppIdChangeTime) {
                            forcedUpdateA11yService(false)
                        }
                    }
                } else {
                    // 切换自动化不会卡顿，直接启动
                    forcedUpdateA11yService(false)
                }
            }
        }
    }
    appScope.launch(Dispatchers.Main) {
        actualFlow.debounce(A11Y_WHITE_APP_AWAIT_TIME.milliseconds).collect {
            if (currentAppBlocked) {
                forcedUpdateA11yService(true)
            }
        }
    }
}
