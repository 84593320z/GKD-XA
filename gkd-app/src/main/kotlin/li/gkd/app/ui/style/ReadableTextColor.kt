package li.gkd.app.ui.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/** Preserve the theme foreground on a solid surface when it meets the small-text target. */
internal fun readableTextColor(background: Color, preferred: Color): Color {
    val surfaceLuminance = background.luminance()
    fun contrast(foreground: Color): Float {
        val visible = foreground.compositeOver(background).luminance()
        val light = maxOf(visible, surfaceLuminance)
        val dark = minOf(visible, surfaceLuminance)
        return (light + 0.05f) / (dark + 0.05f)
    }
    if (contrast(preferred) >= 4.5f) return preferred
    return if (contrast(Color.Black) >= contrast(Color.White)) Color.Black else Color.White
}
