package li.gkd.app.domain.rule

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * A deadline abandons the result, not the underlying Binder call. Unfinished calls
 * retain their worker slot; the zero-capacity queue rejects further work instead
 * of spawning more threads or accumulating requests. Blocks must not mutate state.
 */
internal class BoundedBlockingRunner(workerCount: Int, name: String) : AutoCloseable {
    private val nextThread = AtomicInteger()
    private val executor = ThreadPoolExecutor(
        workerCount, workerCount, 0L, TimeUnit.MILLISECONDS, SynchronousQueue(),
        { task -> Thread(task, "$name-${nextThread.incrementAndGet()}").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy(),
    )

    private fun <T : Any> submit(
        onDiscard: (T) -> Unit,
        onNotStarted: () -> Unit,
        block: () -> T?,
    ): CompletableFuture<T?> {
        val result = CompletableFuture<T?>()
        try {
            executor.execute {
                val value = try {
                    block()
                } catch (_: Exception) {
                    null
                }
                if (!result.complete(value) && value != null) onDiscard(value)
            }
        } catch (_: RejectedExecutionException) {
            onNotStarted()
            result.complete(null)
        }
        return result
    }

    suspend fun <T : Any> run(
        timeoutMillis: Long,
        onDiscard: (T) -> Unit = {},
        onNotStarted: () -> Unit = {},
        block: () -> T?,
    ): T? {
        require(timeoutMillis > 0)
        var submitted = false
        try {
            return withTimeoutOrNull(timeoutMillis) {
                suspendCancellableCoroutine { continuation ->
                    if (!continuation.isActive) return@suspendCancellableCoroutine
                    submitted = true
                    val result = submit(onDiscard, onNotStarted, block)
                    continuation.invokeOnCancellation { result.cancel(false) }
                    result.whenComplete { value, _ ->
                        continuation.resume(value) { _, abandoned, _ ->
                            if (abandoned != null) onDiscard(abandoned)
                        }
                    }
                }
            }
        } finally {
            if (!submitted) onNotStarted()
        }
    }

    /** For synchronous selector adapters; the worker still owns any late result. */
    fun <T : Any> call(
        timeoutMillis: Long,
        onDiscard: (T) -> Unit = {},
        block: () -> T?,
    ): T? {
        require(timeoutMillis > 0)
        val result = submit(onDiscard, {}, block)
        return try {
            result.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            abandon(result, onDiscard)
            null
        } catch (_: InterruptedException) {
            abandon(result, onDiscard)
            Thread.currentThread().interrupt()
            null
        }
    }

    private fun <T : Any> abandon(result: CompletableFuture<T?>, onDiscard: (T) -> Unit) {
        // Completion can win the race immediately after get() times out.
        if (!result.cancel(false)) result.getNow(null)?.let(onDiscard)
    }

    override fun close() {
        executor.shutdownNow()
    }
}
