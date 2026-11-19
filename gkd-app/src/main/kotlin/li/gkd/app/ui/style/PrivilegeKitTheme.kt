package li.gkd.app.ui.style

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import li.gkd.app.ui.share.LocalDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** priv-kit owns the authorization UI. Limit its Material theme adapter to that screen. */
@Composable
internal fun PrivilegeKitTheme(content: @Composable () -> Unit) {
    val c = MiuixTheme.colorScheme
    val dark = LocalDarkTheme.current
    val base = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.primary,
            onSecondary = c.onPrimary,
            secondaryContainer = c.secondaryContainer,
            onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.primary,
            onTertiary = c.onPrimary,
            tertiaryContainer = c.tertiaryContainer,
            onTertiaryContainer = c.onTertiaryContainer,
            background = c.background,
            onBackground = c.onBackground,
            surface = c.surface,
            onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.onSurfaceVariantSummary,
            surfaceContainerLowest = if (dark) c.surface else c.background,
            surfaceContainerLow = if (dark) c.background else c.surface,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.surfaceContainerHigh,
            surfaceContainerHighest = c.surfaceContainerHighest,
            surfaceTint = c.primary,
            inverseSurface = c.onSurface,
            inverseOnSurface = c.surface,
            inversePrimary = c.primaryContainer,
            outline = c.outline,
            outlineVariant = c.dividerLine,
            error = c.error,
            onError = c.onError,
            errorContainer = c.errorContainer,
            onErrorContainer = c.onErrorContainer,
            scrim = c.windowDimming,
        ),
        content = content,
    )
}
