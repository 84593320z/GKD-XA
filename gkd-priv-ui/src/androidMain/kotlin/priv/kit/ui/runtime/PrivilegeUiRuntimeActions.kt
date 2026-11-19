package priv.kit.ui.runtime

import priv.kit.ui.*
import priv.kit.ui.state.*

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import priv.kit.core.Privilege
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.binder.PrivilegeBinderCall
import priv.kit.core.internal.runtime.PrivilegeRuntimeConnectionEvent
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator

internal class PrivilegeUiRuntimeActions(
    private val store: PrivilegeUiViewModelStore,
    private val coroutineScope: CoroutineScope,
    private val acquireStartPermit: () -> AutoCloseable?,
    private val systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator =
        PrivilegeUiSystemPromptCoordinator(),
    private val shutdownServer: () -> Unit = { Privilege.shutdownServer() },
    private val isPermissionRestricted: () -> Boolean =
        Privilege::isPermissionRestricted,
    private val getDeniedServerPermissions: () -> List<String> = Privilege::getDeniedServerPermissions,
    private val operationDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var nextStopOperationId = 0L
    private var activeStopOperationId: Long? = null
    private val permissionRestrictionRefreshGeneration = AtomicLong(0L)
    private var scheduledPermissionRefresh: Pair<Long, Job>? = null
    private var runtimeWatcherJob: Job? = null
    private val runtimeStartCoordinator = PrivilegeUiRuntimeStartCoordinator(
        store = store,
        coroutineScope = coroutineScope,
        isClosed = closed::get,
        publishConnectedServer = ::publishConnectedServerLocked,
        acquireStartPermit = acquireStartPermit,
    )

    val isClosed: Boolean
        get() = closed.get()

    fun startRoot(replaceConnectedServer: Boolean = false): Boolean =
        runServerStart(rootStartAttempt(), replaceConnectedServer)

    fun rootStartAttempt(): PrivilegeUiRuntimeStartAttempt.Connect =
        PrivilegeUiRuntimeStartAttempt.Connect(
            progressText = store.resourceText(R.string.priv_ui_starting_root),
            startupSource = store.text(R.string.priv_ui_auth_method_root),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ROOT,
        ) {
            systemPromptCoordinator.withPrompt(privilegeUiRootAuthorizationPrompt()) {
                PrivilegeRuntimeStartCoordinator.startRoot(
                    launch = requireRuntimeClientLaunch(),
                    timeoutMillis = store.config.startTimeoutMillis,
                    startupLogListener = startupLogListener,
                )
            }
        }

    fun stopServer(beforeShutdown: () -> Unit) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        val operationPermit = acquireStartPermit() ?: return
        var operationId = 0L
        var connectionSerial = 0L
        val accepted = synchronized(store) {
            val current = store.state.value
            if (
                closed.get() ||
                (activeStopOperationId != null) ||
                current.busy ||
                current.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE ||
                current.runtimeStatus != PrivilegeUiRuntimeStatus.CONNECTED
            ) {
                false
            } else {
                operationId = ++nextStopOperationId
                connectionSerial = current.connectionSerial
                activeStopOperationId = operationId
                store.serverShutdownRequestedByOwner = true
                store.updateState { it.copy(busy = true) }
                true
            }
        }
        if (!accepted) {
            operationPermit.close()
            return
        }
        runCatching(beforeShutdown)
        val message = store.text(R.string.priv_ui_stopping_service)
        store.appendStartupLog(message)
        try {
            val job = coroutineScope.launch(operationDispatcher + CoroutineName("priv-ui-stop-server")) {
                try {
                    runInterruptible { shutdownServer() }
                    synchronized(store) {
                        if (!ownsStopOperationLocked(operationId, connectionSerial)) return@synchronized
                        store.updateState {
                            it.toDisconnectedRuntimeIdle()
                        }
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (throwable: Throwable) {
                    synchronized(store) {
                        if (!ownsStopOperationLocked(operationId, connectionSerial)) return@synchronized
                        store.updateState {
                            it.copy(
                                busy = false,
                                runtimeStartSource = null,
                                runtimeStartProviderId = null,
                                runtimeProgressText = null,
                            )
                        }
                        store.showFailure(PrivilegeUiFailureKind.STOP_SERVICE_FAILED)
                        store.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
                    }
                }
            }
            job.invokeOnCompletion {
                synchronized(store) {
                    if (activeStopOperationId == operationId) activeStopOperationId = null
                    store.serverShutdownRequestedByOwner = (activeStopOperationId != null)
                }
                operationPermit.close()
            }
        } catch (throwable: Throwable) {
            synchronized(store) {
                if (activeStopOperationId == operationId) activeStopOperationId = null
                store.serverShutdownRequestedByOwner = (activeStopOperationId != null)
                store.updateState { it.copy(busy = false) }
            }
            operationPermit.close()
            throw throwable
        }
    }

    private fun ownsStopOperationLocked(operationId: Long, connectionSerial: Long): Boolean =
        !closed.get() &&
            operationId == activeStopOperationId &&
            store.state.value.connectionSerial == connectionSerial

    fun stopCurrentStart() {
        runtimeStartCoordinator.stopCurrentStart()
    }

    // Resolve the first frame synchronously; permission details are loaded by the effects later.
    fun initializeRuntimeState(serverInfo: PrivilegeServerInfo? = Privilege.serverState.value) {
        if (serverInfo == null) return
        val restrictionStatus = runCatching {
            if (isPermissionRestricted()) PrivilegeUiPermissionRestrictionStatus.RESTRICTED
            else PrivilegeUiPermissionRestrictionStatus.NOT_RESTRICTED
        }.getOrDefault(PrivilegeUiPermissionRestrictionStatus.UNKNOWN)
        store.updateState {
            it.toConnectedRuntimeIdle(serverInfo, it.connectionSerial + 1L)
                .copy(permissionRestrictionStatus = restrictionStatus)
        }
    }

    suspend fun refreshRuntimeStatus(useCurrentState: Boolean) {
        if (closed.get()) return
        if (useCurrentState) {
            Privilege.serverState.value?.let { serverInfo ->
                runtimeStartCoordinator.handleRefreshedServerConnected(
                    serverInfo = serverInfo,
                    deduplicatePassiveConnection = true,
                )
                return
            }
        }
        val observedConnectionSerial = store.state.value.connectionSerial
        val serverInfo = try {
            PrivilegeBinderCall.orElse(fallback = { null }) {
                withContext(operationDispatcher) {
                    if (Privilege.pingServer()) Privilege.getServerInfo() else null
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            store.appendStartupLog(exception.toPrivilegeUiDiagnosticString())
            return
        }
        if (closed.get()) return
        if (serverInfo == null) {
            updateDisconnectedIfIdle(observedConnectionSerial)
        } else {
            runtimeStartCoordinator.handleRefreshedServerConnected(
                serverInfo = serverInfo,
                deduplicatePassiveConnection = true,
            )
        }
    }

    fun refreshPermissionRestrictionStatus() {
        val current = store.state.value
        if (!current.canRefreshPermissionRestrictionStatus()) return
        schedulePermissionRestrictionRefresh(current.connectionSerial)
    }

    suspend fun loadInitialPermissionDetails() {
        val job = synchronized(store) {
            val current = store.state.value
            if (!current.canRefreshPermissionRestrictionStatus() || closed.get()) return
            scheduledPermissionRefresh?.takeIf { it.first == current.connectionSerial }?.second
                ?: schedulePermissionRestrictionRefresh(
                    current.connectionSerial,
                    current.permissionRestrictionStatus.takeUnless {
                        it == PrivilegeUiPermissionRestrictionStatus.UNKNOWN
                    },
                )
        }
        job.join()
    }

    fun installRuntimeWatchers() {
        runtimeWatcherJob?.cancel()
        runtimeWatcherJob = coroutineScope.launch(CoroutineName("priv-ui-runtime-watcher")) {
            launch {
                PrivilegeRuntimeStartCoordinator.serverConnectionEvents.collect(
                    ::handleServerConnected,
                )
            }
            Privilege.serverState.drop(1).collect { serverInfo ->
                if (serverInfo == null) handleServerDisconnected()
            }
        }
    }

    fun runServerStart(
        attempt: PrivilegeUiRuntimeStartAttempt.Connect,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runtimeStartCoordinator.runServerStart(attempt, replaceConnectedServer)

    fun runServerStartRequest(
        attempt: PrivilegeUiRuntimeStartAttempt.Request,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runtimeStartCoordinator.runServerStartRequest(attempt, replaceConnectedServer)

    fun runServerStartWorkflow(
        attempt: PrivilegeUiRuntimeStartAttempt.Workflow,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runtimeStartCoordinator.runServerStartWorkflow(attempt, replaceConnectedServer)

    fun runServerStartFallback(attempts: List<PrivilegeUiRuntimeStartAttempt>): Boolean =
        runtimeStartCoordinator.runServerStartFallback(attempts)

    /** Entry point for the runtime handshake bridge once it can report an exact origin. */
    fun handleServerConnected(
        event: PrivilegeRuntimeConnectionEvent,
    ) {
        runtimeStartCoordinator.handleServerConnected(
            event = event,
            deduplicatePassiveConnection = true,
        )
    }

    fun <T> runBusy(
        message: String,
        failureKind: PrivilegeUiFailureKind,
        action: suspend () -> T,
        onFailure: ((Throwable) -> Unit)?,
        onSuccess: (T) -> String,
    ) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        val operationPermit = acquireStartPermit() ?: return
        val accepted = synchronized(store) {
            val current = store.state.value
            if (
                closed.get() ||
                current.busy ||
                current.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE
            ) {
                false
            } else {
                store.updateState { it.copy(busy = true) }
                true
            }
        }
        if (!accepted) {
            operationPermit.close()
            return
        }
        store.appendStartupLog(message)
        try {
            val job = coroutineScope.launch(operationDispatcher + CoroutineName("priv-ui-runtime-busy")) {
                try {
                    val result = action()
                    if (!closed.get()) {
                        val resultMessage = onSuccess(result)
                        store.updateState { it.copy(busy = false) }
                        store.appendStartupLog(resultMessage)
                    }
                } catch (_: CancellationException) {
                    return@launch
                } catch (throwable: Throwable) {
                    if (!closed.get()) {
                        onFailure?.invoke(throwable)
                        store.updateState { it.copy(busy = false) }
                        store.showFailure(failureKind)
                        store.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
                    }
                }
            }
            job.invokeOnCompletion { operationPermit.close() }
        } catch (throwable: Throwable) {
            store.updateState { it.copy(busy = false) }
            operationPermit.close()
            throw throwable
        }
    }

    override fun close() {
        if (!synchronized(store) { closed.compareAndSet(false, true) }) return
        permissionRestrictionRefreshGeneration.incrementAndGet()
        runtimeStartCoordinator.close()
        runtimeWatcherJob?.cancel()
        runtimeWatcherJob = null
    }

    private fun handleServerDisconnected() {
        if (closed.get()) return
        runtimeStartCoordinator.handleServerDisconnected()
        val message = if (store.serverShutdownRequestedByOwner) {
            store.text(R.string.priv_ui_service_stopped)
        } else {
            store.text(R.string.priv_ui_binder_died)
        }
        synchronized(store) {
            store.updateStateAndAppendStartupLog(message) {
                if (it.runtimeStartPhase == PrivilegeUiRuntimeStartPhase.IDLE) {
                    it.toDisconnectedRuntimeIdle()
                } else {
                    it.copy(
                        runtimeStatus = PrivilegeUiRuntimeStatus.STARTING,
                        serverInfo = null,
                        deniedServerPermissions = emptyList(),
                        permissionRestrictionStatus =
                            PrivilegeUiPermissionRestrictionStatus.UNKNOWN,
                    )
                }
            }
        }
    }

    private fun updateDisconnectedIfIdle(expectedConnectionSerial: Long) {
        if (closed.get()) return
        store.updateState {
            if (
                it.connectionSerial != expectedConnectionSerial ||
                it.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE
            ) {
                it
            } else {
                it.toDisconnectedRuntimeIdle()
            }
        }
    }

    private fun publishConnectedServerLocked(serverInfo: PrivilegeServerInfo) {
        val shouldAppendLog = store.state.value.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE ||
            store.state.value.runtimeStatus == PrivilegeUiRuntimeStatus.STARTING
        val connectionSerial = store.state.value.connectionSerial + 1L
        val connectedMessage = store.text(R.string.priv_ui_connected).takeIf { shouldAppendLog }
        store.updateStateAndAppendStartupLog(connectedMessage) {
            it.toConnectedRuntimeIdle(
                serverInfo = serverInfo,
                connectionSerial = connectionSerial,
            )
        }
        schedulePermissionRestrictionRefresh(connectionSerial)
    }

    private fun schedulePermissionRestrictionRefresh(
        expectedConnectionSerial: Long,
        initialRestrictionStatus: PrivilegeUiPermissionRestrictionStatus? = null,
    ): Job = synchronized(store) {
        val generation = permissionRestrictionRefreshGeneration.incrementAndGet()
        val job = coroutineScope.launch(
            operationDispatcher + CoroutineName("priv-ui-refresh-permission-restriction"),
            start = CoroutineStart.LAZY,
        ) {
            refreshPermissionRestrictionStatus(expectedConnectionSerial, generation, initialRestrictionStatus)
        }
        scheduledPermissionRefresh = expectedConnectionSerial to job
        job.start()
        job
    }

    private suspend fun refreshPermissionRestrictionStatus(
        expectedConnectionSerial: Long,
        generation: Long,
        initialRestrictionStatus: PrivilegeUiPermissionRestrictionStatus? = null,
    ) {
        val restrictionStatus = initialRestrictionStatus ?: withContext(operationDispatcher) {
            runCatching {
                if (isPermissionRestricted()) {
                    PrivilegeUiPermissionRestrictionStatus.RESTRICTED
                } else {
                    PrivilegeUiPermissionRestrictionStatus.NOT_RESTRICTED
                }
            }.getOrNull()
        } ?: return
        fun isCurrent(current: PrivilegeUiState): Boolean =
            !(
                closed.get() ||
                permissionRestrictionRefreshGeneration.get() != generation ||
                current.connectionSerial != expectedConnectionSerial ||
                !current.canRefreshPermissionRestrictionStatus()
            )
        store.updateState { current ->
            if (isCurrent(current)) {
                current.copy(
                    permissionRestrictionStatus = restrictionStatus,
                    deniedServerPermissions = if (restrictionStatus == PrivilegeUiPermissionRestrictionStatus.RESTRICTED)
                        current.deniedServerPermissions else emptyList(),
                )
            } else current
        }
        if (restrictionStatus != PrivilegeUiPermissionRestrictionStatus.RESTRICTED || !isCurrent(store.state.value)) return
        val permissions = withContext(operationDispatcher) {
            PrivilegeBinderCall.orElse(fallback = { null }) {
                getDeniedServerPermissions()
            }
        } ?: return
        store.updateState { current ->
            if (isCurrent(current)) current.copy(deniedServerPermissions = permissions) else current
        }
    }

    private fun PrivilegeUiState.canRefreshPermissionRestrictionStatus(): Boolean =
        runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED && serverInfo != null
}
