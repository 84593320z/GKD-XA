package li.gkd.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// A marker, not an action: explicit onClick = null still means "switch-only interaction".
internal val DefaultSwitchClick: () -> Unit = {}

@Composable
fun GkTextSwitch(
    modifier: Modifier = Modifier,
    title: String,
    paddingDisabled: Boolean = false,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    checked: Boolean = true,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = DefaultSwitchClick,
    onClickLabel: String? = UiStrings.switch_state_description(title),
) {
    val togglesRow = onClick === DefaultSwitchClick
    val change = onCheckedChange?.let { throttle(fn = it) }
    val rowClick = when {
        !enabled -> null
        togglesRow -> change?.let { { it(!checked) } }
        else -> onClick?.let { throttle(fn = it) }
    }
    BasicComponent(
        modifier = modifier.semantics(mergeDescendants = togglesRow) {
            if (!enabled) disabled()
            if (togglesRow) {
                toggleableState = ToggleableState(checked)
                stateDescription = if (checked) UiStrings.turned_on else UiStrings.turned_off
            }
        },
        insideMargin = if (paddingDisabled) PaddingValues(0.dp) else BasicComponentDefaults.InsideMargin,
        enabled = enabled,
        onClick = rowClick,
        onClickLabel = onClickLabel,
        role = if (togglesRow) Role.Switch else if (rowClick != null) Role.Button else null,
        endActions = {
            suffixIcon?.invoke()
            GkSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = change,
                modifier = if (togglesRow) Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {} else Modifier.semantics {
                    contentDescription = title
                },
            )
        },
    ) {
        Text(title, style = MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSurface)
        GkPreferenceSummary(subtitle, suffix, suffixUnderline, onSuffixClick, enabled)
    }
}
