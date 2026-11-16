package li.gkd.app.priv

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import java.io.IOException

internal sealed interface InstallEvent<out T> {
    data object Success : InstallEvent<Nothing>
    data class Failure(val message: String) : InstallEvent<Nothing>
    data class Pending<T>(val action: T) : InstallEvent<T>
}

/** A terminal callback wins once; repeated pending callbacks cannot fill an unbounded queue. */
internal class InstallCompletion<T> {
    private val events = Channel<InstallEvent<T>>(Channel.CONFLATED)
    private var terminal = false

    @Synchronized
    fun accept(event: InstallEvent<T>) {
        if (terminal) return
        if (event !is InstallEvent.Pending) terminal = true
        events.trySend(event)
    }

    @Synchronized
    fun disconnected() {
        if (!terminal) {
            terminal = true
            events.tryReceive() // Do not launch a stale confirmation after its callback server dies.
            events.close(IOException("安装回调服务已断开"))
        }
    }

    suspend fun await(timeoutMillis: Long, onPending: suspend (T) -> Unit): InstallEvent<Nothing> =
        withTimeout(timeoutMillis) {
            while (true) {
                when (val event = events.receive()) {
                    is InstallEvent.Pending -> onPending(event.action)
                    is InstallEvent.Failure -> return@withTimeout event
                    InstallEvent.Success -> return@withTimeout InstallEvent.Success
                }
            }
            @Suppress("UNREACHABLE_CODE") error("Installation result loop exited")
        }

    fun close() { events.cancel() }
}
