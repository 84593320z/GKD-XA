// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import priv.kit.ui.miuix.PrivilegePrimaryButton
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.R

@Composable
internal fun PrivilegeUiScreenScope.ExternalStartPanel() {
    Panel {
        if (state.externalStartItems.isEmpty()) {
            Text(stringResource(R.string.priv_ui_external_no_provider))
        }
        state.externalStartItems.forEach { item ->
            ItemPanel {
                val action = state.startActionFor(
                    source = PrivilegeUiRuntimeStartSource.EXTERNAL,
                    providerId = item.id,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.label.toString(),
                        style = PrivilegeMiuixTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item.statusLoaded) {
                        StatusText(item.snapshot.externalStartStatusText())
                    }
                }
                PrivilegePrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.startActionEnabled(
                        action = action,
                        startAvailable = interactionEnabled,
                    ),
                    onClick = {
                        when (action) {
                            PrivilegeUiStartAction.START -> actions.authorizeOrStartExternal(item.id)
                            PrivilegeUiStartAction.CANCEL -> actions.stopCurrentStart()
                            PrivilegeUiStartAction.CANCELLING,
                            PrivilegeUiStartAction.NONE,
                            -> Unit
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            privilegeUiStartActionLabel(
                                action = action,
                                startLabel = if (item.snapshot.canStart) {
                                    R.string.priv_ui_external_start
                                } else {
                                    R.string.priv_ui_external_authorize_start
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }
}
