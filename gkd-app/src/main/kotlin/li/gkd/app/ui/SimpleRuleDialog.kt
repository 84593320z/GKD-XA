package li.gkd.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
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
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 简易规则输入与规则编辑器共用表单标签和操作按钮，保留原有提交与示例行为。
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap),
            ) {
                GkFormTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_text_label,
                    singleLine = true,
                )
                GkFormTextField(
                    value = idValue,
                    onValueChange = { idValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = UiStrings.simple_rule_id_label,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                GkTextSwitch(
                    modifier = Modifier.fillMaxWidth().heightIn(min = GkUi.MinTouchTarget),
                    paddingDisabled = true,
                    title = UiStrings.simple_rule_back_close,
                    subtitle = UiStrings.simple_rule_back_close_desc,
                    checked = backToClose,
                    onCheckedChange = { backToClose = it },
                )
                if (showExample) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = UiStrings.simple_rule_example_title,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.primary,
                        )
                        Text(
                            text = UiStrings.simple_rule_example_content,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            GkActionButton(
                emphasized = true,
                enabled = textValue.isNotBlank() || idValue.isNotBlank(),
                onClick = throttle { onSubmit(textValue, idValue, backToClose) },
            ) { Text(UiStrings.simple_rule_add) }
        },
        dismissButton = {
            GkActionButton(onClick = { showExample = !showExample }) {
                Text(UiStrings.simple_rule_example)
            }
            GkActionButton(onClick = throttle(fn = onDismissRequest)) {
                Text(UiStrings.simple_rule_cancel)
            }
        },
    )
}
