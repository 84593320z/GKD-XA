package li.gkd.app.domain.rule

/** Owns queued entries; every discarded entry is released exactly once. */
internal class OwnedEventQueue<T : Any>(
    private val capacity: Int,
    private val sameAs: (T, T) -> Boolean,
    private val release: (T) -> Unit,
) : AutoCloseable {
    init { require(capacity > 0) }
    private val queue = ArrayDeque<T>()
    private var closed = false

    @Synchronized
    fun add(value: T) {
        if (closed) {
            release(value)
            return
        }
        if (queue.size == capacity) release(queue.removeFirst())
        queue.addLast(value)
    }

    @Synchronized
    fun takePrefix(): List<T> {
        val head = queue.firstOrNull() ?: return emptyList()
        val batch = mutableListOf<T>()
        while (queue.firstOrNull()?.let { sameAs(head, it) } == true) {
            batch.add(queue.removeFirst())
        }
        return batch
    }

    @Synchronized
    fun drain(): List<T> = queue.toList().also { queue.clear() }

    @Synchronized
    override fun close() {
        closed = true
        while (queue.isNotEmpty()) release(queue.removeFirst())
    }
}
