package li.gkd.app.ui.component

import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import li.gkd.app.ui.style.TABULAR_NUMBERS_FONT_FEATURE

@Composable
fun GkFixedTimeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MiuixTheme.textStyles.main,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(fontFeatureSettings = TABULAR_NUMBERS_FONT_FEATURE),
        softWrap = false,
        maxLines = 1,
    )
}
