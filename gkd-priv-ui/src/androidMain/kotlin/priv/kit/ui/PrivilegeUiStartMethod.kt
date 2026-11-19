package priv.kit.ui

/** A startup method that the UI can save for automatic recovery. */
public sealed class PrivilegeUiStartMethod {
    public data object Root : PrivilegeUiStartMethod()
    public data object AdbWireless : PrivilegeUiStartMethod()
    public data object AdbTcpip : PrivilegeUiStartMethod()

    public data class External public constructor(
        public val providerId: String,
    ) : PrivilegeUiStartMethod()
}
