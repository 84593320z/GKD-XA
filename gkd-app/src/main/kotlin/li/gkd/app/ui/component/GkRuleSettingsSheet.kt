package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SheetState
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.rememberModalBottomSheetState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import li.gkd.app.text.UiStrings
import li.gkd.app.domain.rule.RuleControlState
import li.gkd.app.data.ExcludeData
import li.gkd.app.domain.rule.RuleSetting
import li.gkd.app.ui.icon.ResetSettings

@Composable
fun GkRuleSettingsSheet(
    title: String,
    onDismissRequest: () -> Unit,
    titleContent: @Composable () -> Unit = {
        Text(title, modifier = Modifier.fillMaxWidth(), style = MiuixTheme.textStyles.title3)
    },
    subtitle: String? = null,
    subtitleContent: @Composable () -> Unit = {
        if (subtitle != null) {
            Text(subtitle, style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    },
    footerLeadingContent: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    headerBottomPadding: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    GkModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = headerBottomPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                titleContent()
                subtitleContent()
            }
            content()
            if (footerLeadingContent != null || actions != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(end = 8.dp)) {
                        footerLeadingContent?.invoke()
                    }
                    actions?.invoke()
                }
            }
        }
    }
}

@Composable
fun GkRuleSettingsContent(
    state: RuleControlState,
    onSettingChange: (RuleSetting) -> Unit,
    title: String = UiStrings.rule_enable,
    onViewControl: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MiuixTheme.textStyles.title3)
                // Keep restrictions visible independently of the saved switch value.
                val restriction = RulePropertyText.restrictionSummary(state)
                if (restriction != null) {
                    Text(restriction, style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
            if (onViewControl != null) {
                GkIconButton(
                    imageVector = GkIcons.Flowchart,
                    contentDescription = UiStrings.rule_control_view,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    ),
                    onClick = onViewControl,
                )
            }
            GkIconButton(
                imageVector = ResetSettings,
                contentDescription = UiStrings.settings_reset_default,
                enabled = state.hasCustomSetting,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                ),
                onClick = { onSettingChange(RuleSetting.FollowDefault) },
            )
            GkRuleEnableControl(state, onSettingChange,
                showCustomSettingIcon = false,
                modifier = Modifier.width(84.dp).fillMaxHeight())
        }
    }
}

@Composable
fun GkRuleExclusionsCard(
    exclude: ExcludeData,
    appId: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (appId == null) RuleProperty.Personal.label else UiStrings.page_exclusion, style = MiuixTheme.textStyles.title3)
                val summary = RulePropertyText.personalSummary(exclude, appId)
                if (summary != null) {
                    Text(summary, style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
            GkRulePropertyIcon(RuleProperty.Personal, modifier = Modifier.size(24.dp), contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}
