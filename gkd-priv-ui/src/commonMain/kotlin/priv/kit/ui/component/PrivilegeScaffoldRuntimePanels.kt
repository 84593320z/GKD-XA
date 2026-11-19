// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import priv.kit.ui.miuix.PrivilegePrimaryButton
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.R

@Composable
internal fun PrivilegeUiScreenScope.RootPanel() {
    Panel {
        val action = state.startActionFor(
            source = PrivilegeUiRuntimeStartSource.ROOT,
            providerId = null,
        )
        PrivilegePrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.startActionEnabled(
                action = action,
                startAvailable = interactionEnabled,
            ),
            onClick = {
                when (action) {
                    PrivilegeUiStartAction.START -> actions.startRoot()
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
                        startLabel = R.string.priv_ui_root_authorization_action,
                    ),
                ),
            )
        }
    }
}

@Composable
internal fun PrivilegeUiScreenScope.ManualShellPanel() {
    val copiedMessage = stringResource(R.string.priv_ui_manual_command_copied)
    Panel {
        Text(
            text = stringResource(R.string.priv_ui_manual_authorization_desc),
            style = PrivilegeMiuixTheme.typography.bodyMedium,
            color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
        )
        val commandLine = state.manualShellCommandLine
        if (commandLine == null) {
            Text(
                text = stringResource(R.string.priv_ui_manual_command_unavailable),
                style = PrivilegeMiuixTheme.typography.bodyMedium,
                color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CommandBlock(commandLine)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TutorialButton(priv.kit.ui.PrivilegeUiTutorialTopic.MANUAL, outlined = true)
            PrivilegePrimaryButton(
                modifier = Modifier.weight(1f),
                enabled = commandLine != null && interactionEnabled && !state.busy,
                onClick = {
                    if (!actions.canInteract()) return@PrivilegePrimaryButton
                    actions.copyManualCommand()
                    showFeedback(copiedMessage)
                },
            ) {
                Text(stringResource(R.string.priv_ui_manual_copy_command))
            }
        }
    }
}
