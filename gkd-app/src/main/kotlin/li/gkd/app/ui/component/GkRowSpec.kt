package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全 App 统一的分组 / 行排版规格。
 *
 * 这套数字来自「分组卡片 + 行」的目标设计：
 * - 卡片圆角 24、左右外边距 16、分组之间 16
 * - 行高 ≥52、行内边距 水平 16 / 垂直 12、行首图标 24 + 间距 16
 * - 分组小标题 14 Bold、缩进 32、上下 4/8
 * - 分割线 0.33dp、10% 前景色；有行首图标时左缩进到文字起始位置
 * - 按压反馈：按住时整行叠一层 6% 前景色（与 MIUIX 组件一致，不用涟漪）
 */
object GkRowDefaults {
    val SidePadding = 16.dp
    val GroupSpacing = 16.dp
    val IconSize = 24.dp
    val IconTextGap = 16.dp
    val RowMinHeight = 52.dp
    val CardCornerRadius = 24.dp

    /** 分割线 / 内容的起始位置：有行首图标时对齐文字，否则对齐卡片内边距 */
    fun contentStart(hasLeading: Boolean): Dp =
        if (hasLeading) SidePadding + IconSize + IconTextGap else SidePadding
}

/**
 * 组内行序号计数器：由 [GkGroup] / PreferenceGroup 提供，
 * [GkRow] 首次组合时领一个序号，用来决定是否需要画上方的分割线。
 */
internal class GkRowIndexCounter {
    private var next = -1

    fun take(): Int = ++next

    /** 每次分组重组时重置；已有行靠 remember 保持自己的序号，不会重领。 */
    fun reset() {
        next = -1
    }
}

internal val LocalGkRowIndexCounter = androidx.compose.runtime.compositionLocalOf<GkRowIndexCounter?> { null }

/** 列表行尾的细箭头（8x14dp），与 MIUIX 观感一致。 */
val GkArrowChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "GkArrowChevron",
        defaultWidth = 8.dp,
        defaultHeight = 14.dp,
        viewportWidth = 16f,
        viewportHeight = 28f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3f, 3f)
            lineTo(13f, 14f)
            lineTo(3f, 25f)
        }
    }.build()
}

/** 分组小标题：副标题字号 + 加粗、次级色。 */
@Composable
fun GkGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 8.dp),
    )
}

/** 圆角 24dp 的分组卡片。 */
@Composable
fun GkCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    showIndication: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = GkRowDefaults.CardCornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        showIndication = showIndication,
        onClick = onClick,
        content = content,
    )
}

/** 分组：可选小标题 + 圆角卡片（左右 16、底部 16）。 */
@Composable
fun GkGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val counter = remember { GkRowIndexCounter() }
    counter.reset()
    Column(modifier = modifier) {
        if (!title.isNullOrBlank()) {
            GkGroupTitle(title)
        }
        GkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GkRowDefaults.SidePadding)
                .padding(bottom = GkRowDefaults.GroupSpacing),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalGkRowIndexCounter provides counter,
            ) {
                content()
            }
        }
    }
}

/** 分组内分割线：细 0.33dp、低对比。 */
@Composable
fun GkDivider(hasLeading: Boolean = false, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(
            start = GkRowDefaults.contentStart(hasLeading),
            end = GkRowDefaults.SidePadding,
        ),
        thickness = 0.33.dp,
        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.1f),
    )
}

/** 行的标题/摘要文字块。 */
@Composable
fun GkRowText(
    title: String,
    summary: String? = null,
    titleColor: Color = Color.Unspecified,
    summaryColor: Color = Color.Unspecified,
) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.Medium,
        color = if (titleColor == Color.Unspecified) LocalContentColor.current else titleColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (!summary.isNullOrBlank()) {
        Text(
            text = summary,
            style = MiuixTheme.textStyles.body2,
            color = if (summaryColor == Color.Unspecified) {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            } else {
                summaryColor
            },
        )
    }
}

/**
 * 分组内的一行。三栏：行首（固定 24dp）→ 标题块（自适应）→ 行尾（右对齐，可放开关/箭头/文字）。
 *
 * [onClick] / [onLongClick] 非空时整行可点，并且按住会叠一层浅色背景（无涟漪）。
 * [interaction] 用于调用方自带点击语义（如拖拽、多选）时覆盖内置点击。
 */
@Composable
fun GkRow(
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    interaction: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    title: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressBackground = if (pressed && enabled) {
        MiuixTheme.colorScheme.onBackground.copy(alpha = 0.06f)
    } else {
        Color.Transparent
    }
    val clickModifier = if (onClick != null || onLongPress != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = { onClick?.invoke() },
            onLongClick = onLongPress,
        )
    } else {
        interaction
    }
    val rowCounter = LocalGkRowIndexCounter.current
    val rowIndex = remember { rowCounter?.take() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(pressBackground)
            .then(clickModifier),
    ) {
        if (rowIndex != null && rowIndex > 0) {
            GkDivider(hasLeading = startAction != null)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GkRowDefaults.RowMinHeight)
                .padding(horizontal = GkRowDefaults.SidePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (startAction != null) {
                startAction()
                Spacer(Modifier.width(GkRowDefaults.IconTextGap))
            }
            CompositionLocalProvider(
                LocalContentColor provides if (enabled) titleColor.color else titleColor.disabledColor,
            ) {
                Column(modifier = Modifier.weight(1f)) { title() }
            }
            if (endActions != null) {
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, content = endActions)
            }
        }
        bottomAction?.let { action ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GkRowDefaults.SidePadding)
                    .padding(bottom = 12.dp),
            ) {
                action()
            }
        }
    }
}

/** 行尾箭头。 */
@Composable
fun GkRowArrow(end: Boolean = false) {
    Icon(
        imageVector = GkArrowChevron,
        contentDescription = null,
        modifier = Modifier
            .size(8.dp, 14.dp)
            .graphicsLayer { rotationZ = if (end) 90f else 0f },
        tint = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.3f),
    )
}

/** 跳转行：标题 + 摘要 + 右箭头。 */
@Composable
fun GkArrowRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
) = GkRow(
    modifier = modifier,
    startAction = startAction,
    bottomAction = bottomAction,
    enabled = enabled,
    onClick = onClick,
    onClickLabel = onClickLabel,
    endActions = {
        endActions?.invoke(this)
        GkRowArrow()
    },
) {
    GkRowText(title = title, summary = summary)
}

/** 开关行：整行可点，行尾是 MIUIX 开关。 */
@Composable
fun GkSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
) = GkRow(
    modifier = modifier,
    startAction = startAction,
    enabled = enabled,
    onClick = { onCheckedChange(!checked) },
    role = Role.Switch,
    endActions = {
        endActions?.invoke(this)
        GkSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    },
) {
    GkRowText(title = title, summary = summary)
}

/** 无底色的普通信息行（不可点）。 */
@Composable
fun GkPlainRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
) = GkRow(
    modifier = modifier,
    startAction = startAction,
    endActions = endActions,
) {
    GkRowText(title = title, summary = summary)
}

/**
 * 页面底部操作条：替代 Material3 的 BottomAppBar（miuix 没有对应组件）。
 * 只是「一条底色 + 横向操作行」，外观跟随 [GkRowDefaults]。
 */
@Composable
fun GkBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MiuixTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GkRowDefaults.SidePadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** 行首图标占位（固定 24dp，保证所有行文字与分割线对齐）。 */
@Composable
fun GkRowIconSlot(content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(GkRowDefaults.IconSize), contentAlignment = Alignment.Center) {
        content()
    }
}
