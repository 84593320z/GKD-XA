package priv.kit.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class PrivilegeStatusContrastTest {
    @Test
    fun serviceStatusForegroundMeetsSmallTextContrastAcrossLightAndDarkPalettes() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { colors ->
            assertStatusContrast(colors.tertiaryContainer, colors.onTertiaryContainer)
            assertStatusContrast(colors.surfaceContainer, colors.onSurfaceContainer)
        }
    }

    @Test
    fun actionForegroundMeetsSmallTextContrastOnPrimaryAndErrorContainers() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { colors ->
            assertStatusContrast(colors.primaryContainer, colors.onPrimaryContainer)
            assertStatusContrast(colors.errorContainer, colors.onErrorContainer)
        }
    }

    @Test
    fun alphaForegroundIsEvaluatedAsRenderedOverOpaqueStatusSurface() {
        val background = Color(0xFF303840)
        val translucentForeground = Color.White.copy(alpha = 0.32f)
        val resolved = PrivilegeMiuixTheme.textColorForContrast(background, translucentForeground)
        assertTrue(contrast(background, resolved) >= 4.5f)
    }

    private fun assertStatusContrast(background: Color, preferred: Color) {
        val resolved = PrivilegeMiuixTheme.textColorForContrast(background, preferred)
        assertTrue(
            "palette foreground contrast was ${contrast(background, resolved)}",
            contrast(background, resolved) >= 4.5f,
        )
    }

    private fun contrast(background: Color, foreground: Color): Float {
        val surface = background.luminance()
        val visibleForeground = foreground.compositeOver(background).luminance()
        val lighter = maxOf(surface, visibleForeground)
        val darker = minOf(surface, visibleForeground)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
