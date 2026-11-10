package li.gkd.app.ui.icon

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon

/**
 * 工作模式入口图标：未连接时火箭斜停在发射台上（-45°、略微下沉），连接后点火升空。
 *
 * 与 [Rocket] 的区别只在「拆成两段」——火焰和箭体各自独立做位移，所以不能直接复用
 * 那条整段 path。这里把同一份路径数据手工拆成 [rocketFlame]（前两个子路径）与
 * [rocketBody]（其余子路径），几何完全一致。
 */
@Composable
fun GkPrivilegeConnectionIcon(
    connected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val transition = updateTransition(connected, label = "Privilege connection")
    val launchProgress by transition.animateFloat(
        transitionSpec = { tween(250) },
        label = "Rocket launch",
    ) { launched -> if (launched) 1f else 0f }
    // 升空后火焰与箭体做往返漂移；余弦缓动让端点处速度归零，不会有顿挫感。
    // 只在连接中或正在起飞时才跑无限动画，静止时不要白白占一帧一帧的刷新。
    val drift = if (connected || launchProgress > 0f) {
        val flight = rememberInfiniteTransition(label = "Rocket flight")
        flight.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = Easing { fraction -> ((1.0 - cos(PI * fraction)) / 2.0).toFloat() },
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Rocket drift",
        )
    } else {
        rememberUpdatedState(0f)
    }
    Box(
        modifier = modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription }
            .graphicsLayer {
                val idle = 1f - launchProgress
                rotationZ = -45f * idle
                translationY = 3.75.dp.toPx() * idle
            },
    ) {
        listOf(rocketFlame, rocketBody).forEachIndexed { index, vector ->
            val isFlame = index == 0
            MiuixIcon(
                imageVector = vector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        val launch = launchProgress
                        alpha = if (isFlame) launch else 1f
                        val distance = (if (isFlame) -0.6.dp else 0.4.dp).toPx() * drift.value * launch
                        translationX = distance
                        translationY = -distance
                    },
            )
        }
    }
}

/** 尾焰：火箭图标里最靠下的两个子路径 */
private val rocketFlame: ImageVector
    get() {
        if (_rocketFlame != null) return _rocketFlame!!
        _rocketFlame = ImageVector.Builder(
            name = "RocketFlame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 15f)
                curveToRelative(-0.83f, 0f, -1.58f, 0.34f, -2.12f, 0.88f)
                curveTo(2.7f, 17.06f, 2f, 22f, 2f, 22f)
                reflectiveCurveToRelative(4.94f, -0.7f, 6.12f, -1.88f)
                curveTo(8.66f, 19.58f, 9f, 18.83f, 9f, 18f)
                curveTo(9f, 16.34f, 7.66f, 15f, 6f, 15f)
                close()
                moveTo(6.71f, 18.71f)
                curveToRelative(-0.28f, 0.28f, -2.17f, 0.76f, -2.17f, 0.76f)
                reflectiveCurveToRelative(0.47f, -1.88f, 0.76f, -2.17f)
                curveTo(5.47f, 17.11f, 5.72f, 17f, 6f, 17f)
                curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
                curveTo(7f, 18.28f, 6.89f, 18.53f, 6.71f, 18.71f)
                close()
            }
        }.build()
        return _rocketFlame!!
    }

/** 箭体：火箭图标里其余子路径 */
private val rocketBody: ImageVector
    get() {
        if (_rocketBody != null) return _rocketBody!!
        _rocketBody = ImageVector.Builder(
            name = "RocketBody",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.42f, 13.65f)
                curveToRelative(6.36f, -6.36f, 4.24f, -11.31f, 4.24f, -11.31f)
                reflectiveCurveToRelative(-4.95f, -2.12f, -11.31f, 4.24f)
                lineToRelative(-2.49f, -0.5f)
                curveTo(7.21f, 5.95f, 6.53f, 6.16f, 6.05f, 6.63f)
                lineTo(2f, 10.69f)
                lineToRelative(5f, 2.14f)
                lineToRelative(4.03f, 4.17f)
                lineToRelative(2.14f, 5f)
                lineToRelative(4.05f, -4.05f)
                curveToRelative(0.47f, -0.47f, 0.68f, -1.15f, 0.55f, -1.81f)
                lineToRelative(-0.5f, -2.49f)
                close()
                moveTo(7.41f, 10.83f)
                lineToRelative(-1.91f, -0.82f)
                lineToRelative(1.97f, -1.97f)
                lineToRelative(1.44f, 0.29f)
                curveTo(8.34f, 9.16f, 7.83f, 10.03f, 7.41f, 10.83f)
                close()
                moveTo(13.99f, 18.5f)
                lineToRelative(-0.82f, -1.91f)
                curveToRelative(0.8f, -0.42f, 1.67f, -0.93f, 2.49f, -1.5f)
                lineToRelative(0.29f, 1.44f)
                lineTo(13.99f, 18.5f)
                close()
                moveTo(16f, 12.24f)
                curveToRelative(-1.32f, 1.32f, -3.38f, 2.4f, -4.04f, 2.73f)
                lineToRelative(-2.93f, -2.93f)
                curveToRelative(0.32f, -0.65f, 1.4f, -2.71f, 2.73f, -4.04f)
                curveToRelative(4.68f, -4.68f, 8.23f, -3.99f, 8.23f, -3.99f)
                reflectiveCurveTo(20.68f, 7.56f, 16f, 12.24f)
                close()
                moveTo(15f, 11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                close()
            }
        }.build()
        return _rocketBody!!
    }

@Suppress("ObjectPropertyName")
private var _rocketFlame: ImageVector? = null

@Suppress("ObjectPropertyName")
private var _rocketBody: ImageVector? = null
