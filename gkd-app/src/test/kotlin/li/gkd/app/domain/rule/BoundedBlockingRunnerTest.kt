package li.gkd.app.domain.rule

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BoundedBlockingRunnerTest {
    @Test
    fun timedOutUninterruptibleCallsKeepTheirSlotsAndLateValuesAreDiscarded() = runBlocking {
        val runner = BoundedBlockingRunner(2, "binder-test")
        val entered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val discarded = CountDownLatch(2)
        val startedCount = AtomicInteger()
        val rejectedCount = AtomicInteger()
        try {
            val requests = (1..2).map {
                async(start = CoroutineStart.UNDISPATCHED) {
                    runner.run(100, onDiscard = { _: Int -> discarded.countDown() }) {
                        startedCount.incrementAndGet()
                        entered.countDown()
                        // Model a blocking platform call by intentionally ignoring interruption.
                        while (true) {
                            try { release.await(); break } catch (_: InterruptedException) { }
                        }
                        42
                    }
                }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            requests.forEach { assertNull(it.await()) }
            repeat(100) {
                assertNull(runner.run(1_000, onNotStarted = { rejectedCount.incrementAndGet() }) {
                    startedCount.incrementAndGet(); 7
                })
            }
            assertEquals(2, startedCount.get())
            assertEquals(100, rejectedCount.get())
            release.countDown()
            assertTrue(discarded.await(2, TimeUnit.SECONDS))
        } finally { release.countDown(); runner.close() }
    }

    @Test
    fun cancellingTheCallerDiscardsLateValueWithoutFreeingTheWorkerEarly() = runBlocking {
        val runner = BoundedBlockingRunner(1, "cancel-test")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val discarded = CountDownLatch(1)
        try {
            val request = async(start = CoroutineStart.UNDISPATCHED) {
                runner.run(5_000, onDiscard = { _: Int -> discarded.countDown() }) {
                    entered.countDown(); release.await(); 1
                }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            request.cancelAndJoin()
            assertTrue(request.isCancelled)
            var unexpectedlyStarted = false
            assertNull(runner.run(1_000) { unexpectedlyStarted = true; 2 })
            assertFalse(unexpectedlyStarted)
            release.countDown()
            assertTrue(discarded.await(2, TimeUnit.SECONDS))
        } finally { release.countDown(); runner.close() }
    }

    @Test
    fun synchronousDeadlineDoesNotQueueOrPublishLateResults() {
        val runner = BoundedBlockingRunner(1, "sync-test")
        val release = CountDownLatch(1)
        val discarded = CountDownLatch(1)
        try {
            assertNull(runner.call(30, onDiscard = { _: Int -> discarded.countDown() }) {
                release.await(); 1
            })
            var unexpectedlyStarted = false
            assertNull(runner.call(30) { unexpectedlyStarted = true; 2 })
            assertFalse(unexpectedlyStarted)
            release.countDown()
            assertTrue(discarded.await(2, TimeUnit.SECONDS))
        } finally { release.countDown(); runner.close() }
    }

    @Test
    fun successfulValueIsDeliveredWithoutDiscardAndExceptionsReturnNull() = runBlocking {
        BoundedBlockingRunner(1, "success-test").use { runner ->
            assertEquals(9, runner.run(1_000, onDiscard = { _: Int -> fail("successful value discarded") }) { 9 })
        }
        BoundedBlockingRunner(1, "failure-test").use { runner ->
            assertNull(runner.run<Int>(1_000) { throw IllegalStateException("platform failure") })
        }
    }
}
