// SPDX-License-Identifier: Apache-2.0
package priv.kit.ui.miuix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.addSquircleRect
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles

/** Presentation tokens for the retained upstream layouts, not a Material theme bridge. */
internal object PrivilegeMiuixTheme {
    fun textColorForContrast(background: Color, preferred: Color): Color {
        val surfaceLuminance = background.luminance()
        fun contrast(foreground: Color): Float {
            val visible = foreground.compositeOver(background).luminance()
            val light = maxOf(visible, surfaceLuminance)
            val dark = minOf(visible, surfaceLuminance)
            return (light + 0.05f) / (dark + 0.05f)
        }
        if (contrast(preferred) >= 4.5f) return preferred
        return if (contrast(Color.Black) >= contrast(Color.White)) {
            Color.Black
        } else {
            Color.White
        }
    }

    val colorScheme: PrivilegePalette
        @Composable get() {
            val colors = MiuixTheme.colorScheme
            return remember(colors) { PrivilegePalette(colors) }
        }
    val typography: PrivilegeTypography
        @Composable get() {
            val styles = MiuixTheme.textStyles
            return remember(styles) { PrivilegeTypography(styles) }
        }
    val shapes = PrivilegeShapes
}

internal class PrivilegePalette(private val colors: Colors) {
    val background get() = colors.surface
    val primary get() = colors.primary
    val onPrimary get() = colors.onPrimary
    val primaryContainer get() = colors.primaryContainer
    val onPrimaryContainer get() = colors.onPrimaryContainer
    val onSurface get() = colors.onSurface
    val onSurfaceVariant get() = colors.onSurfaceVariantSummary
    val surfaceContainerLow get() = colors.surfaceContainer
    val surfaceContainerHigh get() = colors.surfaceContainerHigh
    val surfaceContainerHighest get() = colors.surfaceContainerHighest
    val secondaryContainer get() = colors.secondaryContainer
    val onSecondaryContainer get() = colors.onSecondaryContainer
    val tertiary get() = colors.onTertiaryContainer
    val tertiaryContainer get() = colors.tertiaryContainer
    val onTertiaryContainer get() = colors.onTertiaryContainer
    val error get() = colors.error
    val errorContainer get() = colors.errorContainer
    val onErrorContainer get() = colors.onErrorContainer
    val outlineVariant get() = colors.dividerLine
    val inverseSurface get() = colors.onSurface
    val inverseOnSurface get() = colors.surface
}

/** Names follow retained layout roles; every value comes from the active Miuix typography. */
internal class PrivilegeTypography(private val styles: TextStyles) {
    val bodyMedium: TextStyle get() = styles.body1
    val bodySmall: TextStyle get() = styles.footnote1
    val labelSmall: TextStyle get() = styles.footnote2
    val labelMedium: TextStyle get() = styles.body2
    val labelLarge: TextStyle get() = styles.body1
    val titleSmall: TextStyle get() = styles.headline1
    val titleMedium: TextStyle get() = styles.title4
    val headlineSmall: TextStyle get() = styles.title3
}

internal object PrivilegeShapes {
    val small = PrivilegeSquircleShape(10.dp)
    val medium = PrivilegeSquircleShape(top.yukonga.miuix.kmp.basic.CardDefaults.CornerRadius)
    val extraLarge = PrivilegeSquircleShape(top.yukonga.miuix.kmp.layout.DialogDefaults.CornerRadius)
}

@Immutable
internal data class PrivilegeSquircleShape(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply {
            addSquircleRect(size.width, size.height, with(density) { radius.toPx() })
        })
}

@Composable
internal fun privilegeContentColorFor(background: Color): Color {
    val colors = MiuixTheme.colorScheme
    return when (background) {
        colors.background -> colors.onBackground
        colors.primary -> colors.onPrimary
        colors.primaryContainer -> colors.onPrimaryContainer
        colors.errorContainer -> colors.onErrorContainer
        colors.tertiaryContainer -> colors.onTertiaryContainer
        else -> colors.onSurface
    }
}
