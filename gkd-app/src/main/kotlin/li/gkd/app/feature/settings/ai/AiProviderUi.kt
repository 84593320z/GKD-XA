package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.readableTextColor
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分组小标题 + 圆角卡片：AI 设置相关页面统一用这个壳。 */
@Composable
fun AiSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    PreferenceGroup(title = title, content = content)
}

@Composable
fun AiRowDivider(hasLeading: Boolean = true) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (hasLeading) 16.dp else 0.dp),
        thickness = 1.dp,
    )
}

/** 行首圆角图标底座，对齐 MIUIX 设置的分组图标。 */
@Composable
fun AiRowIcon(
    imageVector: ImageVector,
    tint: Color = MiuixTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                MiuixTheme.colorScheme.surfaceContainerHigh,
                GkShapes.control,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GkIcon(
            imageVector = imageVector,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) tint else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 能力 / 状态标签使用成对主题色，允许窄屏和大字号下自然换行。 */
@Composable
fun AiTagChip(
    text: String,
    emphasized: Boolean = false,
) {
    val containerColor = if (emphasized) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.secondaryVariant
    }
    val preferredContentColor = if (emphasized) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSecondaryVariant
    }
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = readableTextColor(containerColor, preferredContentColor),
        modifier = Modifier
            .background(containerColor, GkShapes.compact)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 单一当前服务商：独立触控区，使用 selected 而非多选的 checked 语义。 */
@Composable
internal fun AiProviderSelection(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val selection = if (enabled) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    } else {
        // Keep the disabled indicator separate from the still-clickable details row.
        Modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            this.selected = selected
            disabled()
        }
    }
    Box(
        modifier = Modifier.size(48.dp).then(selection).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AiRadioIndicator(selected = selected, enabled = enabled)
    }
}

/** Decorative marker. Its enclosing row or control supplies selection and accessibility. */
@Composable
internal fun AiRadioIndicator(selected: Boolean, enabled: Boolean = true) {
    val colors = MiuixTheme.colorScheme
    val color = when {
        !enabled -> colors.disabledOnSurface
        selected -> colors.primary
        else -> colors.onSurface
    }
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = (size.minDimension - strokeWidth) / 2,
            style = Stroke(width = strokeWidth),
        )
        if (selected) {
            drawCircle(color = color, radius = size.minDimension / 4)
        }
    }
}

@Composable
fun AiHint(
    text: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = if (error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(contentPadding),
    )
}

/** 当前值单独换行，窄屏与大字体下仍能读全；点击后由调用方弹出选择框。 */
@Composable
fun AiPickerRow(
    title: String,
    value: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        onClick = throttle(fn = onClick),
        bottomAction = {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/** 128000 -> 128K，1048576 -> 1M；0 表示未记录。 */
fun formatTokens(count: Int): String = when {
    count <= 0 -> "未知"
    count % 1_000_000 == 0 -> "${count / 1_000_000}M"
    count >= 1000 -> "${count / 1000}K"
    else -> count.toString()
}
