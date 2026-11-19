package priv.kit.ui

/** Stable sections of the standalone user guide, also available to host-owned navigation. */
public enum class PrivilegeUiTutorialTopic(public val anchor: String) {
    ADB("adb"),
    WIRELESS_DEBUGGING("wireless-debugging"),
    PAIRING("pairing"),
    MANUAL("manual"),
    TCP_IP("tcp-ip"),
    PERMISSION_RESTRICTIONS("permission-restrictions"),
}

internal fun privilegeUiTutorialUrl(baseUrl: String, topic: PrivilegeUiTutorialTopic): String =
    baseUrl.substringBefore('#') + "#" + topic.anchor

internal fun openPrivilegeUiTutorial(
    topic: PrivilegeUiTutorialTopic,
    baseUrl: String,
    onViewPermissionSolutions: (() -> Unit)?,
    onViewStartupTutorial: ((PrivilegeUiTutorialTopic) -> Unit)?,
    openUri: (String) -> Unit,
    onOpenFailure: () -> Unit,
) {
    when {
        topic == PrivilegeUiTutorialTopic.PERMISSION_RESTRICTIONS && onViewPermissionSolutions != null ->
            onViewPermissionSolutions()
        onViewStartupTutorial != null -> onViewStartupTutorial(topic)
        else -> try {
            openUri(privilegeUiTutorialUrl(baseUrl, topic))
        } catch (_: Exception) {
            onOpenFailure()
        }
    }
}
