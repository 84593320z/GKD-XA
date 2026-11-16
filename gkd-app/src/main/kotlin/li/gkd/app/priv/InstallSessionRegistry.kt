package li.gkd.app.priv

/** A terminal outcome outlives the coroutine waiting for the installation callback. */
internal sealed interface InstallSessionTerminal {
    data object Success : InstallSessionTerminal
    data class Failure(val message: String) : InstallSessionTerminal
}

/** The handle is an identity token as well as a platform id, which Android may reuse. */
internal class InstallSessionHandle internal constructor(
    val sessionId: Int,
    val apkPath: String,
    private val registry: InstallSessionRegistry,
) {
    @Volatile
    var terminal: InstallSessionTerminal? = null
        internal set

    fun settle(outcome: InstallSessionTerminal): Boolean = registry.settle(this, outcome)

    /** Only a confirmed pre-commit abandon may forget an unresolved session. */
    fun forget(): Boolean = registry.forget(this)
}

/**
 * Process-owned pending state, independent of an awaiter or ViewModel. Success deliberately keeps
 * the pending/restart guard until this process is replaced. Unknown results do not remove it.
 * All changes to the restart guard run under the same lock, so an old failure cannot release a
 * newer session's suppression even if delivery races registration.
 */
internal class InstallSessionRegistry(
    private val onRestartSuppressionChanged: (Boolean) -> Unit = {},
) {
    private val lock = Any()
    private val sessions = HashMap<Int, InstallSessionHandle>()

    val hasPendingInstallations: Boolean
        get() = synchronized(lock) { sessions.isNotEmpty() }

    fun isPending(handle: InstallSessionHandle): Boolean = synchronized(lock) {
        sessions[handle.sessionId] === handle
    }

    fun register(sessionId: Int, apkPath: String): InstallSessionHandle = synchronized(lock) {
        val handle = InstallSessionHandle(sessionId, apkPath, this)
        val wasEmpty = sessions.isEmpty()
        sessions[sessionId] = handle
        if (wasEmpty) onRestartSuppressionChanged(true)
        handle
    }

    internal fun settle(handle: InstallSessionHandle, outcome: InstallSessionTerminal): Boolean =
        synchronized(lock) {
            if (sessions[handle.sessionId] !== handle || handle.terminal != null) return@synchronized false
            handle.terminal = outcome
            if (outcome is InstallSessionTerminal.Failure) {
                sessions.remove(handle.sessionId)
                if (sessions.isEmpty()) onRestartSuppressionChanged(false)
            }
            true
        }

    internal fun forget(handle: InstallSessionHandle): Boolean = synchronized(lock) {
        if (sessions[handle.sessionId] !== handle || handle.terminal != null) return@synchronized false
        sessions.remove(handle.sessionId)
        if (sessions.isEmpty()) onRestartSuppressionChanged(false)
        true
    }
}

/** The actual listener bridge: settle process state before waking the disposable local waiter. */
internal class InstallSessionCallback<T>(
    private val session: InstallSessionHandle,
    private val completion: InstallCompletion<T>,
) {
    fun accept(event: InstallEvent<T>) {
        val outcome = when (event) {
            InstallEvent.Success -> InstallSessionTerminal.Success
            is InstallEvent.Failure -> InstallSessionTerminal.Failure(event.message)
            is InstallEvent.Pending -> null
        }
        if (outcome == null) {
            if (session.terminal == null) completion.accept(event)
        } else if (session.settle(outcome)) {
            completion.accept(event)
        }
    }
}
