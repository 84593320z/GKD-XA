package priv.kit.ui.adb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import priv.kit.ui.state.PrivilegeUiViewModelStore

internal fun privilegeUiRequiredLocalNetworkPermission(context: Context): String? {
    if (!isPrivilegeUiLocalNetworkPermissionSupported()) return null
    val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
    return permission.takeIf {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.CINNAMON_BUN)
private fun isPrivilegeUiLocalNetworkPermissionSupported(): Boolean =
    privilegeUiRequiresLocalNetworkPermissionForSdk(Build.VERSION.SDK_INT)

internal fun privilegeUiRequiresLocalNetworkPermissionForSdk(sdkInt: Int): Boolean =
    sdkInt >= Build.VERSION_CODES.CINNAMON_BUN

/** Refresh the advisory permission card; this is not an ADB operation gate. */
internal fun PrivilegeUiViewModelStore.refreshLocalNetworkPermission(): Boolean {
    val missing = !hasAdbLocalNetworkPermission()
    if (missing == state.value.localNetworkPermissionMissing) return !missing
    updateState {
        it.copy(
            localNetworkPermissionMissing = missing,
            localNetworkPermissionSettingsRequired = missing && it.localNetworkPermissionSettingsRequired,
        )
    }
    return !missing
}

internal fun PrivilegeUiViewModelStore.hasAdbLocalNetworkPermission(): Boolean =
    applicationContext?.let { privilegeUiRequiredLocalNetworkPermission(it) == null }
        ?: !state.value.localNetworkPermissionMissing
