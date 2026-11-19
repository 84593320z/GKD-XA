package priv.kit.ui.state

import androidx.annotation.StringRes
import priv.kit.core.isPrivilegeExistingServerStopFailure
import priv.kit.core.PrivilegeStartupException
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.R

internal enum class PrivilegeUiFailureKind(
    @field:StringRes val messageResId: Int,
) {
    START_FAILED(R.string.priv_ui_start_failed),
    RESTART_STOP_FAILED(R.string.priv_ui_restart_stop_failed),
    ROOT_UNAVAILABLE(R.string.priv_ui_root_unavailable),
    ROOT_START_FAILED(R.string.priv_ui_root_start_failed),
    ADB_START_FAILED(R.string.priv_ui_adb_start_failed),
    EXTERNAL_START_FAILED(R.string.priv_ui_external_start_failed),
    STOP_SERVICE_FAILED(R.string.priv_ui_stop_service_failed),
    TCP_ENABLE_FAILED(R.string.priv_ui_tcp_enable_failed),
    TCP_DISABLE_FAILED(R.string.priv_ui_tcp_disable_failed),
    TCP_RESTART_FAILED(R.string.priv_ui_tcp_restart_failed),
    TCP_AUTHORIZATION_FAILED(R.string.priv_ui_tcp_authorization_failed),
    PAIRING_CODE_REQUIRED(R.string.priv_ui_pairing_code_required),
    PAIRING_PORT_UNAVAILABLE(R.string.priv_ui_pairing_port_unavailable),
    PAIRING_FAILED(R.string.priv_ui_pairing_failed),
    PAIRING_NOTIFICATION_FAILED(R.string.priv_ui_pairing_notification_failed),
    NOTIFICATION_PERMISSION_REQUIRED(R.string.priv_ui_notification_permission_required),
}

internal fun privilegeUiRuntimeStartFailureKind(
    runtimeStartSource: PrivilegeUiRuntimeStartSource?,
    throwable: Throwable,
): PrivilegeUiFailureKind =
    if (throwable.isPrivilegeExistingServerStopFailure()) {
        PrivilegeUiFailureKind.RESTART_STOP_FAILED
    } else when (runtimeStartSource) {
        PrivilegeUiRuntimeStartSource.ROOT if throwable.hasRootUnavailableDiagnostic() -> PrivilegeUiFailureKind.ROOT_UNAVAILABLE
        PrivilegeUiRuntimeStartSource.ROOT ->
            PrivilegeUiFailureKind.ROOT_START_FAILED
        PrivilegeUiRuntimeStartSource.ADB_WIRELESS, PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP ->
            PrivilegeUiFailureKind.ADB_START_FAILED
        PrivilegeUiRuntimeStartSource.EXTERNAL ->
            PrivilegeUiFailureKind.EXTERNAL_START_FAILED
        else -> PrivilegeUiFailureKind.START_FAILED
    }

private fun Throwable.hasRootUnavailableDiagnostic(): Boolean =
    generateSequence(this) { it.cause }.any { throwable ->
        throwable is PrivilegeStartupException &&
            throwable.message.equals(ROOT_UNAVAILABLE_DIAGNOSTIC_MESSAGE, ignoreCase = true)
    }

private const val ROOT_UNAVAILABLE_DIAGNOSTIC_MESSAGE = "Root is not available"
