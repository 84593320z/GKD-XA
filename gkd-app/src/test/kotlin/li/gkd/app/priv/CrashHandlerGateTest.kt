package li.gkd.app.priv

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashHandlerGateTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun secondaryExceptionCannotKillBeforePrimaryWriterPersists() {
        val gate = CrashHandlerGate(writerTimeoutMillis = 3_500)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val persisted = CountDownLatch(1)
        val writes = AtomicInteger()
        val kills = AtomicInteger()
        val state = temporary.root.resolve("crash.state")
        val primary = Thread {
            gate.handle(
                writeCrash = {
                    started.countDown()
                    release.await()
                    writes.incrementAndGet()
                    PersistedCrashWindow(state, threshold = 2).recordCrash(1_000)
                    persisted.countDown()
                },
                terminate = { kills.incrementAndGet() },
            )
        }
        val secondary = Thread {
            gate.handle(
                writeCrash = { writes.incrementAndGet() },
                terminate = { kills.incrementAndGet() },
            )
        }
        try {
            primary.start()
            assertTrue(started.await(1, TimeUnit.SECONDS))
            secondary.start()
            secondary.join(1_000)
            assertFalse(secondary.isAlive)
            assertEquals(0, kills.get())
            assertFalse(state.exists())
            release.countDown()
            primary.join(2_000)
            assertFalse(primary.isAlive)
            assertEquals(0L, persisted.count)
            assertEquals(1, writes.get())
            assertEquals(1, kills.get())
            // The first persisted crash survives recreating the process-owned window.
            assertFalse(PersistedCrashWindow(state, threshold = 2).isBlocked)
            assertTrue(PersistedCrashWindow(state, threshold = 2).recordCrash(2_000))
        } finally {
            release.countDown()
            primary.join(4_000)
            if (secondary.state != Thread.State.NEW) secondary.join(1_000)
        }
    }

    @Test fun stuckWriterStillEndsAtPrimaryDeadline() {
        val gate = CrashHandlerGate(writerTimeoutMillis = 100)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writerDone = CountDownLatch(1)
        val kills = AtomicInteger()
        val primary = Thread {
            gate.handle(
                writeCrash = {
                    started.countDown()
                    try { release.await() } finally { writerDone.countDown() }
                },
                terminate = { kills.incrementAndGet() },
            )
        }
        try {
            primary.start()
            assertTrue(started.await(1, TimeUnit.SECONDS))
            primary.join(2_000)
            assertFalse("Primary handler exceeded bounded writer wait", primary.isAlive)
            assertEquals(1, kills.get())
            assertEquals(1L, writerDone.count)
        } finally {
            release.countDown()
            primary.join(2_000)
            assertTrue(writerDone.await(1, TimeUnit.SECONDS))
        }
    }

    @Test fun interruptedPrimaryDoesNotSkipPersistence() {
        val gate = CrashHandlerGate(writerTimeoutMillis = 3_500)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writes = AtomicInteger()
        val kills = AtomicInteger()
        val interruptRestored = AtomicBoolean()
        val primary = Thread {
            gate.handle(
                writeCrash = {
                    started.countDown()
                    release.await()
                    writes.incrementAndGet()
                },
                terminate = {
                    interruptRestored.set(Thread.currentThread().isInterrupted)
                    kills.incrementAndGet()
                },
            )
        }
        try {
            primary.start()
            assertTrue(started.await(1, TimeUnit.SECONDS))
            primary.interrupt()
            // A reentrant/secondary handler must not steal writer or kill ownership either.
            gate.handle({ writes.incrementAndGet() }, { kills.incrementAndGet() })
            assertEquals(0, kills.get())
            release.countDown()
            primary.join(2_000)
            assertFalse(primary.isAlive)
            assertEquals(1, writes.get())
            assertEquals(1, kills.get())
            assertTrue(interruptRestored.get())
        } finally {
            release.countDown()
            primary.join(4_000)
        }
    }
}
