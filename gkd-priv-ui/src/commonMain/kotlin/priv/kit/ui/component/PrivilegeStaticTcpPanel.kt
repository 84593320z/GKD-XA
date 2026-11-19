// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import priv.kit.ui.miuix.PrivilegeDialog
import priv.kit.ui.miuix.PrivilegePrimaryButton
import priv.kit.ui.miuix.PrivilegeButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import priv.kit.ui.miuix.PrivilegeSecondaryButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import priv.kit.ui.miuix.PrivilegeTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiRuntimeStartPhase
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.PrivilegeUiWirelessAdbStatus
import priv.kit.ui.R
import priv.kit.ui.adb.PrivilegeUiStaticTcpPanelStatus
import priv.kit.ui.adb.PrivilegeUiStaticTcpSwitchAction
import priv.kit.ui.adb.staticTcpActionLabel
import priv.kit.ui.adb.staticTcpStartActionEnabled
import priv.kit.ui.adb.staticTcpCommandHelpVisible
import priv.kit.ui.adb.staticTcpPanelStatus
import priv.kit.ui.state.privilegeUiStaticTcpOpenCommand

@Composable
internal fun PrivilegeUiScreenScope.TcpAuthorizationFailureDialog() {
    PrivilegeDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(stringResource(R.string.priv_ui_system_prompt_tcp_authorization_title))
        },
        text = {
            Text(stringResource(R.string.priv_ui_tcp_authorization_timeout_message))
        },
        confirmButton = {
            PrivilegeTextButton(
                enabled = interactionEnabled,
                onClick = actions.dismissTcpAuthorizationFailureDialog,
            ) {
                Text(stringResource(R.string.priv_ui_ok))
            }
        },
    )
}

@Composable
internal fun PrivilegeUiScreenScope.StaticTcpSwitchConfirmationDialog(
    action: PrivilegeUiStaticTcpSwitchAction,
) {
    PrivilegeDialog(
        onDismissRequest = {
            if (interactionEnabled) actions.cancelStaticTcpSwitch()
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(stringResource(R.string.priv_ui_adb_static_switch_confirmation_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.priv_ui_adb_static_switch_confirmation_message),
                    style = PrivilegeMiuixTheme.typography.bodyMedium,
                )
                AdbConnectionWarning()
            }
        },
        confirmButton = {
            PrivilegeTextButton(
                enabled = interactionEnabled,
                onClick = actions.confirmStaticTcpSwitch,
            ) {
                Text(
                    stringResource(
                        when (action) {
                            PrivilegeUiStaticTcpSwitchAction.START_SERVICE ->
                                R.string.priv_ui_adb_static_switch_continue_start_action
                            PrivilegeUiStaticTcpSwitchAction.ENABLE_PORT ->
                                R.string.priv_ui_adb_static_switch_continue_enable_action
                        },
                    ),
                )
            }
        },
        dismissButton = {
            PrivilegeTextButton(
                enabled = interactionEnabled,
                onClick = actions.cancelStaticTcpSwitch,
            ) {
                Text(stringResource(R.string.priv_ui_adb_static_switch_cancel_action))
            }
        },
    )
}

