// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.adb

import priv.kit.ui.PrivilegeUiAdbTcpAuthorizationStatus
import priv.kit.ui.PrivilegeUiScreenState
import priv.kit.ui.PrivilegeUiWirelessAdbStatus
import priv.kit.ui.R
import priv.kit.ui.component.PrivilegeUiStartAction
import priv.kit.ui.component.privilegeUiStartActionEnabled
import priv.kit.ui.component.privilegeUiStartActionLabel

internal enum class PrivilegeUiWirelessAdbPanelStatus {
    WIFI_REQUIRED,
    OFF,
    UNPAIRED,
    PAIRABLE,
    PAIRED,
}

internal enum class PrivilegeUiStaticTcpPanelStatus {
    PORT_NOT_CONFIGURED,
    ADB_SERVICE_STOPPED,
    UNAUTHORIZED,
    AUTHORIZED,
}

internal fun wirelessAdbPanelStatus(
    wifiConnected: Boolean,
    wirelessDebuggingStatus: PrivilegeUiWirelessAdbStatus,
    wirelessPairingServiceStatus: PrivilegeUiWirelessAdbStatus,
    wirelessPairingCheckStatus: PrivilegeUiWirelessAdbStatus,
): PrivilegeUiWirelessAdbPanelStatus {
    return when {
        !wifiConnected -> PrivilegeUiWirelessAdbPanelStatus.WIFI_REQUIRED
        wirelessDebuggingStatus == PrivilegeUiWirelessAdbStatus.OFF ->
            PrivilegeUiWirelessAdbPanelStatus.OFF
        wirelessPairingCheckStatus == PrivilegeUiWirelessAdbStatus.ON ->
            PrivilegeUiWirelessAdbPanelStatus.PAIRED
        wirelessPairingCheckStatus == PrivilegeUiWirelessAdbStatus.OFF ->
            PrivilegeUiWirelessAdbPanelStatus.UNPAIRED
        wirelessPairingServiceStatus == PrivilegeUiWirelessAdbStatus.ON ->
            PrivilegeUiWirelessAdbPanelStatus.PAIRABLE
        wirelessDebuggingStatus == PrivilegeUiWirelessAdbStatus.ON ->
            PrivilegeUiWirelessAdbPanelStatus.PAIRED
        else -> PrivilegeUiWirelessAdbPanelStatus.OFF
    }
}

internal fun privilegeUiWirelessAdbStartActionLabel(
    action: PrivilegeUiStartAction,
): Int = privilegeUiStartActionLabel(action, R.string.priv_ui_adb_wireless_start_action)

internal fun staticTcpPanelStatus(
    tcpModeConfigured: Boolean,
    tcpModeActive: Boolean,
    status: PrivilegeUiAdbTcpAuthorizationStatus,
): PrivilegeUiStaticTcpPanelStatus =
    when {
        !tcpModeConfigured -> PrivilegeUiStaticTcpPanelStatus.PORT_NOT_CONFIGURED
        !tcpModeActive || status == PrivilegeUiAdbTcpAuthorizationStatus.UNAVAILABLE ->
            PrivilegeUiStaticTcpPanelStatus.ADB_SERVICE_STOPPED
        status == PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZED -> PrivilegeUiStaticTcpPanelStatus.AUTHORIZED
        else -> PrivilegeUiStaticTcpPanelStatus.UNAUTHORIZED
    }

internal fun staticTcpActionEnabled(
    action: PrivilegeUiStartAction,
    busy: Boolean,
    wirelessAdbSupported: Boolean,
    tcpModeConfigured: Boolean,
): Boolean =
    privilegeUiStartActionEnabled(
        action = action,
        startEnabled = !busy &&
            (tcpModeConfigured || wirelessAdbSupported),
    )

internal fun PrivilegeUiScreenState.staticTcpStartActionEnabled(
    action: PrivilegeUiStartAction,
    wirelessAdbSupported: Boolean,
    interactionEnabled: Boolean,
): Boolean =
    staticTcpActionEnabled(
        action = action,
        busy = busy,
        wirelessAdbSupported = wirelessAdbSupported,
        tcpModeConfigured = staticTcp.configuredPort != null,
    ) &&
        interactionEnabled

internal fun staticTcpCommandHelpVisible(
    wirelessAdbSupported: Boolean,
): Boolean =
    !wirelessAdbSupported

internal fun staticTcpActionLabel(
    action: PrivilegeUiStartAction,
): Int = privilegeUiStartActionLabel(action, R.string.priv_ui_adb_static_start_action)
