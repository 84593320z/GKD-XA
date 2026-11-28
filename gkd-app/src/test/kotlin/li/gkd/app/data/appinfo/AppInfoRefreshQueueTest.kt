package li.gkd.app.data.appinfo

import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppInfoRefreshQueueTest {
    @Test
    fun sameIdEnqueuedDuringSuccessfulBatchGetsAnotherBatch() = runBlocking {
        val queue = AppInfoRefreshQueue(debounceMillis = 0, waitBeforeRetry = {})
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val batches = CopyOnWriteArrayList<Set<String>>()
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.collectBatches({ batch ->
                batches += batch
                if (batches.size == 1) {
                    entered.complete(Unit)
                    finish.await()
                }
            }, {})
        }
        try {
            queue.enqueue("app.same")
            withTimeout(5_000) { entered.await() }
            queue.enqueue("app.same")
            finish.complete(Unit)
            withTimeout(5_000) { while (batches.size < 2) delay(1) }
            assertEquals(listOf(setOf("app.same"), setOf("app.same")), batches.toList())
        } finally {
            worker.cancelAndJoin()
        }
    }

    @Test
    fun failedBatchRequeuesIdsArrivingDuringFailure() = runBlocking {
        val queue = AppInfoRefreshQueue(debounceMillis = 0, waitBeforeRetry = {})
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val batches = CopyOnWriteArrayList<Set<String>>()
        var first = true
        val worker = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.collectBatches({ batch ->
                batches += batch
                if (first) {
                    first = false
                    entered.complete(Unit)
                    finish.await()
                    error("first update failed")
                }
            }, {})
        }
        try {
            queue.enqueue("app.same")
            withTimeout(5_000) { entered.await() }
            queue.enqueue("app.same")
            finish.complete(Unit)
            withTimeout(5_000) { while (batches.size < 2) delay(1) }
            assertEquals(setOf("app.same"), batches[1])
        } finally {
            worker.cancelAndJoin()
        }
    }

    @Test
    fun cancellationRequeuesBatchForRestartedCollector() = runBlocking {
        val queue = AppInfoRefreshQueue(debounceMillis = 0, waitBeforeRetry = {})
        val entered = CompletableDeferred<Unit>()
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.collectBatches({
                entered.complete(Unit)
                awaitCancellation()
            }, {})
        }
        queue.enqueue("app.restart")
        withTimeout(5_000) { entered.await() }
        first.cancelAndJoin()

        val retried = CompletableDeferred<Set<String>>()
        val restarted = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.collectBatches({ retried.complete(it) }, {})
        }
        try {
            assertEquals(setOf("app.restart"), withTimeout(5_000) { retried.await() })
        } finally {
            restarted.cancelAndJoin()
        }
    }
}
