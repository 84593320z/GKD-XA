package li.gkd.app.ui.component

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.util.TimeUtils.throttle

@Composable
fun GkAuthButtonGroup(
    buttons: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
    ) {
        buttons.forEach { (text, click) ->
            TextButton(onClick = throttle(click)) {
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }
    }
}
