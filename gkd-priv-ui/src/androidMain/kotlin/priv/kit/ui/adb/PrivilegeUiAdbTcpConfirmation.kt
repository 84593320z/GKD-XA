package priv.kit.ui.adb

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PrivilegeUiStaticTcpConfirmationController {
    private val lock = Any()
    private val pendingActionState = MutableStateFlow<PrivilegeUiStaticTcpSwitchAction?>(null)
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    val pendingAction: StateFlow<PrivilegeUiStaticTcpSwitchAction?> =
        pendingActionState.asStateFlow()

    suspend fun awaitConfirmation(action: PrivilegeUiStaticTcpSwitchAction): Boolean {
        var replacedRequest: CompletableDeferred<Boolean>? = null
        val request = synchronized(lock) {
            if (pendingRequest != null) {
                if (
                    pendingActionState.value == PrivilegeUiStaticTcpSwitchAction.START_SERVICE ||
                    action != PrivilegeUiStaticTcpSwitchAction.START_SERVICE
                ) {
                    return false
                }
                replacedRequest = pendingRequest
            }
            CompletableDeferred<Boolean>().also {
                pendingRequest = it
                pendingActionState.value = action
            }
        }
        replacedRequest?.complete(false)
        return try {
            request.await()
        } finally {
            synchronized(lock) {
                if (pendingRequest === request) {
                    pendingRequest = null
                    pendingActionState.value = null
                }
            }
        }
    }

    fun confirm() {
        resolve(confirmed = true)
    }

    fun cancel() {
        resolve(confirmed = false)
    }

    private fun resolve(confirmed: Boolean) {
        val request = synchronized(lock) {
            pendingRequest?.also {
                pendingRequest = null
                pendingActionState.value = null
            }
        } ?: return
        request.complete(confirmed)
    }
}
