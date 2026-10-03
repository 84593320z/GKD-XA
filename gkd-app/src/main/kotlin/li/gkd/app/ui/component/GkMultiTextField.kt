package li.gkd.app.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import li.gkd.app.MainActivity

@Composable
fun GkMultiTextField(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    immediateFocus: Boolean = false,
    indicatorSize: Int? = null,
    placeholderText: String? = null,
) {
    Box(modifier = modifier) {
        val modifier = Modifier
            .autoFocus(immediateFocus = immediateFocus)
            .fillMaxSize()
            .optimizedImePadding()
        GkTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = placeholderText,
            modifier = modifier,
            shape = RectangleShape,
        )
        val actualSize = indicatorSize ?: text.length
        if (actualSize > 0 && text.isNotEmpty()) {
            Text(
                text = actualSize.toString(),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 2.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.secondary,
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
) {
    val text by textFlow.collectAsStateWithLifecycle()
    GkMultiTextField(
        modifier = modifier,
        text = text,
        onTextChange = { textFlow.value = it },
        immediateFocus = immediateFocus,
        indicatorSize = indicatorSize,
        placeholderText = placeholderText,
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
