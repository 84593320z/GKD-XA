package priv.kit.ui

import kotlinx.coroutines.CompletableDeferred

internal sealed class PrivilegeUiPermissionRequest(
    private val interactionPermit: AutoCloseable,
    private val systemPrompt: PrivilegeUiSystemPrompt,
    private val beginSystemPrompt: (PrivilegeUiSystemPrompt, String) -> AutoCloseable,
) : AutoCloseable {
    private val lock = Any()
    private val completion = CompletableDeferred<PrivilegeUiPermissionState?>()
    private var launchedHostId: String? = null
    private var systemPromptSession: AutoCloseable? = null
    private var finished = false

    internal val wasLaunched: Boolean
        get() = synchronized(lock) { launchedHostId != null }

    internal fun tryMarkLaunched(hostId: String): Boolean =
        synchronized(lock) {
            if (finished || launchedHostId != null) false
            else {
                launchedHostId = hostId
                systemPromptSession = beginSystemPrompt(systemPrompt, hostId)
                true
            }
        }

    internal fun wasLaunchedBy(hostId: String): Boolean =
        synchronized(lock) { launchedHostId == hostId }

    internal fun complete(
        hostId: String,
        permissionState: PrivilegeUiPermissionState,
    ) {
        finish(permissionState) { launchedHostId == hostId }
    }

    internal fun completeUnlaunched(permissionState: PrivilegeUiPermissionState) {
        finish(permissionState) { launchedHostId == null }
    }

    internal fun cancel(hostId: String) {
        finish(null) { launchedHostId == null || launchedHostId == hostId }
    }

    internal suspend fun awaitCompletion(): PrivilegeUiPermissionState? = completion.await()

    final override fun close() {
        finish(null) { true }
    }

    private fun finish(
        result: PrivilegeUiPermissionState?,
        canFinish: () -> Boolean,
    ) {
        var promptSession: AutoCloseable? = null
        val claimed = synchronized(lock) {
            if (finished || !canFinish()) false
            else {
                finished = true
                promptSession = systemPromptSession
                systemPromptSession = null
                true
            }
        }
        if (!claimed) return
        completion.complete(result)
        runCatching { promptSession?.close() }
        runCatching { interactionPermit.close() }
    }

    class Notification(
        interactionPermit: AutoCloseable,
        beginSystemPrompt: (PrivilegeUiSystemPrompt, String) -> AutoCloseable,
    ) : PrivilegeUiPermissionRequest(
        interactionPermit = interactionPermit,
        systemPrompt = privilegeUiNotificationPermissionPrompt(),
        beginSystemPrompt = beginSystemPrompt,
    )

    class LocalNetwork(
        val permission: String,
        interactionPermit: AutoCloseable,
        beginSystemPrompt: (PrivilegeUiSystemPrompt, String) -> AutoCloseable,
    ) : PrivilegeUiPermissionRequest(
        interactionPermit = interactionPermit,
        systemPrompt = privilegeUiLocalNetworkPermissionPrompt(),
        beginSystemPrompt = beginSystemPrompt,
    )
}
