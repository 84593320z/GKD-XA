package priv.kit.ui

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PrivilegeUiPermissionCoordinator(
    private val acquireInteractivePermit: () -> AutoCloseable?,
    private val interactionsEnabled: () -> Boolean,
    private val ownerClosed: () -> Boolean,
    private val cancelPairingWithoutInteractionHost: () -> Unit,
    private val systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator =
        PrivilegeUiSystemPromptCoordinator(),
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val lock = Any()
    private val requestMutex = Mutex()
    private val attachedHostIds = mutableSetOf<String>()
    private val activeRequest = MutableStateFlow<PrivilegeUiPermissionRequest?>(null)

    val requests: Flow<PrivilegeUiPermissionRequest> = activeRequest.filterNotNull()

    fun hasInteractionHost(): Boolean = synchronized(lock) { attachedHostIds.isNotEmpty() }

    suspend fun requestNotificationPermission(): PrivilegeUiPermissionState? =
        awaitRequest {
            PrivilegeUiPermissionRequest.Notification(
                interactionPermit = it,
                beginSystemPrompt = systemPromptCoordinator::begin,
            )
        }

    suspend fun requestLocalNetworkPermission(permission: String): PrivilegeUiPermissionState? =
        awaitRequest {
            PrivilegeUiPermissionRequest.LocalNetwork(
                permission = permission,
                interactionPermit = it,
                beginSystemPrompt = systemPromptCoordinator::begin,
            )
        }

    fun completeNotificationPermissionRequest(
        hostId: String,
        permissionState: PrivilegeUiPermissionState,
    ) {
        (activeRequest.value as? PrivilegeUiPermissionRequest.Notification)
            ?.complete(hostId, permissionState)
    }

    fun completeUnlaunchedNotificationPermissionRequest(
        hostId: String,
        request: PrivilegeUiPermissionRequest.Notification,
        permissionState: PrivilegeUiPermissionState,
    ) {
        if (synchronized(lock) { hostId in attachedHostIds } && activeRequest.value === request) {
            request.completeUnlaunched(permissionState)
        }
    }

    fun completeLocalNetworkPermissionRequest(
        hostId: String,
        permissionState: PrivilegeUiPermissionState,
    ) {
        (activeRequest.value as? PrivilegeUiPermissionRequest.LocalNetwork)
            ?.complete(hostId, permissionState)
    }

    fun registerHost(hostId: String) {
        synchronized(lock) {
            if (!isClosed()) attachedHostIds += hostId
        }
    }

    fun unregisterHost(
        hostId: String,
        changingConfigurations: Boolean,
    ) {
        val requestToCancel: PrivilegeUiPermissionRequest?
        val noHostsRemain: Boolean
        synchronized(lock) {
            if (!attachedHostIds.remove(hostId)) return
            noHostsRemain = attachedHostIds.isEmpty()
            requestToCancel = activeRequest.value.takeIf {
                !changingConfigurations && (noHostsRemain || it?.wasLaunchedBy(hostId) == true)
            }
        }
        requestToCancel?.close()
        if (!changingConfigurations && noHostsRemain) cancelPairingWithoutInteractionHost()
    }

    fun cancelRequest(
        hostId: String,
        request: PrivilegeUiPermissionRequest,
    ) {
        if (activeRequest.value === request) request.cancel(hostId)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(lock) { attachedHostIds.clear() }
        activeRequest.value?.close()
    }

    private suspend fun awaitRequest(
        createRequest: (AutoCloseable) -> PrivilegeUiPermissionRequest,
    ): PrivilegeUiPermissionState? = requestMutex.withLock {
        if (!interactionsEnabled()) return null
        val permit = acquireInteractivePermit() ?: return null
        val request = createRequest(permit)
        val accepted = synchronized(lock) {
            if (isClosed() || attachedHostIds.isEmpty()) false
            else {
                activeRequest.value = request
                true
            }
        }
        if (!accepted) {
            request.close()
            return null
        }
        try {
            request.awaitCompletion()
        } finally {
            if (activeRequest.value === request) activeRequest.value = null
            request.close()
        }
    }

    private fun isClosed(): Boolean = closed.get() || ownerClosed()
}
