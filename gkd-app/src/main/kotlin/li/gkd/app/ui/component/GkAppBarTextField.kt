package li.gkd.app.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkAppBarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MiuixTheme.textStyles.title3.copy(color = LocalContentColor.current)

    // Start at the end, then retain IME composition and selection across external text updates.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    textFieldValue = textFieldValue.copy(text = value)

    GkTextFieldSelection {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                // Preserve the existing single-line search sanitization.
                onValueChange(it.text.replace("\n", ""))
            },
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            textStyle = textStyle,
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                // No Material indicator or container: the surrounding Miuix app bar is the chrome.
                Box(
                    modifier = Modifier.padding(bottom = 4.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = textStyle,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
