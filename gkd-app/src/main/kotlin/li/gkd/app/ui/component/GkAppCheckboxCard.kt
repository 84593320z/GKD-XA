package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.data.AppInfo
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkAppCheckboxCard(
    appInfo: AppInfo,
    checked: Boolean,
    onCheckedChange: (() -> Unit),
) {
    val toggle = throttle(onCheckedChange)
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = GkUi.PageHorizontal, vertical = GkUi.SectionGap / 2),
        colors = CardDefaults.defaultColors(color = if (checked) MiuixTheme.colorScheme.primaryContainer
            else MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { toggle() })
                .clearAndSetSemantics {
                    contentDescription = UiStrings.app_name_description(appInfo.name)
                    role = Role.Checkbox
                    toggleableState = ToggleableState(checked)
                    stateDescription = if (checked) UiStrings.list_member else UiStrings.list_not_member
                    onClick(label = if (checked) UiStrings.list_remove else UiStrings.list_add) {
                        toggle()
                        true
                    }
                }
                .heightIn(min = 72.dp)
                .padding(start = GkUi.ContentPadding, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GkAppIcon(appId = appInfo.id)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GkAppNameText(appId = appInfo.id, appInfo = appInfo)
                Text(
                    text = appInfo.id,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
            Box(Modifier.width(68.dp).heightIn(min = GkUi.MinTouchTarget), contentAlignment = Alignment.Center) {
                GkCheckbox(key = appInfo.id, checked = checked)
            }
        }
    }
}
