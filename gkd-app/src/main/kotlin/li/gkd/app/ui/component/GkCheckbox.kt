package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox

/** 布尔状态适配 Miuix 的三态复选框。 */
@Composable
fun GkCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    key: Any? = null,
    enabled: Boolean = true,
) = androidx.compose.runtime.key(key) {
    Checkbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = onCheckedChange?.let { cb -> { cb(!checked) } },
        modifier = modifier,
        enabled = enabled,
    )
}
