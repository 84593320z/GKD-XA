package priv.kit.ui.adb.pairing

import priv.kit.ui.PrivilegeUiAdbPairingStatus
import priv.kit.ui.PrivilegeUiState
import priv.kit.ui.PrivilegeUiText
import priv.kit.ui.PrivilegeUiWirelessAdbStatus

internal fun PrivilegeUiState.resetPairing(text: PrivilegeUiText? = pairingText): PrivilegeUiState = copy(
    pairingStatus = PrivilegeUiAdbPairingStatus.NOT_PAIRED,
    pairingText = text,
    pairingCode = "",
    pairingDialogVisible = false,
    pairingNotificationPermissionWarningVisible = false,
    notificationPairingRunning = false,
)

internal fun PrivilegeUiState.searchingPairing(text: PrivilegeUiText): PrivilegeUiState =
    resetPairing(text).copy(
        pairingStatus = PrivilegeUiAdbPairingStatus.SEARCHING,
        pairingDialogVisible = true,
    )

internal fun PrivilegeUiState.completedPairing(
    text: PrivilegeUiText,
    fingerprint: String,
): PrivilegeUiState = resetPairing(text).copy(
    pairingStatus = PrivilegeUiAdbPairingStatus.PAIRED,
    adbKeyFingerprint = fingerprint,
    wirelessPairingCheckStatus = PrivilegeUiWirelessAdbStatus.ON,
)
