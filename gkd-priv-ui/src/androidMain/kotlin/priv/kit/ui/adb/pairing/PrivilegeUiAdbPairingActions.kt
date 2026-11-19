package priv.kit.ui.adb.pairing

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import priv.kit.core.Privilege
import priv.kit.shared.PRIVILEGE_INTERNAL_ADB_LOOPBACK_HOST
import priv.kit.shared.isPrivilegeAdbPort
import priv.kit.ui.PrivilegeAdbPairingService
import priv.kit.ui.PrivilegeUiAdbPairingStatus
import priv.kit.ui.PrivilegeUiText
import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiPermissionState
import priv.kit.ui.PrivilegeUiWirelessAdbStatus
import priv.kit.ui.R
import priv.kit.ui.isPrivilegeUiNotificationPermissionSupported
import priv.kit.ui.toPrivilegeUiPairingCodeDigits
import priv.kit.ui.adb.retryOnLocalNetworkPermissionGrant
import priv.kit.ui.adb.currentConfiguredTcpPort
import priv.kit.ui.runtime.PrivilegeUiStartGate
import priv.kit.ui.state.PrivilegeUiFailureKind
import priv.kit.ui.state.PrivilegeUiViewModelStore
import priv.kit.ui.state.isPrivilegeUiWirelessAdbSupported
import priv.kit.ui.state.toPrivilegeUiDiagnosticString
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds

