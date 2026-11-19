package li.gkd.app.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import li.gkd.app.MainActivity
import li.gkd.app.ui.style.GkShapes
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
    Box(modifier = modifier) {
        GkTextFieldSelection {
            // Full-page editors need top-aligned, internally scrolling text, not form-field chrome.
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .autoFocus(immediateFocus = immediateFocus)
                    .fillMaxSize()
                    .optimizedImePadding()
                    .background(MiuixTheme.colorScheme.surface),
                textStyle = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
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
        val actualSize = indicatorSize ?: text.length
        if (actualSize > 0 && text.isNotEmpty()) {
            Text(
                text = actualSize.toString(),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(GkShapes.compact)
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 2.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
    val context = LocalActivity.current as MainActivity
    if (context.imeController.showAnimationRunningFlow.collectAsStateWithLifecycle().value) {
        this
    } else {
        imePadding()
    }
}
