package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置项：标题 + 摘要 + 可选行尾图标 / 右箭头。
 *
 * 排版走全 App 统一的 [GkRowDefaults]：行高 ≥52、内边距 16/12、
 * 标题 body1 Medium、摘要 body2；按压时整行叠浅色（不用涟漪）。
 */
@Composable
fun SettingItem(
    title: String,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    imageVector: ImageVector? = PerfIcon.KeyboardArrowRight,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val hasSuffixLink = subtitle != null && suffix != null && onSuffixClick != null
    val summaryText = when {
        hasSuffixLink -> null
        subtitle != null && suffix != null -> "$subtitle $suffix"
        else -> subtitle
    }
    val click = onClick?.let { throttle(fn = it) }
    val bottomAction: (@Composable () -> Unit)? = if (hasSuffixLink) {
        {
            SettingSuffixRow(
                subtitle = subtitle,
                suffix = suffix,
                onSuffixClick = onSuffixClick,
            )
        }
    } else {
        null
    }
    val showArrow = imageVector == PerfIcon.KeyboardArrowRight

    GkRow(
        bottomAction = bottomAction,
        onClick = click,
        onClickLabel = onClickLabel,
        endActions = when {
            showArrow -> ({ GkRowArrow() })
            imageVector != null -> ({
                GkIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            })
            else -> null
        },
    ) {
        GkRowText(title = title, summary = summaryText)
    }
}

@Composable
private fun SettingSuffixRow(
    subtitle: String,
    suffix: String,
    onSuffixClick: (() -> Unit)?,
) {
    Row {
        MiuixText(
            text = subtitle,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        MiuixText(
            text = suffix,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
            modifier = if (onSuffixClick != null) {
                Modifier.clickable(onClick = throttle(fn = onSuffixClick))
            } else {
                Modifier
            },
        )
    }
}
