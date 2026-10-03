package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统一输入框：底层是官方 miuix [TextField]，但保留旧版 Material3 风格的调用参数
 * （字符串 label / placeholder、支持文字、错误态、矩形无边框等），方便逐页迁移。
 *
 * 与 MIUIX 的差异只有一处：miuix 没有独立的 placeholder 槽位，
 * 因此 [label] 为空时用 [placeholder] 顶替（`useLabelAsPlaceholder`）。
 * 两者都传时只显示 label —— 这是 MIUIX 的惯用写法。
 */
@Composable
fun GkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    /** 仅用于兼容旧调用：`RectangleShape` 表示无边框（编辑器场景），其余忽略。 */
    shape: Shape? = null,
) {
    val borderless = shape == RectangleShape
    val colors = TextFieldDefaults.textFieldColors(
        backgroundColor = if (borderless) Color.Transparent else MiuixTheme.colorScheme.secondaryContainer,
        borderColor = when {
            borderless -> Color.Transparent
            isError -> MiuixTheme.colorScheme.error
            else -> MiuixTheme.colorScheme.primary
        },
    )
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label ?: placeholder.orEmpty(),
            useLabelAsPlaceholder = true,
            modifier = fieldModifier,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = colors,
        )
    }
    if (supportingText == null) {
        field(modifier)
        return
    }
    Column(modifier = modifier) {
        field(Modifier.fillMaxWidth())
        supportingText?.let { hint ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp)) { hint() }
        }
    }
}
