package priv.kit.ui.state

internal fun privilegeUiStaticTcpOpenCommand(tcpPort: Int): String =
    "adb tcpip $tcpPort"
