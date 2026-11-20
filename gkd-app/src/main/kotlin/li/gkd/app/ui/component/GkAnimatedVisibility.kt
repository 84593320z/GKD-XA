package li.gkd.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal data class GkVisibilityProgress(val mounted: Boolean, val fraction: Float)

/** Re-targets from the current fraction on every visibility edge, including a reversed exit. */
@Composable
internal fun rememberGkVisibilityProgress(visible: Boolean, durationMillis: Int): GkVisibilityProgress {
    var mounted by remember { mutableStateOf(visible) }
    val fraction = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        if (visible) mounted = true
        fraction.animateTo(if (visible) 1f else 0f, animationSpec = tween(durationMillis))
        if (!visible) mounted = false
    }
    return GkVisibilityProgress(mounted, fraction.value)
}
