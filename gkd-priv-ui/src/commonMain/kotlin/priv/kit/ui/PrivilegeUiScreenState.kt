package priv.kit.ui

/** Host-owned presentation snapshot. Replace it to update the page; no runtime is consulted.
 * [startupModes] must be nonempty. Text supplied by the host must already be localized.
 * [connectionSerial] changes when a connection is replaced, resetting connection-specific UI.
 */
public data class PrivilegeUiScreenState(
    public val localNetworkPermissionMissing: Boolean = false,
    public val localNetworkPermissionSettingsRequired: Boolean = false,
    public val wirelessAdbSupported: Boolean = true,
    public val adbTcpPolicy: PrivilegeUiAdbTcpPolicy = PrivilegeUiAdbTcpPolicy.PREFER_EXISTING,
    public val tcpPort: Int = 5555,
    public val batteryOptimizationPromptVisible: Boolean = false,
    public val staticTcpSwitchConfirmation: priv.kit.ui.adb.PrivilegeUiStaticTcpSwitchAction? = null,
    public val busy: Boolean = false,
    public val runtimeStatus: PrivilegeUiRuntimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED,
    public val runtimeStartSource: PrivilegeUiRuntimeStartSource? = null,
    public val serverUid: Int? = null,
    public val connectionSerial: Long = 0L,
    public val deniedServerPermissions: List<String> = emptyList(),
    public val selectedStartupMode: PrivilegeUiStartupMode = PrivilegeUiStartupMode.ADB,
    public val startupModes: List<PrivilegeUiStartupMode> = listOf(
        PrivilegeUiStartupMode.ROOT,
        PrivilegeUiStartupMode.ADB,
        PrivilegeUiStartupMode.MANUAL_SHELL,
    ),
    public val runtimeProgressText: String? = null,
    public val manualShellCommandLine: String? = null,
    public val pairingCode: String = "",
    public val pairingStatus: PrivilegeUiAdbPairingStatus = PrivilegeUiAdbPairingStatus.NOT_PAIRED,
    public val pairingText: String? = null,
    public val pairingDialogVisible: Boolean = false,
    public val pairingNotificationPermissionWarningVisible: Boolean = false,
    public val wirelessDebuggingStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    public val wirelessPairingServiceStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    public val wirelessPairingCheckStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    public val wirelessAdbStatusLoaded: Boolean = false,
    public val managedWirelessAdbStatus: PrivilegeUiManagedWirelessAdbStatus =
        PrivilegeUiManagedWirelessAdbStatus.UNKNOWN,
    public val wifiConnected: Boolean = false,
    public val staticTcp: PrivilegeUiStaticTcpState = PrivilegeUiStaticTcpState(),
    public val tcpAuthorizationFailureDialogVisible: Boolean = false,
    public val adbKeyFingerprint: String? = null,
    public val notificationPairingRunning: Boolean = false,
    public val externalStartItems: List<PrivilegeUiExternalStartItemState> = emptyList(),
    public val startupLogLines: List<String> = emptyList(),
    public val runtimeStartPhase: PrivilegeUiRuntimeStartPhase = PrivilegeUiRuntimeStartPhase.IDLE,
    public val runtimeStartProviderId: String? = null,
    public val permissionRestrictionStatus: PrivilegeUiPermissionRestrictionStatus =
        PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
    public val desiredEnabled: Boolean = false,
    public val restartConfirmationTarget: PrivilegeUiServerRestartRequest? = null,
)
