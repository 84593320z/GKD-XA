package priv.kit.ui.adb

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.selects.select
import priv.kit.ui.state.PrivilegeUiViewModelStore

/** Only use for discovery/checks, never for a command that starts or changes a service. */
internal suspend fun <T> PrivilegeUiViewModelStore.retryOnLocalNetworkPermissionGrant(
    action: suspend () -> T,
): T = retryOnLocalNetworkPermissionGrant(state.map { it.localNetworkPermissionMissing }, action)

internal suspend fun <T> retryOnLocalNetworkPermissionGrant(
    permissionMissing: Flow<Boolean>,
    action: suspend () -> T,
): T = coroutineScope {
    while (true) {
        val granted = async(start = CoroutineStart.UNDISPATCHED) {
            permissionMissing.distinctUntilChanged().dropWhile { !it }.first { !it }
        }
        val attempt = async { runCatching { action() } }
        try {
            val completed = select {
                attempt.onAwait { true }
                granted.onAwait { false }
            }
            if (completed) return@coroutineScope attempt.await().getOrThrow()
            // Join ensures sockets/listeners and old callbacks are gone before retrying.
            attempt.cancelAndJoin()
        } finally {
            granted.cancel()
            attempt.cancel()
        }
    }
    @Suppress("UNREACHABLE_CODE")
    error("Unreachable")
}
