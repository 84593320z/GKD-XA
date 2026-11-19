package priv.kit.ui

import android.content.Context
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.PrivilegeStartupException
import priv.kit.core.PrivilegeStartupLogListener
import priv.kit.core.adb.PRIVILEGE_ADB_DEFAULT_TCP_PORT
import priv.kit.ui.adb.pairing.PrivilegeAdbPairingIntentContract
import priv.kit.shared.PRIVILEGE_INTERNAL_DEFAULT_ADB_AUTHORIZATION_TIMEOUT_MILLIS
import priv.kit.shared.PRIVILEGE_INTERNAL_DEFAULT_START_TIMEOUT_MILLIS
import priv.kit.shared.isPrivilegeAdbPort
import priv.kit.shared.toPrivilegeAdbPairingCodeDigits
import priv.kit.ui.runtime.requireValidManualShellScriptPath

public data class PrivilegeUiConfig public constructor(
    public val startupModes: List<PrivilegeUiStartupMode> = listOf(
        PrivilegeUiStartupMode.ROOT,
        PrivilegeUiStartupMode.ADB,
        PrivilegeUiStartupMode.MANUAL_SHELL,
    ),
    public val externalStartProviders: List<PrivilegeUiExternalStartProvider> = emptyList(),
    public val adbDeviceName: String? = null,
    public val tcpPort: Int = PRIVILEGE_ADB_DEFAULT_TCP_PORT,
    public val adbTcpPolicy: PrivilegeUiAdbTcpPolicy = PrivilegeUiAdbTcpPolicy.PREFER_EXISTING,
    public val enableManagedWirelessAdb: Boolean = true,
    /** Stable channel ID reserved by the host application for notification pairing. */
    public val notificationPairingChannelId: String =
        PrivilegeAdbPairingIntentContract.NOTIFICATION_CHANNEL_ID,
    /** First of two consecutive notification IDs reserved for notification pairing. */
    public val notificationPairingNotificationId: Int =
        PrivilegeAdbPairingIntentContract.NOTIFICATION_ID,
    /**
     * Maximum wait for the ADB key response after an active static TCP port is selected.
     * Wireless Debugging discovery, pairing, and static-port enablement are outside this timeout.
     */
    public val adbAuthorizationTimeoutMillis: Long =
        PRIVILEGE_INTERNAL_DEFAULT_ADB_AUTHORIZATION_TIMEOUT_MILLIS,
    public val wirelessStatusPollIntervalMillis: Long = DEFAULT_WIRELESS_STATUS_POLL_INTERVAL_MILLIS,
    public val wirelessStatusDiscoveryTimeoutMillis: Long = DEFAULT_WIRELESS_STATUS_DISCOVERY_TIMEOUT_MILLIS,
    public val externalStartStatusPollIntervalMillis: Long = DEFAULT_EXTERNAL_START_STATUS_POLL_INTERVAL_MILLIS,
    public val startTimeoutMillis: Long = DEFAULT_START_TIMEOUT_MILLIS,
    /**
     * Defaults to `null`, using the full native starter command without writing an external script.
     * Other shell processes can modify external scripts before execution, causing altered code to
     * run with the executing caller's privileges. Only opt in if the host accepts this risk.
     * The recommended opt-in path is `priv.sh`.
     *
     * Startup script path relative to `Context.getExternalFilesDir(null)?.parentFile`, the app's
     * external `Android/data/<package>` directory, including the filename. For example,
     * `files/sh/priv.sh` uses the `files/sh` subdirectory; `priv.sh` sits alongside `files`.
     * The script command is displayed and can be copied immediately. The UI prepares the script in
     * the background once during ViewModel initialization, independently of silent startup. Matching
     * content is reused; missing or changed scripts are written, creating parent directories.
     * Switching tabs or resuming the page does not prepare it again. Successful preparation leaves
     * the displayed command unchanged.
     * Call [PrivilegeUi.prepareExternalShellScript] to prepare the script without opening UI.
     * Writes use the system-returned directory; the displayed command uses `/sdcard/Android/data`.
     * Absolute paths, empty components, `.` and `..`, backslashes, and control characters are
     * rejected.
     * Unavailable storage or a file read/write failure falls back to the full native starter command.
     * Nonzero Android users always use the full command without accessing external storage,
     * because the executing shell may not be able to read their external directories.
     * Use letters, digits, `/`, `.`, `_`, and `-` for portable desktop commands; other characters
     * use POSIX host-shell quoting.
     */
    public val manualShellScriptPath: String? = null,
) {
    init {
        requireValidManualShellScriptPath(manualShellScriptPath)
        require(startupModes.distinct().size == startupModes.size) {
            "startup modes must be unique"
        }
        require(startTimeoutMillis > 0L) { "startTimeoutMillis must be positive" }
        require(tcpPort.isPrivilegeAdbPort()) { "tcpPort must be between 1 and 65535" }
        require(notificationPairingChannelId.isNotBlank()) {
            "notificationPairingChannelId must not be blank"
        }
        require(notificationPairingNotificationId in 1 until Int.MAX_VALUE) {
            "notificationPairingNotificationId must be between 1 and ${Int.MAX_VALUE - 1}"
        }
        require(adbAuthorizationTimeoutMillis > 0L) { "adbAuthorizationTimeoutMillis must be positive" }
        require(wirelessStatusPollIntervalMillis > 0L) {
            "wirelessStatusPollIntervalMillis must be positive"
        }
        require(wirelessStatusDiscoveryTimeoutMillis > 0L) {
            "wirelessStatusDiscoveryTimeoutMillis must be positive"
        }
        require(externalStartStatusPollIntervalMillis > 0L) {
            "externalStartStatusPollIntervalMillis must be positive"
        }
        require(externalStartProviders.map { it.id }.distinct().size == externalStartProviders.size) {
            "external start provider ids must be unique"
        }
    }

    internal companion object {
        const val DEFAULT_START_TIMEOUT_MILLIS: Long =
            PRIVILEGE_INTERNAL_DEFAULT_START_TIMEOUT_MILLIS
        const val DEFAULT_WIRELESS_STATUS_POLL_INTERVAL_MILLIS: Long = 3_000L
        const val DEFAULT_WIRELESS_STATUS_DISCOVERY_TIMEOUT_MILLIS: Long = 1_500L
        const val DEFAULT_EXTERNAL_START_STATUS_POLL_INTERVAL_MILLIS: Long = 3_000L
    }
}

