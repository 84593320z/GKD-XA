package li.gkd.app.domain.rule

/** Reservation and pending requests share one lock, including the exit window. */
internal class QueryWakeup<T : Any>(private val merge: (T, T) -> T) {
    private var running = false
    private var pending: T? = null

    @Synchronized
    fun request(value: T): Boolean {
        pending = pending?.let { merge(it, value) } ?: value
        if (running) return false
        running = true
        return true
    }

    @Synchronized
    fun takeOrFinish(): T? {
        val next = pending
        pending = null
        if (next == null) running = false
        return next
    }

    @Synchronized
    fun cancel() {
        running = false
        pending = null
    }
}
