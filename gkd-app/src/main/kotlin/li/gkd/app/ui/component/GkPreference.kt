package li.gkd.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keep native text insets without adding 32dp around a 48dp control. */
@Composable
internal fun GkPreferenceRow(
    modifier: Modifier = Modifier,
    paddingDisabled: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // BasicComponent supplies the 56dp minimum. Text keeps 16dp above/below;
    // a 48dp trailing target gets only 4dp, so a single-line row is also 56dp.
    BasicComponent(
        modifier = modifier,
        insideMargin = if (paddingDisabled) PaddingValues(0.dp)
            else PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        enabled = enabled,
        onClick = onClick,
        onClickLabel = onClickLabel,
        role = role,
        endActions = endActions,
    ) {
        Column(
            modifier = Modifier.padding(vertical = if (paddingDisabled) 0.dp else 12.dp),
            content = content,
        )
    }
}

/** Decorative navigation hint matching Miuix 0.9.4 ArrowPreference, not an action. */
@Composable
internal fun GkPreferenceArrow() {
    val layoutDirection = LocalLayoutDirection.current
    Image(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
        modifier = Modifier.size(width = 10.dp, height = 16.dp).graphicsLayer {
            scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        },
    )
}
