package li.gkd.app.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor

/**
 * 固定尺寸的圆形图标按钮，直接使用官方 miuix [MiuixIconButton]。
 *
 * 旧实现是自绘 Box + Material ripple + M3 [androidx.compose.material3.IconButtonColors]，
 * 与 miuix 的按压反馈/命中区域不一致；现在只保留「尺寸可指定」这一点差异。
 */
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
    containerColor: Color = Color.Unspecified,
) = GkTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    MiuixIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .semantics {
                if (onClickLabel != null) {
                    onClick(label = onClickLabel, action = null)
                }
            },
        enabled = enabled,
        backgroundColor = containerColor,
        cornerRadius = size / 2,
        minWidth = size,
        minHeight = size,
    ) {
        GkIcon(
            modifier = Modifier.size(iconSize),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
