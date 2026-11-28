package li.gkd.app.a11y

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.data.ActionPerformer
import li.gkd.app.data.ActionResult
import li.gkd.app.data.GkdAction
import li.gkd.app.data.RpcError
import li.gkd.app.priv.AutomationService
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.service.A11yService
import li.gkd.app.service.AutomatorServiceHandoff
import li.gkd.app.service.currentAppBlocked
import li.gkd.app.service.currentAppUseA11y
import li.gkd.app.util.AutomatorModeOption
import li.gkd.app.util.runMainPost
import li.gkd.selector.MatchOptions
import li.gkd.selector.Selector
import li.gkd.selector.SelectorCompileResult
import li.gkd.selector.SelectorTypeResult

object A11yRuntime {
    private val handoff = AutomatorServiceHandoff<A11yCommonImpl>()

    val service: A11yCommonImpl?
        get() = handoff.current?.service?.takeIf(::isLive)

    internal fun generation(service: A11yCommonImpl): Any? =
        handoff.current?.takeIf { it.service === service && isLive(service) }

    fun isEffective(service: A11yCommonImpl): Boolean = generation(service) != null

    private fun otherService(service: A11yCommonImpl): A11yCommonImpl? = when (service.mode) {
        AutomatorModeOption.A11yMode -> uiAutomationFlow.value
        AutomatorModeOption.AutomationMode -> A11yService.instance
    }

    private fun isLive(service: A11yCommonImpl): Boolean = when (service) {
        is AutomationService -> uiAutomationFlow.value === service && service.isAvailable
        is A11yService -> A11yService.instance === service && service.isAvailable
        else -> false
    }

    fun hasOtherService(service: A11yCommonImpl): Boolean = otherService(service)?.let(::isLive) == true

    internal fun onA11yConnected(service: A11yCommonImpl): Boolean {
        // Construct the engine before promotion so an initialization failure leaves the old service alone.
        val engine = service.ruleEngine
        val lease = handoff.accept(service, otherService(service)?.takeIf(::isLive)) {
            !CrashRecovery.isRecoveryBlocked && isLive(service) && prefers(service)
        } ?: return isEffective(service)
        engine.onA11yConnected()
        runMainPost(1000L) {
            if (handoff.current !== lease) return@runMainPost
            if (!isLive(service)) {
                service.shutdown(true)
            } else {
                handoff.retire(lease, { otherService(service) }) {
                    isLive(service) && prefers(service) && !CrashRecovery.isRecoveryBlocked
                }?.shutdown(true)
            }
        }
        return true
    }

    private fun prefers(service: A11yCommonImpl): Boolean =
        currentAppUseA11y == (service.mode == AutomatorModeOption.A11yMode)

    internal fun onA11yDisconnected(service: A11yCommonImpl, allowRollback: Boolean) {
        val restored = handoff.disconnect(service) {
            // A failed candidate does not invalidate the last working service, even if the
            // candidate is still preferred. A blocked app or crash recovery must not restore it.
            allowRollback && isLive(it) && !currentAppBlocked && !CrashRecovery.isRecoveryBlocked
        } ?: return
        restored.service.ruleEngine.onA11yConnected()
    }

    // Keep each read bound to its selected service and update that engine's root cache.
    fun getRoot(service: A11yCommonImpl? = this.service): AccessibilityNodeInfo? =
        service?.ruleEngine?.safeActiveWindow

    fun compatWindows(): List<AccessibilityWindowInfo> {
        return try {
            service?.windowInfos
        } catch (_: Throwable) {
            null
        } ?: emptyList()
    }

    fun onScreenForcedActive() {
        service?.ruleEngine?.onScreenForcedActive()
    }

    fun performActionBack(): Boolean {
        val result = privilegeContextFlow.value?.keyevent(KeyEvent.KEYCODE_BACK)
        if (result == true) return true
        return A11yService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) == true
    }

    suspend fun screenshot(): Bitmap? = service?.screenshot()

    suspend fun execAction(gkdAction: GkdAction): ActionResult {
        val selectorResult = Selector.compile(gkdAction.selector)
        val selector = (selectorResult as? SelectorCompileResult.Success)?.value
            ?: throw RpcError(UiStrings.selector_invalid)
        val typeResult = selector.validateType(selectorTypeModel)
        if (typeResult is SelectorTypeResult.Failure) {
            throw RpcError(UiStrings.selector_type_error(typeResult.error.message))
        }
        val service = service ?: throw RpcError(UiStrings.service_not_connected)
        val root = getRoot(service) ?: throw RpcError(UiStrings.screen_nodes_unavailable)
        val targetNode = A11yContext(
            getRoot = { getRoot(service) },
            interruptable = false,
        ).querySelfOrSelector(
            root, selector, MatchOptions(fastQuery = gkdAction.fastQuery)
        ) ?: throw RpcError(UiStrings.selector_node_not_found)
        return withContext(Dispatchers.IO) {
            ActionPerformer
                .getAction(gkdAction.action ?: ActionPerformer.None.action)
                .perform(targetNode, gkdAction)
        }
    }
}
