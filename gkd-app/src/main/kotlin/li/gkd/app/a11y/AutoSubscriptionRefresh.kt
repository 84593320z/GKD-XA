package li.gkd.app.a11y

import kotlinx.coroutines.Job
import li.gkd.app.data.subscription.SubscriptionResult

/** Owns the launcher-event admission and the whole asynchronous refresh lifetime. */
internal class AutoSubscriptionRefresh(
    private val interval: () -> Long,
    private val minimumInterval: Long,
    private val now: () -> Long,
    private val launch: (suspend () -> Unit) -> Job,
    private val refresh: suspend () -> SubscriptionResult,
) {
    private val lock = Any()
    private var lastRefreshTime: Long? = null
    private var pending: Any? = null

    fun onEvent(packageName: CharSequence?, launcherPackage: String) {
        if (packageName != launcherPackage) return
        val requestedInterval = interval()
        if (requestedInterval <= 0) return
        val startedAt = now()
        val attempt = synchronized(lock) {
            if (pending != null) return
            val previous = lastRefreshTime
            if (previous != null && startedAt - previous <= requestedInterval.coerceAtLeast(minimumInterval)) return
            Any().also { pending = it }
        }
        try {
            launch {
                try {
                    if (refresh() !is SubscriptionResult.Busy) {
                        synchronized(lock) { lastRefreshTime = startedAt }
                    }
                } finally {
                    finish(attempt)
                }
            }.invokeOnCompletion { finish(attempt) } // Also releases a job cancelled before it starts.
        } catch (e: Throwable) {
            finish(attempt)
            throw e
        }
    }

    private fun finish(attempt: Any) = synchronized(lock) {
        if (pending === attempt) pending = null
    }
}
