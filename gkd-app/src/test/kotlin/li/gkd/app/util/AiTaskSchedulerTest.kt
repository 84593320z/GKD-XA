package li.gkd.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class AiTaskSchedulerTest {
    @Test fun bothModesRunSeriallyAndCapacityIncludesActiveTask() = runBlocking {
        withTimeout(5_000) {
            val owner = SupervisorJob(coroutineContext[Job])
            val started = Channel<String>(Channel.UNLIMITED)
            val release = Channel<Unit>(Channel.UNLIMITED)
            val processed = mutableListOf<String>()
            var active = 0
            var maxActive = 0
            val scheduler = AiTaskScheduler<String>(CoroutineScope(coroutineContext + owner), capacity = 2) { mode ->
                active++
                maxActive = maxOf(maxActive, active)
                try {
                    processed += mode
                    started.send(mode)
                    release.receive()
                } finally {
                    active--
                }
            }
            try {
                assertTrue(scheduler.enqueue("ordinary"))
                assertTrue(scheduler.enqueue("enhanced"))
                assertFalse(scheduler.enqueue("overflow"))
                assertEquals(2, scheduler.pendingCount.value)
                assertEquals("ordinary", started.receive())
                assertTrue(scheduler.isGenerating.value)
                assertTrue("second mode must wait for the first", started.tryReceive().isFailure)
                assertFalse(scheduler.enqueue("overflow while active"))
                assertEquals(2, scheduler.pendingCount.value)

                release.send(Unit)
                assertEquals("enhanced", started.receive())
                assertEquals(1, scheduler.pendingCount.value)
                assertTrue(scheduler.isGenerating.value)
                release.send(Unit)
                scheduler.pendingCount.first { it == 0 }
                assertFalse(scheduler.isGenerating.value)
                assertEquals(listOf("ordinary", "enhanced"), processed)
                assertEquals(1, maxActive)
            } finally {
                owner.cancelAndJoin()
                started.cancel()
                release.cancel()
            }
        }
    }

    @Test fun taskFailureAndLocalCancellationDoNotStrandFollowingTask() = runBlocking {
        withTimeout(5_000) {
            val owner = SupervisorJob(coroutineContext[Job])
            val failures = mutableListOf<Exception>()
            val processed = mutableListOf<String>()
            val scheduler = AiTaskScheduler<String>(
                CoroutineScope(coroutineContext + owner),
                onFailure = { failures += it },
            ) { task ->
                processed += task
                when (task) {
                    "invalid response" -> error("invalid response")
                    "changed context" -> throw CancellationException("changed context")
                    else -> Unit
                }
            }
            try {
                assertTrue(scheduler.enqueue("invalid response"))
                assertTrue(scheduler.enqueue("changed context"))
                assertTrue(scheduler.enqueue("success"))
                scheduler.pendingCount.first { it == 0 }
                assertEquals(listOf("invalid response", "changed context", "success"), processed)
                assertEquals(2, failures.size)
                assertTrue(failures[0] is IllegalStateException)
                assertTrue(failures[1] is CancellationException)
                assertFalse(scheduler.isGenerating.value)

                assertTrue(scheduler.enqueue("next request"))
                scheduler.pendingCount.first { it == 0 }
                assertEquals("next request", processed.last())
            } finally {
                owner.cancelAndJoin()
            }
        }
    }

    @Test fun awaitedTaskRetainsCallerResourcesThroughQueueAndExecution(): Unit = runBlocking {
        withTimeout(5_000) {
            val owner = SupervisorJob(coroutineContext[Job])
            val started = Channel<String>(Channel.UNLIMITED)
            val release = Channel<Unit>(Channel.UNLIMITED)
            val admitted = CompletableDeferred<Unit>()
            var hidden = false
            val scheduler = AiTaskScheduler<String>(CoroutineScope(coroutineContext + owner)) { task ->
                if (task == "enhanced") assertTrue(hidden)
                started.send(task)
                release.receive()
                if (task == "enhanced") assertTrue(hidden)
            }
            try {
                assertTrue(scheduler.enqueue("ordinary"))
                assertEquals("ordinary", started.receive())
                val caller = async {
                    hidden = true
                    try { scheduler.enqueueAndAwait("enhanced") { admitted.complete(Unit) } }
                    finally { hidden = false }
                }
                admitted.await()
                assertTrue(hidden)
                assertFalse(caller.isCompleted)
                release.send(Unit)
                assertEquals("enhanced", started.receive())
                assertTrue(hidden)
                assertFalse(caller.isCompleted)
                release.send(Unit)
                assertTrue(caller.await())
                assertFalse(hidden)
                scheduler.pendingCount.first { it == 0 }
            } finally {
                owner.cancelAndJoin()
                started.cancel()
                release.cancel()
            }
        }
    }

    @Test fun cancellingAwaitedCallerSkipsQueuedWorkAndJoinsActiveCleanup() = runBlocking {
        withTimeout(5_000) {
            val owner = SupervisorJob(coroutineContext[Job])
            val blockerRelease = CompletableDeferred<Unit>()
            val blockerStarted = CompletableDeferred<Unit>()
            val activeStarted = CompletableDeferred<Unit>()
            val cleanupStarted = CompletableDeferred<Unit>()
            val cleanupRelease = CompletableDeferred<Unit>()
            val processed = mutableListOf<String>()
            var hidden = false
            val scheduler = AiTaskScheduler<String>(CoroutineScope(coroutineContext + owner)) { task ->
                processed += task
                when (task) {
                    "blocker" -> { blockerStarted.complete(Unit); blockerRelease.await() }
                    "active" -> {
                        activeStarted.complete(Unit)
                        try { awaitCancellation() }
                        finally {
                            withContext(NonCancellable) {
                                cleanupStarted.complete(Unit)
                                cleanupRelease.await()
                                assertTrue(hidden)
                            }
                        }
                    }
                }
            }
            try {
                assertTrue(scheduler.enqueue("blocker"))
                blockerStarted.await()
                val admitted = CompletableDeferred<Unit>()
                val queued = launch { scheduler.enqueueAndAwait("cancelled queued") { admitted.complete(Unit) } }
                admitted.await()
                queued.cancelAndJoin()
                blockerRelease.complete(Unit)
                scheduler.pendingCount.first { it == 0 }
                assertEquals(listOf("blocker"), processed)
                val active = launch {
                    hidden = true
                    try { scheduler.enqueueAndAwait("active") }
                    finally { hidden = false }
                }
                activeStarted.await()
                active.cancel()
                cleanupStarted.await()
                assertTrue(hidden)
                assertFalse(active.isCompleted)
                cleanupRelease.complete(Unit)
                active.join()
                assertFalse(hidden)
                assertTrue(scheduler.enqueue("next"))
                scheduler.pendingCount.first { it == 0 }
                assertEquals(listOf("blocker", "active", "next"), processed)
            } finally {
                blockerRelease.complete(Unit)
                cleanupRelease.complete(Unit)
                owner.cancelAndJoin()
            }
        }
    }

    @Test fun ownerCancellationReleasesActiveTaskAndRejectsNewWork() = runBlocking {
        withTimeout(5_000) {
            val owner = SupervisorJob(coroutineContext[Job])
            val started = CompletableDeferred<Unit>()
            val processed = mutableListOf<String>()
            val failures = mutableListOf<Exception>()
            var released = false
            val scheduler = AiTaskScheduler<String>(
                CoroutineScope(coroutineContext + owner),
                onFailure = { failures += it },
            ) { task ->
                processed += task
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    released = true
                }
            }
            try {
                assertTrue(scheduler.enqueue("active"))
                assertTrue(scheduler.enqueue("queued"))
                started.await()
                owner.cancelAndJoin()
                assertTrue(released)
                assertEquals(listOf("active"), processed)
                assertEquals(0, scheduler.pendingCount.value)
                assertFalse(scheduler.isGenerating.value)
                assertTrue("owner cancellation must propagate without being reported as a task failure", failures.isEmpty())
                assertFalse(scheduler.enqueue("after shutdown"))
                assertEquals(0, scheduler.pendingCount.value)
            } finally {
                owner.cancelAndJoin()
            }
        }
    }
}
