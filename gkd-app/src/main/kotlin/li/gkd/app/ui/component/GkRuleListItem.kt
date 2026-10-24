package li.gkd.app.ui.component

import li.gkd.app.text.UiStrings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun GkRuleListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedMode: Boolean = false,
    selected: Boolean = false,
    highlighted: Boolean = false,
    selectable: Boolean = true,
    selectionEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    handlesLongPress: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onSelect: () -> Unit = {},
    legacyStyle: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable (Modifier) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = if (selected || highlighted) MiuixTheme.colorScheme.primaryContainer
            else MiuixTheme.colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = 300),
        label = "Rule card background",
    )
    val cardModifier = if (legacyStyle) {
        modifier.padding(horizontal = 12.dp).fillMaxWidth()
    } else {
        modifier.padding(horizontal = 8.dp, vertical = 3.dp).fillMaxWidth()
    }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.defaultColors(color = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                .then(
                    if (!selectedMode || selectable) {
                        Modifier.combinedClickable(
                            interactionSource = clickInteractionSource,
                            indication = LocalIndication.current,
                            enabled = !selectedMode || selectionEnabled,
                            onClick = if (selectedMode) onSelect else onClick,
                            onLongClick = if (handlesLongPress && selectable && selectionEnabled) onLongClick else null,
                            onClickLabel = if (selectedMode) UiStrings.selection_toggle else UiStrings.details_view,
                        )
                    } else {
                        Modifier
                    }
                ).semantics {
                    // 长按被拖拽手势接管时，给无障碍留一条"进入多选"的动作
                    if (!handlesLongPress && selectable && selectionEnabled && onLongClick != null) {
                        this.onLongClick(label = UiStrings.selection_mode_enter) {
                            onLongClick()
                            true
                        }
                    }
                    if (selectedMode && selectable) {
                        role = Role.Checkbox
                        this.selected = selected
                    }
                }.heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f)
                    .then(
                        if (legacyStyle) Modifier.padding(8.dp)
                        else Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                leading?.invoke()
                Column(Modifier.weight(1f), content = content)
            }
            // Include the switch's surrounding space in its hit target, up to the card edges.
            // 52 dp switch + 8 dp on either side matches the content inset.
            Box(Modifier.width(68.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                if (selectedMode) {
                    if (selectable) GkCheckbox(selected, onCheckedChange = null, enabled = selectionEnabled)
                } else {
                    trailing(Modifier.fillMaxSize())
                }
            }
        }
    }
}
