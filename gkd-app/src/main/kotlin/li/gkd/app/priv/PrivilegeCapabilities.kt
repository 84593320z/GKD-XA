package li.gkd.app.priv

import android.content.pm.PackageManager
import li.gkd.app.permission.AndroidPermissions
import priv.kit.core.Privilege

/**
 * 恢复自本地构建 APK（仓库 main 无此文件）。
 * 属性顺序来自 toString()/component1..4 的字节码顺序：
 * grantRuntimePermissions, injectEvents, writeSecureSettings, updateAppOps
 */
data class PrivilegeCapabilities(
    val grantRuntimePermissions: Boolean,
    val injectEvents: Boolean,
    val writeSecureSettings: Boolean,
    val updateAppOps: Boolean,
) {
    val restricted: Boolean
        get() = !(grantRuntimePermissions && injectEvents && writeSecureSettings && updateAppOps)
}

private fun hasServerPermission(permission: String): Boolean =
    Privilege.checkServerPermission(permission) == PackageManager.PERMISSION_GRANTED

fun hasAppOpsPermission(): Boolean =
    hasServerPermission(AndroidPermissions.MANAGE_APP_OPS_MODES) ||
        hasServerPermission(AndroidPermissions.UPDATE_APP_OPS_STATS)

fun queryPrivilegeCapabilities(): PrivilegeCapabilities =
    PrivilegeCapabilities(
        grantRuntimePermissions = hasServerPermission(AndroidPermissions.GRANT_RUNTIME_PERMISSIONS),
        injectEvents = hasServerPermission(AndroidPermissions.INJECT_EVENTS),
        writeSecureSettings = hasServerPermission(AndroidPermissions.WRITE_SECURE_SETTINGS),
        updateAppOps = hasAppOpsPermission(),
    )
