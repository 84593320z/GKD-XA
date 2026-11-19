package priv.kit.ui.runtime

import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiStartupMode
import priv.kit.ui.PrivilegeUiState

internal sealed interface PrivilegeUiDirectStartTarget {
    data object Adb : PrivilegeUiDirectStartTarget
    data object Root : PrivilegeUiDirectStartTarget
    data class External(val providerId: String) : PrivilegeUiDirectStartTarget
}

internal fun PrivilegeUiState.directStartTargets(
    tcpPolicy: PrivilegeUiAdbTcpPolicy,
    wirelessAdbSupported: Boolean,
): List<PrivilegeUiDirectStartTarget> =
    buildList {
        directStartModeOrder().forEach { mode ->
            when (mode) {
                PrivilegeUiStartupMode.ADB -> {
                    if (tcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED || wirelessAdbSupported) {
                        add(PrivilegeUiDirectStartTarget.Adb)
                    }
                }
                PrivilegeUiStartupMode.EXTERNAL -> {
                    externalStartItems
                        .filter { it.snapshot.canStart }
                        .forEach { add(PrivilegeUiDirectStartTarget.External(it.id)) }
                }
                PrivilegeUiStartupMode.MANUAL_SHELL -> Unit
                PrivilegeUiStartupMode.ROOT -> add(PrivilegeUiDirectStartTarget.Root)
            }
        }
    }

internal fun PrivilegeUiState.directStartModeOrder(): List<PrivilegeUiStartupMode> =
    buildList {
        if (selectedStartupMode in startupModes) {
            add(selectedStartupMode)
        }
        startupModes.forEach { mode ->
            if (mode !in this) {
                add(mode)
            }
        }
    }
