package li.gkd.app.platform.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.service.A11yService
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.text.UiStrings
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ToastUtils.toast

/**
 * 保活窗口的唯一持有者。
 *
 * 窗口的生命周期不再挂在任何单个 Service 上，而是由 [initialize] 按「开关 + 悬浮窗权限 + 无障碍是否已自带窗口」
 * 自动决定：无障碍服务在跑时它的 TYPE_ACCESSIBILITY_OVERLAY 就够用，应用侧的窗口让出去；否则应用侧自己挂一个
 * TYPE_APPLICATION_OVERLAY。这样常驻通知服务被杀掉、被系统回收都不会把保活窗口一起带走。
 */
object KeepAliveOverlayCoordinator {
    enum class Source {
        Application,
        Accessibility,
    }

    private val applicationOverlay = MutableStateFlow<AttachedOverlay?>(null)
    private val accessibilityOverlay = MutableStateFlow<AttachedOverlay?>(null)

    fun initialize() {
        appScope.launch {
            try {
                combine(
                    storeFlow.map { it.enableStatusService }.distinctUntilChanged(),
                    PermissionStates.drawOverlays.stateFlow,
                    combine(A11yService.isRunning, accessibilityOverlay) { a11yRunning, a11yOverlay ->
                        a11yRunning && a11yOverlay != null
                    },
                ) { enabled, granted, a11yAttached ->
                    Pair(enabled && granted, a11yAttached)
                }.distinctUntilChanged().collectLatest { (enabled, a11yAttached) ->
                    when {
                        !enabled -> release(Source.Application, app)
                        a11yAttached -> releaseAfterHandoff(Source.Application, app)
                        else -> acquire(
                            source = Source.Application,
                            owner = app,
                            context = app,
                            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        ).onFailure(LogUtils::d)
                    }
                }
            } finally {
                release(Source.Application, app)
            }
        }
    }

    /**
     * 无障碍服务连接后挂 TYPE_ACCESSIBILITY_OVERLAY。
     *
     * ColorOS 17 上偶发：onServiceConnected 里立刻 addView 会拿到尚未就绪的窗口 token，
     * 抛 "BadTokenException: token ... is not valid"（2026-10-25/26/31 日志各一次）。
     * token 通常几百毫秒内就生效，只对这种失败退避重试；其它失败（权限/窗口类型不合法）
     * 重试也没用，直接放弃并提示。acquire 幂等，重复调用不会叠窗口。
     */
    suspend fun acquireAccessibility(source: Source, service: AccessibilityService) {
        val connectedAt = SystemClock.elapsedRealtime()
        for (attempt in 0..RETRY_DELAYS.size) {
            if (attempt > 0) delay(RETRY_DELAYS[attempt - 1])
            val result = acquire(
                source = source,
                owner = service,
                context = service,
                windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            )
            val elapsed = SystemClock.elapsedRealtime() - connectedAt
            if (result.isSuccess) {
                LogUtils.d("保活窗口附加成功 attempt=${attempt + 1} elapsed=${elapsed}ms")
                return
            }
            val error = result.exceptionOrNull()
            LogUtils.d("保活窗口附加失败 attempt=${attempt + 1} elapsed=${elapsed}ms", error)
            if (error !is WindowManager.BadTokenException || attempt >= RETRY_DELAYS.size) break
        }
        toast(UiStrings.a11y_keep_alive_failed)
    }

    /**
     * 无障碍服务销毁时把手上的窗口交出去：应用侧窗口还能挂就交给应用侧，交接期间两边都不空窗。
     */
    fun releaseAccessibilityAfterHandoff(source: Source, owner: Any) {
        appScope.launch {
            val replacement = if (storeFlow.value.enableStatusService && PermissionStates.drawOverlays.value) {
                Source.Application
            } else {
                null
            }
            releaseAfterHandoff(source, owner, replacement)
        }
    }

    @Synchronized
    private fun acquire(
        source: Source,
        owner: Any,
        context: Context,
        windowType: Int,
    ): Result<Unit> {
        val current = overlayState(source).value
        if (current?.owner === owner) return Result.success(Unit)
        val overlay = runCatching { KeepAliveOverlay(context, windowType) }
            .onFailure(LogUtils::d)
            .getOrElse { return Result.failure(it) }
        current?.overlay?.close()
        overlayState(source).value = AttachedOverlay(owner, overlay)
        return Result.success(Unit)
    }

    private suspend fun releaseAfterHandoff(
        source: Source,
        owner: Any,
        replacement: Source? = null,
    ) {
        if (replacement != null) {
            withTimeoutOrNull(HANDOFF_DELAY_MILLIS) {
                overlayState(replacement).first { it != null }
            }
        }
        delay(HANDOFF_DELAY_MILLIS)
        release(source, owner)
    }

    @Synchronized
    private fun release(source: Source, owner: Any) {
        val state = overlayState(source)
        val current = state.value
        if (current?.owner !== owner) return
        state.value = null
        current.overlay.close()
    }

    private fun overlayState(source: Source): MutableStateFlow<AttachedOverlay?> = when (source) {
        Source.Application -> applicationOverlay
        Source.Accessibility -> accessibilityOverlay
    }

    private data class AttachedOverlay(
        val owner: Any,
        val overlay: KeepAliveOverlay,
    )

    private class KeepAliveOverlay(
        context: Context,
        windowType: Int,
    ) : AutoCloseable {
        private val windowManager = context.getSystemService(WindowManager::class.java)
        private val view = View(context)
        private var attached = false

        init {
            val layoutParams = WindowManager.LayoutParams().apply {
                type = windowType
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                gravity = Gravity.START or Gravity.TOP
                width = 1
                height = 1
                alpha = 0f
                packageName = context.packageName
            }
            windowManager.addView(view, layoutParams)
            attached = true
        }

        @Synchronized
        override fun close() {
            if (!attached) return
            attached = false
            runCatching { windowManager.removeView(view) }
                .onFailure(LogUtils::d)
        }
    }

    private val RETRY_DELAYS = longArrayOf(300L, 1_000L, 3_000L)
    private const val HANDOFF_DELAY_MILLIS = 1_000L
}
