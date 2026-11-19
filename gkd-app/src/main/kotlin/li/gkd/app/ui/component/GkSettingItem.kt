package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.graphics.vector.ImageVector

/** One native row implementation for both historical setting-item entry points. */
@Composable
fun GkSettingItem(
    title: String,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    imageVector: ImageVector? = GkIcons.KeyboardArrowRight,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    subtitleOverflow: TextOverflow = TextOverflow.Clip,
) {
    val trailing = imageVector.takeUnless { it == GkIcons.KeyboardArrowRight && onClick == null }
    BasicComponent(
        onClick = onClick?.let { throttle(fn = it) },
        onClickLabel = onClickLabel ?: UiStrings.page_enter_description(title),
        endActions = trailing?.let { { GkIcon(it, contentDescription = null) } },
    ) {
        Text(title, style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium))
        GkPreferenceSummary(subtitle, suffix, suffixUnderline, onSuffixClick,
            maxLines = subtitleMaxLines, overflow = subtitleOverflow)
    }
}

@Composable
internal fun GkPreferenceSummary(
    subtitle: String?,
    suffix: String?,
    underline: Boolean,
    onSuffixClick: (() -> Unit)?,
    enabled: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    if (subtitle == null && suffix == null) return
    val scheme = MiuixTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        subtitle?.let {
            Text(it, style = MiuixTheme.textStyles.body2, maxLines = maxLines, overflow = overflow,
                color = if (enabled) scheme.onSurfaceVariantSummary else scheme.disabledOnSurface)
        }
        suffix?.let {
            Text(it, style = MiuixTheme.textStyles.body2,
                textDecoration = if (underline) TextDecoration.Underline else null,
                color = if (!enabled) scheme.disabledOnSurface else if (onSuffixClick != null) scheme.primary
                    else scheme.onSurfaceVariantSummary,
                modifier = if (onSuffixClick != null) Modifier.clickable(enabled = enabled,
                    onClick = throttle(fn = onSuffixClick)) else Modifier)
        }
    }
}
