package li.gkd.app.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldColors
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Native Miuix input with a persistent label and an independent placeholder/supporting row. */
@Composable
internal fun GkFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelMaxLines: Int = Int.MAX_VALUE,
    labelOverflow: TextOverflow = TextOverflow.Clip,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String = UiStrings.error_label,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    GkTextFieldSelection {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (label != null) {
                Text(
                    text = label,
                    // The editable node below owns the accessible label, not a separate sibling.
                    modifier = Modifier.clearAndSetSemantics {},
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = labelMaxLines,
                    overflow = labelOverflow,
                    color = when {
                        !enabled -> colors.disabledOnSurface
                        isError -> colors.error
                        else -> colors.onSurfaceVariantSummary
                    },
                )
            }
            Box {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            if (label != null) contentDescription = label
                            if (isError) error(errorMessage)
                        },
                    label = placeholder,
                    useLabelAsPlaceholder = true,
                    enabled = enabled,
                    textStyle = MiuixTheme.textStyles.main.copy(
                        color = if (enabled) colors.onSurface else colors.disabledOnSurface,
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        labelColor = if (enabled) colors.onSurfaceVariantSummary else colors.disabledOnSurface,
                        borderColor = if (isError) colors.error else colors.primary,
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                )
                if (isError) {
                    // Native Miuix only outlines a focused field. Draw validation above its
                    // background as well, without intercepting gestures or changing its size.
                    Box(
                        modifier = Modifier.matchParentSize().squircleBorder(
                            width = 1.dp,
                            color = colors.error,
                            cornerRadius = TextFieldDefaults.CornerRadius,
                        ),
                    )
                }
            }
            if (supportingText != null) {
                CompositionLocalProvider(
                    LocalContentColor provides if (isError) colors.error else colors.onSurfaceVariantSummary,
                ) {
                    supportingText()
                }
            }
        }
    }
}

/**
 * Slot-compatible input for existing callers that need composable labels/placeholders.
 * Miuix 0.9.4 accepts only String labels, so retain Foundation editing/decoration here rather
 * than losing those slots or retaining Material's field defaults. New forms use [GkFormTextField].
 */
@Composable
fun GkOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MiuixTheme.textStyles.main,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    cornerRadius: Dp = TextFieldDefaults.CornerRadius,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val scheme = MiuixTheme.colorScheme
    val contentColor = if (enabled) scheme.onSurface else scheme.disabledOnSurface
    val accentColor = if (isError) scheme.error else colors.borderColor
    val transformedText = remember(value, visualTransformation) {
        visualTransformation.filter(AnnotatedString(value)).text
    }
    GkTextFieldSelection {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.semantics { if (isError) error(UiStrings.error_label) },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.copy(color = textStyle.color.takeOrElse { contentColor }),
            cursorBrush = SolidColor(accentColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactions,
            decorationBox = { innerTextField ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (label != null) {
                        CompositionLocalProvider(LocalContentColor provides if (isError) scheme.error else colors.labelColor) {
                            label()
                        }
                    }
                    Row(
                        modifier = Modifier
                            .squircleBackground(colors.backgroundColor, cornerRadius)
                            .squircleBorder(
                                width = if (focused) 2.dp else if (isError) 1.dp else 0.dp,
                                color = accentColor,
                                cornerRadius = cornerRadius,
                            )
                            .padding(contentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        leadingIcon?.invoke()
                        prefix?.invoke()
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                            if (transformedText.isEmpty() && placeholder != null) {
                                CompositionLocalProvider(LocalContentColor provides colors.labelColor) {
                                    placeholder()
                                }
                            }
                            innerTextField()
                        }
                        suffix?.invoke()
                        trailingIcon?.invoke()
                    }
                    supportingText?.invoke()
                }
            },
        )
    }
}

/** Keep Foundation and native Miuix inputs on the same theme-aware selection palette. */
@Composable
internal fun GkTextFieldSelection(content: @Composable () -> Unit) {
    val primary = MiuixTheme.colorScheme.primary
    val selectionColors = remember(primary) {
        TextSelectionColors(handleColor = primary, backgroundColor = primary.copy(alpha = 0.4f))
    }
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors, content = content)
}
