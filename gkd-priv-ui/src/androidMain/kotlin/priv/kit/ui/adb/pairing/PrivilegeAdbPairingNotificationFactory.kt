package priv.kit.ui.adb.pairing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import priv.kit.ui.PrivilegeAdbPairingService
import priv.kit.ui.PrivilegeUi
import priv.kit.ui.R
import priv.kit.ui.asString
import priv.kit.ui.privilegeUiText

internal class PrivilegeAdbPairingNotificationFactory(
    private val context: Context,
    private val notificationSpec: PrivilegeAdbPairingNotificationSpec,
) {
    fun ensureNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                notificationSpec.channelId,
                text(R.string.priv_ui_pairing_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                setShowBadge(false)
            },
        )
    }

    fun statusNotification(
        text: String,
    ): Notification =
        baseNotification(
            title = text(R.string.priv_ui_pairing_working_title),
            text = text,
        )
            .addAction(stopAction())
            .addAction(replyAction())
            .buildPersistent()

    fun inputNotification(
        state: PrivilegeAdbPairingInputState,
    ): Notification {
        return baseNotification(
            title = text(R.string.priv_ui_pairing_reply_notification_title),
            text = text(R.string.priv_ui_pairing_reply_notification_text),
        )
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(pairingInputRemoteViews(state))
            .setCustomBigContentView(pairingInputRemoteViews(state))
            .setCustomHeadsUpContentView(pairingInputRemoteViews(state))
            .buildPersistent()
    }

    fun workingNotification(text: String): Notification =
        baseNotification(
            title = text(R.string.priv_ui_pairing_working_title),
            text = text,
        )
            .addAction(stopAction())
            .buildPersistent()

    private fun baseNotification(
        title: String,
        text: String,
    ): Notification.Builder =
        Notification.Builder(context, notificationSpec.channelId)
            .setSmallIcon(notificationSmallIcon())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)

    private fun notificationSmallIcon(): Int =
        context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.sym_def_app_icon

    private fun pairingInputRemoteViews(state: PrivilegeAdbPairingInputState): RemoteViews =
        RemoteViews(context.packageName, R.layout.priv_ui_notification_pairing_code).apply {
            applyPairingInputTextColor()
            setContentDescription(
                R.id.priv_ui_pairing_close,
                text(R.string.priv_ui_pairing_close_description),
            )
            setOnClickPendingIntent(
                R.id.priv_ui_pairing_close,
                stopPendingIntent(),
            )
            setTextViewText(R.id.priv_ui_pairing_code_text, state.displayText)
            setContentDescription(
                R.id.priv_ui_pairing_code_text,
                text(R.string.priv_ui_pairing_submit_code_description),
            )
            setOnClickPendingIntent(
                R.id.priv_ui_pairing_code_text,
                pairingInputPendingIntent(
                    action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_SUBMIT,
                    requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_SUBMIT,
                ),
            )
            setContentDescription(
                R.id.priv_ui_pairing_confirm,
                text(R.string.priv_ui_pairing_submit_code_description),
            )
            setOnClickPendingIntent(
                R.id.priv_ui_pairing_confirm,
                pairingInputPendingIntent(
                    action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_SUBMIT,
                    requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_SUBMIT,
                ),
            )
            bindPairingInputAction(
                viewId = R.id.priv_ui_pairing_arrow_left,
                contentDescription = text(R.string.priv_ui_pairing_arrow_left_description),
                action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_LEFT,
                requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_LEFT,
            )
            bindPairingInputAction(
                viewId = R.id.priv_ui_pairing_arrow_up,
                contentDescription = text(R.string.priv_ui_pairing_arrow_up_description),
                action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_UP,
                requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_UP,
            )
            bindPairingInputAction(
                viewId = R.id.priv_ui_pairing_arrow_down,
                contentDescription = text(R.string.priv_ui_pairing_arrow_down_description),
                action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_DOWN,
                requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_DOWN,
            )
            bindPairingInputAction(
                viewId = R.id.priv_ui_pairing_arrow_right,
                contentDescription = text(R.string.priv_ui_pairing_arrow_right_description),
                action = PrivilegeAdbPairingIntentContract.ACTION_INPUT_RIGHT,
                requestCode = PrivilegeAdbPairingIntentContract.REQUEST_INPUT_RIGHT,
            )
        }

    private fun RemoteViews.applyPairingInputTextColor() {
        val color = pairingInputTextColor()
        pairingInputTextViewIds.forEach { viewId ->
            setTextColor(viewId, color)
        }
    }

    private fun pairingInputTextColor(): Int =
        if (context.resources.configuration.isNightMode()) {
            Color.rgb(230, 225, 229)
        } else {
            Color.rgb(29, 27, 32)
        }

    private fun Configuration.isNightMode(): Boolean =
        uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun RemoteViews.bindPairingInputAction(
        viewId: Int,
        contentDescription: String,
        action: String,
        requestCode: Int,
    ) {
        setContentDescription(viewId, contentDescription)
        setOnClickPendingIntent(
            viewId,
            pairingInputPendingIntent(
                action = action,
                requestCode = requestCode,
            ),
        )
    }

    private fun pairingInputPendingIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntentCompat.getForegroundService(
            context,
            requestCode,
            serviceIntent(action),
            PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )

    private fun replyAction(): Notification.Action {
        val remoteInput = RemoteInput.Builder(PrivilegeAdbPairingIntentContract.REMOTE_INPUT_PAIRING_CODE)
            .setLabel(text(R.string.priv_ui_pairing_reply_label))
            .setAllowFreeFormInput(true)
            .build()
        val pendingIntent = PendingIntentCompat.getForegroundService(
            context,
            PrivilegeAdbPairingIntentContract.REQUEST_REPLY,
            serviceIntent(PrivilegeAdbPairingIntentContract.ACTION_REPLY),
            PendingIntent.FLAG_UPDATE_CURRENT,
            true,
        )
        val actionBuilder = Notification.Action.Builder(
            null,
            text(R.string.priv_ui_pairing_reply_action),
            pendingIntent,
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            actionBuilder.setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY)
        }
        return actionBuilder.build()
    }

    private fun stopAction(): Notification.Action {
        return Notification.Action.Builder(
            null,
            text(R.string.priv_ui_pairing_stop_action),
            stopPendingIntent(),
        ).build()
    }

    private fun stopPendingIntent(): PendingIntent =
        PendingIntentCompat.getForegroundService(
            context,
            PrivilegeAdbPairingIntentContract.REQUEST_STOP,
            serviceIntent(PrivilegeAdbPairingIntentContract.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )

    private fun serviceIntent(action: String): Intent =
        Intent(context, PrivilegeAdbPairingService::class.java)
            .setAction(action)

    private fun Notification.Builder.buildPersistent(): Notification =
        build().apply {
            flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        }

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(context)

    private fun text(@StringRes id: Int): String =
        privilegeUiText(id).asString(context, PrivilegeUi.localeOverride)

    private companion object {
        val pairingInputTextViewIds = intArrayOf(
            R.id.priv_ui_pairing_close,
            R.id.priv_ui_pairing_code_text,
            R.id.priv_ui_pairing_confirm,
            R.id.priv_ui_pairing_arrow_left,
            R.id.priv_ui_pairing_arrow_up,
            R.id.priv_ui_pairing_arrow_down,
            R.id.priv_ui_pairing_arrow_right,
        )
    }
}
