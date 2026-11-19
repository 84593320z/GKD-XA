package li.gkd.app.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalIsTalkbackEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

internal const val CompactLightQualifiers = "en-rUS-w320dp-h640dp-notnight-mdpi"
internal const val CompactDarkQualifiers = "en-rUS-w320dp-h640dp-night-mdpi"

/**
 * A real Compose hierarchy, without AppTheme, GkdApp, MainActivity, stores or a backend.
 *
 * Run with Robolectric LEGACY graphics on Linux, including ARM64 hosts without a compatible
 * native graphics runtime. These are layout/semantics/input regressions, not golden-image tests.
 * Do not replace the runner with instrumentation or launch an application/privilege host.
 */
internal fun ComposeContentTestRule.setMiuixContent(
    dark: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        val controller = remember(dark) {
            ThemeController(colorSchemeMode = if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
        }
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
            LocalDarkTheme provides dark,
            LocalIsTalkbackEnabled provides false,
        ) {
            MiuixTheme(controller = controller) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .testTag("miuix-test-root"),
                ) {
                    content()
                }
            }
        }
    }
}

internal fun hasRole(role: Role): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

/** Compose 1.12.1 has a minimum-width assertion, but no maximum-width counterpart. */
internal fun SemanticsNodeInteraction.assertWidthAtMost(maxWidth: Dp): SemanticsNodeInteraction {
    val bounds = getUnclippedBoundsInRoot()
    val width = bounds.right - bounds.left
    assertTrue("Expected width at most $maxWidth, but was $width", width <= maxWidth)
    return this
}
