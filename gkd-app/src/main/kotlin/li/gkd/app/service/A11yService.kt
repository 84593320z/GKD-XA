package li.gkd.app.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import li.gkd.app.app
import li.gkd.app.appRuntimeReady
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.text.UiStrings
import li.gkd.app.a11y.A11yCommonImpl
import li.gkd.app.a11y.A11yRuleEngine
import li.gkd.app.a11y.A11yRuntime
import li.gkd.app.a11y.A11yState
import li.gkd.app.a11y.currentTopActivity
import li.gkd.app.a11y.updateTopActivity
import li.gkd.app.platform.lifecycle.LifecycleHooks
import li.gkd.app.platform.overlay.KeepAliveOverlayCoordinator
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.store.AppStore.updateEnableAutomator
import li.gkd.app.util.AndroidTarget
import li.gkd.app.util.AutomatorModeOption
import li.gkd.app.util.LogUtils
import li.gkd.app.util.componentName
import li.gkd.app.util.runMainPost
import li.gkd.app.util.ToastUtils.toast
import kotlin.coroutines.resume

@SuppressLint("AccessibilityPolicy")
abstract class A11yService : AccessibilityService(), A11yCommonImpl {
    private val lifecycleHooks = LifecycleHooks()
    override val scope = MainScope()
    override val mode get() = AutomatorModeOption.A11yMode
    override val windowNodeInfo: AccessibilityNodeInfo? get() = rootInActiveWindow
    override val windowInfos: List<AccessibilityWindowInfo> get() = windows
    override suspend fun screenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        if (AndroidTarget.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                application.mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) {
                            cont.resume(null)
                        }
                    }

                    override fun onSuccess(screenshot: ScreenshotResult) {
                        try {
                            if (cont.isActive) {
                                cont.resume(
                                    Bitmap.wrapHardwareBuffer(
                                        screenshot.hardwareBuffer, screenshot.colorSpace
                                    )
                                )
                            }
                        } finally {
                            screenshot.hardwareBuffer.close()
                        }
                    }
                }
            )
        } else {
            cont.resume(null)
        }
    }

    override val ruleEngine by lazy { A11yRuleEngine(this) }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAvailable || CrashRecovery.isRecoveryBlocked) return
        ruleEngine.onA11yEvent(event)
    }

    private val startTime = SystemClock.elapsedRealtime()
    override var justStarted: Boolean = true
        get() {
            if (field) {
                field = SystemClock.elapsedRealtime() - startTime < 3_000
            }
            return field
        }

    private var tempShutdownFlag = false
    private var announced = false
    private var belongedToLatestOnDestroy = false
    @Volatile
    private var stopping = false
    @Volatile
    private var destroyed = false
    @Volatile
    private var connected = false
    @Volatile
    private var startRequest: AccessibilityStartIntent.Request? = null
    internal val isAvailable get() = connected && !stopping && !destroyed &&
        startRequest?.let(startIntent::permits) == true

    override fun shutdown(temp: Boolean) = runMainPost { stopOnMain(temp) }

    private fun stopOnMain(temp: Boolean, closeRequest: AccessibilityStartIntent.Request? = null) {
        val stop = synchronized(startIntent) {
            if (startIntent.created !== this || stopping || destroyed ||
                (closeRequest != null && !startIntent.isCurrent(closeRequest))
            ) false else {
                stopping = true
                tempShutdownFlag = temp
                true
            }
        }
        if (!stop) return
        keepAliveOverlayJob?.cancel()
        try {
            A11yRuntime.onA11yDisconnected(this, allowRollback = temp)
        } finally {
            disableSelf()
            synchronized(startIntent) {
                if (startIntent.created === this && !startIntent.requestedEnabled) {
                    startIntent.observeSystemDisable(readSystemEnabled())
                }
            }
        }
    }

    private fun readSystemEnabled(): Boolean? = try {
        app.getSecureA11yServices().contains(a11yCn)
    } catch (error: RuntimeException) {
        LogUtils.d("无法读取无障碍服务开关", error)
        null
    }

    private var systemConnected = false
    private var runtimeInitialized = false

    val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    init {
        lifecycleHooks.useLogLifecycle(this)
        lifecycleHooks.onCreated {
            if (!currentAppUseA11y) {
                toast(UiStrings.a11y_automation_mode_notice, forced = true)
                scope.launch {
                    delay(1)
                    if (!currentAppUseA11y) shutdown(true)
                }
            }
            StatusService.autoStart()
            scope.launch {
                delay(3000)
                if (!(destroyed || stopping || connected) && startRequest?.let(startIntent::permits) == true) {
                    toast(UiStrings.a11y_start_timeout, forced = true)
                }
            }
        }
        lifecycleHooks.onDestroyed {
            releaseKeepAliveOverlayAfterHandoff()
            if (!announced || !belongedToLatestOnDestroy) return@onDestroyed
            if (tempShutdownFlag) {
                toast(UiStrings.a11y_partially_disabled)
            } else {
                toast(UiStrings.a11y_stopped)
                if (A11yRuntime.service == null) updateEnableAutomator(false)
            }
            A11yState.withTopActivityLock {
                privilegeContextFlow.value?.run {
                    topCpn()?.let { cpn ->
                        // com.android.systemui
                        if (!currentTopActivity.sameAs(cpn.packageName, cpn.className)) {
                            updateTopActivity(cpn.packageName, cpn.className)
                        }
                    }
                }
            }
        }
    }

    private var keepAliveOverlayJob: Job? = null

    private fun attachKeepAliveOverlay() {
        keepAliveOverlayJob?.cancel()
        // 这里刻意用默认的 CoroutineStart：UNDISPATCHED 会把第一次 addView 塞回
        // onServiceConnected 回调里同步执行，正是 ColorOS 17 上抛 "BadTokenException: token ... is not valid"
        // 的场景（2026-10-25/26/31 日志各一次）。让协程等当前回调结束再跑，第一次尝试
        // 通常就能拿到已生效的 token，重试退避只作为兜底。
        keepAliveOverlayJob = scope.launch {
            KeepAliveOverlayCoordinator.acquireAccessibility(
                source = KeepAliveOverlayCoordinator.Source.Accessibility,
                service = this@A11yService,
            )
        }
    }

    private fun releaseKeepAliveOverlayAfterHandoff() {
        KeepAliveOverlayCoordinator.releaseAccessibilityAfterHandoff(
            source = KeepAliveOverlayCoordinator.Source.Accessibility,
            owner = this@A11yService,
        )
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(startIntent) {
            startRequest = startIntent.onCreated(this, readSystemEnabled())
            isRunning.value = true
        }
        scope.launch {
            appRuntimeReady.await()
            if (destroyed || stopping) return@launch
            if (CrashRecovery.isRecoveryBlocked || !startIntent.requestedEnabled) {
                shutdown(temp = CrashRecovery.isRecoveryBlocked)
                return@launch
            }
            runtimeInitialized = true
            lifecycleHooks.dispatchCreated()
            connectRuntimeIfReady()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        systemConnected = true
        connectRuntimeIfReady()
    }

    private fun connectRuntimeIfReady() {
        if (!runtimeInitialized || !systemConnected || connected || destroyed || stopping) return
        if (CrashRecovery.isRecoveryBlocked || !startIntent.requestedEnabled) {
            shutdown(temp = CrashRecovery.isRecoveryBlocked)
            return
        }
        synchronized(startIntent) {
            if (!startIntent.canConnect(this, startRequest)) return
            connected = true
            instance = this
        }
        if (!currentAppUseA11y) return
        if (!A11yRuntime.onA11yConnected(this)) return
        synchronized(startIntent) {
            if (!isAvailable) return
            announced = true
            updateEnableAutomator(true)
        }
        LogUtils.d("onA11yConnected -> ${this::class.simpleName}")
        attachKeepAliveOverlay()
        toast(UiStrings.a11y_started)
    }

    override fun onDestroy() {
        destroyed = true
        scope.cancel()
        synchronized(startIntent) {
            belongedToLatestOnDestroy = startIntent.onDestroyed(this)
            if (instance === this) instance = null
            if (belongedToLatestOnDestroy) {
                if (!startIntent.requestedEnabled) startIntent.observeSystemDisable(readSystemEnabled())
                isRunning.value = false
            }
        }
        A11yRuntime.onA11yDisconnected(
            this, startIntent.allowRollback(startRequest, stopping, tempShutdownFlag)
        )
        if (runtimeInitialized) {
            lifecycleHooks.dispatchDestroyed()
            runtimeInitialized = false
        }
        super.onDestroy()
    }

    companion object {
        private val startIntent = AccessibilityStartIntent<A11yService>()

        internal fun toggleStartup(onToggled: (AccessibilityStartIntent.Request) -> Unit = {}): AccessibilityStartIntent.Request =
            synchronized(startIntent) { startIntent.toggle().also(onToggled) }

        internal fun beginStartup(allowAfterUserStop: () -> Boolean = { false }): AccessibilityStartIntent.Request? =
            synchronized(startIntent) { startIntent.beginStart(allowAfterUserStop()) }

        internal fun finishStartup(request: AccessibilityStartIntent.Request) =
            synchronized(startIntent) { startIntent.finish(request) }

        internal fun recordSystemSetting(enabled: Boolean) =
            synchronized(startIntent) { startIntent.recordSettingWrite(enabled) }

        internal fun isStartupCurrent(request: AccessibilityStartIntent.Request): Boolean =
            startIntent.isCurrent(request)

        internal fun <T> withStartup(request: AccessibilityStartIntent.Request, block: () -> T): T? =
            synchronized(startIntent) { if (startIntent.isCurrent(request)) block() else null }

        internal fun closeStartup(request: AccessibilityStartIntent.Request) = runMainPost {
            val created = withStartup(request) { startIntent.created }
            created?.stopOnMain(temp = false, closeRequest = request)
            withStartup(request) { updateEnableAutomator(false) }
        }

        val a11yCn by lazy { SelectToSpeakService::class.componentName }
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)

        @Volatile
        var instance: A11yService? = null
            private set

        /**
         * isRunning 只代表系统把服务 onCreate 起来了，instance 要到 onServiceConnected 才赋值。
         * 自动修复如果只认 isRunning，「创建了但一直连不上」会被判定成运行正常，
         * 于是永远不再重试、用户在开关上也关不掉它。需要「确认可用」的地方一律用这个。
         */
        val isReady get() = instance?.isAvailable == true
    }
}
