package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.data.RawSubscription
import li.gkd.app.domain.rule.RuleControlState
import li.gkd.app.domain.rule.RuleSetting
import li.gkd.app.domain.rule.toRuleGroupTarget
import li.gkd.app.domain.rule.toSwitchTarget
import li.gkd.app.util.TimeUtils.throttle


@Composable
fun GkRuleGroupCard(
    modifier: Modifier = Modifier,
    subs: RawSubscription,
    appId: String?,
    group: RawSubscription.RawGroupProps,
    control: RuleControlState,
    onOpen: () -> Unit,
    onSettingChange: (RuleSetting) -> Unit,
    highlighted: Boolean = false,
    hideCategoryPrefix: Boolean = false,
    isSelectedMode: Boolean = false,
    isSelected: Boolean = false,
    selectionEnabled: Boolean = true,
    onLongClick: () -> Unit = {},
    onSelectedChange: () -> Unit = {},
    legacyStyle: Boolean = false,
) {
    GkRuleListItem(
        modifier = modifier, onClick = throttle(onOpen),
        selectedMode = isSelectedMode, selected = isSelected, highlighted = highlighted,
        selectable = control.canEnable,
        selectionEnabled = selectionEnabled,
        onLongClick = onLongClick, onSelect = onSelectedChange,
        legacyStyle = legacyStyle,
        trailing = {
            GkRuleEnableControl(
                control,
                onSettingChange,
                modifier = if (legacyStyle) Modifier.padding(8.dp).minimumInteractiveComponentSize() else it,
                identity = group.toRuleGroupTarget(subs.id, appId).toSwitchTarget(),
            )
        },
    ) {
        val description = if (!group.valid) group.errorDesc ?: UiStrings.rule_format_error
            else group.desc?.takeIf { it.isNotBlank() }
        if (legacyStyle) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = if (control.limitations.hasPersonalProperties) 12.dp else 0.dp),
                ) {
                    GkGroupNameText(
                        text = group.name,
                        modifier = Modifier.fillMaxWidth(),
                        style = MiuixTheme.textStyles.body1,
                        isGlobal = group is RawSubscription.RawGlobalGroup,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (description != null) {
                        top.yukonga.miuix.kmp.basic.Text(
                            text = description,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                            style = MiuixTheme.textStyles.body2,
                            color = if (group.valid) MiuixTheme.colorScheme.onSurfaceVariantSummary
                                else MiuixTheme.colorScheme.error,
                        )
                    }
                }
                if (control.limitations.hasPersonalProperties) {
                    GkIcon(
                        imageVector = GkIcons.Block,
                        contentDescription = UiStrings.rule_scope_and_exclusions,
                        tint = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(8.dp),
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GkGroupNameText(text = group.name, modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body1,
                    isGlobal = group is RawSubscription.RawGlobalGroup, maxLines = 2,
                    categoryName = if (group is RawSubscription.RawAppGroup) subs.getCategory(group.name)?.name else null,
                    hideCategoryPrefix = hideCategoryPrefix,
                    overflow = TextOverflow.Ellipsis)
                if (description == null) GkRulePropertyIndicators(control)
            }
            if (description != null) {
                GkRuleSupportingContent(control, text = description,
                    textColor = if (group.valid) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.error)
            }
        }
    }
}
