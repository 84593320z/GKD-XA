package li.gkd.app.priv

import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InstallSessionRegistryTest {
    @Test fun pendingTimeoutThenLateFailureReleasesProcessPendingAndRestartSuppression() = runBlocking {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val session = registry.register(10, "update.apk")
        val completion = InstallCompletion<String>()
        val callback = InstallSessionCallback(session, completion)
        var confirmations = 0
        var fallbacks = 0
        callback.accept(InstallEvent.Pending("confirm"))
        try {
            // This is the production listener bridge, including the disposable awaiter's finally.
            val result = completion.await(50) { confirmations++ }
            if (result is InstallEvent.Failure) fallbacks++
            fail("The pending session must still be awaiting a terminal callback")
        } catch (_: TimeoutCancellationException) {
            assertTrue(registry.isPending(session)) // Session is still alive/unknown.
        } finally {
            completion.close()
        }
        assertEquals(1, confirmations)
        assertTrue(registry.hasPendingInstallations)
        assertTrue(suppressed)
        assertNull(session.terminal)

        callback.accept(InstallEvent.Failure("user rejected"))
        assertEquals(InstallSessionTerminal.Failure("user rejected"), session.terminal)
        assertFalse(registry.isPending(session))
        assertFalse(registry.hasPendingInstallations)
        assertFalse(suppressed)
        assertEquals(0, fallbacks) // A late terminal only unlocks retry, never starts another installer.
    }

    @Test fun lateSuccessRetainsGuardAndCannotBecomeFailure() = runBlocking {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val session = registry.register(11, "update.apk")
        val completion = InstallCompletion<Unit>()
        val callback = InstallSessionCallback(session, completion)
        try {
            completion.await(30) {}
            fail("Missing callback must time out")
        } catch (_: TimeoutCancellationException) {
            // Unknown keeps the guard until an actual terminal arrives.
        } finally {
            completion.close()
        }
        callback.accept(InstallEvent.Success)
        callback.accept(InstallEvent.Failure("duplicate"))
        callback.accept(InstallEvent.Success)
        assertEquals(InstallSessionTerminal.Success, session.terminal)
        assertTrue(registry.isPending(session))
        assertTrue(suppressed)
        assertFalse(session.forget()) // Success is not an abandoned pre-commit session.
    }

    @Test fun cancelledViewModelWaiterDoesNotOwnLateTerminal() = runBlocking {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val session = registry.register(12, "update.apk")
        val completion = InstallCompletion<Unit>()
        val callback = InstallSessionCallback(session, completion)
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            try { completion.await(60_000) {} } finally { completion.close() }
        }
        waiter.cancelAndJoin()
        assertTrue(registry.hasPendingInstallations)
        assertTrue(suppressed)
        callback.accept(InstallEvent.Failure("late cancellation"))
        assertFalse(registry.hasPendingInstallations)
        assertFalse(suppressed)
    }

    @Test fun callbackDisconnectDoesNotPreventLateExplicitFailureSettlement() = runBlocking {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val session = registry.register(13, "update.apk")
        val completion = InstallCompletion<Unit>()
        val callback = InstallSessionCallback(session, completion)
        completion.disconnected()
        try { completion.await(1_000) {}; fail("Disconnected waiter did not stop") }
        catch (_: IOException) { }
        finally { completion.close() }
        assertTrue(registry.hasPendingInstallations)
        callback.accept(InstallEvent.Failure("late definitive failure"))
        assertFalse(registry.hasPendingInstallations)
        assertFalse(suppressed)
    }

    @Test fun terminalIsSettledBeforeLocalWaiterReceivesFailure() = runBlocking {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val session = registry.register(14, "update.apk")
        val completion = InstallCompletion<Unit>()
        val callback = InstallSessionCallback(session, completion)
        try {
            callback.accept(InstallEvent.Failure("failed"))
            callback.accept(InstallEvent.Success)
            assertEquals(InstallEvent.Failure("failed"), completion.await(1_000) {})
            assertEquals(InstallSessionTerminal.Failure("failed"), session.terminal)
            assertFalse(registry.hasPendingInstallations)
            assertFalse(suppressed)
        } finally { completion.close() }
    }

    @Test fun oldFailureDoesNotReleaseNewSessionsSuppression() {
        val transitions = mutableListOf<Boolean>()
        val registry = InstallSessionRegistry { transitions += it }
        val old = registry.register(20, "old.apk")
        val current = registry.register(21, "current.apk")
        assertTrue(old.settle(InstallSessionTerminal.Failure("old rejected")))
        assertFalse(registry.isPending(old))
        assertTrue(registry.isPending(current))
        assertTrue(registry.hasPendingInstallations)
        assertEquals(listOf(true), transitions)
        assertTrue(current.settle(InstallSessionTerminal.Failure("current rejected")))
        assertEquals(listOf(true, false), transitions)
    }

    @Test fun duplicateOldTerminalCannotClearNewSession() {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val old = registry.register(30, "old.apk")
        assertTrue(old.settle(InstallSessionTerminal.Failure("old rejected")))
        val current = registry.register(31, "current.apk")
        assertFalse(old.settle(InstallSessionTerminal.Failure("duplicate")))
        assertFalse(old.settle(InstallSessionTerminal.Success))
        assertFalse(old.forget())
        assertTrue(registry.isPending(current))
        assertTrue(suppressed)
    }

    @Test fun reusedNumericIdDoesNotGiveStaleHandleOwnership() {
        val transitions = mutableListOf<Boolean>()
        val registry = InstallSessionRegistry { transitions += it }
        val old = registry.register(40, "old.apk")
        val current = registry.register(40, "current.apk")
        assertFalse(old.settle(InstallSessionTerminal.Failure("stale")))
        assertFalse(old.settle(InstallSessionTerminal.Success))
        assertFalse(old.forget())
        assertNull(current.terminal)
        assertTrue(registry.isPending(current))
        assertEquals(listOf(true), transitions)
        assertTrue(current.settle(InstallSessionTerminal.Failure("current")))
        assertEquals(listOf(true, false), transitions)
    }

    @Test fun confirmedPreCommitAbandonReleasesOnlyItsOwnGuard() {
        var suppressed = false
        val registry = InstallSessionRegistry { suppressed = it }
        val old = registry.register(50, "old.apk")
        val current = registry.register(51, "current.apk")
        assertTrue(old.forget())
        assertFalse(old.settle(InstallSessionTerminal.Success))
        assertTrue(registry.isPending(current))
        assertTrue(suppressed)
        assertTrue(current.forget())
        assertFalse(registry.hasPendingInstallations)
        assertFalse(suppressed)
    }
}
