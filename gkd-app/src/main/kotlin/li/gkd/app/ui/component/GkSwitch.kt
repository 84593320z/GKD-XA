package li.gkd.app.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Switch

/** Native track/thumb geometry, one stateful semantic target, at least 48dp to tap. */
@Composable
fun GkSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    key: Any? = null,
    @Suppress("UNUSED_PARAMETER") thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) = key(key) {
    val change = onCheckedChange?.let { throttle(fn = it) }
    val interaction = if (change != null) Modifier.toggleable(
        value = checked, enabled = enabled, role = Role.Switch,
        interactionSource = interactionSource, indication = null, onValueChange = change,
    ) else Modifier
    Box(
        modifier = modifier.sizeIn(minWidth = GkUi.MinTouchTarget, minHeight = GkUi.MinTouchTarget)
            .then(interaction).semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                stateDescription = if (checked) UiStrings.turned_on else UiStrings.turned_off
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = change,
            modifier = Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {},
            enabled = enabled,
        )
    }
}
