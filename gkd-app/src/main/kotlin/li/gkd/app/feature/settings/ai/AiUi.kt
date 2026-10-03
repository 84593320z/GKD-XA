package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.GkTopAppBar

/**
 * AI 设置页的排版规格。所有 AI 相关页面统一从这里取值，保证「分组卡片 + 行」的
 * 边距、圆角、字号层次完全一致。
 */
object AiUiDefaults {
    /** 卡片左右外边距 */
    val SidePadding = 16.dp

    /** 分组之间的垂直间距 */
    val GroupSpacing = 16.dp

    /** 行首图标占位宽度 */
    val IconSize = 24.dp

    /** 行首图标与文字之间的间距 */
    val IconTextGap = 16.dp

    /** 行最小高度 */
    val RowMinHeight = 52.dp

    /** 分组卡片圆角 */
    val CardCornerRadius = 24.dp

    /** 主按钮圆角与最小高度 */
    val ButtonCornerRadius = 22.dp
    val ButtonMinHeight = 44.dp
    val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

    /** 弹窗内外边距 */
    val DialogInsideMargin = DpSize(24.dp, 24.dp)
    val DialogOutsideMargin = DpSize(16.dp, 16.dp)

    /** 分割线起始位置：有行首图标时对齐文字，否则对齐卡片内边距 */
    fun contentStart(hasLeading: Boolean): Dp =
        if (hasLeading) SidePadding + IconSize + IconTextGap else SidePadding
}

/** 细箭头（8x14dp），与 MIUIX 的列表箭头观感一致。 */
val AiArrowRight: ImageVector by lazy {
    ImageVector.Builder(
        name = "AiArrowRight",
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

/** 圆角 24dp 的分组卡片。 */
@Composable
fun AiCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = CardDefaults.defaultColors(),
    onClick: (() -> Unit)? = null,
    showIndication: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = AiUiDefaults.CardCornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        showIndication = showIndication,
        onClick = onClick,
        content = content,
    )
}

/** 分组小标题（副标题字号、次级颜色）。 */
@Composable
fun AiGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 8.dp),
    )
}

/** 分组：可选小标题 + 卡片；卡片左右 16dp、分组之间 16dp。 */
@Composable
fun AiGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (title != null) {
            AiGroupTitle(title)
        }
        AiCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AiUiDefaults.SidePadding)
                .padding(bottom = AiUiDefaults.GroupSpacing),
            content = content,
        )
    }
}

/** 分组内分割线：细 0.33dp、低对比，起点随行首图标对齐。 */
@Composable
fun AiDivider(hasLeading: Boolean = false, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(
            start = AiUiDefaults.contentStart(hasLeading),
            end = AiUiDefaults.SidePadding,
        ),
        thickness = 0.33.dp,
        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.1f),
    )
}

/**
 * 分组内的一行。三栏布局：[startAction]（固定 [AiUiDefaults.IconSize]）+
 * 标题块（自适应剩余宽度）+ [endActions]（右对齐）。
 */
@Composable
fun AiRow(
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    interaction: Modifier = Modifier,
    title: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().then(interaction)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AiUiDefaults.RowMinHeight)
                .padding(horizontal = AiUiDefaults.SidePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (startAction != null) {
                startAction()
                Spacer(Modifier.width(AiUiDefaults.IconTextGap))
            }
            Column(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalContentColor provides if (enabled) titleColor.color else titleColor.disabledColor,
                ) {
                    title()
                }
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
                    .padding(horizontal = AiUiDefaults.SidePadding)
                    .padding(bottom = 12.dp),
            ) {
                action()
            }
        }
    }
}

/** 标题 + 摘要的标准两行文字块。 */
@Composable
fun AiRowText(
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
    if (summary != null) {
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

/** 可点击行。 */
@Composable
fun AiPreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) = AiRow(
    modifier = modifier,
    startAction = startAction,
    endActions = endActions,
    bottomAction = bottomAction,
    interaction = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier,
) {
    AiRowText(title = title, summary = summary)
}

/** 带右箭头的跳转行。 */
@Composable
fun AiArrowRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = AiRow(
    modifier = modifier,
    startAction = startAction,
    bottomAction = bottomAction,
    endActions = {
        Icon(
            imageVector = AiArrowRight,
            contentDescription = null,
            modifier = Modifier.size(8.dp, 14.dp),
            tint = if (enabled) {
                MiuixTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            } else {
                MiuixTheme.colorScheme.disabledOnSurface
            },
        )
    },
    interaction = Modifier.clickable(enabled = enabled, onClick = onClick),
) {
    AiRowText(title = title, summary = summary)
}

/** 开关行：整行可点，右侧是缩小到 44x24dp 的 MIUIX 开关。 */
@Composable
fun AiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) = AiRow(
    modifier = modifier,
    startAction = startAction,
    endActions = {
        AiSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    },
    interaction = Modifier.clickable(
        enabled = enabled,
        role = Role.Switch,
        onClick = { onCheckedChange(!checked) },
    ),
) {
    AiRowText(title = title, summary = summary)
}

