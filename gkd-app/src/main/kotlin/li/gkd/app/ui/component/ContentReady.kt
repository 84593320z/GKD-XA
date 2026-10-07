package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import top.yukonga.miuix.kmp.nav.core.LocalNavTransitionScope

/** 当前 Nav 条目是否正在播放进/退场动画。 */
@Composable
fun rememberNavTransitionRunning(): Boolean =
    LocalNavTransitionScope.current.isRunning

/**
 * KernelSU `rememberContentReady` 同款：
 * Nav 转场结束后再等 1 帧才变为 true（sticky，之后不再变回 false）。
 *
 * 用于首页 HorizontalPager：转场未落定前只组当前 Tab，邻页延后，避免和进场动画抢帧。
 */
@Composable
fun rememberContentReady(): Boolean {
    val transitionRunning = rememberNavTransitionRunning()
    val ready = remember { mutableStateOf(false) }
    LaunchedEffect(transitionRunning) {
        if (!transitionRunning && !ready.value) {
            withFrameNanos { }
            ready.value = true
        }
    }
    return ready.value
}
