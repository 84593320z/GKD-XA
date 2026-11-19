package priv.kit.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

internal sealed interface PrivilegeUiPermissionState {
    data object Granted : PrivilegeUiPermissionState

    sealed interface NotGranted : PrivilegeUiPermissionState {
        data object Denied : NotGranted

        data object PermanentlyDenied : NotGranted
    }
}

internal fun privilegeUiPermissionState(
    activity: Activity,
    permission: String,
): PrivilegeUiPermissionState {
    if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
        return PrivilegeUiPermissionState.Granted
    }
    return privilegeUiPermissionState(
        granted = false,
        requested = PrivilegeUiPermissionRequestHistory.contains(permission),
        shouldShowRequestPermissionRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission),
    )
}

internal fun markPrivilegeUiPermissionRequested(permission: String) {
    PrivilegeUiPermissionRequestHistory.add(permission)
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
internal fun isPrivilegeUiNotificationPermissionSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

internal fun PrivilegeUiPermissionState.shouldLaunchPermissionRequest(): Boolean =
    this is PrivilegeUiPermissionState.NotGranted

internal fun privilegeUiPermissionState(
    granted: Boolean,
    requested: Boolean,
    shouldShowRequestPermissionRationale: Boolean,
): PrivilegeUiPermissionState =
    when {
        granted -> PrivilegeUiPermissionState.Granted
        !requested || shouldShowRequestPermissionRationale ->
            PrivilegeUiPermissionState.NotGranted.Denied
        else -> PrivilegeUiPermissionState.NotGranted.PermanentlyDenied
    }

private object PrivilegeUiPermissionRequestHistory {
    private val requestedPermissions = ConcurrentHashMap.newKeySet<String>()

    fun add(permission: String) {
        requestedPermissions += permission
    }

    fun contains(permission: String): Boolean = permission in requestedPermissions
}
