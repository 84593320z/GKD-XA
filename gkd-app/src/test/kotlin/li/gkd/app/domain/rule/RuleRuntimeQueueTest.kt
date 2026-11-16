package li.gkd.app.domain.rule

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class RuleRuntimeQueueTest {
    private data class Event(val key: String, val id: Int)

    @Test
    fun batchesOnlyTheContinuousPrefixWithoutLosingOrDuplicatingInterleavedEvents() {
        val released = mutableListOf<Event>()
        val queue = OwnedEventQueue<Event>(8, { a, b -> a.key == b.key }, released::add)
        val input = listOf(Event("A", 1), Event("B", 2), Event("A", 3), Event("A", 4))
        input.forEach(queue::add)
        val batches = listOf(queue.takePrefix(), queue.takePrefix(), queue.takePrefix())
        assertEquals(listOf(listOf(input[0]), listOf(input[1]), input.takeLast(2)), batches)
        assertEquals(input, batches.flatten())
        assertTrue(queue.takePrefix().isEmpty())
        assertTrue(released.isEmpty()) // ownership was transferred to the consumer
        batches.flatten().forEach(released::add)
        queue.close()
        assertEquals(input, released)
    }

    @Test
    fun overflowAndCloseReleaseSnapshotsExactlyOnceAndDrainTransfersOwnership() {
        val released = mutableListOf<Int>()
        val queue = OwnedEventQueue<Int>(2, { a, b -> a == b }, released::add)
        listOf(1, 2, 3).forEach(queue::add)
        assertEquals(listOf(1), released)
        assertEquals(listOf(2, 3), queue.drain())
        queue.add(4)
        queue.close()
        queue.close()
        queue.add(5)
        assertEquals(listOf(1, 4, 5), released)
    }

    @Test
    fun resourceTransferAndAbandonmentDoNotDoubleRelease() {
        val releases = mutableListOf<Int>()
        val transferred = OwnedResource(1, releases::add)
        assertEquals(1, transferred.take())
        assertNull(transferred.take())
        transferred.close()
        val abandoned = OwnedResource(2, releases::add)
        abandoned.close()
        abandoned.close()
        assertEquals(listOf(2), releases)
    }

    @Test
    fun requestArrivingAfterSnapshotIsConsumedWithoutASecondWakeup() {
        val wakeup = QueryWakeup<Set<Int>> { a, b -> a + b }
        assertTrue(wakeup.request(setOf(1)))
        assertEquals(setOf(1), wakeup.takeOrFinish())
        assertFalse(wakeup.request(setOf(2)))
        assertFalse(wakeup.request(setOf(3)))
        assertEquals(setOf(2, 3), wakeup.takeOrFinish())
        assertNull(wakeup.takeOrFinish())
        assertTrue(wakeup.request(setOf(4)))
        assertEquals(setOf(4), wakeup.takeOrFinish())
    }

    @Test
    fun racingExitAndProducerAlwaysRetainEitherTheCurrentOrANewReservation() {
        val workers = Executors.newFixedThreadPool(2)
        try {
            repeat(300) { id ->
                val wakeup = QueryWakeup<Int> { _, b -> b }
                assertTrue(wakeup.request(-1))
                assertEquals(-1, wakeup.takeOrFinish())
                val start = CountDownLatch(1)
                val finish = workers.submit<Int?> { start.await(); wakeup.takeOrFinish() }
                val producer = workers.submit<Boolean> { start.await(); wakeup.request(id) }
                start.countDown()
                val next = finish.get(2, TimeUnit.SECONDS)
                val reserved = producer.get(2, TimeUnit.SECONDS)
                if (reserved) {
                    assertNull(next)
                    assertEquals(id, wakeup.takeOrFinish())
                } else {
                    assertEquals(id, next)
                }
                assertNull(wakeup.takeOrFinish())
            }
        } finally {
            workers.shutdownNow()
        }
    }

    @Test
    fun mergedRequestsPreserveEventsAndAllDelayedRulesWithoutNarrowingNormalWork() {
        val event = QueryRequest<String>(true, false, true, emptySet())
        val forced = QueryRequest<String>(false, true, true, emptySet())
        val delayed = QueryRequest(false, false, false, setOf("first"))
        val anotherDelay = QueryRequest(false, false, false, setOf("second"))
        val merged = event.merge(forced).merge(delayed).merge(anotherDelay)
        assertTrue(merged.byEvent)
        assertTrue(merged.normal)
        assertFalse(merged.byForced)
        assertEquals(setOf("first", "second"), merged.delayRules)
        assertFalse(delayed.merge(anotherDelay).normal)
        assertTrue(forced.merge(forced).byForced)
    }
}
