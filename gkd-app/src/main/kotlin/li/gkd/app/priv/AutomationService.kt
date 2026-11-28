package li.gkd.app.priv

import android.annotation.SuppressLint
import android.app.UiAutomation
import android.app.UiAutomationHidden
import android.graphics.Bitmap
import android.os.HandlerThread
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.a11y.A11yCommonImpl
import li.gkd.app.a11y.A11yRuleEngine
import li.gkd.app.a11y.A11yRuntime
import li.gkd.app.service.currentAppUseA11y
import li.gkd.app.store.AppStore.updateEnableAutomator
import li.gkd.app.util.AndroidTarget
import li.gkd.app.util.AutomatorModeOption
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ToastUtils.toast

class AutomationService private constructor(
    private val privilegeContext: PrivilegeContext,
) : A11yCommonImpl {
    override val mode get() = AutomatorModeOption.AutomationMode
    private val handlerThread = HandlerThread("UiAutomatorHandlerThread")
    private val uiAutomationDelegate = lazy {
        UiAutomationHidden(
            handlerThread.looper,
            ProxyUiAutomationConnection(privilegeContext),
        ).toPublic
    }
    private val uiAutomation by uiAutomationDelegate

    override val scope = MainScope()

    override val ruleEngine by lazy { A11yRuleEngine(this) }

    private val listener = UiAutomation.OnAccessibilityEventListener {
        ruleEngine.onA11yEvent(it)
    }

    override suspend fun screenshot(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            privilegeContextFlow.value?.screenshot()
        } catch (e: Throwable) {
            LogUtils.d("takeScreenshot failed", e)
            null
        }
    }

    override val windowNodeInfo: AccessibilityNodeInfo? get() = uiAutomation.rootInActiveWindow
    override val windowInfos: List<AccessibilityWindowInfo> get() = uiAutomation.windows
    private val startTime = android.os.SystemClock.elapsedRealtime()
    override var justStarted: Boolean = true
        get() {
            if (field) {
                field = android.os.SystemClock.elapsedRealtime() - startTime < 3_000
            }
            return field
        }

    @Volatile
    private var connected = false

    internal val isAvailable get() = connected && !shutdown.value &&
        privilegeContextFlow.value === privilegeContext && privilegeContext.serverLifecycleBinder.isBinderAlive

    // https://github.com/android-cs/16/blob/main/cmds/uiautomator/library/testrunner-src/com/android/uiautomator/core/UiAutomationShellWrapper.java#L25
    private fun connect() {
        handlerThread.start()
        uiAutomation.toHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        uiAutomation.setOnAccessibilityEventListener(listener)
        connected = true
        // An absent root is normal with the screen off. Probe the registered accessibility client instead.
        checkNotNull(uiAutomation.serviceInfo) { "UiAutomation service is not registered" }
        check(privilegeContext.serverLifecycleBinder.pingBinder()) { "Privilege service is not alive" }
    }

    private fun disconnect() {
        scope.cancel()
        handlerThread.quit()
        if (!uiAutomationDelegate.isInitialized()) return
        val wasConnected = connected
        try {
            if (wasConnected) {
                uiAutomation.setOnAccessibilityEventListener(null)
            }
            uiAutomation.toHidden.disconnect()
        } catch (e: Exception) {
            LogUtils.d("disconnect automation failed", e)
        } finally {
            uiAutomation.quitRemoteCallbackThread()
            connected = false
            if (announced) {
                if (tempShutdownFlag) {
                    toast(UiStrings.automation_partially_disabled)
                } else {
                    toast(UiStrings.automation_stopped)
                    updateEnableAutomator(false)
                }
            }
        }
    }

    private var tempShutdownFlag = false
    private var announced = false
    private val shutdown = atomic(false)
    override fun shutdown(temp: Boolean) {
        synchronized(this) {
            if (!shutdown.compareAndSet(expect = false, update = true)) return
            tempShutdownFlag = temp
        }
        try {
            A11yRuntime.onA11yDisconnected(this, allowRollback = temp)
        } finally {
            try {
                disconnect()
            } finally {
                uiAutomationFlow.compareAndSet(this, null)
            }
        }
    }

    companion object {
        private val connectLock = Any()

        fun isOtherUiAutomationRunning(): Boolean {
            if (uiAutomationFlow.value != null) return false
            return privilegeContextFlow.value?.isUiAutomationRunning() == true
        }

        fun showOccupiedWarning(silent: Boolean = false) {
            toast(UiStrings.automation_service_occupied)
            if (!silent) {
                uiAutomationOccupiedFlow.value = true
            }
        }

        fun dismissOccupiedWarning() {
            uiAutomationOccupiedFlow.value = false
        }

        fun tryConnect(silent: Boolean = false) {
            if (CrashRecovery.isRecoveryBlocked) {
                if (!silent) toast("自动运行已因连续崩溃暂停，请在主界面手动恢复")
                return
            }
            synchronized(connectLock) {
                uiAutomationOccupiedFlow.value = false
                if (currentAppUseA11y) return@synchronized
                uiAutomationFlow.value?.let { existing ->
                    if (existing.isAvailable) A11yRuntime.onA11yConnected(existing)
                    return@synchronized
                }
                val privilegeContext = privilegeContextFlow.value ?: return@synchronized
                try {
                    if (isOtherUiAutomationRunning()) {
                        showOccupiedWarning(silent)
                        return@synchronized
                    }
                } catch (e: Exception) {
                    toast(UiStrings.automation_state_check_failed(e.message))
                    LogUtils.d("detect automation state failed", e)
                    return@synchronized
                }
                val instance = AutomationService(privilegeContext)
                try {
                    instance.connect()
                    val promoted = synchronized(instance) {
                        if (!instance.isAvailable || currentAppUseA11y || CrashRecovery.isRecoveryBlocked ||
                            !uiAutomationFlow.compareAndSet(expect = null, update = instance) ||
                            !A11yRuntime.onA11yConnected(instance)
                        ) {
                            false
                        } else {
                            updateEnableAutomator(true)
                            instance.announced = true
                            toast(UiStrings.automation_started)
                            true
                        }
                    }
                    if (!promoted) instance.shutdown(true)
                } catch (e: Exception) {
                    instance.shutdown(true)
                    toast(UiStrings.automation_start_failed(e.message))
                    LogUtils.d(e)
                }
            }
        }
    }
}

val uiAutomationFlow: StateFlow<AutomationService?>
    field = MutableStateFlow(null)
val uiAutomationOccupiedFlow: StateFlow<Boolean>
    field = MutableStateFlow(false)

private val remoteCallbackThreadField by lazy {
    if (AndroidTarget.P) {
        // UiAutomation 在 CONNECTING 阶段同步失败时，disconnect() 会在进入自身的
        // finally 前直接抛错，因此需要通过内部字段兜底关闭其回调线程。
        @SuppressLint("SoonBlockedPrivateApi")
        UiAutomation::class.java.getDeclaredField("mRemoteCallbackThread").apply {
            isAccessible = true
        }
    } else {
        null
    }
}

private fun UiAutomation.quitRemoteCallbackThread() {
    try {
        remoteCallbackThreadField?.let { field ->
            (field.get(this) as? HandlerThread)?.quit()
            field.set(this, null)
        }
    } catch (e: Exception) {
        LogUtils.d("quit UiAutomation callback thread failed", e)
    }
}
