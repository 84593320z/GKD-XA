package priv.kit.ui.runtime

import priv.kit.core.PrivilegeServerInfo
import priv.kit.ui.PrivilegeUiPermissionRestrictionStatus
import priv.kit.ui.PrivilegeUiRuntimeStartPhase
import priv.kit.ui.PrivilegeUiRuntimeStatus
import priv.kit.ui.PrivilegeUiState

internal fun PrivilegeUiState.startingAttempt(
    attempt: PrivilegeUiRuntimeStartAttempt,
    phase: PrivilegeUiRuntimeStartPhase,
): PrivilegeUiState =
    if (runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED) {
        copy(
            busy = true,
            runtimeStartPhase = phase,
            runtimeStartSource = attempt.runtimeStartSource,
            runtimeStartProviderId = attempt.runtimeStartProviderId,
            runtimeProgressText = attempt.progressText,
        )
    } else {
        copy(
            busy = true,
            runtimeStatus = PrivilegeUiRuntimeStatus.STARTING,
            runtimeStartPhase = phase,
            runtimeStartSource = attempt.runtimeStartSource,
            runtimeStartProviderId = attempt.runtimeStartProviderId,
            serverInfo = null,
            permissionRestrictionStatus = PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
            deniedServerPermissions = emptyList(),
            runtimeProgressText = attempt.progressText,
        )
    }

internal fun PrivilegeUiState.finishRuntimeStartPreservingStatus(): PrivilegeUiState =
    copy(
        busy = false,
        runtimeStartPhase = PrivilegeUiRuntimeStartPhase.IDLE,
        runtimeStartSource = null,
        runtimeStartProviderId = null,
        runtimeProgressText = null,
    )

internal fun PrivilegeUiState.toDisconnectedRuntimeIdle(): PrivilegeUiState =
    finishRuntimeStartPreservingStatus().copy(
        runtimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED,
        serverInfo = null,
        permissionRestrictionStatus = PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
        deniedServerPermissions = emptyList(),
    )

internal fun PrivilegeUiState.toFailedRuntimeIdle(): PrivilegeUiState =
    finishRuntimeStartPreservingStatus().copy(
        runtimeStatus = PrivilegeUiRuntimeStatus.FAILED,
        serverInfo = null,
        permissionRestrictionStatus = PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
        deniedServerPermissions = emptyList(),
    )

internal fun PrivilegeUiState.toConnectedRuntimeIdle(
    serverInfo: PrivilegeServerInfo,
    connectionSerial: Long,
): PrivilegeUiState =
    finishRuntimeStartPreservingStatus().copy(
        runtimeStatus = PrivilegeUiRuntimeStatus.CONNECTED,
        serverInfo = serverInfo,
        permissionRestrictionStatus = PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
        deniedServerPermissions = emptyList(),
        connectionSerial = connectionSerial,
    )

internal fun PrivilegeUiState.finishRuntimeStartDisconnected(): PrivilegeUiState =
    if (runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED) {
        finishRuntimeStartPreservingStatus()
    } else {
        toDisconnectedRuntimeIdle()
    }

internal fun PrivilegeUiState.finishRuntimeStartFailed(): PrivilegeUiState =
    if (runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED) {
        finishRuntimeStartPreservingStatus()
    } else {
        toFailedRuntimeIdle()
    }
