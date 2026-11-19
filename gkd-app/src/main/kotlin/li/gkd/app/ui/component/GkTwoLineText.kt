package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import li.gkd.app.ui.style.GkProvideTextStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GkTwoLineText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showApp: Boolean = false,
    appFallbackName: String? = null,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.MiddleEllipsis,
            style = MiuixTheme.textStyles.headline1,
        )
        GkProvideTextStyle(MiuixTheme.textStyles.body2) {
            if (showApp) {
                GkAppNameText(appId = subtitle, fallbackName = appFallbackName)
            } else {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
            }
        }
    }
}
