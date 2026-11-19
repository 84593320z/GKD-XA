package li.gkd.app.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
inline fun GkMenuGroupCard(inTop: Boolean = false, title: String, content: @Composable () -> Unit) {
    SmallTitle(
        text = title,
        modifier = Modifier.padding(top = if (inTop) 4.dp else 8.dp),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    )
    content()
}

@Composable
fun GkMenuItemCheckbox(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    PerfDropdownMenuItem(
        text = text,
        modifier = Modifier.semantics {
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        },
        role = Role.Checkbox,
        endActions = {
            // The whole menu row owns the action and checked semantics, not a second tap target.
            GkCheckbox(
                modifier = Modifier.clearAndSetSemantics {},
                checked = checked,
                enabled = enabled,
            )
        },
        onClick = throttle(onClick),
        enabled = enabled,
    )
}

@Composable
fun GkMenuItemCheckbox(
    text: String,
    stateFlow: MutableStateFlow<Boolean>,
    enabled: Boolean = true,
) = GkMenuItemCheckbox(
    text = text,
    checked = stateFlow.collectAsStateWithLifecycle().value,
    onClick = { stateFlow.update { !it } },
    enabled = enabled,
)

@Composable
fun GkMenuItemRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    PerfDropdownMenuItem(
        text = text,
        modifier = Modifier.semantics { this.selected = selected },
        role = Role.RadioButton,
        endActions = {
            RadioButton(
                modifier = Modifier.clearAndSetSemantics {},
                selected = selected,
                onClick = null,
                enabled = enabled,
            )
        },
        onClick = throttle(onClick),
        enabled = enabled,
    )
}
