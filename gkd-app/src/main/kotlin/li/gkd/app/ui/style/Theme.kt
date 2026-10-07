package li.gkd.app.ui.style

import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import li.gkd.app.app
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalIsTalkbackEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors as MiuixColors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

private fun createAppearanceFlow(scope: CoroutineScope) =
    storeFlow
        .map { it.enableDarkTheme to it.enableDynamicColor }
        .distinctUntilChanged()
        .debounce(300)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            storeFlow.value.let { it.enableDarkTheme to it.enableDynamicColor },
        )

/**
 * 应用主题（MIUIX 主导版）。
 *
 * 结构：MiuixTheme 提供 MIUIX 配色/字体作为**视觉主导**；
 * 内层再套一层 MaterialTheme，把 MIUIX 配色桥接成 Material [ColorScheme]，
 * 使尚未替换为 MIUIX 的 Material3 组件仍能正常工作。
 * 这样视觉立刻变为 MIUIX，同时支持渐进式替换，不必一次性改完所有组件。
 */
@Composable
fun AppTheme(
    invertedTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val appearanceFlow = remember(scope) { createAppearanceFlow(scope) }
    val (enableDarkTheme, enableDynamicColor) = appearanceFlow.collectAsStateWithLifecycle().value
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = (enableDarkTheme ?: systemInDarkTheme).let {
        if (invertedTheme) !it else it
    }

    var isTalkbackEnabled by remember { mutableStateOf(app.a11yManager.isTouchExplorationEnabled) }
    DisposableEffect(null) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            isTalkbackEnabled = it
        }
        app.a11yManager.addTouchExplorationStateChangeListener(listener)
        onDispose {
            app.a11yManager.removeTouchExplorationStateChangeListener(listener)
        }
    }

    val colorSchemeMode = when {
        enableDynamicColor && darkTheme -> ColorSchemeMode.MonetDark
        enableDynamicColor && !darkTheme -> ColorSchemeMode.MonetLight
        darkTheme -> ColorSchemeMode.Dark
        else -> ColorSchemeMode.Light
    }
    val controller = remember(colorSchemeMode) { ThemeController(colorSchemeMode = colorSchemeMode) }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalIsTalkbackEnabled provides isTalkbackEnabled,
    ) {
        MiuixTheme(controller = controller) {
            // 把 MIUIX 配色桥接为 Material ColorScheme，兼容尚未替换的 Material3 组件。
            val materialScheme = MiuixTheme.colorScheme
                .toMaterialColorScheme(darkTheme)
                .animation()
            ApplyWindowChrome(darkTheme = darkTheme, background = materialScheme.background)
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onSurface,
                // 没被 M3 Surface 包住的 Material3 Text / Icon 读的是这个 Local；不提供的话它会退到
                // 未指定色，最终按平台默认（黑）绘制 —— 深色下就是「内容不显示」。
                MaterialLocalContentColor provides materialScheme.onSurface,
            ) {
                MaterialTheme(
                    colorScheme = materialScheme,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun ApplyWindowChrome(darkTheme: Boolean, background: Color) {
    val activity = LocalActivity.current
    if (activity == null) return
    LaunchedEffect(darkTheme) {
        // https://github.com/gkd-kit/gkd/pull/421
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
        }
    }
    val bg = background.toArgb()
    LaunchedEffect(darkTheme, bg) {
        activity.window.decorView.setBackgroundColor(bg)
    }
}

/**
 * 把 MIUIX [MiuixColors] 映射为 Material3 [ColorScheme]。
 * 用于让尚未替换为 MIUIX 的 Material3 组件跟随 MIUIX 配色。
 */
internal fun MiuixColors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariantSummary,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        tertiary = secondary,
        onTertiary = onSecondary,
        // MIUIX 色板没有下面这些槽位。留着 Material3 默认值会出事：MIUIX 深色的 surface 是纯黑、
        // background 是 #242424，明度阶梯方向和 Material3 的默认 token 相反，于是 M3 的
        // Surface / Card / Snackbar / Tooltip 会拿到和内容色对冲的面板色。这里按 MIUIX 自己的
        // 明度补齐，注意深浅两色的「最暗 / 最亮」落在不同字段上，必须分模式取。
        surfaceContainerLowest = if (darkTheme) surface else background,
        surfaceContainerLow = if (darkTheme) background else surface,
        surfaceBright = if (darkTheme) surfaceContainerHighest else background,
        surfaceDim = if (darkTheme) surface else surfaceContainerHigh,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        inversePrimary = primaryContainer,
        surfaceTint = primary,
        outlineVariant = dividerLine,
        scrim = windowDimming,
    )
}

@Composable
private fun Color.animation() = animateColorAsState(
    targetValue = this,
    animationSpec = tween(durationMillis = 500),
    label = "animation"
).value

@Composable
private fun ColorScheme.animation(): ColorScheme {
    return copy(
        primary = primary.animation(),
        onPrimary = onPrimary.animation(),
        primaryContainer = primaryContainer.animation(),
        onPrimaryContainer = onPrimaryContainer.animation(),
        inversePrimary = inversePrimary.animation(),
        secondary = secondary.animation(),
        onSecondary = onSecondary.animation(),
        secondaryContainer = secondaryContainer.animation(),
        onSecondaryContainer = onSecondaryContainer.animation(),
        tertiary = tertiary.animation(),
        onTertiary = onTertiary.animation(),
        tertiaryContainer = tertiaryContainer.animation(),
        onTertiaryContainer = onTertiaryContainer.animation(),
        background = background.animation(),
        onBackground = onBackground.animation(),
        surface = surface.animation(),
        onSurface = onSurface.animation(),
        surfaceVariant = surfaceVariant.animation(),
        onSurfaceVariant = onSurfaceVariant.animation(),
        surfaceTint = surfaceTint.animation(),
        inverseSurface = inverseSurface.animation(),
        inverseOnSurface = inverseOnSurface.animation(),
        error = error.animation(),
        onError = onError.animation(),
        errorContainer = errorContainer.animation(),
        onErrorContainer = onErrorContainer.animation(),
        outline = outline.animation(),
        outlineVariant = outlineVariant.animation(),
        scrim = scrim.animation(),
        surfaceBright = surfaceBright.animation(),
        surfaceDim = surfaceDim.animation(),
        surfaceContainer = surfaceContainer.animation(),
        surfaceContainerHigh = surfaceContainerHigh.animation(),
        surfaceContainerHighest = surfaceContainerHighest.animation(),
        surfaceContainerLow = surfaceContainerLow.animation(),
        surfaceContainerLowest = surfaceContainerLowest.animation(),
    )
}
