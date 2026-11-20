// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import priv.kit.ui.miuix.PrivilegeDialog
import priv.kit.ui.miuix.PrivilegeStatusIconButton
import top.yukonga.miuix.kmp.basic.Icon
import priv.kit.ui.miuix.PrivilegeIconButton
import priv.kit.ui.miuix.PrivilegeIconButtonDefaults
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import priv.kit.ui.miuix.PrivilegeTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import priv.kit.ui.PrivilegeUiPermissionRestrictionStatus
import priv.kit.ui.PrivilegeUiExternalStartSnapshot
import priv.kit.ui.PrivilegeUiRuntimeStartPhase
import priv.kit.ui.PrivilegeUiRuntimeStatus
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.PrivilegeUiScreenState
import priv.kit.ui.R

@Composable
internal fun Panel(
    topPadding: Dp = PrivilegeUiSpacing.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrivilegeMiuixTheme.colorScheme.surfaceContainerLow,
        shape = PrivilegeMiuixTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(
                start = PrivilegeUiSpacing.large,
                top = topPadding,
                end = PrivilegeUiSpacing.large,
                bottom = PrivilegeUiSpacing.large,
            ),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium),
            content = content,
        )
    }
}

@Composable
internal fun ItemPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrivilegeMiuixTheme.colorScheme.surfaceContainerHighest,
        shape = PrivilegeMiuixTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(PrivilegeUiSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.small),
            content = content,
        )
    }
}

@Composable
@Suppress("DEPRECATION")
internal fun PrivilegeUiScreenScope.PermissionRestrictionWarning() {
    var permissionsDialogVisible by remember(state.connectionSerial) { mutableStateOf(false) }
    val permissions = state.deniedServerPermissions
    val clipboard = LocalClipboardManager.current
    val viewSolutions = tutorialAction(priv.kit.ui.PrivilegeUiTutorialTopic.PERMISSION_RESTRICTIONS)
    if (permissionsDialogVisible) {
        val permissionText = permissions.joinToString("\n")
        PrivilegeDialog(
            onDismissRequest = { permissionsDialogVisible = false },
            title = { Text(stringResource(R.string.priv_ui_denied_permissions_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.medium)) {
                    Text(stringResource(R.string.priv_ui_denied_permissions_message))
                    SelectionContainer {
                        Text(
                            text = permissionText,
                            fontFamily = FontFamily.Monospace,
                            style = PrivilegeMiuixTheme.typography.bodySmall,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                                .background(PrivilegeMiuixTheme.colorScheme.surfaceContainerHighest, PrivilegeMiuixTheme.shapes.small)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(PrivilegeUiSpacing.medium),
                        )
                    }
                }
            },
            dismissButton = {
                PrivilegeTextButton(onClick = { clipboard.setText(AnnotatedString(permissionText)) }) {
                    Text(stringResource(R.string.priv_ui_denied_permissions_copy))
                }
            },
            confirmButton = {
                PrivilegeTextButton(onClick = { permissionsDialogVisible = false }) {
                    Text(stringResource(R.string.priv_ui_denied_permissions_close))
                }
            },
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrivilegeMiuixTheme.colorScheme.errorContainer,
        contentColor = PrivilegeMiuixTheme.colorScheme.onErrorContainer,
        shape = PrivilegeMiuixTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PrivilegeUiSpacing.large),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = PrivilegeUiIcons.Warning,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(PrivilegeUiSpacing.medium))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.priv_ui_permission_restricted_title),
                    style = PrivilegeMiuixTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.priv_ui_permission_restricted_message),
                style = PrivilegeMiuixTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrivilegeTextButton(
                    onClick = viewSolutions,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(stringResource(R.string.priv_ui_permission_solutions_view))
                }
                PrivilegeTextButton(
                    onClick = { permissionsDialogVisible = true },
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(stringResource(R.string.priv_ui_denied_permissions_view))
                }
            }
        }
    }
}

internal fun privilegeUiPermissionRestrictionWarningVisible(
    runtimeStatus: PrivilegeUiRuntimeStatus,
    restrictionStatus: PrivilegeUiPermissionRestrictionStatus,
): Boolean =
    runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED &&
        restrictionStatus == PrivilegeUiPermissionRestrictionStatus.RESTRICTED

