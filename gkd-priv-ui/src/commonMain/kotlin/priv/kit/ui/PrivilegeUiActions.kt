package priv.kit.ui

/**
 * Explicit host handlers. Unsupported operations should report feedback instead of silently succeeding.
 * [canInteract] checks the current interaction lease at click time; it must allow cancellation while busy.
 * [requestBatteryOptimization] returns whether the host opened a request/settings surface.
 * Handlers run on the UI thread; launch long-running work in a host-owned scope.
 */
public class PrivilegeUiActions(
    public val authorizeOrStartExternal: (String) -> Unit,
    public val canInteract: () -> Boolean,
    public val cancelPendingPairingStart: () -> Unit,
    public val cancelServerRestart: () -> Unit,
    public val cancelStaticTcpSwitch: () -> Unit,
    public val clearStartupLog: () -> Unit,
    public val confirmServerRestart: () -> Unit,
    public val confirmStaticTcpSwitch: () -> Unit,
    public val continuePairingWithoutNotification: () -> Unit,
    public val copyManualCommand: () -> Unit,
    public val copyStartupLog: () -> Unit,
    public val copyStaticTcpCommand: () -> Unit,
    public val disableAutoRecovery: () -> Unit,
    public val disableTcpMode: () -> Unit,
    public val dismissTcpAuthorizationFailureDialog: () -> Unit,
    public val enableTcpMode: () -> Unit,
    public val openNotificationSettings: () -> Unit,
    public val requestLocalNetworkPermission: () -> Unit,
    public val requestBatteryOptimization: () -> Boolean,
    public val restartTcpMode: () -> Unit,
    public val selectStartupMode: (PrivilegeUiStartupMode) -> Unit,
    public val startInteractive: () -> Unit,
    public val startNotificationPairing: () -> Unit,
    public val startRoot: () -> Unit,
    public val startStaticTcpAdb: () -> Unit,
    public val startWirelessAdb: () -> Unit,
    public val stopCurrentStart: () -> Unit,
    public val stopNotificationPairing: () -> Unit,
    public val stopServer: () -> Unit,
    public val submitNotificationPairingCode: () -> Unit,
    public val updatePairingCode: (String) -> Unit,
)
