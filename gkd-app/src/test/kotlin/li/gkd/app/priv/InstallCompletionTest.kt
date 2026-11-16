package li.gkd.app.priv

import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class InstallCompletionTest {
    @Test fun firstTerminalCallbackWinsAndPendingFloodIsBounded() = runBlocking {
        val completion = InstallCompletion<Int>()
        try {
            repeat(10_000) { completion.accept(InstallEvent.Pending(it)) }
            completion.accept(InstallEvent.Success)
            completion.accept(InstallEvent.Failure("late duplicate"))
            completion.disconnected()
            assertEquals(InstallEvent.Success, completion.await(1_000) { fail("Stale pending action") })
        } finally { completion.close() }
    }

    @Test fun pendingActionCanProduceTerminalFailure() = runBlocking {
        val completion = InstallCompletion<String>()
        try {
            completion.accept(InstallEvent.Pending("confirm"))
            val result = completion.await(1_000) {
                assertEquals("confirm", it)
                completion.accept(InstallEvent.Failure("user rejected"))
            }
            assertEquals(InstallEvent.Failure("user rejected"), result)
        } finally { completion.close() }
    }

    @Test fun callbackDeathDiscardsPendingAndIgnoresLateSuccess() = runBlocking {
        val completion = InstallCompletion<String>()
        try {
            completion.accept(InstallEvent.Pending("stale"))
            completion.disconnected()
            completion.accept(InstallEvent.Success)
            try { completion.await(1_000) { fail("Opened action after death") }; fail("Death did not complete") }
            catch (_: IOException) { }
        } finally { completion.close() }
    }

    @Test fun missingCallbackHasDeadline() = runBlocking {
        val completion = InstallCompletion<Unit>()
        try {
            try { completion.await(30) {}; fail("Missing callback waited forever") }
            catch (_: TimeoutCancellationException) { }
        } finally { completion.close() }
    }

    @Test fun pendingActionAlsoSharesDeadline() = runBlocking {
        val completion = InstallCompletion<Unit>()
        try {
            completion.accept(InstallEvent.Pending(Unit))
            try { completion.await(30) { delay(5_000) }; fail("Pending action escaped deadline") }
            catch (_: TimeoutCancellationException) { }
        } finally { completion.close() }
    }

    @Test fun externalCancellationCompletesWaitingJob() = runBlocking {
        val completion = InstallCompletion<Unit>()
        try {
            val waiter = async(start = CoroutineStart.UNDISPATCHED) { completion.await(60_000) {} }
            waiter.cancelAndJoin()
            assertTrue(waiter.isCancelled)
            assertTrue(waiter.isCompleted)
        } finally { completion.close() }
    }
}
