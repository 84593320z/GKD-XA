package li.gkd.app.domain.rule

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SubmittedActionTest {
    @Test
    fun rejectedSubmissionCannotBecomeSuccessEvenWithASynchronousCallback() = runBlocking {
        assertFalse(awaitSubmittedAction(1_000) { callback -> callback(true); false })
    }

    @Test
    fun acceptedSubmissionRemainsPendingUntilCompletionAndIgnoresDuplicates() = runBlocking {
        val submitted = CompletableDeferred<(Boolean) -> Unit>()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            awaitSubmittedAction(2_000) { callback -> submitted.complete(callback); true }
        }
        val callback = submitted.await()
        assertFalse(result.isCompleted)
        callback(true)
        callback(false)
        assertTrue(withTimeout(2_000) { result.await() })
    }

    @Test
    fun platformCancellationIsFailureAndLateSuccessCannotChangeIt() = runBlocking {
        lateinit var callback: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            awaitSubmittedAction(2_000) { callback = it; true }
        }
        callback(false)
        callback(true)
        assertFalse(result.await())
    }

    @Test
    fun absentCallbackTimesOutAndLateCallbackDoesNotReopenTheResult() = runBlocking {
        lateinit var callback: (Boolean) -> Unit
        val result = awaitSubmittedAction(20) { callback = it; true }
        assertFalse(result)
        callback(true)
        callback(false)
    }

    @Test
    fun callerCancellationPropagatesAndIgnoresALateCompletion() = runBlocking {
        lateinit var callback: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            awaitSubmittedAction(2_000) { callback = it; true }
        }
        result.cancelAndJoin()
        assertTrue(result.isCancelled)
        callback(true)
    }
}
