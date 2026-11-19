// GKD-XA modification: use the same Miuix glyph family as the app; only domain glyphs lack a native equivalent.
package priv.kit.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Report

internal object PrivilegeUiIcons {
    val KeyboardArrowDown get() = MiuixIcons.Regular.ExpandMore
    val KeyboardArrowUp get() = MiuixIcons.Regular.ExpandLess
    val Warning get() = MiuixIcons.Regular.Report
    val ContentCopy get() = MiuixIcons.Regular.Copy
    val Close get() = MiuixIcons.Basic.Close
    val ArrowBack get() = MiuixIcons.Regular.Back
    val PlayArrow get() = MiuixIcons.Regular.Play

    val OpenInNew: ImageVector by lazy {
        ImageVector.Builder(
            name = "open_in_new",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(12f)
                verticalLineTo(3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(12f)
                horizontalLineTo(19f)
                close()
                moveTo(14f, 3f)
                verticalLineTo(5f)
                horizontalLineTo(17.59f)
                lineTo(7.76f, 14.83f)
                lineTo(9.17f, 16.24f)
                lineTo(19f, 6.41f)
                verticalLineTo(10f)
                horizontalLineTo(21f)
                verticalLineTo(3f)
                close()
            }
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(name = "Stop", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f); lineTo(18f, 6f); lineTo(18f, 18f); lineTo(6f, 18f); close()
            }
        }.build()
    }
}