public interface PrivilegeUiExternalStartProvider {
    /** Stable identifier used to restore this provider across process restarts and app upgrades. */
    public val id: String

    public val label: CharSequence

    public suspend fun snapshot(context: Context): PrivilegeUiExternalStartSnapshot =
        PrivilegeUiExternalStartSnapshot()

    public suspend fun requestAuthorization(context: Context): PrivilegeUiExternalStartSnapshot =
        snapshot(context)

    @Throws(PrivilegeStartupException::class)
    public suspend fun start(
        context: Context,
        commandLine: String,
    )
}

public interface PrivilegeUiStreamingExternalStartProvider : PrivilegeUiExternalStartProvider {
    @Throws(PrivilegeStartupException::class)
    public suspend fun start(
        context: Context,
        commandLine: String,
        startupLogListener: PrivilegeStartupLogListener,
    )
}

internal data class PrivilegeUiState(
    val localNetworkPermissionMissing: Boolean = false,
    val localNetworkPermissionSettingsRequired: Boolean = false,
    val busy: Boolean = false,
    val runtimeStatus: PrivilegeUiRuntimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED,
    val runtimeStartSource: PrivilegeUiRuntimeStartSource? = null,
    val serverInfo: PrivilegeServerInfo? = null,
    val selectedStartupMode: PrivilegeUiStartupMode = PrivilegeUiStartupMode.ADB,
    val startupModes: List<PrivilegeUiStartupMode> = listOf(
        PrivilegeUiStartupMode.ROOT,
        PrivilegeUiStartupMode.ADB,
        PrivilegeUiStartupMode.MANUAL_SHELL,
    ),
    val runtimeProgressText: PrivilegeUiText? = null,
    val manualShellCommandLine: String? = null,
    val pairingCode: String = "",
    val pairingStatus: PrivilegeUiAdbPairingStatus = PrivilegeUiAdbPairingStatus.NOT_PAIRED,
    val pairingText: PrivilegeUiText? = null,
    val pairingDialogVisible: Boolean = false,
    val pairingNotificationPermissionWarningVisible: Boolean = false,
    val wirelessDebuggingStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    val wirelessPairingServiceStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    val wirelessPairingCheckStatus: PrivilegeUiWirelessAdbStatus = PrivilegeUiWirelessAdbStatus.UNKNOWN,
    val wirelessAdbStatusLoaded: Boolean = false,
    val managedWirelessAdbStatus: PrivilegeUiManagedWirelessAdbStatus =
        PrivilegeUiManagedWirelessAdbStatus.UNKNOWN,
    val wifiConnected: Boolean = false,
    val staticTcp: PrivilegeUiStaticTcpState = PrivilegeUiStaticTcpState(),
    val tcpAuthorizationFailureDialogVisible: Boolean = false,
    val adbKeyFingerprint: String? = null,
    val notificationPairingRunning: Boolean = false,
    val externalStartItems: List<PrivilegeUiExternalStartItemState> = emptyList(),
    val startupLogLines: List<String> = emptyList(),
    val connectionSerial: Long = 0L,
    val deniedServerPermissions: List<String> = emptyList(),
    val runtimeStartPhase: PrivilegeUiRuntimeStartPhase = PrivilegeUiRuntimeStartPhase.IDLE,
    val runtimeStartProviderId: String? = null,
    val permissionRestrictionStatus: PrivilegeUiPermissionRestrictionStatus =
        PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
    val desiredEnabled: Boolean = false,
    val restartConfirmationTarget: PrivilegeUiServerRestartRequest? = null,
)

internal fun String.toPrivilegeUiPairingCodeDigits(): String =
    toPrivilegeAdbPairingCodeDigits()
