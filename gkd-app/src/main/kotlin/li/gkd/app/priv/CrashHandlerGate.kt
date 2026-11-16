package li.gkd.app.priv

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** One uncaught handler owns persistence, the deadline and termination; later handlers just return. */
internal class CrashHandlerGate(private val writerTimeoutMillis: Long = 3_500L) {
    private val primaryClaimed = AtomicBoolean(false)

    init { require(writerTimeoutMillis > 0) }

    fun handle(writeCrash: () -> Unit, terminate: () -> Unit) {
        if (!primaryClaimed.compareAndSet(false, true)) return
        try {
            // No coroutine/main dispatcher dependency: either may be the crashing thread.
            val writer = Thread({ writeCrash() }, "gkd-crash-writer").apply { isDaemon = true }
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(writerTimeoutMillis)
            writer.start()
            var interrupted = false
            try {
                while (writer.isAlive) {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0L) break
                    try {
                        TimeUnit.NANOSECONDS.timedJoin(writer, remaining)
                    } catch (_: InterruptedException) {
                        // An interruption must not let another failing thread skip persistence.
                        interrupted = true
                    }
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt()
            }
        } finally {
            terminate()
        }
    }
}
