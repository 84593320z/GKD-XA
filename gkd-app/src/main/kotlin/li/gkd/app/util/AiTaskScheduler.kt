package li.gkd.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Admission, pending accounting and both generation modes share one serialization point. */
internal class AiTaskScheduler<T>(
    scope: CoroutineScope,
    private val capacity: Int = 8,
    private val onFailure: (Exception) -> Unit = {},
    private val process: suspend (T) -> Unit,
) {
    private class Entry<T>(val task: T) {
        val completed = CompletableDeferred<Unit>()
        var cancelled = false
        var job: Job? = null
    }

    private val lock = Any()
    private val queue = Channel<Entry<T>>(capacity, onUndeliveredElement = { it.completed.cancel() })
    private val pending = MutableStateFlow(0)
    private val running = MutableStateFlow(false)
    val pendingCount: StateFlow<Int> = pending
    val isGenerating: StateFlow<Boolean> = running

    init {
        require(capacity > 0)
        scope.launch {
            try {
                for (entry in queue) {
                    synchronized(lock) { running.value = true }
                    try {
                        coroutineScope {
                            synchronized(entry) {
                                if (!entry.cancelled) entry.job = launch {
                                    try {
                                        process(entry.task)
                                    } catch (e: CancellationException) {
                                        currentCoroutineContext().ensureActive()
                                        onFailure(e)
                                    } catch (e: Exception) {
                                        onFailure(e)
                                    }
                                }
                            }
                        }
                    } finally {
                        synchronized(lock) {
                            pending.value--
                            running.value = false
                        }
                        entry.completed.complete(Unit)
                    }
                }
            } finally {
                synchronized(lock) {
                    queue.cancel()
                    pending.value = 0
                    running.value = false
                }
            }
        }
    }

    /** Non-suspending admission cannot leave a phantom count when its caller is cancelled. */
    fun enqueue(task: T): Boolean = admit(task) != null

    /** Keep the caller's resources (notably hidden overlays) until this task has stopped. */
    suspend fun enqueueAndAwait(task: T, onAccepted: () -> Unit = {}): Boolean {
        currentCoroutineContext().ensureActive()
        val entry = admit(task) ?: return false
        try {
            onAccepted()
            entry.completed.await()
            return true
        } finally {
            val job = synchronized(entry) {
                entry.cancelled = true
                entry.job?.also { it.cancel() }
            }
            // A cancelled caller must not restore overlays while a task still owns a live action.
            withContext(NonCancellable) { job?.join() }
        }
    }

    private fun admit(task: T): Entry<T>? = synchronized(lock) {
        if (pending.value >= capacity) return@synchronized null
        val entry = Entry(task)
        pending.value++
        if (queue.trySend(entry).isSuccess) entry else {
            pending.value--
            null
        }
    }
}
