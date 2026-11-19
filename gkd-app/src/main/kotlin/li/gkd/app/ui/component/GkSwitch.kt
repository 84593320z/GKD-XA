package li.gkd.app.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import li.gkd.app.text.UiStrings
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Switch

/** Miuix 开关，保留状态的读屏提示。 */
@Composable
fun GkSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    key: Any? = null,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) = key(key) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { throttle(it) },
        modifier = modifier.semantics {
            stateDescription = if (checked) UiStrings.turned_on else UiStrings.turned_off
        },
        enabled = enabled,
    )
}
