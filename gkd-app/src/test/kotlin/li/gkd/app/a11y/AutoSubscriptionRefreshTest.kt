package li.gkd.app.a11y

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import li.gkd.app.data.subscription.SubscriptionResult
import org.junit.Assert.*
import org.junit.Test

class AutoSubscriptionRefreshTest {
    private val day = 86_400_000L

    @Test
    fun launcherEventAtOneHourRefreshesImmediatelyBusyRetriesAndCompletedRefreshWaits() = runBlocking {
        var now = 3_600_000L
        var calls = 0
        var answer: SubscriptionResult = SubscriptionResult.Busy
        val refresh = AutoSubscriptionRefresh(
            interval = { day }, minimumInterval = day, now = { now },
            launch = { block -> launch(start = CoroutineStart.UNDISPATCHED) { block() } },
            refresh = { calls++; answer },
        )
        refresh.onEvent("other.app", "launcher")
        assertEquals(0, calls)
        refresh.onEvent("launcher", "launcher")
        assertEquals(1, calls)
        answer = SubscriptionResult.Success()
        refresh.onEvent("launcher", "launcher")
        assertEquals(2, calls) // Busy did not consume the interval.
        now += day
        refresh.onEvent("launcher", "launcher")
        assertEquals(2, calls)
        now++
        refresh.onEvent("launcher", "launcher")
        assertEquals(3, calls)
    }

    @Test
    fun pendingJobCoalescesEventsAndANonBusyFailureConsumesTheIntervalEvenAtTimeZero() = runBlocking {
        var now = 0L
        var calls = 0
        val completed = CompletableDeferred<SubscriptionResult>()
        val jobs = mutableListOf<Job>()
        val refresh = AutoSubscriptionRefresh(
            interval = { 1L }, minimumInterval = day, now = { now },
            launch = { block -> launch(start = CoroutineStart.UNDISPATCHED) { block() }.also(jobs::add) },
            refresh = { calls++; completed.await() },
        )
        refresh.onEvent("launcher", "launcher")
        repeat(10) { refresh.onEvent("launcher", "launcher") }
        assertEquals(1, calls)
        completed.complete(SubscriptionResult.Failure(SubscriptionResult.FailureReason.Download))
        jobs.single().join()
        now = day
        refresh.onEvent("launcher", "launcher")
        assertEquals(1, calls)
        now++
        refresh.onEvent("launcher", "launcher")
        assertEquals(2, calls)
    }

    @Test
    fun disabledRefreshDoesNotReserveAndCancellationBeforeStartReleasesPending() = runBlocking {
        var interval = -1L
        var calls = 0
        var queued: Job? = null
        val refresh = AutoSubscriptionRefresh(
            interval = { interval }, minimumInterval = day, now = { 3_600_000L },
            launch = { block -> launch(start = CoroutineStart.LAZY) { block() }.also { queued = it } },
            refresh = { calls++; SubscriptionResult.Success() },
        )
        refresh.onEvent("launcher", "launcher")
        assertNull(queued)
        interval = day
        refresh.onEvent("launcher", "launcher")
        val abandoned = requireNotNull(queued)
        abandoned.cancel()
        abandoned.join()
        refresh.onEvent("launcher", "launcher")
        assertNotSame(abandoned, queued)
        requireNotNull(queued).start()
        requireNotNull(queued).join()
        assertEquals(1, calls)
    }
}
