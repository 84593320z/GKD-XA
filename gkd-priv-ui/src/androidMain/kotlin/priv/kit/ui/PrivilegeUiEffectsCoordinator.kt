package priv.kit.ui

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import priv.kit.ui.adb.PrivilegeUiAdbActions
import priv.kit.ui.external.PrivilegeUiExternalStartActions
import priv.kit.ui.runtime.PrivilegeUiInteractiveStartOwner
import priv.kit.ui.runtime.PrivilegeUiRuntimeActions
import priv.kit.ui.runtime.PrivilegeUiStartGate
import priv.kit.ui.runtime.PrivilegeUiStartGateState
import priv.kit.ui.runtime.loadManualShellCommand
import priv.kit.ui.state.PrivilegeUiViewModelStore
import priv.kit.ui.state.isPrivilegeUiWirelessAdbSupported

internal class PrivilegeUiEffectsCoordinator(
    private val store: PrivilegeUiViewModelStore,
    private val interactiveStartOwner: PrivilegeUiInteractiveStartOwner,
    private val runtimeActions: PrivilegeUiRuntimeActions,
    private val adbActions: PrivilegeUiAdbActions,
    private val externalStartActions: PrivilegeUiExternalStartActions,
    private val coroutineScope: CoroutineScope,
) : AutoCloseable {
    private val resumedHosts = mutableSetOf<String>()
    private val pageVisible = MutableStateFlow(false)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private var initialLoadCompleted = false
    private var observerJob: Job? = null
    private var reconciledSilentCompletionSerial = 0L

    val startGateState: StateFlow<PrivilegeUiStartGateState> = PrivilegeUiStartGate.state

    fun initialize() {
        val gateState = startGateState.value
        reconciledSilentCompletionSerial = gateState.silentCompletionSerial
        observerJob = coroutineScope.launch(CoroutineName("priv-ui-effects")) {
            val manualCommandJob = launch(CoroutineName("priv-ui-initial-manual-command")) {
                if (PrivilegeUiStartupMode.MANUAL_SHELL in store.state.value.startupModes) {
                    store.loadManualShellCommand()
                }
            }
            startGateState
                .map { state ->
                    EffectsGate(
                        canInteract = interactiveStartOwner.canInteract(state),
                        silentCompletionSerial = state.silentCompletionSerial,
                    )
                }
                .distinctUntilChanged()
                .collectLatest { gate -> runEffects(gate, manualCommandJob) }
        }
    }

    fun canInteract(
        gateState: PrivilegeUiStartGateState = startGateState.value,
    ): Boolean =
        interactiveStartOwner.canInteract(gateState)

    fun setHostResumed(hostId: String, resumed: Boolean) {
        if (resumed) resumedHosts.add(hostId) else resumedHosts.remove(hostId)
        pageVisible.value = resumedHosts.isNotEmpty()
    }

    fun removeHost(hostId: String) = setHostResumed(hostId, false)

    fun refreshHostResumeState() {
        refreshRequests.trySend(Unit)
    }

    override fun close() {
        refreshRequests.close()
        resumedHosts.clear()
        pageVisible.value = false
        observerJob?.cancel()
        observerJob = null
    }

    private suspend fun runEffects(gate: EffectsGate, manualCommandJob: Job) {
        if (!gate.canInteract) return
        val silentCompletionChanged =
            gate.silentCompletionSerial != reconciledSilentCompletionSerial

        val loadingInitialState = !initialLoadCompleted
        runtimeActions.refreshRuntimeStatus(
            useCurrentState = !silentCompletionChanged,
        )
        if (loadingInitialState) runtimeActions.loadInitialPermissionDetails()
        manualCommandJob.join()
        if (!gate.isCurrent()) return

        reconciledSilentCompletionSerial = gate.silentCompletionSerial
        if (loadingInitialState) {
            initialLoadCompleted = true
        }

        combine(store.state, pageVisible) { state, visible ->
            if (visible) VisiblePage(
                mode = state.selectedStartupMode,
                wirelessDiscoveryAllowed = state.selectedStartupMode == PrivilegeUiStartupMode.ADB &&
                    !state.localNetworkPermissionMissing,
            ) else null
        }.distinctUntilChanged().collectLatest(::runVisiblePageEffects)
    }

    private suspend fun runVisiblePageEffects(page: VisiblePage?): Unit = coroutineScope {
        if (page == null) return@coroutineScope
        val mode = page.mode
        // A new visible page always refreshes. Merge resume/focus signals already queued.
        while (refreshRequests.tryReceive().isSuccess) { /* merged into the initial refresh */ }
        launch(CoroutineName("priv-ui-page-refresh")) {
            refreshVisibleMode(page)
            for (request in refreshRequests) refreshVisibleMode(page)
        }
        when (mode) {
            PrivilegeUiStartupMode.ADB -> launch { pollAdbStatus(page.wirelessDiscoveryAllowed) }
            PrivilegeUiStartupMode.EXTERNAL -> launch {
                externalStartActions.pollExternalStartStatus()
            }
            else -> Unit
        }
        awaitCancellation()
    }

    private suspend fun refreshVisibleMode(page: VisiblePage): Unit = supervisorScope {
        launch { runtimeActions.refreshPermissionRestrictionStatus() }
        when (page.mode) {
            PrivilegeUiStartupMode.ADB -> {
                if (page.wirelessDiscoveryAllowed && isPrivilegeUiWirelessAdbSupported()) launch {
                    adbActions.refreshWirelessAdbStatusNow(markChecking = false)
                }
                if (store.config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED) launch {
                    adbActions.refreshTcpModeEnabledNow(markChecking = false)
                }
            }
            PrivilegeUiStartupMode.EXTERNAL -> launch {
                externalStartActions.refreshExternalStartStatusNow(providerId = null)
            }
            else -> Unit
        }
    }

    private suspend fun pollAdbStatus(wirelessDiscoveryAllowed: Boolean): Unit = coroutineScope {
        if (wirelessDiscoveryAllowed && isPrivilegeUiWirelessAdbSupported()) launch {
            adbActions.pollWirelessAdbStatus()
        }
        if (store.config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED) launch {
            adbActions.pollTcpModeStatus()
        }
        awaitCancellation()
    }

    private fun EffectsGate.isCurrent(): Boolean {
        val current = startGateState.value
        return canInteract &&
            current.silentCompletionSerial == silentCompletionSerial &&
            interactiveStartOwner.canInteract(current)
    }

    private data class VisiblePage(
        val mode: PrivilegeUiStartupMode,
        val wirelessDiscoveryAllowed: Boolean,
    )

    private data class EffectsGate(
        val canInteract: Boolean,
        val silentCompletionSerial: Long,
    )
}
