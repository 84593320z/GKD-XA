package li.gkd.app.ui.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Authorization inherits the application's native Miuix palette, typography and indication.
 * Its local priv-ui 0.20.0 fork renders Miuix components; there is intentionally no Material
 * theme adapter here. Do not restore the published Material-rendered priv-ui dependency.
 */
@Composable
internal fun PrivilegeKitTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onBackground) {
        content()
    }
}
