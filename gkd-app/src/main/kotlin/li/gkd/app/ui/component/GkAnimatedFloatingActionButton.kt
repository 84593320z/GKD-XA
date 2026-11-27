package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import li.gkd.app.ui.component.rememberGkVisibilityProgress
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.liquid.lens
import li.gkd.app.ui.liquid.vibrancy
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val elevationDurationMillis = 50

private val FabGlassHighlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.16f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

@Composable
fun GkAnimatedFloatingActionButton(
    visible: Boolean,
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    contentDescription: String? = getIconDefaultDesc(imageVector),
) {
    val density = LocalDensity.current
    val maxTranslationX = remember(density.density) { density.run { 24.dp.toPx() } }
    val visibility = rememberGkVisibilityProgress(
        visible = visible,
        durationMillis = androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis - elevationDurationMillis,
    )
    val percent = visibility.fraction
    if (visibility.mounted) {
        val fabModifier = modifier
            .graphicsLayer(
                alpha = percent,
                translationX = (1f - percent) * maxTranslationX
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
        GkTooltipIconButtonBox(contentDescription) {
            LiquidGlassFab(
                onClick = throttle(onClick),
                imageVector = imageVector,
                enabled = visible,
                onClickLabel = onClickLabel,
                modifier = fabModifier,
            )
        }
    }
}

@Composable
private fun LiquidGlassFab(
    onClick: () -> Unit,
    imageVector: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
) {
    val backdrop = LocalLayerBackdrop.current
    val isDark = LocalDarkTheme.current
    val shaderOk = isRuntimeShaderSupported()
    val liquidEnabled = if (backdrop != null && shaderOk) {
        val store by storeFlow.collectAsStateWithLifecycle()
        store.enableLiquidGlass && store.enableMiuixBlur
    } else {
        false
    }
    val liquidBackdrop = if (liquidEnabled) backdrop else null
    val liquidOk = liquidBackdrop != null
    val shape = remember { CircleShape }
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "FabPressScale",
    )
    val containerColor = if (liquidOk) {
        surfaceContainer.copy(alpha = if (isPressed) 0.56f else 0.4f)
    } else if (isPressed) {
        MiuixTheme.colorScheme.secondaryContainer
    } else {
        surfaceContainer
    }
    val iconTint = MiuixTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(56.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 10.dp,
                    color = Color.Black,
                    alpha = if (isDark) 0.22f else 0.12f,
                ),
            )
            .then(
                if (liquidOk) {
                    Modifier.drawBackdrop(
                        backdrop = checkNotNull(liquidBackdrop),
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx(), 4.dp.toPx())
                            padding = maxOf(padding, 28.dp.toPx())
                            lens(
                                refractionHeight = 14.dp.toPx(),
                                refractionAmount = 16.dp.toPx(),
                            )
                        },
                        highlight = { FabGlassHighlight.copy(alpha = 0.75f) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                } else {
                    Modifier
                        .clip(shape)
                        .background(containerColor)
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PerfIcon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
        )
    }
}
