package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox

/**
 * 复选框（MIUIX 渲染版）。
 *
 * 上游底座用 Material3 [androidx.compose.material3.Checkbox] 渲染；
 * 本版本改用 MIUIX [Checkbox]，以达成「外观用 MIUIX」的目标。
 *
 * 说明：MIUIX 的 Checkbox 以 [ToggleableState] 表达状态，此处做 Boolean 适配；
 * 原 `colors: CheckboxColors` 参数已移除（底座内无调用方传入）。
 */
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
