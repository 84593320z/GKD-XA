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

/**
 * 开关控件（MIUIX 渲染版）。
 *
 * 上游底座用 Material3 [androidx.compose.material3.Switch] 渲染；
 * 本版本改用 MIUIX [Switch]，达成「外观用 MIUIX」的目标。
 *
 * 说明：原 Material3 版的 `colors: SwitchColors` 参数已移除——底座内
 * 无调用方传入该参数，改用 MIUIX 主题自带的开关配色。
 */
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