internal class PrivilegeUiAdbPairingActions(
    private val store: PrivilegeUiViewModelStore,
    private val coroutineScope: CoroutineScope,
    private val enableTcpMode: suspend () -> Unit,
    private val acquireInteractivePermit: () -> AutoCloseable?,
    private val hasInteractionHost: () -> Boolean,
) : AutoCloseable {
    private val notificationFallbackDecisionLock = Any()
    private var pendingNotificationFallbackDecision:
        CompletableDeferred<NotificationFallbackDecision>? = null
    private var notificationEventsJob: Job? = null
    private val pairingSession = PrivilegeUiPairingSession(acquireInteractivePermit)
    private val notificationOwnerId: String
        get() = store.notificationPairingOwnerId

    fun observePairingNotificationEvents() {
        notificationEventsJob?.cancel()
        notificationEventsJob = coroutineScope.launch(
            Dispatchers.Main.immediate + CoroutineName("priv-ui-pairing-notification-events"),
        ) {
            PrivilegeAdbPairingService.notificationEvents.collect { event ->
                if (event.ownerId == notificationOwnerId) {
                    handleNotificationEvent(event)
                }
            }
        }
    }

    fun updatePairingCode(value: String) {
        val recoveringFromFailure = store.state.value.pairingStatus == PrivilegeUiAdbPairingStatus.FAILED
        store.updateState { current ->
            if (!recoveringFromFailure) {
                current.copy(pairingCode = value.toPrivilegeUiPairingCodeDigits())
            } else {
                val serviceFound = pairingSession.port != null
                current.copy(
                    pairingCode = value.toPrivilegeUiPairingCodeDigits(),
                    pairingStatus = if (serviceFound) {
                        PrivilegeUiAdbPairingStatus.FOUND
                    } else {
                        PrivilegeUiAdbPairingStatus.SEARCHING
                    },
                    pairingText = store.resourceText(
                        if (serviceFound) {
                            R.string.priv_ui_pairing_service_found_text
                        } else {
                            R.string.priv_ui_pairing_search_text
                        },
                    ),
                )
            }
        }
        if (recoveringFromFailure) {
            store.state.value.pairingText?.let { text ->
                PrivilegeAdbPairingService.updateStatus(notificationOwnerId, text)
            }
        }
    }

    suspend fun startNotificationPairing(
        requestNotificationPermission: suspend () -> PrivilegeUiPermissionState?,
    ) {

        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        if (store.state.value.pairingNotificationPermissionWarningVisible) {
            return
        }
        if (!pairingSession.acquire()) return
        val context = store.requireContext()
        if (!isNotificationPermissionGranted()) {
            resetPairingSessionForNotificationPermission()
            PrivilegeAdbPairingService.stop(context, notificationOwnerId)
            val permissionState = try {
                requestNotificationPermission()
            } catch (throwable: Throwable) {
                cancelNotificationPermissionRequest()
                throw throwable
            }
            continueAfterNotificationPermission(permissionState)
            return
        }
        startPairingWithNotification()
    }

    fun stopNotificationPairing() {
        resolveNotificationFallbackDecision(NotificationFallbackDecision.CANCEL)
        stopPairingSession(
            text = store.resourceText(R.string.priv_ui_pairing_stopped),
            stopNotification = true,
        )
    }

    fun cancelPendingPairingStart() {
        cancelNotificationPermissionRequest()
    }

    fun cancelNotificationPermissionRequest() {
        if (!resolveNotificationFallbackDecision(NotificationFallbackDecision.CANCEL)) {
            store.updateState {
                it.copy(pairingNotificationPermissionWarningVisible = false)
            }
            if (!store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()) {
                pairingSession.release()
            }
        }
    }

    fun cancelPairingWithoutInteractionHost(
        notificationLost: Boolean,
    ) {
        if (!notificationLost && PrivilegeAdbPairingService.isRequested(notificationOwnerId)) return
        resolveNotificationFallbackDecision(NotificationFallbackDecision.CANCEL)
        stopPairingSession(
            text = store.resourceText(R.string.priv_ui_pairing_stopped),
            stopNotification = true,
        )
    }

    fun continuePairingWithoutNotification() {
        if (!store.state.value.pairingNotificationPermissionWarningVisible) return
        resolveNotificationFallbackDecision(NotificationFallbackDecision.WITHOUT_NOTIFICATION)
    }

    fun continuePendingPairingIfNotificationPermissionGranted() {
        if (!store.state.value.pairingNotificationPermissionWarningVisible) return
        if (!isNotificationPermissionGranted()) return
        resolveNotificationFallbackDecision(NotificationFallbackDecision.WITH_NOTIFICATION)
    }

    fun closePairingDialog() {
        store.updateState {
            it.copy(
                pairingCode = "",
                pairingDialogVisible = false,
            )
        }
    }

    fun submitNotificationPairingCode() {
        submitPairingCode(store.state.value.pairingCode)
    }

    private suspend fun continueAfterNotificationPermission(
        permissionState: PrivilegeUiPermissionState?,
    ) {
        if (permissionState == null) {
            if (!store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()) {
                pairingSession.release()
            }
            return
        }
        if (!pairingSession.acquire()) {
            continueAfterNotificationFallbackDecision(awaitNotificationFallbackDecision())
            return
        }

        when (permissionState) {
            PrivilegeUiPermissionState.Granted -> {
                startPairingWithNotification()
            }
            PrivilegeUiPermissionState.NotGranted.Denied -> {
                startPairingSession()
                store.showFailure(PrivilegeUiFailureKind.NOTIFICATION_PERMISSION_REQUIRED)
            }
            PrivilegeUiPermissionState.NotGranted.PermanentlyDenied -> {
                continueAfterNotificationFallbackDecision(awaitNotificationFallbackDecision())
            }
        }
    }

    private suspend fun awaitNotificationFallbackDecision(): NotificationFallbackDecision {
        val response = CompletableDeferred<NotificationFallbackDecision>()
        val registered = synchronized(notificationFallbackDecisionLock) {
            if (pendingNotificationFallbackDecision != null) {
                false
            } else {
                pendingNotificationFallbackDecision = response
                true
            }
        }
        if (!registered) return NotificationFallbackDecision.CANCEL
        store.updateState {
            it.copy(pairingNotificationPermissionWarningVisible = true)
        }
        return try {
            response.await()
        } catch (throwable: CancellationException) {
            if (!store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()) {
                pairingSession.release()
            }
            throw throwable
        } finally {
            val stillPending = synchronized(notificationFallbackDecisionLock) {
                if (pendingNotificationFallbackDecision === response) {
                    pendingNotificationFallbackDecision = null
                    true
                } else {
                    false
                }
            }
            if (stillPending) {
                store.updateState {
                    it.copy(pairingNotificationPermissionWarningVisible = false)
                }
            }
        }
    }

    private fun resolveNotificationFallbackDecision(
        decision: NotificationFallbackDecision,
    ): Boolean {
        val response = synchronized(notificationFallbackDecisionLock) {
            pendingNotificationFallbackDecision.also {
                pendingNotificationFallbackDecision = null
            }
        } ?: return false
        store.updateState {
            it.copy(pairingNotificationPermissionWarningVisible = false)
        }
        return response.complete(decision)
    }

    private fun continueAfterNotificationFallbackDecision(
        decision: NotificationFallbackDecision,
    ) {
        when (decision) {
            NotificationFallbackDecision.WITH_NOTIFICATION -> {
                if (isNotificationPermissionGranted() && pairingSession.acquire()) {
                    startPairingWithNotification()
                } else {
                    pairingSession.release()
                }
            }
            NotificationFallbackDecision.WITHOUT_NOTIFICATION -> {
                if (pairingSession.acquire()) {
                    startPairingSession()
                } else {
                    pairingSession.release()
                }
            }
            NotificationFallbackDecision.CANCEL -> {
                if (!store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()) {
                    pairingSession.release()
                }
            }
        }
    }

    override fun close() {
        resolveNotificationFallbackDecision(NotificationFallbackDecision.CANCEL)
        store.updateState {
            it.copy(pairingNotificationPermissionWarningVisible = false)
        }
        pairingSession.invalidate()
        store.applicationContext?.let { context ->
            PrivilegeAdbPairingService.stop(context, notificationOwnerId)
        }
        notificationEventsJob?.cancel()
        notificationEventsJob = null
        pairingSession.release()
    }

    private enum class NotificationFallbackDecision {
        WITH_NOTIFICATION,
        WITHOUT_NOTIFICATION,
        CANCEL,
    }

    private fun resetPairingSessionForNotificationPermission() {
        pairingSession.invalidate()
        store.updateState { it.resetPairing() }
    }

    private fun startPairingSession() {
        if (!pairingSession.acquire()) return
        val adbDeviceName = store.currentAdbDeviceNameOverride()
        val session = pairingSession.begin(adbDeviceName)
        val searchText = store.resourceText(R.string.priv_ui_pairing_search_text)
        store.updateState { it.searchingPairing(searchText) }
        store.appendStartupLog(store.text(R.string.priv_ui_notification_pairing_started))
        PrivilegeAdbPairingService.updateStatus(notificationOwnerId, searchText)
        launchPairingDiscovery(
            session = session,
            adbDeviceName = adbDeviceName,
            initialPort = null,
        )
    }

    private fun startPairingWithNotification() {
        if (!pairingSession.acquire()) return
        startPairingSession()
        startNotificationUi()
    }

    private fun isNotificationPermissionGranted(): Boolean =
        !isPrivilegeUiNotificationPermissionSupported() ||
            ContextCompat.checkSelfPermission(
                store.requireContext(),
                Manifest.permission.POST_NOTIFICATIONS,
            ) ==
            PackageManager.PERMISSION_GRANTED

    private fun launchPairingDiscovery(
        session: Int,
        adbDeviceName: String?,
        initialPort: Int?,
    ) {
        if (!isPrivilegeUiWirelessAdbSupported()) return
        val job = createPairingJob("priv-ui-pairing-session") discovery@{
            val manager = Privilege.createAdbManager(adbDeviceName = adbDeviceName)
            var monitoredPort = initialPort
            while (isActive && pairingSession.isCurrent(session)) {
                if (monitoredPort == null) {
                    val discovery = try {
                        val port = store.retryOnLocalNetworkPermissionGrant {
                            manager.discoverPairingPort(PAIRING_DISCOVERY_ATTEMPT_TIMEOUT_MILLIS)
                        }
                        val identityInfo = withContext(Dispatchers.IO) {
                            runCatching { manager.getIdentityInfo() }.getOrNull()
                        }
                        port to identityInfo
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException || !pairingSession.isCurrent(session)) {
                            return@discovery
                        }
                        val retryText = store.resourceText(R.string.priv_ui_pairing_search_attempt)
                        updatePairingStatus(
                            status = PrivilegeUiAdbPairingStatus.SEARCHING,
                            text = retryText,
                            fingerprint = null,
                        )
                        delay(PAIRING_DISCOVERY_RETRY_DELAY_MILLIS.milliseconds)
                        continue
                    }

                    if (!pairingSession.isCurrent(session)) return@discovery
                    val (port, identityInfo) = discovery
                    val transition = privilegeAdbPairingDiscoveryTransition(pairingSession.port, port)
                    pairingSession.discovered(port, adbDeviceName)
                    monitoredPort = port
                    if (transition == PrivilegeUiAdbPairingStatus.FOUND) {
                        val text = store.resourceText(R.string.priv_ui_pairing_service_found_text)
                        updatePairingStatus(
                            status = transition,
                            text = text,
                            fingerprint = identityInfo?.publicKeyFingerprint,
                        )
                        store.appendStartupLog(store.resolveText(text))
                    }
                }

                while (isActive && pairingSession.isCurrent(session)) {
                    val port = monitoredPort
                    val portListening = runInterruptible(Dispatchers.IO) {
                        isLocalPairingPortListening(port)
                    }
                    if (!portListening) break
                    delay(PAIRING_PORT_MONITOR_INTERVAL_MILLIS.milliseconds)
                }
                if (!pairingSession.isCurrent(session)) return@discovery

                val transition = privilegeAdbPairingDiscoveryTransition(pairingSession.port, null)
                pairingSession.lostEndpoint()
                monitoredPort = null
                if (transition == PrivilegeUiAdbPairingStatus.SEARCHING) {
                    updatePairingStatus(
                        status = transition,
                        text = store.resourceText(R.string.priv_ui_pairing_search_text),
                        fingerprint = null,
                    )
                }
                delay(PAIRING_DISCOVERY_RETRY_DELAY_MILLIS.milliseconds)
            }
        }
        pairingSession.attach(session, job)
    }

    private fun submitPairingCode(pairingCode: String) {
        if (!isPrivilegeUiWirelessAdbSupported()) return
        if (!store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()) return
        if (!pairingSession.acquire()) return
        val code = pairingCode.trim()
        if (!code.isPrivilegeUiPairingCode()) {
            val text = store.resourceText(R.string.priv_ui_pairing_code_required)
            updatePairingStatus(
                status = PrivilegeUiAdbPairingStatus.FAILED,
                text = text,
                fingerprint = null,
            )
            store.showFailure(PrivilegeUiFailureKind.PAIRING_CODE_REQUIRED)
            return
        }
        val port = pairingSession.port?.takeIf { it.isPrivilegeAdbPort() }
        if (port == null) {
            val text = store.resourceText(R.string.priv_ui_pairing_port_unavailable)
            updatePairingStatus(
                status = PrivilegeUiAdbPairingStatus.SEARCHING,
                text = text,
                fingerprint = null,
            )
            store.showFailure(PrivilegeUiFailureKind.PAIRING_PORT_UNAVAILABLE)
            return
        }

        val adbDeviceName = pairingSession.deviceName
        val session = pairingSession.nextOperation()
        val pairingText = store.resourceText(R.string.priv_ui_pairing_with_port)
        updatePairingStatus(
            status = PrivilegeUiAdbPairingStatus.PAIRING,
            text = pairingText,
            fingerprint = null,
        )
        store.appendStartupLog(store.resolveText(pairingText))
        val job = createPairingJob("priv-ui-pairing-submit") submit@{
            try {
                val result = Privilege.createAdbManager(adbDeviceName = adbDeviceName).pair(
                    pairingCode = code,
                    port = port,
                )
                if (!pairingSession.isCurrent(session)) return@submit

                pairingSession.finishOperation()
                val text = store.resourceText(R.string.priv_ui_pairing_success_text)
                store.updateState { it.completedPairing(text, result.publicKeyFingerprint) }
                val message = store.resolveText(text)
                store.appendStartupLog(message)
                PrivilegeAdbPairingService.stop(store.requireContext(), notificationOwnerId)
                pairingSession.release()
                enableTcpModeAfterPairing()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException || !pairingSession.isCurrent(session)) {
                    return@submit
                }
                pairingSession.discovered(port, adbDeviceName)
                val failureKind = PrivilegeUiFailureKind.PAIRING_FAILED
                val text = store.resourceText(failureKind.messageResId)
                updatePairingStatus(
                    status = PrivilegeUiAdbPairingStatus.FAILED,
                    text = text,
                    fingerprint = null,
                )
                store.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
                store.showFailure(failureKind)
                launchPairingDiscovery(
                    session = session,
                    adbDeviceName = adbDeviceName,
                    initialPort = port,
                )
            }
        }
        pairingSession.attach(session, job)
    }

    private fun startNotificationUi() {
        val context = store.requireContext()
        val text = store.state.value.pairingText
            ?: store.resourceText(R.string.priv_ui_pairing_search_text)
        var startFailure: Throwable? = null
        val started = try {
            PrivilegeAdbPairingService.startWithText(
                context = context,
                ownerId = notificationOwnerId,
                statusText = text,
                notificationChannelId = store.config.notificationPairingChannelId,
                notificationId = store.config.notificationPairingNotificationId,
            )
        } catch (throwable: Throwable) {
            startFailure = throwable
            store.showFailure(PrivilegeUiFailureKind.PAIRING_NOTIFICATION_FAILED)
            store.appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
            false
        }
        store.updateState { it.copy(notificationPairingRunning = started) }
        if (!started && startFailure == null) {
            store.showFailure(PrivilegeUiFailureKind.NOTIFICATION_PERMISSION_REQUIRED)
        }
    }

    private fun stopPairingSession(
        text: PrivilegeUiText,
        stopNotification: Boolean,
    ) {
        val wasActive = store.state.value.pairingStatus.isPrivilegeUiPairingSessionActive()
        pairingSession.invalidate()
        if (stopNotification) {
            PrivilegeAdbPairingService.stop(store.requireContext(), notificationOwnerId)
        }
        store.updateState { it.resetPairing(text) }
        if (wasActive) {
            store.appendStartupLog(store.resolveText(text))
        }
        pairingSession.release()
    }

    fun handleNotificationEvent(event: PrivilegeAdbPairingNotificationEvent) {
        if (event.ownerId != notificationOwnerId) return
        when (event) {
            is PrivilegeAdbPairingNotificationEvent.Submit -> {
                updatePairingCode(event.pairingCode)
                submitPairingCode(event.pairingCode)
            }
            is PrivilegeAdbPairingNotificationEvent.Unavailable -> {
                store.updateState { it.copy(notificationPairingRunning = false) }
                store.showFailure(event.reason.toPrivilegeUiFailureKind())
                stopIfNotificationLostWithoutInteractionHost()
            }
            is PrivilegeAdbPairingNotificationEvent.Stop -> {
                stopPairingSession(
                    text = store.resourceText(R.string.priv_ui_pairing_stopped),
                    stopNotification = false,
                )
            }
            is PrivilegeAdbPairingNotificationEvent.Detached -> {
                store.updateState { it.copy(notificationPairingRunning = false) }
                stopIfNotificationLostWithoutInteractionHost()
            }
        }
    }

    private fun stopIfNotificationLostWithoutInteractionHost() {
        if (!hasInteractionHost()) {
            cancelPairingWithoutInteractionHost(notificationLost = true)
        }
    }

    private fun updatePairingStatus(
        status: PrivilegeUiAdbPairingStatus,
        text: PrivilegeUiText,
        fingerprint: String?,
    ) {
        store.updateState {
            it.copy(
                pairingStatus = status,
                pairingText = text,
                adbKeyFingerprint = fingerprint ?: it.adbKeyFingerprint,
            )
        }
        PrivilegeAdbPairingService.updateStatus(
            ownerId = notificationOwnerId,
            text = text,
            acceptsPairingCode = status != PrivilegeUiAdbPairingStatus.PAIRING,
        )
    }

    private suspend fun enableTcpModeAfterPairing() {
        if (
            !PrivilegeUiStartGate.isSilentStartInProgress &&
            store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.AUTO_ENABLE_AFTER_WIRELESS_PAIRED &&
            store.currentConfiguredTcpPort() == null
        ) {
            enableTcpMode()
        }
    }

    private fun createPairingJob(
        name: String,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = coroutineScope.launch(
        context = CoroutineName(name),
        start = CoroutineStart.LAZY,
        block = block,
    )

    private fun isLocalPairingPortListening(port: Int): Boolean =
        try {
            ServerSocket().use { socket ->
                socket.bind(InetSocketAddress(PRIVILEGE_INTERNAL_ADB_LOOPBACK_HOST, port), 1)
            }
            false
        } catch (_: IOException) {
            true
        }

}

internal fun privilegeAdbPairingDiscoveryTransition(
    previousPort: Int?,
    observedPort: Int?,
): PrivilegeUiAdbPairingStatus? = when {
    previousPort == observedPort -> null
    observedPort == null -> PrivilegeUiAdbPairingStatus.SEARCHING
    else -> PrivilegeUiAdbPairingStatus.FOUND
}


private const val PAIRING_DISCOVERY_ATTEMPT_TIMEOUT_MILLIS = 6_000L
private const val PAIRING_DISCOVERY_RETRY_DELAY_MILLIS = 500L
private const val PAIRING_PORT_MONITOR_INTERVAL_MILLIS = 500L
