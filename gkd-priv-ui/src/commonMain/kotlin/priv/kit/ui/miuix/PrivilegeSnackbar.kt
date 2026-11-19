// SPDX-License-Identifier: Apache-2.0
package priv.kit.ui.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import priv.kit.ui.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Native host retains timeout/swipe behavior; full error instructions stay scrollable, never ellipsized. */
@Composable
internal fun SnackbarHost(state: SnackbarHostState) {
    val maxHeight = (LocalWindowInfo.current.containerDpSize.height * 0.45f).coerceAtLeast(96.dp)
    top.yukonga.miuix.kmp.basic.SnackbarHost(state = state) { data ->
        val scope = rememberCoroutineScope()
        val colors = SnackbarDefaults.snackbarColors()
        Card(
            modifier = Modifier.padding(SnackbarDefaults.OuterPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
            cornerRadius = SnackbarDefaults.CornerRadius,
            colors = CardDefaults.defaultColors(colors.containerColor, colors.contentColor),
            insideMargin = SnackbarDefaults.InsideMargin,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(data.visuals.message,
                    modifier = Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
                    style = MiuixTheme.textStyles.body2)
                if (!data.visuals.actionLabel.isNullOrBlank() || data.visuals.withDismissAction) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        data.visuals.actionLabel?.takeIf { it.isNotBlank() }?.let { label ->
                            PrivilegeTextButton(onClick = { scope.launch { data.performAction() } }) { Text(label) }
                        }
                        if (data.visuals.withDismissAction) {
                            PrivilegeTextButton(onClick = { scope.launch { data.dismiss() } }) {
                                Text(stringResource(R.string.priv_ui_denied_permissions_close))
                            }
                        }
                    }
                }
            }
        }
    }
}
