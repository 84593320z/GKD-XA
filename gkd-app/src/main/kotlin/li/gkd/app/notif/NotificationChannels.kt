package li.gkd.app.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.app

enum class AppNotificationChannel(
    val id: String,
    private val label: String? = null,
    val description: String? = null,
    val importance: Int = NotificationManager.IMPORTANCE_LOW,
) {
    Service(id = "0"),
    Snapshot(id = "1", label = UiStrings.snapshot_notification_channel),

    /** 触发提示 / 实时通知（默认重要性，便于系统与厂商提升为岛/胶囊） */
    Action(
        id = "2",
        label = "触发提示",
        description = "规则触发时的实时状态通知：ColorOS 流体云 / HyperOS 超级岛",
        importance = NotificationManager.IMPORTANCE_DEFAULT,
    );

    val displayName: String
        get() = label ?: META.appName
}

object NotificationChannels {
    fun initialize() {
        val manager = NotificationManagerCompat.from(app)
        val channelIds = AppNotificationChannel.entries.mapTo(mutableSetOf()) { it.id }

        manager.notificationChannels
            .filter { it.id !in channelIds }
            .forEach { manager.deleteNotificationChannel(it.id) }

        manager.createNotificationChannels(
            AppNotificationChannel.entries.map { spec ->
                NotificationChannel(
                    spec.id,
                    spec.displayName,
                    spec.importance,
                ).apply {
                    description = spec.description
                }
            }
        )
    }
}
