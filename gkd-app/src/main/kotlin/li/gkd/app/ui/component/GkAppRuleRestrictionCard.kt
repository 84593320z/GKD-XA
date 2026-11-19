package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkAppRuleRestrictionCard(
    whitelisted: Boolean,
    partialDisabled: Boolean,
    partialFollowsWhitelist: Boolean,
    onRemoveWhitelist: () -> Unit,
    onRemovePartialDisable: () -> Unit,
) {
    if (whitelisted || partialDisabled) {
        Card(
            modifier = Modifier.padding(horizontal = GkUi.PageHorizontal, vertical = GkUi.SectionGap / 2).fillMaxWidth(),
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
        ) {
            if (whitelisted) {
                RestrictionRow(UiStrings.whitelist_member,
                    if (partialFollowsWhitelist) UiStrings.app_rule_whitelist_follow_notice else UiStrings.app_rule_whitelist_notice,
                    UiStrings.whitelist_remove, onRemoveWhitelist)
            }
            if (partialDisabled) {
                RestrictionRow(UiStrings.app_rule_partial_disable_title, UiStrings.app_rule_partial_disable_notice,
                    UiStrings.app_rule_partial_disable_remove, onRemovePartialDisable)
            }
        }
    }
}

@Composable
private fun RestrictionRow(title: String, description: String, action: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = GkUi.ContentPadding, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        GkIcon(GkIcons.Info, Modifier.size(GkUi.IconSize), contentDescription = null)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MiuixTheme.textStyles.body1)
            Text(description, style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        GkIconButton(GkIcons.RemoveCircleOutline, onClick = onRemove, contentDescription = action)
    }
}
