package li.gkd.app.data.backup

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Draining the queue and lost history are different: export surviving logs with an explicit warning. */
internal data class LogFlushStatus(val drained: Boolean, val failedWrites: Long) {
    fun diagnosticWarning(): String? = when {
        !drained -> "WARNING: Log queue was not fully drained; diagnostic logs may be incomplete."
        failedWrites > 0 -> "WARNING: $failedWrites log writes failed during this process; diagnostic logs may be incomplete."
        else -> null
    }
}

/** Serial queue with a completion fence shared by exports and the crash handler. */
internal class SerialLogWriter(private val onError: (Throwable) -> Unit = {}) {
    @Volatile private var worker: Thread? = null
    private val failedWrites = AtomicLong(0)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "gkd-log-writer").apply { isDaemon = true; worker = this }
    }

    fun execute(write: () -> Unit) {
        executor.execute {
            try { write() } catch (error: Throwable) {
                failedWrites.incrementAndGet()
                runCatching { onError(error) }
            }
        }
    }

    fun flush(timeoutMillis: Long): Boolean = flushStatus(timeoutMillis).drained

    fun flushStatus(timeoutMillis: Long): LogFlushStatus {
        // The active record and any queued records cannot be drained by their own worker.
        if (Thread.currentThread() === worker) return LogFlushStatus(false, failedWrites.get())
        val fence = CountDownLatch(1)
        val failuresAtFence = AtomicLong(failedWrites.get())
        val drained = try {
            executor.execute { failuresAtFence.set(failedWrites.get()); fence.countDown() }
            fence.await(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (_: RejectedExecutionException) {
            false
        }
        return LogFlushStatus(drained, if (drained) failuresAtFence.get() else failedWrites.get())
    }

    internal fun close() { executor.shutdownNow() }
}
