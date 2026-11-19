package li.gkd.app.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import li.gkd.app.MainActivity
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkMultiTextField(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    immediateFocus: Boolean = false,
    indicatorSize: Int? = null,
    placeholderText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.optimizedImePadding()) {
        val actualSize = indicatorSize ?: text.length
        if (actualSize > 0 && text.isNotEmpty()) {
            Text(actualSize.toString(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = GkUi.ContentPadding, vertical = 4.dp),
                style = MiuixTheme.textStyles.footnote1,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        GkTextFieldSelection {
            // Full-page editors need top-aligned, internally scrolling text, not form-field chrome.
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .autoFocus(immediateFocus = immediateFocus)
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .semantics {
                        placeholderText?.let { contentDescription = it }
                    },
                textStyle = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(GkUi.ContentPadding),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        if (text.isEmpty() && placeholderText != null) {
                            Text(
                                text = placeholderText,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

    }
}

@Composable
fun GkMultiTextField(
    modifier: Modifier = Modifier,
    textFlow: MutableStateFlow<String>,
    immediateFocus: Boolean = false,
    indicatorSize: Int? = null,
    placeholderText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val text by textFlow.collectAsStateWithLifecycle()
    GkMultiTextField(
        modifier = modifier,
        text = text,
        onTextChange = { textFlow.value = it },
        immediateFocus = immediateFocus,
        indicatorSize = indicatorSize,
        placeholderText = placeholderText,
        visualTransformation = visualTransformation,
    )
}

private fun Modifier.optimizedImePadding() = composed {
    val context = LocalActivity.current as? MainActivity
    if (context != null && context.imeController.showAnimationRunningFlow.collectAsStateWithLifecycle().value) {
        this
    } else {
        imePadding()
    }
}
