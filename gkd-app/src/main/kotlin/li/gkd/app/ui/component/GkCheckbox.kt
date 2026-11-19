package li.gkd.app.ui.component

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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.basic.Checkbox

/** Native checkbox dimensions and animation, shared by all Boolean checkbox entry points. */
@Composable
fun GkCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    key: Any? = null,
    enabled: Boolean = true,
) = key(key) {
    val interaction = onCheckedChange?.let { change -> Modifier.toggleable(
        value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = change,
    ) } ?: Modifier
    Box(
        modifier = modifier.sizeIn(minWidth = GkUi.MinTouchTarget, minHeight = GkUi.MinTouchTarget)
            .then(interaction).semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(checked)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Checkbox(
            state = ToggleableState(checked),
            onClick = onCheckedChange?.let { { it(!checked) } },
            modifier = Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {},
            enabled = enabled,
        )
    }
}
