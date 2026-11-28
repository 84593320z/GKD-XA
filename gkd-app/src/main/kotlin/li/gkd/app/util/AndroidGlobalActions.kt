package li.gkd.app.util

import android.accessibilityservice.AccessibilityService
import li.gkd.app.a11y.A11yCommonImpl
import li.gkd.app.a11y.A11yRuntime
import li.gkd.app.service.A11yService

internal object AndroidGlobalActions {
    /** Null means unavailable to query, not that the shade action is absent. */
    fun queryNotificationShadeDismissAvailability(service: A11yCommonImpl): Boolean? {
        if (!AndroidTarget.S || service !is A11yService || A11yRuntime.service !== service) return null
        val action = AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        return try {
            // AOSP SystemUI registers this only while the unlocked notification shade is expanded.
            service.systemActions.any { it.id == action }
        } catch (_: RuntimeException) {
            null
        }
    }

    /** Called only after the public SystemUI action is advertised; never substitutes global Back. */
    fun dismissNotificationShade(service: A11yCommonImpl): Boolean {
        val accessibilityService = service as? A11yService ?: return false
        if (queryNotificationShadeDismissAvailability(accessibilityService) != true) return false
        val action = AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        return try {
            A11yRuntime.service === accessibilityService && accessibilityService.performGlobalAction(action)
        } catch (_: RuntimeException) {
            false
        }
    }
}
