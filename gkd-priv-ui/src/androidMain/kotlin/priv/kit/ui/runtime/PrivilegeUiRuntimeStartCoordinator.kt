package priv.kit.ui.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.PrivilegeServerLaunchUncertainException
import priv.kit.core.PrivilegeStartupLogLine
import priv.kit.core.internal.runtime.PrivilegeRuntimeConnectionEvent
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import priv.kit.ui.PrivilegeUi
import priv.kit.ui.PrivilegeUiRuntimeStartPhase
import priv.kit.ui.PrivilegeUiRuntimeStatus
import priv.kit.ui.PrivilegeUiStartMethod
import priv.kit.ui.PrivilegeUiState
import priv.kit.ui.PrivilegeUiText
import priv.kit.ui.R
import priv.kit.ui.state.PrivilegeUiFailureKind
import priv.kit.ui.state.PrivilegeUiViewModelStore
import priv.kit.ui.state.privilegeUiRuntimeStartFailureKind
import priv.kit.ui.state.toPrivilegeUiDiagnosticString
import kotlin.time.Duration.Companion.milliseconds

internal class PrivilegeUiRuntimeStartCoordinator(
    private val store: PrivilegeUiViewModelStore,
    private val coroutineScope: CoroutineScope,
    private val isClosed: () -> Boolean,
    private val publishConnectedServer: (PrivilegeServerInfo) -> Unit,
    private val acquireStartPermit: () -> AutoCloseable?,
) {
    @Volatile private var activeSession: PrivilegeUiRuntimeStartSession? = null
    private var activeJob: Job? = null

    fun stopCurrentStart() {
        val session = activeSession ?: return
        if (store.state.value.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.RUNNING) return
        store.updateState {
            it.copy(
                busy = true,
                runtimeStartPhase = PrivilegeUiRuntimeStartPhase.CANCELLING,
                runtimeProgressText = store.resourceText(R.string.priv_ui_startup_cancelling),
            )
        }
        session.cancel()
    }

    fun runServerStart(
        attempt: PrivilegeUiRuntimeStartAttempt.Connect,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runServerStartAttempt(attempt, "priv-ui-runtime-start", replaceConnectedServer)

    fun runServerStartRequest(
        attempt: PrivilegeUiRuntimeStartAttempt.Request,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runServerStartAttempt(attempt, "priv-ui-runtime-start-request", replaceConnectedServer)

    fun runServerStartWorkflow(
        attempt: PrivilegeUiRuntimeStartAttempt.Workflow,
        replaceConnectedServer: Boolean = false,
    ): Boolean =
        runServerStartAttempt(attempt, "priv-ui-runtime-start-workflow", replaceConnectedServer)

    fun runServerStartFallback(attempts: List<PrivilegeUiRuntimeStartAttempt>): Boolean {
        if (attempts.isEmpty()) {
            reportStartFailedIfIdle()
            return false
        }
        return launchRuntimeStart(
            initialAttempt = attempts.first(),
            showAttemptFeedback = false,
            name = "priv-ui-runtime-start-fallback",
            beforeStart = { store.clearStartupLog() },
        ) { session -> runFallbackAttempts(session, attempts) }
    }

    fun handleServerConnected(
        event: PrivilegeRuntimeConnectionEvent,
        deduplicatePassiveConnection: Boolean,
    ) {
        val session = activeSession
        if (session != null) {
            val exactStart = session.active && session.ownsRuntimeConnection(
                origin = event.origin,
                clientStartOperationId = event.clientStartOperationId,
                launchCorrelationId = event.launchCorrelationId,
            )
            session.connect(
                PrivilegeUiRuntimeConnection(
                    serverInfo = event.serverInfo,
                    successfulMethod = session.committedMethod().takeIf {
                        exactStart && session.recordSuccessfulMethod
                    },
                ),
            )
            completeCancelledSessionFromConnection(session)
            return
        }
        publishPassiveConnection(event.serverInfo, deduplicatePassiveConnection)
    }

    fun handleRefreshedServerConnected(
        serverInfo: PrivilegeServerInfo,
        deduplicatePassiveConnection: Boolean,
    ) {
        if (activeSession == null) {
            publishPassiveConnection(serverInfo, deduplicatePassiveConnection)
        }
    }

    fun close() {
        activeSession?.cancel()
    }

    fun handleServerDisconnected() {
        activeSession?.disconnect()
    }

    private fun runServerStartAttempt(
        attempt: PrivilegeUiRuntimeStartAttempt,
        name: String,
        replaceConnectedServer: Boolean,
    ): Boolean = launchRuntimeStart(
        initialAttempt = attempt,
        showAttemptFeedback = true,
        name = name,
        replaceConnectedServer = replaceConnectedServer,
        beforeStart = {
            store.clearStartupLog()
            appendStartupSource(it, attempt.startupSource)
            it.appendStartupLog(store.resolveText(attempt.progressText))
        },
    ) { session -> runSingleAttempt(session, attempt) }

    private fun launchRuntimeStart(
        initialAttempt: PrivilegeUiRuntimeStartAttempt,
        showAttemptFeedback: Boolean,
        name: String,
        replaceConnectedServer: Boolean = false,
        beforeStart: (PrivilegeUiRuntimeStartSession) -> Unit,
        block: suspend (PrivilegeUiRuntimeStartSession) -> RuntimeStartCompletion,
    ): Boolean {
        val startPermit = acquireStartPermit() ?: return false
        val current = store.state.value
        if (
            isClosed() || current.busy ||
            current.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE ||
            activeSession != null || activeJob != null
        ) {
            startPermit.close()
            return false
        }

        val preflight = PrivilegeRuntimeStartCoordinator.beginPreflight()
        lateinit var session: PrivilegeUiRuntimeStartSession
        session = PrivilegeUiRuntimeStartSession(
            startPermit = startPermit,
            showAttemptFeedback = showAttemptFeedback,
            startupLogSink = { line -> appendSessionStartupLog(session, line) },
            structuredStartupLogSink = { line -> appendSessionStartupLog(session, line) },
        )
        val job = coroutineScope.launch(
            context = CoroutineName(name),
            start = CoroutineStart.LAZY,
        ) {
            val completion = try {
                if (preflight.remainingReconnectGraceMillis > 0L) {
                    delay(preflight.remainingReconnectGraceMillis.milliseconds)
                }
                session.connected?.let {
                    return@launch withContext(NonCancellable) {
                        completeRuntimeStart(
                            session,
                            RuntimeStartCompletion.Connected(it.serverInfo, it.successfulMethod),
                        )
                    }
                }
                session.checkActive()
                val lease = PrivilegeRuntimeStartCoordinator.tryCommitClientStart(
                    preflight = preflight,
                    replaceConnectedServer = replaceConnectedServer,
                )
                if (lease == null) {
                    session.connected?.let {
                        RuntimeStartCompletion.Connected(it.serverInfo, it.successfulMethod)
                    } ?: RuntimeStartCompletion.Superseded
                } else {
                    session.attachRuntimeStartLease(lease)
                    block(session)
                }
            } catch (_: CancellationException) {
                session.connected?.let {
                    RuntimeStartCompletion.Connected(it.serverInfo, it.successfulMethod)
                } ?: RuntimeStartCompletion.Cancelled
            } catch (throwable: Throwable) {
                RuntimeStartCompletion.Failure(
                    throwable = throwable,
                    failureKind = privilegeUiRuntimeStartFailureKind(
                        runtimeStartSource = initialAttempt.runtimeStartSource,
                        throwable = throwable,
                    ),
                )
            }
            withContext(NonCancellable) {
                completeRuntimeStart(session, completion)
            }
        }
        session.job = job
        activeSession = session
        activeJob = job
        store.updateState {
            it.startingAttempt(initialAttempt, PrivilegeUiRuntimeStartPhase.RUNNING)
        }
        return try {
            beforeStart(session)
            if (job.start()) {
                true
            } else {
                activeSession = null
                activeJob = null
                session.cancel()
                session.releaseStartPermit()
                store.updateState { it.finishRuntimeStartFailed() }
                false
            }
        } catch (throwable: Throwable) {
            activeSession = null
            activeJob = null
            session.cancel()
            startPermit.close()
            store.updateState { it.finishRuntimeStartFailed() }
            job.cancel()
            throw throwable
        }
    }

    private suspend fun runSingleAttempt(
        session: PrivilegeUiRuntimeStartSession,
        attempt: PrivilegeUiRuntimeStartAttempt,
    ): RuntimeStartCompletion = try {
        raceAttemptWithConnection(session, attempt)
    } catch (_: CancellationException) {
        RuntimeStartCompletion.Cancelled
    } catch (throwable: Throwable) {
        if (session.cancellationRequested) {
            RuntimeStartCompletion.Cancelled
        } else {
            attempt.failureDisposition(throwable)
                ?.let(RuntimeStartCompletion::HandledFailure)
                ?: RuntimeStartCompletion.Failure(
                    throwable = throwable,
                    failureKind = privilegeUiRuntimeStartFailureKind(
                        runtimeStartSource = attempt.runtimeStartSource,
                        throwable = throwable,
                    ),
                )
        }
    }

    private suspend fun runFallbackAttempts(
        session: PrivilegeUiRuntimeStartSession,
        attempts: List<PrivilegeUiRuntimeStartAttempt>,
    ): RuntimeStartCompletion {
        attempts.forEach { attempt ->
            if (!isCurrent(session)) return RuntimeStartCompletion.Superseded
            updateCurrentAttempt(session, attempt)
            appendStartupSource(session, attempt.startupSource)
            session.appendStartupLog(store.resolveText(attempt.progressText))
            try {
                when (val completion = raceAttemptWithConnection(session, attempt)) {
                    is RuntimeStartCompletion.Connected,
                    is RuntimeStartCompletion.Failure,
                    -> return completion
                    RuntimeStartCompletion.Cancelled -> return completion
                    RuntimeStartCompletion.Finished -> Unit
                    RuntimeStartCompletion.Superseded -> return completion
                    is RuntimeStartCompletion.HandledFailure -> Unit
                }
            } catch (_: CancellationException) {
                return RuntimeStartCompletion.Cancelled
            } catch (throwable: Throwable) {
                if (!isCurrent(session)) return RuntimeStartCompletion.Superseded
                attempt.failureDisposition(throwable)?.let { disposition ->
                    store.updateState(disposition.stateTransform)
                    disposition.afterCommit?.let { runCatching(it) }
                }
                session.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
                if (
                    attempt is PrivilegeUiRuntimeStartAttempt.Request ||
                    throwable is PrivilegeServerLaunchUncertainException
                ) {
                    return startFailedWithoutDiagnostic()
                }
            }
        }
        return startFailedWithoutDiagnostic()
    }

    private suspend fun raceAttemptWithConnection(
        session: PrivilegeUiRuntimeStartSession,
        attempt: PrivilegeUiRuntimeStartAttempt,
    ): RuntimeStartCompletion = supervisorScope {
        val task = async { executeAttempt(session, attempt) }
        select {
            session.connection.onAwait { connection ->
                task.cancel()
                RuntimeStartCompletion.Connected(
                    connection.serverInfo,
                    connection.successfulMethod,
                )
            }
            task.onAwait { result ->
                when (result) {
                    is PrivilegeUiRuntimeStartResult.Connected -> {
                        val connection = PrivilegeUiRuntimeConnection(
                            result.serverInfo,
                            session.committedMethod().takeIf { session.recordSuccessfulMethod },
                        )
                        session.connect(connection)
                        RuntimeStartCompletion.Connected(
                            connection.serverInfo,
                            connection.successfulMethod,
                        )
                    }
                    is PrivilegeUiRuntimeStartResult.RequestSent ->
                        awaitExternalConnection(session, result.text)
                    PrivilegeUiRuntimeStartResult.Finished -> RuntimeStartCompletion.Finished
                }
            }
        }
    }

    private suspend fun executeAttempt(
        session: PrivilegeUiRuntimeStartSession,
        attempt: PrivilegeUiRuntimeStartAttempt,
    ): PrivilegeUiRuntimeStartResult {
        session.commitStartMethod(
            privilegeUiStartMethod(attempt.runtimeStartSource, attempt.runtimeStartProviderId),
        )
        return when (attempt) {
            is PrivilegeUiRuntimeStartAttempt.Connect ->
                PrivilegeUiRuntimeStartResult.Connected(attempt.start(session))
            is PrivilegeUiRuntimeStartAttempt.Request -> {
                attempt.start(session)
                PrivilegeUiRuntimeStartResult.RequestSent(attempt.startedText)
            }
            is PrivilegeUiRuntimeStartAttempt.Workflow -> attempt.start(session)
        }
    }

    private suspend fun awaitExternalConnection(
        session: PrivilegeUiRuntimeStartSession,
        text: PrivilegeUiText,
    ): RuntimeStartCompletion {
        updateCurrentState(session) { current ->
            current.takeIf { it.runtimeStartPhase == PrivilegeUiRuntimeStartPhase.RUNNING }
                ?.copy(runtimeProgressText = text)
                ?: current
        }
        session.appendStartupLog(store.resolveText(text))
        val connection = withTimeoutOrNull(store.config.startTimeoutMillis.milliseconds) {
            session.connection.await()
        } ?: return startFailedWithoutDiagnostic()
        return RuntimeStartCompletion.Connected(
            connection.serverInfo,
            connection.successfulMethod,
        )
    }

    private suspend fun completeRuntimeStart(
        session: PrivilegeUiRuntimeStartSession,
        initialCompletion: RuntimeStartCompletion,
    ) {
        if (!session.tryBeginCompletion()) return
        if (activeSession !== session) {
            try {
                session.close(onFailure = {})
            } finally {
                session.releaseStartPermit()
            }
            return
        }
        try {
            try {
                session.close { throwable ->
                    if (!isClosed()) store.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
                }
            } finally {
                activeSession = null
                activeJob = null
            }
            val completion = session.connected?.let { connection ->
                RuntimeStartCompletion.Connected(connection.serverInfo, connection.successfulMethod)
            } ?: initialCompletion.takeUnless { it is RuntimeStartCompletion.Connected }
                ?: RuntimeStartCompletion.Finished
            if (isClosed()) return
            when (completion) {
                is RuntimeStartCompletion.Connected -> {
                    completion.successfulMethod?.let(::recordSuccessfulUiStart)
                    publishConnectedServer(completion.serverInfo)
                }
                RuntimeStartCompletion.Cancelled -> {
                    store.appendStartupLog(store.text(R.string.priv_ui_startup_interrupted))
                    store.updateState { it.finishRuntimeStartDisconnected() }
                }
                is RuntimeStartCompletion.Failure -> {
                    store.showFailure(completion.failureKind)
                    if (completion.appendDiagnostic) {
                        store.appendStartupLog(completion.throwable.toPrivilegeUiDiagnosticString())
                    }
                    store.updateState { it.finishRuntimeStartFailed() }
                }
                RuntimeStartCompletion.Finished ->
                    store.updateState { it.finishRuntimeStartDisconnected() }
                RuntimeStartCompletion.Superseded ->
                    store.updateState { it.finishRuntimeStartPreservingStatus() }
                is RuntimeStartCompletion.HandledFailure -> {
                    val disposition = completion.disposition
                    disposition.snackbarText?.let(store::showSnackbar)
                    disposition.startupLogLines.forEach(store::appendStartupLog)
                    store.updateState {
                        disposition.stateTransform(it).finishRuntimeStartDisconnected()
                    }
                    disposition.afterCommit?.let { runCatching(it) }
                }
            }
        } finally {
            session.releaseStartPermit()
        }
    }

    private fun completeCancelledSessionFromConnection(session: PrivilegeUiRuntimeStartSession) {
        if (!session.cancellationRequested) return
        val connection = session.connected ?: return
        coroutineScope.launch(
            context = CoroutineName("priv-ui-runtime-cancelled-connection"),
            start = CoroutineStart.UNDISPATCHED,
        ) {
            withContext(NonCancellable) {
                completeRuntimeStart(
                    session,
                    RuntimeStartCompletion.Connected(
                        connection.serverInfo,
                        connection.successfulMethod,
                    ),
                )
            }
        }
    }

    private fun publishPassiveConnection(
        serverInfo: PrivilegeServerInfo,
        deduplicate: Boolean,
    ) {
        if (isClosed()) return
        val current = store.state.value
        if (
            deduplicate && current.runtimeStartPhase == PrivilegeUiRuntimeStartPhase.IDLE &&
            current.runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED &&
            current.serverInfo == serverInfo
        ) {
            return
        }
        publishConnectedServer(serverInfo)
    }

    private fun updateCurrentAttempt(
        session: PrivilegeUiRuntimeStartSession,
        attempt: PrivilegeUiRuntimeStartAttempt,
    ) = updateCurrentState(session) {
        if (it.runtimeStartPhase == PrivilegeUiRuntimeStartPhase.RUNNING) {
            it.startingAttempt(attempt, PrivilegeUiRuntimeStartPhase.RUNNING)
        } else {
            it
        }
    }

    private fun updateCurrentState(
        session: PrivilegeUiRuntimeStartSession,
        transform: (PrivilegeUiState) -> PrivilegeUiState,
    ) {
        if (isCurrent(session)) store.updateState(transform)
    }

    private fun isCurrent(session: PrivilegeUiRuntimeStartSession): Boolean =
        !isClosed() && activeSession === session && session.active

    private fun appendStartupSource(
        session: PrivilegeUiRuntimeStartSession,
        startupSource: String?,
    ) {
        val source = startupSource?.trim()?.takeIf(String::isNotEmpty) ?: return
        session.appendStartupLog(store.text(R.string.priv_ui_startup_source, source))
    }

    private fun appendSessionStartupLog(
        session: PrivilegeUiRuntimeStartSession,
        line: String,
    ) {
        if (isCurrent(session)) store.appendStartupLog(line)
    }

    private fun appendSessionStartupLog(
        session: PrivilegeUiRuntimeStartSession,
        line: PrivilegeStartupLogLine,
    ) {
        if (isCurrent(session)) store.appendStartupLog(line)
    }

    private fun recordSuccessfulUiStart(method: PrivilegeUiStartMethod) {
        store.applicationContext?.let { context ->
            runCatching {
                PrivilegeUiStartMethodStore(context).write(method)
                PrivilegeUi.setDesiredEnabled(true)
            }
        }
    }

    private fun reportStartFailedIfIdle() {
        val current = store.state.value
        if (
            isClosed() || current.busy ||
            current.runtimeStartPhase != PrivilegeUiRuntimeStartPhase.IDLE ||
            current.runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED
        ) {
            return
        }
        store.showFailure(PrivilegeUiFailureKind.START_FAILED)
        store.appendStartupLog(store.text(R.string.priv_ui_start_failed))
    }

    private fun startFailedWithoutDiagnostic() = RuntimeStartCompletion.Failure(
        throwable = IllegalStateException(store.text(R.string.priv_ui_start_failed)),
        failureKind = PrivilegeUiFailureKind.START_FAILED,
        appendDiagnostic = false,
    )

    private fun PrivilegeUiRuntimeStartAttempt.failureDisposition(
        throwable: Throwable,
    ): PrivilegeUiRuntimeStartFailureDisposition? = when (this) {
        is PrivilegeUiRuntimeStartAttempt.Connect -> onFailure?.invoke(throwable)
        is PrivilegeUiRuntimeStartAttempt.Workflow -> onFailure?.invoke(throwable)
        is PrivilegeUiRuntimeStartAttempt.Request -> null
    }
}
