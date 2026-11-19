package priv.kit.ui.adb.pairing

import androidx.annotation.RestrictTo
import priv.kit.ui.state.PrivilegeUiFailureKind

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public enum class PrivilegeAdbPairingNotificationUnavailableReason {
    NOTIFICATION_PERMISSION_REQUIRED,
    FOREGROUND_SERVICE_FAILED,
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed interface PrivilegeAdbPairingNotificationEvent {
    public val ownerId: String

    public data class Submit public constructor(
        public override val ownerId: String,
        public val pairingCode: String,
    ) : PrivilegeAdbPairingNotificationEvent

    public data class Unavailable public constructor(
        public override val ownerId: String,
        public val message: String,
        public val reason: PrivilegeAdbPairingNotificationUnavailableReason,
    ) : PrivilegeAdbPairingNotificationEvent {
        public constructor(ownerId: String, message: String) : this(
            ownerId = ownerId,
            message = message,
            reason = PrivilegeAdbPairingNotificationUnavailableReason.FOREGROUND_SERVICE_FAILED,
        )
    }

    public data class Stop public constructor(
        public override val ownerId: String,
    ) : PrivilegeAdbPairingNotificationEvent

    public data class Detached public constructor(
        public override val ownerId: String,
    ) : PrivilegeAdbPairingNotificationEvent
}

internal fun PrivilegeAdbPairingNotificationUnavailableReason.toPrivilegeUiFailureKind(): PrivilegeUiFailureKind =
    when (this) {
        PrivilegeAdbPairingNotificationUnavailableReason.NOTIFICATION_PERMISSION_REQUIRED ->
            PrivilegeUiFailureKind.NOTIFICATION_PERMISSION_REQUIRED
        PrivilegeAdbPairingNotificationUnavailableReason.FOREGROUND_SERVICE_FAILED ->
            PrivilegeUiFailureKind.PAIRING_NOTIFICATION_FAILED
    }
