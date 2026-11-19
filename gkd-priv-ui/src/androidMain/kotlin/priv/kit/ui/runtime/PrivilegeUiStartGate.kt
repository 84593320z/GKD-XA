package priv.kit.ui.runtime

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class PrivilegeUiStartGateOwner {
    SILENT,
    INTERACTIVE,
}

internal data class PrivilegeUiStartGateState(
    val owner: PrivilegeUiStartGateOwner? = null,
    val interactiveLeaseCount: Int = 0,
    val silentCompletionSerial: Long = 0L,
)

internal object PrivilegeUiStartGate {
    private val lock = Any()
    private val mutableState = MutableStateFlow(PrivilegeUiStartGateState())
    private var interactiveOwnerToken: Any? = null
    val state: StateFlow<PrivilegeUiStartGateState> = mutableState.asStateFlow()

    val isSilentStartInProgress: Boolean
        get() = state.value.owner == PrivilegeUiStartGateOwner.SILENT

    fun tryAcquireSilent(): AutoCloseable? =
        tryAcquireSilentOwner()

    fun newInteractivePermitAcquirer(): () -> AutoCloseable? {
        val owner = newInteractiveOwner()
        return owner::tryAcquire
    }

    fun newInteractiveOwner(): PrivilegeUiInteractiveStartOwner =
        PrivilegeUiInteractiveStartOwner(Any())

    internal fun tryAcquireInteractiveOwner(ownerToken: Any): AutoCloseable? {
        if (!acquireInteractive(ownerToken)) return null
        val released = AtomicBoolean(false)
        return AutoCloseable {
            check(released.compareAndSet(false, true)) {
                "Privilege UI interactive start permit is already closed"
            }
            releaseInteractive(ownerToken)
        }
    }

    internal fun canInteract(
        ownerToken: Any,
        observedState: PrivilegeUiStartGateState,
    ): Boolean =
        synchronized(lock) {
            if (mutableState.value != observedState) return@synchronized false
            when (observedState.owner) {
                null -> true
                PrivilegeUiStartGateOwner.SILENT -> false
                PrivilegeUiStartGateOwner.INTERACTIVE -> interactiveOwnerToken === ownerToken
            }
        }

    private fun tryAcquireSilentOwner(): AutoCloseable? {
        val acquiredState = acquireSilent() ?: return null
        val released = AtomicBoolean(false)
        return AutoCloseable {
            check(released.compareAndSet(false, true)) {
                "Privilege UI silent start permit is already closed"
            }
            releaseSilent(acquiredState)
        }
    }

    private fun acquireSilent(): PrivilegeUiStartGateState? =
        synchronized(lock) {
            val current = mutableState.value
            if (current.owner != null) return null
            val acquired = current.copy(owner = PrivilegeUiStartGateOwner.SILENT)
            mutableState.value = acquired
            acquired
        }

    private fun acquireInteractive(ownerToken: Any): Boolean =
        synchronized(lock) {
            val current = mutableState.value
            if (current.owner == PrivilegeUiStartGateOwner.SILENT) return false
            if (
                current.owner == PrivilegeUiStartGateOwner.INTERACTIVE &&
                interactiveOwnerToken !== ownerToken
            ) {
                return false
            }
            if (current.owner == null) interactiveOwnerToken = ownerToken
            val acquired = current.copy(
                owner = PrivilegeUiStartGateOwner.INTERACTIVE,
                interactiveLeaseCount = current.interactiveLeaseCount + 1,
            )
            mutableState.value = acquired
            true
        }

    private fun releaseSilent(acquiredState: PrivilegeUiStartGateState) {
        synchronized(lock) {
            check(mutableState.value == acquiredState && interactiveOwnerToken == null) {
                "Privilege UI start permit no longer owns the gate"
            }
            mutableState.value = acquiredState.copy(
                owner = null,
                silentCompletionSerial = acquiredState.silentCompletionSerial + 1L,
            )
        }
    }

    private fun releaseInteractive(ownerToken: Any) {
        synchronized(lock) {
            val current = mutableState.value
            check(
                current.owner == PrivilegeUiStartGateOwner.INTERACTIVE &&
                    current.interactiveLeaseCount > 0 &&
                    interactiveOwnerToken === ownerToken
            ) {
                "Privilege UI interactive permit no longer owns the gate"
            }
            val remaining = current.interactiveLeaseCount - 1
            if (remaining == 0) interactiveOwnerToken = null
            mutableState.value = current.copy(
                owner = if (remaining == 0) null else PrivilegeUiStartGateOwner.INTERACTIVE,
                interactiveLeaseCount = remaining,
            )
        }
    }
}

internal class PrivilegeUiInteractiveStartOwner internal constructor(
    private val ownerToken: Any,
) {
    fun tryAcquire(): AutoCloseable? =
        PrivilegeUiStartGate.tryAcquireInteractiveOwner(ownerToken)

    fun canInteract(observedState: PrivilegeUiStartGateState): Boolean =
        PrivilegeUiStartGate.canInteract(ownerToken, observedState)
}
