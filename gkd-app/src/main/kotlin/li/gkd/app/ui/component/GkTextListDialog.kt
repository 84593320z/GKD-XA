package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.util.TimeUtils.throttle

@Composable
fun GkTextListDialog(
    onDismiss: () -> Unit,
    textList: List<Pair<String, () -> Unit>>
) {
    val textModifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    GkDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            textList.forEach { (text, onClickItem) ->
                Text(
                    text = text, modifier = Modifier
                        .clickable(onClick = throttle {
                            onDismiss()
                            onClickItem()
                        })
                        .then(textModifier)
                )
            }
        }
    }
}
