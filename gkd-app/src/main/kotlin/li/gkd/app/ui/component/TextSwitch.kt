package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 开关设置项：标题 + 摘要 + 行尾开关，可带行尾图标与摘要下方链接。
 *
 * 排版与其他设置项一致（[GkRowDefaults]），按压整行叠浅色、无涟漪；
 * 整行可点即切换开关，语义角色为 Switch。
 */
@Composable
fun TextSwitch(
    title: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") paddingDisabled: Boolean = false,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    checked: Boolean = true,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = { onCheckedChange?.invoke(!checked) },
    onClickLabel: String? = "切换${title}状态",
) {
    val throttledChange = onCheckedChange?.let { throttle(fn = it) }
    val hasSuffixLink = subtitle != null && suffix != null && onSuffixClick != null
    val summaryText = when {
        hasSuffixLink -> null
        subtitle != null && suffix != null -> "$subtitle $suffix"
        else -> subtitle
    }
    val bottomAction: (@Composable () -> Unit)? = if (hasSuffixLink && subtitle != null) {
        {
            Row {
                MiuixText(
                    text = subtitle,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                MiuixText(
                    text = suffix.orEmpty(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = throttle(fn = onSuffixClick)),
                )
            }
        }
    } else {
        null
    }

    GkRow(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        onClickLabel = onClickLabel,
        role = Role.Switch,
        bottomAction = bottomAction,
        endActions = {
            suffixIcon?.invoke()
            GkSwitch(
                checked = checked,
                onCheckedChange = throttledChange,
                enabled = enabled,
            )
        },
    ) {
        GkRowText(title = title, summary = summaryText)
    }
}
