package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import li.gkd.app.ui.style.GkShapes
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
    GkDialog(onDismissRequest = onDismissRequest, properties = properties) {
        GkDialogSurface(
            modifier = modifier,
            shape = GkShapes.dialog,
            containerColor = MiuixTheme.colorScheme.background,
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            title?.let {
                GkProvideDialogTextStyle(
                    style = MiuixTheme.textStyles.title3,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                    content = it,
                )
            }
            text?.let {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GkProvideDialogTextStyle(
                        style = MiuixTheme.textStyles.body1,
                        contentColor = MiuixTheme.colorScheme.onSurfaceSecondary,
                        content = it,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke(this)
                confirmButton()
            }
        }
    }
}
