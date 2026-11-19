package priv.kit.ui.adb

import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiManagedWirelessAdbStatus
import priv.kit.ui.PrivilegeUiStaticTcpState
import priv.kit.ui.state.PrivilegeUiViewModelStore

internal fun PrivilegeUiViewModelStore.currentConfiguredTcpPort(): Int? =
    state.value.staticTcp.configuredPort
        .takeIf { config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED }

internal fun PrivilegeUiViewModelStore.managedWirelessAdbEnabledForStart(): Boolean =
    config.enableManagedWirelessAdb &&
        state.value.managedWirelessAdbStatus != PrivilegeUiManagedWirelessAdbStatus.UNDECLARED

internal fun PrivilegeUiViewModelStore.updateStaticTcp(
    transform: (PrivilegeUiStaticTcpState) -> PrivilegeUiStaticTcpState,
) {
    updateState {
        it.copy(staticTcp = transform(it.staticTcp))
    }
}
