package priv.kit.ui

public enum class PrivilegeUiRuntimeStatus {
    DISCONNECTED,
    STARTING,
    CONNECTED,
    FAILED,
}

public enum class PrivilegeUiRuntimeStartPhase {
    IDLE,
    RUNNING,
    CANCELLING,
}

public enum class PrivilegeUiPermissionRestrictionStatus {
    UNKNOWN,
    NOT_RESTRICTED,
    RESTRICTED,
}

public enum class PrivilegeUiStartupMode {
    ROOT,
    MANUAL_SHELL,
    ADB,
    EXTERNAL,
}

public enum class PrivilegeUiRuntimeStartSource {
    ROOT,
    ADB_WIRELESS,
    ADB_STATIC_TCP,
    EXTERNAL,
}

public sealed interface PrivilegeUiServerRestartRequest {
    public data object Root : PrivilegeUiServerRestartRequest
    public data object Adb : PrivilegeUiServerRestartRequest
    public data object WirelessAdb : PrivilegeUiServerRestartRequest
    public data object StaticTcpAdb : PrivilegeUiServerRestartRequest
    public data class External public constructor(
        public val providerId: String,
    ) : PrivilegeUiServerRestartRequest
}

public enum class PrivilegeUiAdbTcpPolicy {
    DISABLED,
    PREFER_EXISTING,
    AUTO_ENABLE_AFTER_WIRELESS_PAIRED,
}

public enum class PrivilegeUiAdbPairingStatus {
    NOT_PAIRED,
    CHECKING,
    SEARCHING,
    FOUND,
    PAIRING,
    PAIRED,
    FAILED,
}

public enum class PrivilegeUiWirelessAdbStatus {
    UNKNOWN,
    CHECKING,
    ON,
    OFF,
}

public enum class PrivilegeUiManagedWirelessAdbStatus {
    UNKNOWN,
    CHECKING,
    READY,
    UNDECLARED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    FAILED,
}

public enum class PrivilegeUiAdbTcpAuthorizationStatus {
    UNKNOWN,
    CHECKING,
    AUTHORIZING,
    AUTHORIZED,
    UNAUTHORIZED,
    UNAVAILABLE,
    FAILED,
}

public data class PrivilegeUiExternalStartSnapshot public constructor(
    public val available: Boolean = false,
    public val authorized: Boolean = false,
    public val uid: Int? = null,
    public val version: Int? = null,
    public val message: CharSequence = "",
    public val exceptionText: String = "",
) {
    public val canStart: Boolean
        get() = available && authorized
}

public data class PrivilegeUiExternalStartItemState(
    public val id: String,
    public val label: CharSequence,
    public val snapshot: PrivilegeUiExternalStartSnapshot = PrivilegeUiExternalStartSnapshot(),
    public val statusLoaded: Boolean = false,
)

public data class PrivilegeUiStaticTcpState(
    public val activePort: Int? = null,
    public val configuredPort: Int? = null,
    public val authorizationStatus: PrivilegeUiAdbTcpAuthorizationStatus =
        PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN,
    public val loaded: Boolean = false,
)
