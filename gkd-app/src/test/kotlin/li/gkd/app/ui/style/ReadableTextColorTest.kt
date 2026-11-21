package li.gkd.app.ui.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class ReadableTextColorTest {
    @Test
    fun preservesReadableForegroundsFromLightAndDarkMiuixPalettes() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { colors ->
            listOf(
                colors.primary to colors.onPrimary,
                colors.secondaryVariant to colors.onSecondaryVariant,
                colors.surfaceContainer to colors.onSurfaceContainer,
            ).forEach { (background, preferred) ->
                val preferredContrast = contrast(background, preferred)
                val readable = readableTextColor(background, preferred)
                assertTrue("resolved foreground contrast was ${contrast(background, readable)}", contrast(background, readable) >= 4.5f)
                if (preferredContrast >= 4.5f) assertEquals(preferred, readable)
            }
        }
    }

    @Test
    fun fallbackMaintainsSmallTextContrastForBrightAndMiddleToneThemeColors() {
        val cases = listOf(
            lightColorScheme(primary = Color(0xFFBDD5FF), onPrimary = Color.White).let {
                it.primary to it.onPrimary
            },
            (Color(0xFF5D6270) to Color(0xFF777777)),
        )
        cases.forEach { (background, preferred) ->
            assertTrue("fixture must exercise the fallback", contrast(background, preferred) < 4.5f)
            val readable = readableTextColor(background, preferred)
            assertTrue("fallback contrast was ${contrast(background, readable)}", contrast(background, readable) >= 4.5f)
        }
    }

    @Test
    fun alphaForegroundIsComparedAfterCompositingOverItsSurface() {
        val background = Color.Black
        val translucentWhite = Color.White.copy(alpha = 0.5f)
        assertTrue(contrast(background, translucentWhite) >= 4.5f)
        assertEquals(translucentWhite, readableTextColor(background, translucentWhite))
    }

    private fun contrast(background: Color, foreground: Color): Float {
        val surface = background.luminance()
        val visibleForeground = foreground.compositeOver(background).luminance()
        val lighter = maxOf(surface, visibleForeground)
        val darker = minOf(surface, visibleForeground)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
