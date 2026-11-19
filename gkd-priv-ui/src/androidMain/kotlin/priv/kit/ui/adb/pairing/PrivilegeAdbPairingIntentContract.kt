package priv.kit.ui.adb.pairing

internal object PrivilegeAdbPairingIntentContract {
    const val ACTION_START: String = "priv.kit.ui.action.START_ADB_PAIRING_NOTIFICATION"
    const val ACTION_REPLY: String = "priv.kit.ui.action.REPLY_ADB_PAIRING_NOTIFICATION"
    const val ACTION_INPUT_LEFT: String = "priv.kit.ui.action.ADB_PAIRING_INPUT_LEFT"
    const val ACTION_INPUT_UP: String = "priv.kit.ui.action.ADB_PAIRING_INPUT_UP"
    const val ACTION_INPUT_DOWN: String = "priv.kit.ui.action.ADB_PAIRING_INPUT_DOWN"
    const val ACTION_INPUT_RIGHT: String = "priv.kit.ui.action.ADB_PAIRING_INPUT_RIGHT"
    const val ACTION_INPUT_SUBMIT: String = "priv.kit.ui.action.ADB_PAIRING_INPUT_SUBMIT"
    const val ACTION_STOP: String = "priv.kit.ui.action.STOP_ADB_PAIRING_NOTIFICATION"

    const val REMOTE_INPUT_PAIRING_CODE: String = "pairing_code"
    const val EXTRA_NOTIFICATION_OWNER_ID: String = "notification_owner_id"
    const val EXTRA_NOTIFICATION_CHANNEL_ID: String = "notification_channel_id"
    const val EXTRA_NOTIFICATION_ID: String = "notification_id"
    const val NOTIFICATION_CHANNEL_ID: String = "priv_ui_adb_pairing"
    const val NOTIFICATION_ID: Int = 201
    const val REQUEST_REPLY: Int = 1
    const val REQUEST_STOP: Int = 3
    const val REQUEST_INPUT_LEFT: Int = 5
    const val REQUEST_INPUT_UP: Int = 6
    const val REQUEST_INPUT_DOWN: Int = 7
    const val REQUEST_INPUT_RIGHT: Int = 8
    const val REQUEST_INPUT_SUBMIT: Int = 9
}

internal data class PrivilegeAdbPairingNotificationSpec(
    val channelId: String,
    val notificationId: Int,
) {
    init {
        require(channelId.isNotBlank()) { "channelId must not be blank" }
        require(notificationId in 1 until Int.MAX_VALUE) {
            "notificationId must be between 1 and ${Int.MAX_VALUE - 1}"
        }
    }

    val inputNotificationId: Int
        get() = notificationId + 1

    companion object {
        val Default = PrivilegeAdbPairingNotificationSpec(
            channelId = PrivilegeAdbPairingIntentContract.NOTIFICATION_CHANNEL_ID,
            notificationId = PrivilegeAdbPairingIntentContract.NOTIFICATION_ID,
        )
    }
}
