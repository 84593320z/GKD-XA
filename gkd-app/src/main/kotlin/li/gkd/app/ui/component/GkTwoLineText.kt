package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
            style = MiuixTheme.textStyles.title3,
        )
        CompositionLocalProvider(LocalTextStyle provides MiuixTheme.textStyles.subtitle) {
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
