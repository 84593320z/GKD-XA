package li.gkd.app.domain.rule

/** A snapshot has one owner. Taking it transfers cleanup to the consuming worker. */
internal class OwnedResource<T : Any>(value: T, private val release: (T) -> Unit) : AutoCloseable {
    private var value: T? = value

    @Synchronized
    fun take(): T? = value.also { value = null }

    override fun close() {
        take()?.let(release)
    }
}
