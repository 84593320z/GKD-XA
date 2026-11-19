package priv.kit.ui.adb

import priv.kit.core.adb.PrivilegeAdbWirelessDebuggingControlStatus
import priv.kit.ui.PrivilegeUiManagedWirelessAdbStatus
import priv.kit.ui.PrivilegeUiState
import priv.kit.ui.PrivilegeUiWirelessAdbStatus

internal fun privilegeUiPairingCheckStatus(
    wirelessDebuggingOn: Boolean,
    pairingCheckPaired: Boolean?,
    currentStatus: PrivilegeUiWirelessAdbStatus,
): PrivilegeUiWirelessAdbStatus =
    when {
        !wirelessDebuggingOn -> PrivilegeUiWirelessAdbStatus.UNKNOWN
        pairingCheckPaired != null -> if (pairingCheckPaired) {
            PrivilegeUiWirelessAdbStatus.ON
        } else {
            PrivilegeUiWirelessAdbStatus.OFF
        }
        currentStatus == PrivilegeUiWirelessAdbStatus.ON -> PrivilegeUiWirelessAdbStatus.ON
        else -> PrivilegeUiWirelessAdbStatus.UNKNOWN
    }

internal fun privilegeUiWirelessDebuggingStatus(
    wirelessDebuggingEnabled: Boolean,
): PrivilegeUiWirelessAdbStatus =
    if (wirelessDebuggingEnabled) {
        PrivilegeUiWirelessAdbStatus.ON
    } else {
        PrivilegeUiWirelessAdbStatus.OFF
    }

internal fun privilegeUiRefreshingPairingCheckStatus(
    wirelessDebuggingStatus: PrivilegeUiWirelessAdbStatus,
    currentStatus: PrivilegeUiWirelessAdbStatus,
): PrivilegeUiWirelessAdbStatus =
    when {
        wirelessDebuggingStatus == PrivilegeUiWirelessAdbStatus.OFF -> PrivilegeUiWirelessAdbStatus.UNKNOWN
        currentStatus == PrivilegeUiWirelessAdbStatus.ON -> PrivilegeUiWirelessAdbStatus.ON
        else -> PrivilegeUiWirelessAdbStatus.CHECKING
    }

internal fun shouldRequireWirelessPairingForStart(
    pairingCheckStatus: PrivilegeUiWirelessAdbStatus,
): Boolean =
    pairingCheckStatus == PrivilegeUiWirelessAdbStatus.OFF

internal fun shouldRequireWirelessDebuggingForStart(
    wirelessDebuggingStatus: PrivilegeUiWirelessAdbStatus,
    managedWirelessAdbStatus: PrivilegeUiManagedWirelessAdbStatus,
): Boolean =
    wirelessDebuggingStatus != PrivilegeUiWirelessAdbStatus.ON &&
        managedWirelessAdbStatus != PrivilegeUiManagedWirelessAdbStatus.READY &&
        (
            wirelessDebuggingStatus == PrivilegeUiWirelessAdbStatus.OFF ||
                managedWirelessAdbStatus == PrivilegeUiManagedWirelessAdbStatus.PERMISSION_REQUIRED ||
                managedWirelessAdbStatus == PrivilegeUiManagedWirelessAdbStatus.UNDECLARED
            )

internal fun PrivilegeAdbWirelessDebuggingControlStatus.toUiManagedWirelessAdbStatus():
    PrivilegeUiManagedWirelessAdbStatus =
    when {
        !supported -> PrivilegeUiManagedWirelessAdbStatus.UNSUPPORTED
        !permissionDeclared -> PrivilegeUiManagedWirelessAdbStatus.UNDECLARED
        canManage -> PrivilegeUiManagedWirelessAdbStatus.READY
        failureMessage != null -> PrivilegeUiManagedWirelessAdbStatus.FAILED
        !permissionGranted -> PrivilegeUiManagedWirelessAdbStatus.PERMISSION_REQUIRED
        else -> PrivilegeUiManagedWirelessAdbStatus.UNKNOWN
    }

internal fun PrivilegeUiState.withWirelessAdbOffline(
    wifiConnected: Boolean,
    managedWirelessAdbStatus: PrivilegeUiManagedWirelessAdbStatus,
    notificationPairingRunning: Boolean,
): PrivilegeUiState =
    copy(
        wifiConnected = wifiConnected,
        wirelessDebuggingStatus = PrivilegeUiWirelessAdbStatus.OFF,
        wirelessPairingServiceStatus = PrivilegeUiWirelessAdbStatus.OFF,
        wirelessPairingCheckStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
        managedWirelessAdbStatus = managedWirelessAdbStatus,
        notificationPairingRunning = notificationPairingRunning,
    )