@Composable
internal fun PrivilegeUiScreenScope.StaticTcpAdbSection() {
    val adbInteractionEnabled = interactionEnabled
    val copiedMessage = stringResource(R.string.priv_ui_adb_static_command_copied)
    val controlStatusLoadingMessage = stringResource(R.string.priv_ui_adb_static_control_status_loading)
    var controlDialogVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium),
    ) {
        val tcpPolicy = state.adbTcpPolicy
        val configuredTcpPort = state.tcpPort
        val paired = state.wirelessPairingCheckStatus == PrivilegeUiWirelessAdbStatus.ON
        val activeTcpPort = state.staticTcp.activePort
        val staticTcpActive = activeTcpPort != null
        val staticTcpConfigured = state.staticTcp.configuredPort != null
        val runtimeStartInProgress = state.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE
        val wirelessAdbSupported = state.wirelessAdbSupported
        val staticTcpStatus = staticTcpPanelStatus(
            tcpModeConfigured = staticTcpConfigured,
            tcpModeActive = staticTcpActive,
            status = state.staticTcp.authorizationStatus,
        )
        val staticTcpCommand = privilegeUiStaticTcpOpenCommand(
            activeTcpPort ?: configuredTcpPort,
        )
        val prepareActionVisible = !wirelessAdbSupported &&
            tcpPolicy == PrivilegeUiAdbTcpPolicy.AUTO_ENABLE_AFTER_WIRELESS_PAIRED
        val prepareActionEnabled = prepareActionVisible &&
            state.staticTcp.loaded &&
            adbInteractionEnabled &&
            !runtimeStartInProgress &&
            !state.busy &&
            paired &&
            !staticTcpActive
        val startAction = state.startActionFor(
            source = PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP,
            providerId = null,
        )
        val tcpStartActionEnabled = state.staticTcpStartActionEnabled(
            action = startAction,
            wirelessAdbSupported = wirelessAdbSupported,
            interactionEnabled = adbInteractionEnabled,
        )
        val controlActionAvailable = adbInteractionEnabled &&
            !runtimeStartInProgress &&
            !state.busy
        val controlActionEnabled = controlActionAvailable && staticTcpActive
        val commandHelpVisible = staticTcpCommandHelpVisible(
            wirelessAdbSupported = wirelessAdbSupported,
        )
        AdbStatusRow(
            label = stringResource(R.string.priv_ui_adb_tab_static),
            text = if (state.staticTcp.loaded) {
                staticTcpStatus.displayText()
            } else {
                null
            },
            color = if (state.staticTcp.loaded) {
                staticTcpStatus.displayColor()
            } else {
                PrivilegeMiuixTheme.colorScheme.onSurfaceVariant
            },
        )
        if (prepareActionVisible) {
            PrivilegeSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = prepareActionEnabled,
                onClick = actions.enableTcpMode,
            ) {
                Text(stringResource(R.string.priv_ui_adb_static_prepare_action))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrivilegeSecondaryButton(
                enabled = controlActionAvailable && (!state.staticTcp.loaded || staticTcpActive),
                onClick = {
                    if (!state.staticTcp.loaded) {
                        showFeedback(controlStatusLoadingMessage)
                    } else if (staticTcpActive) {
                        controlDialogVisible = true
                    }
                },
            ) {
                Text(stringResource(R.string.priv_ui_adb_static_control_action))
            }
            PrivilegePrimaryButton(
                modifier = Modifier.weight(1f),
                enabled = tcpStartActionEnabled,
                onClick = {
                    when (startAction) {
                        PrivilegeUiStartAction.START ->
                            actions.startStaticTcpAdb()
                        PrivilegeUiStartAction.CANCEL -> actions.stopCurrentStart()
                        PrivilegeUiStartAction.CANCELLING,
                        PrivilegeUiStartAction.NONE,
                        -> Unit
                    }
                },
            ) {
                Text(
                    stringResource(
                        staticTcpActionLabel(
                            action = startAction,
                        ),
                    ),
                )
            }
        }
        if (commandHelpVisible) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.priv_ui_adb_static_command_desc),
                    style = PrivilegeMiuixTheme.typography.bodySmall,
                    color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
                )
                CommandBlock(staticTcpCommand)
                PrivilegeSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = adbInteractionEnabled &&
                        !runtimeStartInProgress &&
                        !state.busy,
                    onClick = {
                        if (!actions.canInteract()) return@PrivilegeSecondaryButton
                        actions.copyStaticTcpCommand()
                        showFeedback(copiedMessage)
                    },
                ) {
                    Text(stringResource(R.string.priv_ui_manual_copy_command))
                }
            }
        }
        if (controlDialogVisible) {
            StaticTcpControlDialog(
                commandLine = staticTcpCommand,
                commandVisible = commandHelpVisible,
                actionEnabled = controlActionEnabled,
                onDismiss = {
                    controlDialogVisible = false
                },
                onStop = {
                    controlDialogVisible = false
                    actions.disableTcpMode()
                },
                onRestart = {
                    controlDialogVisible = false
                    actions.restartTcpMode()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaticTcpControlDialog(
    commandLine: String,
    commandVisible: Boolean,
    actionEnabled: Boolean,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    PrivilegeDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(stringResource(R.string.priv_ui_adb_static_control_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium),
            ) {
                if (commandVisible) {
                    Text(
                        text = stringResource(R.string.priv_ui_adb_static_command_desc),
                        style = PrivilegeMiuixTheme.typography.bodyMedium,
                    )
                    CommandBlock(commandLine)
                }
                AdbConnectionWarning()
            }
        },
        confirmButton = {
            PrivilegeTextButton(
                enabled = actionEnabled,
                onClick = onRestart,
            ) {
                Text(stringResource(R.string.priv_ui_adb_static_control_restart_action))
            }
        },
        dismissButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.small),
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.small),
            ) {
                PrivilegeTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.priv_ui_adb_static_switch_cancel_action))
                }
                PrivilegeTextButton(
                    enabled = actionEnabled,
                    colors = PrivilegeButtonDefaults.textButtonColors(
                        contentColor = PrivilegeMiuixTheme.colorScheme.error,
                    ),
                    onClick = onStop,
                ) {
                    Text(stringResource(R.string.priv_ui_adb_static_control_stop_action))
                }
            }
        },
    )
}

@Composable
private fun AdbConnectionWarning() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrivilegeMiuixTheme.colorScheme.errorContainer,
        contentColor = PrivilegeMiuixTheme.colorScheme.onErrorContainer,
        shape = PrivilegeMiuixTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrivilegeUiSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = PrivilegeUiIcons.Warning,
                    contentDescription = null,
                )
                Spacer(Modifier.width(PrivilegeUiSpacing.medium))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.priv_ui_adb_connection_warning_title),
                    style = PrivilegeMiuixTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.priv_ui_adb_connection_warning_message),
                style = PrivilegeMiuixTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PrivilegeUiStaticTcpPanelStatus.displayText(): String =
    when (this) {
        PrivilegeUiStaticTcpPanelStatus.PORT_NOT_CONFIGURED ->
            stringResource(R.string.priv_ui_adb_static_port_unavailable)
        PrivilegeUiStaticTcpPanelStatus.ADB_SERVICE_STOPPED ->
            stringResource(R.string.priv_ui_adb_static_service_stopped)
        PrivilegeUiStaticTcpPanelStatus.UNAUTHORIZED -> stringResource(R.string.priv_ui_adb_static_status_unauthorized)
        PrivilegeUiStaticTcpPanelStatus.AUTHORIZED -> stringResource(R.string.priv_ui_adb_static_status_authorized)
    }

@Composable
private fun PrivilegeUiStaticTcpPanelStatus.displayColor(): Color =
    when (this) {
        PrivilegeUiStaticTcpPanelStatus.AUTHORIZED -> PrivilegeMiuixTheme.colorScheme.tertiary
        PrivilegeUiStaticTcpPanelStatus.PORT_NOT_CONFIGURED,
        PrivilegeUiStaticTcpPanelStatus.ADB_SERVICE_STOPPED,
        PrivilegeUiStaticTcpPanelStatus.UNAUTHORIZED,
        -> PrivilegeMiuixTheme.colorScheme.onSurfaceVariant
    }