@Composable
internal fun CommandBlock(commandLine: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PrivilegeMiuixTheme.colorScheme.surfaceContainerHighest,
                shape = PrivilegeMiuixTheme.shapes.small,
            )
            .padding(PrivilegeUiSpacing.medium),
    ) {
        SelectionContainer {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = commandLine,
                style = PrivilegeMiuixTheme.typography.bodySmall,
                color = PrivilegeMiuixTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
internal fun PrivilegeUiScreenScope.AutoRecoveryWarning() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PrivilegeMiuixTheme.colorScheme.surfaceContainerLow,
        contentColor = PrivilegeMiuixTheme.colorScheme.onSurface,
        shape = PrivilegeMiuixTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PrivilegeUiSpacing.large,
                    vertical = PrivilegeUiSpacing.medium,
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.extraSmall),
            ) {
                Text(
                    text = stringResource(R.string.priv_ui_auto_recovery_disconnected_title),
                    style = PrivilegeMiuixTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.priv_ui_auto_recovery_disconnected_message),
                    style = PrivilegeMiuixTheme.typography.bodySmall,
                    color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrivilegeTextButton(
                modifier = Modifier.align(Alignment.End),
                enabled = interactionEnabled,
                onClick = actions.disableAutoRecovery,
            ) {
                Text(
                    text = stringResource(R.string.priv_ui_auto_recovery_disable_action),
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun privilegeUiAutoRecoveryWarningVisible(
    desiredEnabled: Boolean,
    runtimeStatus: PrivilegeUiRuntimeStatus,
    runtimeStartPhase: PrivilegeUiRuntimeStartPhase,
): Boolean =
    desiredEnabled &&
        runtimeStartPhase == PrivilegeUiRuntimeStartPhase.IDLE &&
        (
            runtimeStatus == PrivilegeUiRuntimeStatus.DISCONNECTED ||
                runtimeStatus == PrivilegeUiRuntimeStatus.FAILED
        )

@Composable
internal fun PrivilegeUiScreenScope.RestartConfirmationDialog() {
    if (state.restartConfirmationTarget == null) return
    PrivilegeDialog(
        onDismissRequest = {
            if (interactionEnabled) actions.cancelServerRestart()
        },
        title = {
            Text(stringResource(R.string.priv_ui_restart_service_dialog_title))
        },
        text = {
            Text(stringResource(R.string.priv_ui_restart_service_dialog_message))
        },
        confirmButton = {
            PrivilegeTextButton(
                enabled = interactionEnabled,
                onClick = actions.confirmServerRestart,
            ) {
                Text(stringResource(R.string.priv_ui_restart_service_confirm))
            }
        },
        dismissButton = {
            PrivilegeTextButton(
                enabled = interactionEnabled,
                onClick = actions.cancelServerRestart,
            ) {
                Text(stringResource(R.string.priv_ui_restart_service_cancel))
            }
        },
    )
}

@Composable
internal fun PrivilegeUiScreenScope.ServiceStatusPanel() {
    var showStopConfirmation by remember { mutableStateOf(false) }
    val action = privilegeUiServiceStatusAction(
        runtimeStatus = state.runtimeStatus,
        runtimeStartPhase = state.runtimeStartPhase,
    )
    val (
        title,
        detail,
        background,
        foreground,
        icon,
        iconDescription,
        actionContainer,
        actionForeground,
    ) = when (action) {
        PrivilegeUiServiceStatusAction.STOP ->
        StatusUi(
            title = stringResource(R.string.priv_ui_service_started),
            detail = stringResource(
                R.string.priv_ui_service_source,
                runtimeSourceText(state.serverUid),
            ),
            background = PrivilegeMiuixTheme.colorScheme.tertiaryContainer,
            foreground = PrivilegeMiuixTheme.colorScheme.onTertiaryContainer,
            icon = PrivilegeUiIcons.Stop,
            iconDescription = stringResource(R.string.priv_ui_service_stop_action_description),
            actionContainer = PrivilegeMiuixTheme.colorScheme.errorContainer,
            actionForeground = PrivilegeMiuixTheme.colorScheme.onErrorContainer,
        )
        PrivilegeUiServiceStatusAction.CANCEL -> StatusUi(
            title = stringResource(R.string.priv_ui_service_not_started),
            detail = state.runtimeStatusDetail(),
            background = PrivilegeMiuixTheme.colorScheme.surfaceContainerLow,
            foreground = PrivilegeMiuixTheme.colorScheme.onSurface,
            icon = PrivilegeUiIcons.Stop,
            iconDescription = stringResource(R.string.priv_ui_start_cancel_action),
            actionContainer = PrivilegeMiuixTheme.colorScheme.errorContainer,
            actionForeground = PrivilegeMiuixTheme.colorScheme.onErrorContainer,
        )
        PrivilegeUiServiceStatusAction.CANCELLING -> StatusUi(
            title = stringResource(R.string.priv_ui_service_not_started),
            detail = state.runtimeStatusDetail(),
            background = PrivilegeMiuixTheme.colorScheme.surfaceContainerLow,
            foreground = PrivilegeMiuixTheme.colorScheme.onSurface,
            icon = PrivilegeUiIcons.Stop,
            iconDescription = stringResource(R.string.priv_ui_start_cancelling_action),
            actionContainer = PrivilegeMiuixTheme.colorScheme.errorContainer,
            actionForeground = PrivilegeMiuixTheme.colorScheme.onErrorContainer,
        )
        PrivilegeUiServiceStatusAction.START ->
        StatusUi(
            title = stringResource(R.string.priv_ui_service_not_started),
            detail = state.runtimeStatusDetail(),
            background = PrivilegeMiuixTheme.colorScheme.surfaceContainerLow,
            foreground = PrivilegeMiuixTheme.colorScheme.onSurface,
            icon = PrivilegeUiIcons.PlayArrow,
            iconDescription = stringResource(R.string.priv_ui_service_start_action_description),
            actionContainer = PrivilegeMiuixTheme.colorScheme.primaryContainer,
            actionForeground = PrivilegeMiuixTheme.colorScheme.onPrimaryContainer,
        )
    }

    if (showStopConfirmation) {
        PrivilegeDialog(
            onDismissRequest = {
                if (interactionEnabled) showStopConfirmation = false
            },
            title = {
                Text(stringResource(R.string.priv_ui_stop_service_dialog_title))
            },
            text = {
                Text(stringResource(R.string.priv_ui_stop_service_dialog_message))
            },
            confirmButton = {
                PrivilegeTextButton(
                    enabled = interactionEnabled,
                    onClick = {
                        if (!actions.canInteract()) return@PrivilegeTextButton
                        showStopConfirmation = false
                        actions.stopServer()
                    },
                ) {
                    Text(stringResource(R.string.priv_ui_stop_service_confirm))
                }
            },
            dismissButton = {
                PrivilegeTextButton(
                    enabled = interactionEnabled,
                    onClick = { showStopConfirmation = false },
                ) {
                    Text(stringResource(R.string.priv_ui_stop_service_cancel))
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        contentColor = foreground,
        shape = PrivilegeMiuixTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PrivilegeUiSpacing.large,
                    top = PrivilegeUiSpacing.medium,
                    end = PrivilegeUiSpacing.medium,
                    bottom = PrivilegeUiSpacing.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.extraSmall),
            ) {
                Text(
                    text = title,
                    style = PrivilegeMiuixTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    style = PrivilegeMiuixTheme.typography.bodyMedium,
                    // Do not truncate startup/authorization error details at larger font sizes.
                )
            }
            Spacer(modifier = Modifier.width(PrivilegeUiSpacing.medium))
            PrivilegeIconTooltip(
                text = iconDescription,
                modifier = Modifier,
            ) {
                PrivilegeStatusIconButton(
                    modifier = Modifier.size(PrivilegeUiSize.minimumTouchTarget),
                    enabled = interactionEnabled &&
                        privilegeUiServiceStatusActionEnabled(
                            action = action,
                            busy = state.busy,
                        ),
                    colors = PrivilegeIconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = actionContainer,
                        contentColor = actionForeground,
                    ),
                    onClick = {
                        if (!actions.canInteract()) return@PrivilegeStatusIconButton
                        when (action) {
                            PrivilegeUiServiceStatusAction.STOP -> showStopConfirmation = true
                            PrivilegeUiServiceStatusAction.CANCEL -> actions.stopCurrentStart()
                            PrivilegeUiServiceStatusAction.CANCELLING -> Unit
                            PrivilegeUiServiceStatusAction.START -> actions.startInteractive()
                        }
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = icon,
                        contentDescription = iconDescription,
                    )
                }
            }
        }
    }
}

internal enum class PrivilegeUiServiceStatusAction {
    START,
    CANCEL,
    CANCELLING,
    STOP,
}

internal fun privilegeUiServiceStatusAction(
    runtimeStatus: PrivilegeUiRuntimeStatus,
    runtimeStartPhase: PrivilegeUiRuntimeStartPhase,
): PrivilegeUiServiceStatusAction =
    when (runtimeStartPhase) {
        PrivilegeUiRuntimeStartPhase.RUNNING -> PrivilegeUiServiceStatusAction.CANCEL
        PrivilegeUiRuntimeStartPhase.CANCELLING -> PrivilegeUiServiceStatusAction.CANCELLING
        PrivilegeUiRuntimeStartPhase.IDLE -> when (runtimeStatus) {
            PrivilegeUiRuntimeStatus.CONNECTED -> PrivilegeUiServiceStatusAction.STOP
            PrivilegeUiRuntimeStatus.STARTING,
            PrivilegeUiRuntimeStatus.DISCONNECTED,
            PrivilegeUiRuntimeStatus.FAILED,
            -> PrivilegeUiServiceStatusAction.START
        }
    }

internal fun privilegeUiServiceStatusActionEnabled(
    action: PrivilegeUiServiceStatusAction,
    busy: Boolean,
): Boolean =
    when (action) {
        PrivilegeUiServiceStatusAction.CANCEL -> true
        PrivilegeUiServiceStatusAction.CANCELLING -> false
        PrivilegeUiServiceStatusAction.START,
        PrivilegeUiServiceStatusAction.STOP,
        -> !busy
    }

@Composable
internal fun PrivilegeUiScreenScope.StartupLogPanel() {
    val lines = state.startupLogLines
    val copiedMessage = stringResource(R.string.priv_ui_startup_log_copied)
    Panel {
        val copyLogDescription = stringResource(R.string.priv_ui_startup_log_copy_description)
        val closeLogDescription = stringResource(R.string.priv_ui_startup_log_close_description)
        val logScrollState = rememberScrollState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.priv_ui_startup_log_title),
                style = PrivilegeMiuixTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PrivilegeIconTooltip(
                text = copyLogDescription,
                modifier = Modifier,
            ) {
                PrivilegeIconButton(
                    modifier = Modifier.size(PrivilegeUiSize.minimumTouchTarget),
                    enabled = interactionEnabled && lines.isNotEmpty(),
                    onClick = {
                        if (!actions.canInteract()) return@PrivilegeIconButton
                        actions.copyStartupLog()
                        showFeedback(copiedMessage)
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = PrivilegeUiIcons.ContentCopy,
                        contentDescription = copyLogDescription,
                        tint = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrivilegeIconTooltip(
                text = closeLogDescription,
                modifier = Modifier,
            ) {
                PrivilegeIconButton(
                    modifier = Modifier.size(PrivilegeUiSize.minimumTouchTarget),
                    enabled = interactionEnabled,
                    onClick = {
                        if (!actions.canInteract()) return@PrivilegeIconButton
                        actions.clearStartupLog()
                    },
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = PrivilegeUiIcons.Close,
                        contentDescription = closeLogDescription,
                        tint = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 480.dp)
                .background(
                    color = PrivilegeMiuixTheme.colorScheme.surfaceContainerHighest,
                    shape = PrivilegeMiuixTheme.shapes.small,
                )
                .padding(PrivilegeUiSpacing.medium),
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(logScrollState),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = lines.joinToString("\n"),
                    style = PrivilegeMiuixTheme.typography.bodySmall,
                    color = PrivilegeMiuixTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
internal fun StatusText(text: String) {
    Text(
        text = text,
        style = PrivilegeMiuixTheme.typography.labelMedium,
        color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun PrivilegeUiExternalStartSnapshot.externalStartStatusText(): String =
    when {
        canStart -> stringResource(R.string.priv_ui_external_ready)
        available -> stringResource(R.string.priv_ui_external_permission_required)
        else -> stringResource(R.string.priv_ui_external_unavailable)
    }

private data class StatusUi(
    val title: String,
    val detail: String,
    val background: Color,
    val foreground: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconDescription: String,
    val actionContainer: Color,
    val actionForeground: Color,
)

@Composable
private fun PrivilegeUiScreenState.runtimeStatusDetail(): String =
    when (runtimeStartPhase) {
        PrivilegeUiRuntimeStartPhase.CANCELLING -> stringResource(R.string.priv_ui_startup_cancelling)
        PrivilegeUiRuntimeStartPhase.RUNNING -> runtimeProgressText

            ?.takeIf(String::isNotBlank)
            ?: stringResource(R.string.priv_ui_ready)
        PrivilegeUiRuntimeStartPhase.IDLE -> when (runtimeStatus) {
            PrivilegeUiRuntimeStatus.STARTING -> runtimeProgressText

                ?.takeIf(String::isNotBlank)
                ?: stringResource(R.string.priv_ui_ready)
            PrivilegeUiRuntimeStatus.DISCONNECTED,
            PrivilegeUiRuntimeStatus.FAILED,
            PrivilegeUiRuntimeStatus.CONNECTED,
            -> stringResource(R.string.priv_ui_ready)
        }
    }

@Composable
private fun runtimeSourceText(uid: Int?): String = stringResource(runtimeSourceLabel(uid), uid ?: 0)

internal fun runtimeSourceLabel(uid: Int?): Int = when (uid) {
    0 -> R.string.priv_ui_service_source_root
    1000 -> R.string.priv_ui_service_source_system
    2000 -> R.string.priv_ui_service_source_shell
    null -> R.string.priv_ui_service_source_unknown
    else -> R.string.priv_ui_service_source_uid
}
