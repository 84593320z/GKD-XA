package li.gkd.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 简易规则弹窗：输入用于定位弹窗的文字 / 控件 ID，并选择是否通过返回键关闭。
 * 组件与写法跟随 XA 既有弹窗（如 AiProviderModelsTab 的模型编辑弹窗）。
 * 来源：gkd-plus 7e9a38e 的 SimpleRuleDialog。
 */
@Composable
fun SimpleRuleDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (text: String, id: String, backToClose: Boolean) -> Unit,
) {
    var textValue by remember { mutableStateOf("") }
    var idValue by remember { mutableStateOf("") }
    var backToClose by remember { mutableStateOf(false) }
    var showExample by remember { mutableStateOf(false) }

    GkAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onDismissRequest,
        title = { Text(text = UiStrings.simple_rule_title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_text_label,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = idValue,
                    onValueChange = { idValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_id_label,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                Spacer(modifier = Modifier.height(8.dp))
                GkTextSwitch(
                    title = UiStrings.simple_rule_back_close,
                    subtitle = UiStrings.simple_rule_back_close_desc,
                    checked = backToClose,
                    onCheckedChange = { backToClose = it },
                )
                if (showExample) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = UiStrings.simple_rule_example_title,
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UiStrings.simple_rule_example_content,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                text = UiStrings.simple_rule_add,
                enabled = textValue.isNotBlank() || idValue.isNotBlank(),
                onClick = throttle { onSubmit(textValue, idValue, backToClose) },
            )
        },
        dismissButton = {
            TextButton(
                text = UiStrings.simple_rule_example,
                onClick = { showExample = !showExample },
            )
            TextButton(
                text = UiStrings.simple_rule_cancel,
                onClick = throttle(fn = onDismissRequest),
            )
        },
    )
}