/** 行尾下拉选择行：右侧显示当前值 + 箭头，点击由调用方弹出选项。 */
@Composable
fun AiDropdownRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = AiRow(
    modifier = modifier,
    startAction = startAction,
    endActions = {
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = if (enabled) {
                MiuixTheme.colorScheme.onSurfaceVariantActions
            } else {
                MiuixTheme.colorScheme.disabledOnSurface
            },
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = AiArrowRight,
            contentDescription = null,
            modifier = Modifier.size(8.dp, 14.dp).graphicsLayer { rotationZ = 90f },
            tint = if (enabled) {
                MiuixTheme.colorScheme.onSurfaceVariantActions
            } else {
                MiuixTheme.colorScheme.disabledOnSurface
            },
        )
    },
    interaction = Modifier.clickable(enabled = enabled, onClick = onClick),
) {
    AiRowText(title = title, summary = summary)
}

/** 固定 44x24dp 的 MIUIX 开关。 */
@Composable
fun AiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val density = LocalDensity.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = with(density) { 44.dp.roundToPx() }
            val height = with(density) { 24.dp.roundToPx() }
            layout(width, height) {
                placeable.placeWithLayer(
                    (width - placeable.width) / 2,
                    (height - placeable.height) / 2,
                ) {
                    if (placeable.width > 0 && placeable.height > 0) {
                        scaleX = width.toFloat() / placeable.width
                        scaleY = height.toFloat() / placeable.height
                    }
                }
            }
        },
    )
}

/** 分组内直接放输入框时的容器。 */
@Composable
fun AiFieldBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AiUiDefaults.SidePadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** AI 页统一输入框（圆角 12dp、MIUIX 配色）。 */
@Composable
fun AiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        useLabelAsPlaceholder = true,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = textStyle ?: MiuixTheme.textStyles.main,
    )
}

/** 主按钮（实心）/ 次按钮（灰色），与弹窗按钮语言一致。 */
@Composable
fun AiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    colors: TextButtonColors = if (primary) {
        ButtonDefaults.textButtonColorsPrimary()
    } else {
        ButtonDefaults.textButtonColors()
    },
) {
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        cornerRadius = AiUiDefaults.ButtonCornerRadius,
        minHeight = AiUiDefaults.ButtonMinHeight,
        colors = colors,
        insideMargin = AiUiDefaults.ButtonPadding,
    )
}

/** 弹窗底部按钮行：取消在左、确认在右，平分整行。 */
@Composable
fun AiDialogActions(
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "取消",
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AiTextButton(
            text = cancelText,
            onClick = onCancel,
            enabled = cancelEnabled,
            modifier = Modifier.weight(1f),
        )
        AiTextButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f),
            primary = true,
            colors = if (destructive) {
                ButtonDefaults.textButtonColorsPrimary(
                    color = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.textButtonColorsPrimary()
            },
        )
    }
}

/** AI 页统一弹窗：随内容的 Overlay 弹窗，圆角与内外边距统一。 */
@Composable
fun AiDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!show) return
    OverlayDialog(
        show = true,
        title = title,
        summary = summary,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
        onDismissRequest = onDismissRequest,
        insideMargin = AiUiDefaults.DialogInsideMargin,
        outsideMargin = AiUiDefaults.DialogOutsideMargin,
        content = content,
    )
}

/** 页面底部留白，避免内容贴住导航栏。 */
@Composable
fun AiPageBottomSpacer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .defaultMinSize(minHeight = 24.dp)
            .navigationBarsPadding(),
    )
}

/** 顶部搜索框（MIUIX 胶囊样式）。 */
@Composable
fun AiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    top.yukonga.miuix.kmp.basic.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        label = label,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 顶栏返回按钮（圆角方形，与 MIUIX 一致）。 */
@Composable
fun AiBackButton(onClick: () -> Unit) {
    top.yukonga.miuix.kmp.basic.IconButton(onClick = onClick) {
        Icon(
            imageVector = AiArrowRight,
            contentDescription = "返回",
            modifier = Modifier.size(14.dp, 14.dp).graphicsLayer { rotationZ = 180f },
        )
    }
}

/** 空状态文字。 */
@Composable
fun AiEmptyText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 勾选状态图标：选中为主色实心勾，未选中为次级空心圈。 */
@Composable
fun AiSelectionIcon(selected: Boolean) {
    Icon(
        imageVector = if (selected) AiSelectionChecked else AiSelectionUnchecked,
        contentDescription = if (selected) "当前使用" else "设为当前",
        tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

private val AiSelectionChecked: ImageVector by lazy {
    ImageVector.Builder(
        name = "AiSelectionChecked",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 40f,
        viewportHeight = 40f,
    ).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
            moveTo(9f, 21f)
            lineTo(17f, 29f)
            lineTo(31f, 12f)
        }
    }.build()
}

private val AiSelectionUnchecked: ImageVector by lazy {
    ImageVector.Builder(
        name = "AiSelectionUnchecked",
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 40f,
        viewportHeight = 40f,
    ).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.4f) {
            moveTo(20f, 4f)
            arcTo(16f, 16f, 0f, true, true, 19.9f, 4f)
            close()
        }
    }.build()
}

/** 复选框（多选模型等场景）。 */
@Composable
fun AiCheckbox(checked: Boolean, enabled: Boolean = true) {
    top.yukonga.miuix.kmp.basic.Checkbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = null,
        enabled = enabled,
    )
}

/** AI 二级页统一外壳：毛玻璃大标题顶栏 + 返回按钮 + 内容内边距。 */
@Composable
fun AiPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    GkPageScaffold(
        topBar = {
            GkTopAppBar(
                titleText = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = { AiBackButton(onClick = onBack) },
            )
        },
        content = content,
    )
}
