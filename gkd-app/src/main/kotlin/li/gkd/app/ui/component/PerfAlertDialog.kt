package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix-styled alert dialog that retains Compose DialogProperties behavior. */
@Composable
fun PerfAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (RowScope.() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    GkDialog(onDismissRequest = onDismissRequest, properties = properties, nativePlacement = true) {
        GkDialogSurface(
            modifier = modifier,
            shape = GkShapes.dialog,
            containerColor = DialogDefaults.backgroundColor(),
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            title?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    GkProvideDialogTextStyle(
                        style = MiuixTheme.textStyles.title4.copy(textAlign = TextAlign.Center),
                        contentColor = DialogDefaults.titleColor(),
                        content = it,
                    )
                }
            }
            text?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GkProvideDialogTextStyle(
                        style = MiuixTheme.textStyles.body1,
                        contentColor = DialogDefaults.summaryColor(),
                        content = it,
                    )
                }
            }
            // Keep these RowScope slots: existing callers intentionally use Modifier.weight.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GkUi.MinTouchTarget),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke(this)
                confirmButton()
            }
        }
    }
}
