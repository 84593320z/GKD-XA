package li.gkd.app.a11y

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import li.gkd.app.domain.rule.awaitSubmittedAction
import li.gkd.app.service.A11yService

internal suspend fun dispatchGestureAndAwait(gesture: GestureDescription): Boolean {
    val service = A11yService.instance ?: return false
    return awaitSubmittedAction(gesture.getStroke(0).duration + 1_000L) { complete ->
        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) = complete(true)
            override fun onCancelled(gestureDescription: GestureDescription) = complete(false)
        }, null)
    }
}
