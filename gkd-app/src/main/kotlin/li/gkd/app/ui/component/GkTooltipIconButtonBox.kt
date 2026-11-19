package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.ui.share.LocalIsTalkbackEnabled
import top.yukonga.miuix.kmp.basic.PlainTooltip
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.TooltipState
import top.yukonga.miuix.kmp.basic.rememberTooltipState

/**
 * Native Miuix anchored text tooltip. [state] is a Miuix [TooltipState], created with Miuix's
 * [rememberTooltipState]; `scope.launch { state.show() }` supports click-to-show metadata labels.
 * Use [TooltipAnchorPosition.Above] for timestamps and [enableUserInput] = false for click-only
 * anchors (e.g. selectable snapshot rows). The native popup owns positioning, timeout and dismissal;
 * it does not add a click action to the anchor or navigate anywhere.
 */
@Composable
fun GkTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    enableUserInput: Boolean = true,
    focusable: Boolean = false,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        tooltip = { PlainTooltip { Text(text = text) } },
        state = state,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        modifier = modifier,
        enableUserInput = enableUserInput,
        focusable = focusable,
        content = content,
    )
}

@Composable
fun GkTooltipIconButtonBox(contentDescription: String?, content: @Composable () -> Unit) {
    // 视障用户使用 TalkBack 朗读 contentDescription，不需要 Tooltip
    if (contentDescription.isNullOrEmpty() || LocalIsTalkbackEnabled.current) {
        content()
    } else {
        GkTooltipBox(
            text = contentDescription,
            positioning = TooltipAnchorPosition.Start,
            content = content,
        )
    }
}
