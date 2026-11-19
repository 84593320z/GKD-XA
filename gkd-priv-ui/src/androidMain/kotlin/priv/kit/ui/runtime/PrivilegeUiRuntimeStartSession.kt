package priv.kit.ui.runtime

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.PrivilegeStartupLogLine
import priv.kit.core.PrivilegeStartupLogListener
import priv.kit.core.internal.runtime.PrivilegeRuntimeClientLaunch
import priv.kit.core.internal.runtime.PrivilegeRuntimeConnectionOrigin
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartLease
import priv.kit.ui.PrivilegeUiStartMethod
import priv.kit.ui.state.toPrivilegeUiDiagnosticString

internal class PrivilegeUiRuntimeStartSession(
    private val startPermit: AutoCloseable,
    val showAttemptFeedback: Boolean = true,
    val recordSuccessfulMethod: Boolean = true,
    private val startupLogSink: (String) -> Unit = {},
    structuredStartupLogSink: (PrivilegeStartupLogLine) -> Unit = {
        startupLogSink("[${it.source}] ${it.message}")
    },
) {
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val completionStarted = AtomicBoolean(false)
    private val cleanupLock = Any()
    private val closeables = mutableListOf<AutoCloseable>()
    private val cleanupFailures = mutableListOf<Throwable>()
    private var cleanupJob: Job? = null
    val connection = CompletableDeferred<PrivilegeUiRuntimeConnection>()
    lateinit var job: Job
    @Volatile var active: Boolean = true
        private set
    @Volatile private var committedStart: PrivilegeUiCommittedStart? = null
    @Volatile private var runtimeStartLease: PrivilegeRuntimeStartLease? = null
    @Volatile var connected: PrivilegeUiRuntimeConnection? = null
        private set

    val startupLogListener = PrivilegeStartupLogListener(structuredStartupLogSink)
    val cancellationRequested: Boolean get() = !active
    val runtimeStartOperationId: Long? get() = runtimeStartLease?.operationId

    fun tryBeginCompletion(): Boolean = completionStarted.compareAndSet(false, true)

    fun releaseStartPermit() = startPermit.close()

    fun addCloseable(closeable: AutoCloseable) {
        val closeNow = synchronized(cleanupLock) {
            if (cleanupJob == null) {
                closeables += closeable
                false
            } else {
                true
            }
        }
        if (closeNow) {
            runCatching(closeable::close).exceptionOrNull()?.let {
                startupLogSink(it.toPrivilegeUiDiagnosticString())
            }
        }
    }

    fun checkActive() {
        if (!active || !job.isActive) {
            throw CancellationException("Runtime start session was cancelled")
        }
    }

    fun attachRuntimeStartLease(lease: PrivilegeRuntimeStartLease) {
        check(runtimeStartLease == null) { "Runtime start lease is already attached" }
        runtimeStartLease = lease
    }

    fun commitStartMethod(method: PrivilegeUiStartMethod?) {
        checkActive()
        val lease = checkNotNull(runtimeStartLease) { "Runtime start lease is not attached" }
        val launch = checkNotNull(PrivilegeRuntimeStartCoordinator.beginClientLaunch(lease)) {
            "Runtime start lease no longer owns the coordinator"
        }
        committedStart = PrivilegeUiCommittedStart(method, launch)
    }

    fun committedMethod(): PrivilegeUiStartMethod? = committedStart?.method

    fun requireRuntimeClientLaunch(): PrivilegeRuntimeClientLaunch =
        checkNotNull(committedStart?.launch) { "Runtime start method has not been committed" }

    fun ownsRuntimeConnection(
        origin: PrivilegeRuntimeConnectionOrigin,
        clientStartOperationId: Long?,
        launchCorrelationId: String?,
    ): Boolean = origin == PrivilegeRuntimeConnectionOrigin.INITIAL_LAUNCH &&
        clientStartOperationId == runtimeStartOperationId &&
        launchCorrelationId == committedStart?.launch?.launchCorrelationId

    fun appendStartupLog(line: String) = startupLogSink(line)

    fun connect(connection: PrivilegeUiRuntimeConnection) {
        val previous = connected
        connected = connection.copy(
            successfulMethod = previous?.successfulMethod ?: connection.successfulMethod,
        )
        this.connection.complete(connection)
    }

    fun disconnect() {
        connected = null
    }

    fun cancel() {
        if (!active) return
        active = false
        startCleanup()
        job.cancel(CancellationException("Runtime start was cancelled"))
    }

    suspend fun close(onFailure: (Throwable) -> Unit) {
        active = false
        try {
            startCleanup().join()
            cleanupFailures.forEach(onFailure)
            cleanupFailures.clear()
            runtimeStartLease?.let { runCatching(it::close).exceptionOrNull()?.let(onFailure) }
            runtimeStartLease = null
        } finally {
            cleanupScope.cancel()
        }
    }

    private fun startCleanup(): Job = synchronized(cleanupLock) {
        cleanupJob ?: cleanupScope.launch(
            context = Dispatchers.IO + CoroutineName("priv-ui-runtime-start-cleanup"),
            start = CoroutineStart.LAZY,
        ) {
            val resources = synchronized(cleanupLock) {
                closeables.toList().also { closeables.clear() }
            }
            resources.forEach { closeable ->
                runCatching(closeable::close).exceptionOrNull()?.let { throwable ->
                    synchronized(cleanupLock) { cleanupFailures += throwable }
                }
            }
        }.also {
            cleanupJob = it
            it.invokeOnCompletion { cleanupScope.cancel() }
            it.start()
        }
    }
}

internal data class PrivilegeUiRuntimeConnection(
    val serverInfo: PrivilegeServerInfo,
    val successfulMethod: PrivilegeUiStartMethod?,
)

private data class PrivilegeUiCommittedStart(
    val method: PrivilegeUiStartMethod?,
    val launch: PrivilegeRuntimeClientLaunch,
)
