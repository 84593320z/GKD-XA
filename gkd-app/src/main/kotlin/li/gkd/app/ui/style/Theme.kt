package li.gkd.app.ui.style

import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

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

/** Single source of color and typography for the app, including service overlays. */
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
            ApplyWindowChrome(darkTheme = darkTheme, background = MiuixTheme.colorScheme.background)
            content()
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
