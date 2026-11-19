package li.gkd.app.ui.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val TABULAR_NUMBERS_FONT_FEATURE = "tnum"

@Composable
internal fun GkProvideTextStyle(style: TextStyle, content: @Composable () -> Unit) {
    MiuixTheme(textStyles = MiuixTheme.textStyles.copy(main = style), content = content)
}
