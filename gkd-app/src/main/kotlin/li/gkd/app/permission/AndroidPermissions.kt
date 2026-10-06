package li.gkd.app.permission

/**
 * 恢复自本地构建 APK（仓库 main 没有这个文件）。
 * 常量值逐条取自 debug 包字段初始化，非推测；源码里的书写顺序无法从字节码还原，此处按字母序。
 */
object AndroidPermissions {
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    const val DUMP = "android.permission.DUMP"
    const val GET_APP_OPS_STATS = "android.permission.GET_APP_OPS_STATS"
    const val GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"
    const val GRANT_RUNTIME_PERMISSIONS = "android.permission.GRANT_RUNTIME_PERMISSIONS"
    const val INJECT_EVENTS = "android.permission.INJECT_EVENTS"
    const val MANAGE_APP_OPS_MODES = "android.permission.MANAGE_APP_OPS_MODES"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
    const val UPDATE_APP_OPS_STATS = "android.permission.UPDATE_APP_OPS_STATS"
    const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"
    const val WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"
}
