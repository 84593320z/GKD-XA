package li.gkd.app.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor

@Composable
fun GkSizedIconButton(
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
    onClickLabel: String? = null,
    imageVector: ImageVector,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
) = GkTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    IconButton(
        modifier = Modifier.size(size).semantics(mergeDescendants = true) {
            role = Role.Button
            if (!enabled) disabled()
            if (onClickLabel != null) {
                this.onClick(label = onClickLabel, action = null)
            }
        },
        minWidth = size,
        minHeight = size,
        enabled = enabled,
        onClick = onClick,
    ) {
        GkIcon(
            modifier = Modifier.size(iconSize),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = tint.alpha * 0.38f),
        )
    }
}
