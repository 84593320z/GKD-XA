package li.gkd.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.META
import li.gkd.app.MainViewModel
import li.gkd.app.data.RawSubscription
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.formatTimeAgo
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.db.SubsItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun SubsItemCard(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    subsItem: SubsItem,
    subscription: RawSubscription?,
    index: Int,
    isSelectedMode: Boolean,
    isSelected: Boolean,
    matchingEnabled: Boolean = true,
    selectionEnabled: Boolean = true,
    handlesLongPress: Boolean = true,
    refreshing: Boolean = false,
    loadError: Exception? = null,
    refreshError: Exception? = null,
    onCheckedChange: ((Boolean) -> Unit),
    onSelect: (() -> Unit)? = null,
    onSelectedChange: (() -> Unit)? = null,
) {
    val mainVm = MainViewModel.requireCurrent()
    val dragged by interactionSource.collectIsDraggedAsState()
    val onClick = {
        if (!dragged) {
            if (isSelectedMode) {
                if (selectionEnabled) onSelectedChange?.invoke()
            } else if (!refreshing) {
                mainVm.subsSheet.show(subsItem.id)
            }
        }
    }
    val containerColor = animateColorAsState(
        if (isSelected) {
            MiuixTheme.colorScheme.primaryContainer
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        },
        tween()
    )
    // 点击与按压反馈必须挂在卡片**内部**：MIUIX 的 Card 用 squircleSurface 把子树裁成胶囊形，
    // 挂在卡片外层 Modifier 上的 combinedClickable，其涟漪绘制在卡片背景之下、且不受裁剪，
    // 只在四个圆角外溢出，表现为「按压后颜色加深区域露出四个尖角」。
    Card(
        modifier = modifier
            .padding(horizontal = GkUi.PageHorizontal, vertical = GkUi.SectionGap / 2)
            .fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = containerColor.value
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = !isSelectedMode || selectionEnabled,
                    onClick = onClick,
                    onLongClick = if (handlesLongPress && selectionEnabled) {
                        { (onSelect ?: onSelectedChange)?.invoke() }
                    } else {
                        null
                    },
                )
                .semantics {
                    stateDescription = when {
                        isSelectedMode -> if (isSelected) UiStrings.selected else UiStrings.not_selected
                        !matchingEnabled -> UiStrings.subscription_switch_paused_description
                        else -> if (subsItem.enable) UiStrings.enabled else UiStrings.disabled
                    }
                    if (isSelectedMode) {
                        selected = isSelected
                        toggleableState = ToggleableState(isSelected)
                        role = Role.Checkbox
                    }
                    this.onClick(
                        label = if (isSelectedMode) {
                            if (isSelected) UiStrings.selection_deselect else UiStrings.selection_select
                        } else {
                            UiStrings.subscription_details_view
                        },
                        action = null,
                    )
                    if (selectionEnabled) {
                        this.onLongClick(
                            label = if (isSelectedMode) {
                                UiStrings.selection_select
                            } else {
                                UiStrings.selection_mode_enter
                            },
                        ) {
                            (onSelect ?: onSelectedChange)?.invoke()
                            true
                        }
                    }
                }
                .padding(start = GkUi.ContentPadding, top = 12.dp, bottom = 12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (subscription != null) {
                    Text(
                        modifier = Modifier.semantics {
                            contentDescription = "订阅顺序：$index, 订阅名称 ${subscription.name}"
                        },
                        text = "$index. ${subscription.name}",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MiuixTheme.textStyles.body1,
                    )
                    Text(
                        text = subscription.numText,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (subsItem.id >= 0) {
                            if (subscription.author != null) {
                                Text(
                                    modifier = Modifier.semantics {
                                        contentDescription = "作者 ${subscription.author}"
                                    },
                                    text = subscription.author,
                                    style = MiuixTheme.textStyles.footnote1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            Text(
                                modifier = Modifier.semantics {
                                    contentDescription = "订阅版本号 ${subscription.version}"
                                },
                                text = "v" + (subscription.version.toString()),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        } else {
                            Text(
                                modifier = Modifier.clearAndSetSemantics {},
                                text = META.appName,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        val timeStr = formatTimeAgo(subsItem.mtime)
                        Text(
                            modifier = Modifier.semantics {
                                contentDescription = "更新时间 $timeStr"
                            },
                            text = timeStr,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                } else {
                    Text(
                        text = "id=${subsItem.id}",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MiuixTheme.textStyles.body2,
                    )
                    val color = if (loadError != null) {
                        MiuixTheme.colorScheme.error
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                    Text(
                        text = loadError?.message
                            ?: if (refreshing) "加载中…" else "文件不存在",
                        style = MiuixTheme.textStyles.body2,
                        color = color
                    )
                }
                if (refreshError != null) {
                    Text(
                        text = "更新错误：${refreshError.message ?: UiStrings.data_load_failed}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error
                    )
                }
            }
            Box(
                Modifier.width(68.dp).heightIn(min = GkUi.MinTouchTarget)
                    .semantics { contentDescription = subscription?.name ?: subsItem.id.toString() }
                    .then(if (isSelectedMode) Modifier else Modifier.toggleable(
                        value = subsItem.enable,
                        role = Role.Switch,
                        onValueChange = throttle(fn = onCheckedChange),
                    )),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelectedMode) {
                    GkCheckbox(checked = isSelected, enabled = selectionEnabled,
                        modifier = Modifier.clearAndSetSemantics {})
                } else {
                    PerfSwitch(
                        modifier = Modifier.clearAndSetSemantics {},
                        key = subsItem.id,
                        checked = subsItem.enable,
                        onCheckedChange = throttle(fn = onCheckedChange),
                    )
                }
            }
        }
    }
}
