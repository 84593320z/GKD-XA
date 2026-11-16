package li.gkd.app.a11y

import android.view.accessibility.AccessibilityNodeInfo
import li.gkd.app.domain.rule.BoundedBlockingRunner

// Process-wide bounds survive engine/service recreation. A stuck source cannot
// consume the root/fallback slot, and no pool queues additional Binder requests.
internal object A11yBlockingCalls {
    val source = BoundedBlockingRunner(2, "a11y-source")
    val root = BoundedBlockingRunner(1, "a11y-root")
    val foreground = BoundedBlockingRunner(1, "a11y-foreground")
}

@Suppress("DEPRECATION")
internal fun AccessibilityNodeInfo.recycleUnused() = recycle()
