package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkSettingsDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    GkDialog(onDismissRequest = onDismissRequest, nativePlacement = true) {
        GkDialogSurface(
            shape = GkShapes.dialog,
            containerColor = DialogDefaults.backgroundColor(),
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GkUi.MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MiuixTheme.textStyles.title4,
                    color = DialogDefaults.titleColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                GkIconButton(
                    imageVector = GkIcons.Close,
                    contentDescription = UiStrings.dialog_close_description(title),
                    onClick = onDismissRequest,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}
