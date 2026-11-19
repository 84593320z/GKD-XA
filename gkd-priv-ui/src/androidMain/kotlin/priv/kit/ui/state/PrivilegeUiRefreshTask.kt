package priv.kit.ui.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Concurrent callers share one refresh; cancellation never leaves an orphaned refresh job. */
internal class PrivilegeUiRefreshTask {
    private val lock = Any()
    private var idleCleanup: (() -> Unit)? = null
    private var running: CompletableDeferred<Unit>? = null

    fun releaseWhenIdle(cleanup: () -> Unit) {
        synchronized(lock) {
            if (running == null) cleanup() else idleCleanup = cleanup
        }
    }

    suspend fun run(refresh: suspend () -> Unit) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val (completion, owner) = synchronized(lock) {
                running?.let { it to false } ?: CompletableDeferred<Unit>().also {
                    running = it
                }.let { it to true }
            }
            if (owner) {
                val result = runCatching { refresh() }
                synchronized(lock) {
                    idleCleanup?.let { runCatching(it) }
                    idleCleanup = null
                    if (running === completion) running = null
                }
                result.fold(completion::complete, completion::completeExceptionally)
                result.getOrThrow()
                return
            }
            try {
                completion.await()
                return
            } catch (_: CancellationException) {
                // The original caller may have left the page while this caller still
                // needs the result. A surviving caller takes over the cancelled refresh.
                currentCoroutineContext().ensureActive()
            }
        }
    }
}
