package li.gkd.app.service

import java.util.IdentityHashMap

/** One effective service at a time. A lease also distinguishes A -> B -> A. */
internal class AutomatorServiceHandoff<S : Any> {
    class Lease<S : Any>(val service: S, internal var predecessor: S?)

    private val retiring = IdentityHashMap<S, Unit>()

    @Volatile
    var current: Lease<S>? = null
        private set

    // Validation must be nonblocking. Do the actual connection/probe before this point.
    @Synchronized
    fun accept(candidate: S, other: S?, canPublish: () -> Boolean): Lease<S>? {
        if (current?.service === candidate || retiring.containsKey(candidate) || !canPublish()) return null
        return Lease(candidate, other).also { current = it }
    }

    @Synchronized
    fun disconnect(service: S, canRestore: (S) -> Boolean): Lease<S>? {
        retiring.remove(service)
        val lease = current ?: return null
        if (lease.service !== service) return null
        val restored = lease.predecessor?.takeIf(canRestore)?.let { Lease(it, null) }
        current = restored
        return restored
    }

    /** Reserve only the captured instance, so it cannot be re-promoted before shutdown. */
    @Synchronized
    fun retire(lease: Lease<S>, liveOther: () -> S?, canRetire: () -> Boolean): S? {
        val previous = lease.predecessor ?: return null
        if (current !== lease || liveOther() !== previous || !canRetire()) return null
        retiring[previous] = Unit
        lease.predecessor = null
        return previous
    }
}

/** The user can withdraw a startup before Android delivers onServiceConnected. */
internal class AccessibilityStartIntent<S : Any> {
    class Request(val enabled: Boolean)

    @Volatile
    private var desired = Request(false)
    private var explicitUserIntent = false
    val requestedEnabled: Boolean get() = desired.enabled
    private var pending: Request? = null
    private var observedSystemEnabled: Boolean? = null
    private var createdRequest: Request? = null
    var created: S? = null
        private set

    // External callers serialize intent changes with their instance publication.
    fun toggle(): Request {
        val enabled = !(desired.enabled && (created != null || pending === desired))
        explicitUserIntent = true
        return Request(enabled).also {
            desired = it
            pending = it.takeIf { enabled }
        }
    }

    fun beginStart(allowAfterUserStop: Boolean = false): Request? {
        if (!desired.enabled && (!explicitUserIntent || allowAfterUserStop)) desired = Request(true)
        return desired.takeIf { it.enabled }?.also { pending = it }
    }

    fun permits(request: Request): Boolean = request.enabled && desired === request

    fun isCurrent(request: Request): Boolean = desired === request

    /** App-owned writes update the baseline without changing the user's startup intent. */
    fun recordSettingWrite(enabled: Boolean) {
        observedSystemEnabled = enabled
    }

    /** disableSelf() does not require WRITE_SECURE_SETTINGS; verify the resulting setting. */
    fun observeSystemDisable(enabled: Boolean?) {
        if (enabled == false) recordSettingWrite(false)
    }

    /** A real Secure Settings transition is new system intent, not an old bind retry. */
    fun observeSystemSetting(enabled: Boolean): Request? {
        val previous = observedSystemEnabled
        observedSystemEnabled = enabled
        if (previous == enabled || !enabled || desired.enabled) return null
        if (previous == false || !explicitUserIntent) {
            return Request(true).also {
                desired = it
                pending = it
                explicitUserIntent = false
            }
        }
        return null
    }

    fun allowRollback(request: Request?, stopping: Boolean, tempShutdown: Boolean): Boolean =
        if (stopping) tempShutdown else request?.let(::permits) == true

    fun finish(request: Request) {
        if (pending === request) pending = null
    }

    fun onCreated(service: S, systemEnabled: Boolean? = null): Request? {
        val requestBeforeObservation = desired.takeIf { it.enabled }
        val freshSystemEnable = systemEnabled == true && observedSystemEnabled == false
        val existing = created
        if (existing != null && existing !== service) {
            // Never let an old or duplicate bind replace a still-created service instance.
            if (createdRequest != null && !freshSystemEnable) return null
            if (requestBeforeObservation == null && !freshSystemEnable) return null
        }
        systemEnabled?.let(::observeSystemSetting)
        val request = desired.takeIf { it.enabled }
        created = service
        createdRequest = request
        if (request == null && !explicitUserIntent && systemEnabled == null) {
            desired = Request(true)
            createdRequest = desired
            return desired
        }
        return request
    }

    fun canConnect(service: S, request: Request?): Boolean =
        created === service && request != null && permits(request)

    fun onDestroyed(service: S): Boolean {
        if (created !== service) return false
        created = null
        createdRequest = null
        return true
    }
}

/** Re-read the whole setting for every write, but only change this component's entries. */
internal class AccessibilityServicesSetting(
    private val read: () -> String?,
    private val write: (String) -> Boolean,
    private val isOurs: (String) -> Boolean,
    private val component: () -> String,
    private val onUpdate: (Boolean) -> Unit = {},
) {
    fun containsOurs(): Boolean = entries().any(isOurs)

    fun setEnabled(enabled: Boolean): Boolean {
        val current = entries()
        if (enabled && current.any(isOurs)) {
            onUpdate(true)
            return true
        }
        val updated = if (enabled) current + component() else current.filterNot(isOurs)
        val success = updated == current || write(updated.joinToString(":"))
        if (success) onUpdate(enabled)
        return success
    }

    private fun entries(): List<String> = read()?.takeIf { it.isNotEmpty() }?.split(':').orEmpty()
}

/** Nothing read before the restart wait may be reused to write the re-enable list. */
internal suspend fun AccessibilityServicesSetting.restart(
    shouldStart: () -> Boolean,
    update: (Boolean) -> Boolean = ::setEnabled,
    waitForUnbind: suspend () -> Unit,
): Boolean {
    if (!shouldStart()) return false
    if (containsOurs()) {
        if (!update(false)) return false
        waitForUnbind()
    }
    return shouldStart() && update(true)
}
