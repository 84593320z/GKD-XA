package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkTextListDialog(
    onDismiss: () -> Unit,
    textList: List<Pair<String, () -> Unit>>,
) {
    GkDialog(onDismissRequest = onDismiss) {
        GkDialogSurface(
            shape = GkShapes.dialog,
            containerColor = DialogDefaults.backgroundColor(),
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                textList.forEach { (text, onClickItem) ->
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GkUi.MinTouchTarget)
                            .clickable(role = Role.Button, onClick = throttle {
                                onDismiss()
                                onClickItem()
                            })
                            .padding(horizontal = GkUi.ContentPadding, vertical = 12.dp),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
