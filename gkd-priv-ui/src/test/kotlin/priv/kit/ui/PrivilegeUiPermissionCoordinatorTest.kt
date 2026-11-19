package priv.kit.ui

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegeUiPermissionCoordinatorTest {
    @Test
    fun closeIsIdempotentAndReleasesActiveRequestOnce() = runBlocking {
        val permitsClosed = AtomicInteger(0)
        val coordinator = coordinator {
            AutoCloseable { permitsClosed.incrementAndGet() }
        }
        coordinator.registerHost("permission-host")
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.requestNotificationPermission()
        }
        coordinator.requests.first()

        coordinator.close()
        coordinator.close()

        assertEquals(null, result.await())
        assertEquals(1, permitsClosed.get())
    }

    @Test
    fun notificationResultResumesTheRequestingCoroutine() = runBlocking {
        val permitsClosed = AtomicInteger(0)
        val coordinator = coordinator {
            AutoCloseable { permitsClosed.incrementAndGet() }
        }
        val hostId = "permission-host"
        coordinator.registerHost(hostId)
        try {
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                coordinator.requestNotificationPermission()
            }
            val request = coordinator.requests.first() as PrivilegeUiPermissionRequest.Notification
            assertTrue(request.tryMarkLaunched(hostId))

            coordinator.completeNotificationPermissionRequest(
                hostId = hostId,
                permissionState = PrivilegeUiPermissionState.NotGranted.PermanentlyDenied,
            )

            assertEquals(PrivilegeUiPermissionState.NotGranted.PermanentlyDenied, result.await())
            assertEquals(1, permitsClosed.get())
        } finally {
            coordinator.unregisterHost(hostId, changingConfigurations = false)
            coordinator.close()
        }
    }

    @Test
    fun localNetworkResultResumesTheRequestingCoroutine() = runBlocking {
        val permitsClosed = AtomicInteger(0)
        val coordinator = coordinator {
            AutoCloseable { permitsClosed.incrementAndGet() }
        }
        val hostId = "permission-host"
        coordinator.registerHost(hostId)
        try {
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                coordinator.requestLocalNetworkPermission("android.permission.ACCESS_LOCAL_NETWORK")
            }
            val request = coordinator.requests.first() as PrivilegeUiPermissionRequest.LocalNetwork
            assertTrue(request.tryMarkLaunched(hostId))

            coordinator.completeLocalNetworkPermissionRequest(
                hostId = hostId,
                permissionState = PrivilegeUiPermissionState.Granted,
            )

            assertEquals(PrivilegeUiPermissionState.Granted, result.await())
            assertEquals(1, permitsClosed.get())
        } finally {
            coordinator.unregisterHost(hostId, changingConfigurations = false)
            coordinator.close()
        }
    }

    @Test
    fun launchedPermissionUsesSystemPromptUntilHostReturnsToForeground() = runBlocking {
        val hostId = "permission-host"
        val promptCoordinator = PrivilegeUiSystemPromptCoordinator().also {
            it.registerHost(
                hostId = hostId,
                resumed = true,
                hasWindowFocus = true,
            )
        }
        val coordinator = coordinator(
            acquirePermit = { AutoCloseable {} },
            systemPromptCoordinator = promptCoordinator,
        )
        coordinator.registerHost(hostId)
        try {
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                coordinator.requestNotificationPermission()
            }
            val request = coordinator.requests.first() as PrivilegeUiPermissionRequest.Notification
            assertTrue(request.tryMarkLaunched(hostId))

            promptCoordinator.onHostPaused(hostId)

            val visiblePrompt = promptCoordinator.visiblePrompt.value
            assertEquals(
                R.string.priv_ui_system_prompt_notification_title,
                (visiblePrompt?.prompt?.title as PrivilegeUiText.Resource).id,
            )

            coordinator.completeNotificationPermissionRequest(
                hostId = hostId,
                permissionState = PrivilegeUiPermissionState.Granted,
            )
            assertEquals(PrivilegeUiPermissionState.Granted, result.await())
            assertEquals(visiblePrompt, promptCoordinator.visiblePrompt.value)

            promptCoordinator.onHostResumed(hostId, hasWindowFocus = true)
            assertEquals(null, promptCoordinator.visiblePrompt.value)
        } finally {
            coordinator.unregisterHost(hostId, changingConfigurations = false)
            coordinator.close()
            promptCoordinator.close()
        }
    }

    @Test
    fun removingTheLastHostCancelsTheSuspendedPermissionRequest() = runBlocking {
        val permitsClosed = AtomicInteger(0)
        val coordinator = coordinator {
            AutoCloseable { permitsClosed.incrementAndGet() }
        }
        val hostId = "permission-host"
        coordinator.registerHost(hostId)
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.requestLocalNetworkPermission("android.permission.ACCESS_LOCAL_NETWORK")
        }
        coordinator.requests.first()

        coordinator.unregisterHost(hostId, changingConfigurations = false)

        assertEquals(null, result.await())
        assertEquals(1, permitsClosed.get())
        coordinator.close()
    }

    private fun coordinator(
        systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator =
            PrivilegeUiSystemPromptCoordinator(),
        acquirePermit: () -> AutoCloseable?,
    ): PrivilegeUiPermissionCoordinator =
        PrivilegeUiPermissionCoordinator(
            acquireInteractivePermit = acquirePermit,
            interactionsEnabled = { true },
            ownerClosed = { false },
            cancelPairingWithoutInteractionHost = {},
            systemPromptCoordinator = systemPromptCoordinator,
        )
}
