package priv.kit.ui.adb

import priv.kit.ui.*
import priv.kit.ui.adb.pairing.*
import priv.kit.ui.runtime.*
import priv.kit.ui.state.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import priv.kit.core.Privilege
import priv.kit.core.adb.PrivilegeAdbConnectionOptions
import priv.kit.core.adb.PrivilegeAdbWirelessDebuggingControl
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import kotlin.time.Duration.Companion.milliseconds

internal class PrivilegeUiAdbActions(
    private val store: PrivilegeUiViewModelStore,
    private val runtimeActions: PrivilegeUiRuntimeActions,
    private val coroutineScope: CoroutineScope,
    private val acquireInteractivePermit: () -> AutoCloseable?,
    private val hasInteractionHost: () -> Boolean,
    private val systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator,
) : AutoCloseable {
    private var permissionObserver: Job? = null
    private val staticTcpConfirmationController = PrivilegeUiStaticTcpConfirmationController()
    private val statusActions = PrivilegeUiAdbStatusActions(
        store = store,
        coroutineScope = coroutineScope,
    )
    private val tcpActions = PrivilegeUiAdbTcpActions(
        store = store,
        systemPromptCoordinator = systemPromptCoordinator,
    )
    private val pairingActions = PrivilegeUiAdbPairingActions(
        store = store,
        coroutineScope = coroutineScope,
        enableTcpMode = {
            enableTcpMode()
        },
        acquireInteractivePermit = acquireInteractivePermit,
        hasInteractionHost = hasInteractionHost,
    )

    val staticTcpSwitchConfirmation = staticTcpConfirmationController.pendingAction

    fun observePairingNotificationEvents() {
        pairingActions.observePairingNotificationEvents()
    }

    fun updatePairingCode(value: String) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        pairingActions.updatePairingCode(value)
    }

    suspend fun startNotificationPairing(
        requestNotificationPermission: suspend () -> PrivilegeUiPermissionState?,
    ) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        pairingActions.startNotificationPairing(requestNotificationPermission)
    }

    fun stopNotificationPairing() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        pairingActions.stopNotificationPairing()
    }

    fun cancelPendingPairingStart() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        pairingActions.cancelPendingPairingStart()
    }

    fun cancelPairingWithoutInteractionHost() {
        pairingActions.cancelPairingWithoutInteractionHost(notificationLost = false)
    }

    fun continuePairingWithoutNotification() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        pairingActions.continuePairingWithoutNotification()
    }

    fun continuePendingPairingIfNotificationPermissionGranted() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        pairingActions.continuePendingPairingIfNotificationPermissionGranted()
    }

    fun closePairingDialog() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        pairingActions.closePairingDialog()
    }

    fun submitNotificationPairingCode() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        pairingActions.submitNotificationPairingCode()
    }

    fun startWirelessAdb(
        replaceConnectedServer: Boolean = false,
    ) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        refreshAdbStartPrerequisites()
        runtimeActions.runServerStartWorkflow(
            wirelessAdbStartWorkflow(),
            replaceConnectedServer,
        )
    }

    fun startAdb(
        replaceConnectedServer: Boolean = false,
    ) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        val tcpModePort = store.currentConfiguredTcpPort()
        if (
            store.config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED &&
            tcpModePort != null
        ) {
            startStaticTcpAdb(replaceConnectedServer)
        } else {
            startWirelessAdb(replaceConnectedServer)
        }
    }

    fun startStaticTcpAdb(
        replaceConnectedServer: Boolean = false,
    ) {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        runtimeActions.runServerStartWorkflow(
            staticTcpAdbStartWorkflow(
                confirmTcpSwitch = true,
            ),
            replaceConnectedServer,
        )
    }

    private suspend fun prepareWirelessAdbCommand(
        session: PrivilegeUiRuntimeStartSession,
    ): Boolean {
        session.checkActive()
        if (!ensureWifiConnectedForWirelessAdbStart(session)) return false
        session.appendStartupLog(store.text(R.string.priv_ui_checking_wireless_adb))
        if (!statusActions.forceWirelessAdbStatusRefreshForAction()) return false
        session.checkActive()
        if (!ensureWifiConnectedForWirelessAdbStart(session)) return false
        if (!ensureWirelessDebuggingReadyForStart(session)) return false
        return ensureWirelessAdbPairedForStart(session)
    }

    suspend fun enableTcpMode() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        val interactionPermit = acquireInteractivePermit() ?: return
        try {
            if (
                staticTcpConfirmationController.awaitConfirmation(
                    PrivilegeUiStaticTcpSwitchAction.ENABLE_PORT,
                ) &&
                store.config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED &&
                store.state.value.runtimeStatus != PrivilegeUiRuntimeStatus.CONNECTED
            ) {
                runEnableTcpMode()
            }
        } finally {
            interactionPermit.close()
        }
    }

    private fun runEnableTcpMode() {

        runtimeActions.runBusy(
            message = store.text(R.string.priv_ui_tcp_enabling),
            failureKind = PrivilegeUiFailureKind.TCP_ENABLE_FAILED,
            onFailure = {
                store.updateStaticTcp { it.copy(activePort = null) }
            },
            action = tcpActions::enableTcpMode,
            onSuccess = {
                statusActions.refreshTcpModeEnabled()
                store.text(R.string.priv_ui_tcp_enabled)
            },
        )
    }

    fun disableTcpMode() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        val tcpPort = store.state.value.staticTcp.activePort ?: return
        val options = staticTcpControlOptions()
        runtimeActions.runBusy(
            message = store.text(R.string.priv_ui_tcp_disabling),
            failureKind = PrivilegeUiFailureKind.TCP_DISABLE_FAILED,
            onFailure = { statusActions.refreshTcpModeEnabled() },
            action = {
                withTcpControlPrompt(
                    options = options,
                ) {
                    tcpActions.disableTcpMode(tcpPort, options)
                }
            },
            onSuccess = {
                statusActions.refreshTcpModeEnabled()
                store.text(R.string.priv_ui_tcp_disabled)
            },
        )
    }

    fun restartTcpMode() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        val tcpPort = store.state.value.staticTcp.activePort ?: return
        val options = staticTcpControlOptions()
        runtimeActions.runBusy(
            message = store.text(R.string.priv_ui_tcp_restarting),
            failureKind = PrivilegeUiFailureKind.TCP_RESTART_FAILED,
            onFailure = { statusActions.refreshTcpModeEnabled() },
            action = {
                withTcpControlPrompt(
                    options = options,
                ) {
                    tcpActions.restartTcpMode(tcpPort, options)
                }
            },
            onSuccess = {
                statusActions.refreshTcpModeEnabled()
                store.text(R.string.priv_ui_tcp_restarted)
            },
        )
    }

    fun confirmStaticTcpSwitch() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) {
            staticTcpConfirmationController.cancel()
            return
        }
        if (
            store.state.value.runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED &&
            store.state.value.runtimeStartPhase == PrivilegeUiRuntimeStartPhase.IDLE
        ) {
            staticTcpConfirmationController.cancel()
            return
        }
        staticTcpConfirmationController.confirm()
    }

    fun cancelStaticTcpSwitch() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        staticTcpConfirmationController.cancel()
    }

    fun directStartAttempts(): List<PrivilegeUiRuntimeStartAttempt> {

        if (PrivilegeUiStartGate.isSilentStartInProgress) return emptyList()
        return buildList {
            if (store.config.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED) {
                add(
                    staticTcpAdbStartWorkflow(
                        confirmTcpSwitch = false,
                    ),
                )
            }
            if (isPrivilegeUiWirelessAdbSupported()) {
                add(wirelessAdbStartWorkflow())
            }
        }
    }

    fun refreshTcpModeEnabled() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return
        statusActions.refreshTcpModeEnabled()
    }

    suspend fun refreshTcpModeEnabledNow(markChecking: Boolean = true) =
        statusActions.forceTcpModeStatusRefreshForAction(markChecking)

    suspend fun pollTcpModeStatus() = statusActions.pollTcpModeStatus()

    suspend fun refreshWirelessAdbStatusNow(markChecking: Boolean = true) =
        statusActions.forceWirelessAdbStatusRefreshForAction(markChecking)

    suspend fun pollWirelessAdbStatus() = statusActions.pollWirelessAdbStatus()

    fun refreshAdbStartPrerequisites() {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return

        statusActions.refreshWifiConnected()
    }

    fun observeLocalNetworkPermission(onGranted: () -> Unit) {
        permissionObserver?.cancel()
        permissionObserver = coroutineScope.launch {
            var wasMissing = store.state.value.localNetworkPermissionMissing
            store.state.map { it.localNetworkPermissionMissing }.distinctUntilChanged().collect { missing ->
                if (wasMissing && !missing) onGranted()
                wasMissing = missing
            }
        }
    }

    override fun close() {
        permissionObserver?.cancel()
        permissionObserver = null
        staticTcpConfirmationController.cancel()
        pairingActions.close()
        statusActions.close()
    }

    private fun ensureWifiConnectedForWirelessAdbStart(
        session: PrivilegeUiRuntimeStartSession,
    ): Boolean {
        session.checkActive()
        val wifiConnected = store.isWifiConnected()
        if (wifiConnected) {
            store.updateState { it.copy(wifiConnected = true) }
            return true
        }
        val text = store.resourceText(R.string.priv_ui_wifi_required_for_wireless_adb_start)
        statusActions.resetWirelessPairingSession()
        store.updateState {
            it.withWirelessAdbOffline(
                wifiConnected = false,
                managedWirelessAdbStatus = it.managedWirelessAdbStatus,
                notificationPairingRunning = it.notificationPairingRunning,
            )
        }
        if (session.showAttemptFeedback) {
            store.showSnackbar(text)
        }
        session.appendStartupLog(store.resolveText(text))
        return false
    }

    private fun ensureWirelessAdbPairedForStart(
        session: PrivilegeUiRuntimeStartSession,
    ): Boolean {
        session.checkActive()
        if (!shouldRequireWirelessPairingForStart(store.state.value.wirelessPairingCheckStatus)) {
            return true
        }
        val text = store.resourceText(R.string.priv_ui_wireless_pair_required_for_wireless_adb_start)
        if (session.showAttemptFeedback) {
            store.showSnackbar(text)
        }
        session.appendStartupLog(store.resolveText(text))
        return false
    }

    private fun ensureWirelessDebuggingReadyForStart(
        session: PrivilegeUiRuntimeStartSession,
    ): Boolean {
        session.checkActive()
        val state = store.state.value
        val managedWirelessAdbStatus = managedWirelessAdbStatusForStart(state.managedWirelessAdbStatus)
        if (
            !shouldRequireWirelessDebuggingForStart(
                wirelessDebuggingStatus = state.wirelessDebuggingStatus,
                managedWirelessAdbStatus = managedWirelessAdbStatus,
            )
        ) {
            return true
        }
        val text = store.resourceText(R.string.priv_ui_wireless_debugging_required_for_wireless_adb_start)
        if (session.showAttemptFeedback) {
            store.showSnackbar(text)
        }
        session.appendStartupLog(store.resolveText(text))
        return false
    }

    private fun managedWirelessAdbStatusForStart(
        currentStatus: PrivilegeUiManagedWirelessAdbStatus,
    ): PrivilegeUiManagedWirelessAdbStatus {
        if (!isPrivilegeUiWirelessAdbSupported()) return currentStatus
        if (!store.config.enableManagedWirelessAdb || currentStatus == PrivilegeUiManagedWirelessAdbStatus.READY) {
            return currentStatus
        }
        val refreshedStatus = runCatching {
            Privilege.createAdbManager(
                adbDeviceName = store.currentAdbDeviceNameOverride(),
            ).getWirelessDebuggingControlStatus().toUiManagedWirelessAdbStatus()
        }.getOrNull() ?: return currentStatus
        if (refreshedStatus != currentStatus) {
            store.updateState {
                it.copy(managedWirelessAdbStatus = refreshedStatus)
            }
        }
        return refreshedStatus
    }

    private fun wirelessAdbStartWorkflow(): PrivilegeUiRuntimeStartAttempt.Workflow =
        PrivilegeUiRuntimeStartAttempt.Workflow(
            progressText = store.resourceText(R.string.priv_ui_wireless_adb_starting),
            startupSource = store.text(R.string.priv_ui_auth_method_adb),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ADB_WIRELESS,
            onFailure = { throwable ->
                handleWirelessAdbStartFailure(throwable)
            },
        ) {
            if (!prepareWirelessAdbCommand(this)) {
                PrivilegeUiRuntimeStartResult.Finished
            } else {
                val serverInfo = wirelessAdbStartAttempt().start(this)
                PrivilegeUiRuntimeStartResult.Connected(serverInfo)
            }
        }

    private fun staticTcpAdbStartWorkflow(
        confirmTcpSwitch: Boolean,
    ): PrivilegeUiRuntimeStartAttempt.Workflow =
        PrivilegeUiRuntimeStartAttempt.Workflow(
            progressText = store.resourceText(R.string.priv_ui_tcp_starting),
            startupSource = store.text(R.string.priv_ui_auth_method_adb),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP,
            onFailure = { throwable ->
                handleWirelessAdbStartFailure(throwable)
            },
        ) {
            if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) {
                return@Workflow PrivilegeUiRuntimeStartResult.Finished
            }
            val allowWirelessTcpSwitch = isPrivilegeUiWirelessAdbSupported()
            appendStartupLog(store.text(R.string.priv_ui_adb_static_check_action))
            if (!statusActions.forceTcpModeStatusRefreshForAction()) {
                return@Workflow PrivilegeUiRuntimeStartResult.Finished
            }
            checkActive()
            val tcpPort = store.currentConfiguredTcpPort()
            if (tcpPort == null) {
                if (!allowWirelessTcpSwitch) {
                    refreshTcpModeEnabled()
                    showStaticTcpUnavailable(this, R.string.priv_ui_adb_static_port_unavailable)
                    return@Workflow PrivilegeUiRuntimeStartResult.Finished
                }
                return@Workflow handleStaticTcpSwitchRequired(
                    session = this,
                    confirmTcpSwitch = confirmTcpSwitch,
                )
            }
            appendStartupLog(store.text(R.string.priv_ui_adb_static_preparing))
            val preparation = tcpActions.prepareStaticTcpForStart(tcpPort, this@Workflow)
            checkActive()
            continueStaticTcpStart(
                session = this,
                tcpPort = preparation.tcpPort,
                authorizationStatus = preparation.authorizationStatus,
                allowWirelessTcpSwitch = allowWirelessTcpSwitch,
                confirmTcpSwitch = confirmTcpSwitch,
            )
        }

    private suspend fun continueStaticTcpStart(
        session: PrivilegeUiRuntimeStartSession,
        tcpPort: Int?,
        authorizationStatus: PrivilegeUiAdbTcpAuthorizationStatus,
        allowWirelessTcpSwitch: Boolean,
        confirmTcpSwitch: Boolean,
    ): PrivilegeUiRuntimeStartResult {
        if (tcpPort == null) {
            if (allowWirelessTcpSwitch) {
                return handleStaticTcpSwitchRequired(session, confirmTcpSwitch)
            }
            showStaticTcpUnavailable(session, R.string.priv_ui_adb_static_port_unavailable)
            return PrivilegeUiRuntimeStartResult.Finished
        }
        return when (authorizationStatus) {
            PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZED -> {
                val serverInfo = tcpActions.tcpAdbStartAttempt(tcpPort).start(session)
                PrivilegeUiRuntimeStartResult.Connected(serverInfo)
            }
            PrivilegeUiAdbTcpAuthorizationStatus.UNAUTHORIZED,
            PrivilegeUiAdbTcpAuthorizationStatus.FAILED,
            PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN,
            PrivilegeUiAdbTcpAuthorizationStatus.CHECKING,
            PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZING,
            -> if (
                !shouldRequestStaticTcpAuthorizationForStart(
                    authorizationStatus = authorizationStatus,
                    showAttemptFeedback = session.showAttemptFeedback,
                ) || !tcpActions.requestTcpAuthorizationForStart(session, tcpPort)
            ) {
                PrivilegeUiRuntimeStartResult.Finished
            } else {
                val serverInfo = tcpActions.tcpAdbStartAttempt(tcpPort).start(session)
                PrivilegeUiRuntimeStartResult.Connected(serverInfo)
            }
            PrivilegeUiAdbTcpAuthorizationStatus.UNAVAILABLE -> {
                if (allowWirelessTcpSwitch) {
                    handleStaticTcpSwitchRequired(session, confirmTcpSwitch)
                } else {
                    showStaticTcpUnavailable(session, R.string.priv_ui_adb_static_service_stopped)
                    PrivilegeUiRuntimeStartResult.Finished
                }
            }
        }
    }

    private suspend fun handleStaticTcpSwitchRequired(
        session: PrivilegeUiRuntimeStartSession,
        confirmTcpSwitch: Boolean,
    ): PrivilegeUiRuntimeStartResult {
        if (!confirmTcpSwitch) return PrivilegeUiRuntimeStartResult.Finished
        if (
            !staticTcpConfirmationController.awaitConfirmation(
                PrivilegeUiStaticTcpSwitchAction.START_SERVICE,
            )
        ) {
            return PrivilegeUiRuntimeStartResult.Finished
        }
        session.checkActive()
        return startStaticTcpAdbThroughWireless(session)
    }

    private suspend fun startStaticTcpAdbThroughWireless(
        session: PrivilegeUiRuntimeStartSession,
    ): PrivilegeUiRuntimeStartResult {
        if (!prepareWirelessAdbCommand(session)) {
            return PrivilegeUiRuntimeStartResult.Finished
        }
        val serverInfo = staticTcpAdbThroughWirelessStartAttempt().start(session)
        return PrivilegeUiRuntimeStartResult.Connected(serverInfo)
    }

    private fun wirelessAdbStartAttempt(): PrivilegeUiRuntimeStartAttempt.Connect {
        return PrivilegeUiRuntimeStartAttempt.Connect(
            progressText = store.resourceText(R.string.priv_ui_wireless_adb_starting),
            startupSource = store.text(R.string.priv_ui_auth_method_adb),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ADB_WIRELESS,
            onFailure = { throwable ->
                handleWirelessAdbStartFailure(throwable)
            },
        ) {
            val adbDeviceName = store.currentAdbDeviceNameOverride()
            val activeTcpPort = activeTcpPortForWirelessAdbStart(adbDeviceName)
            val options = privilegeUiWirelessAdbStartOptions(
                tcpPolicy = store.config.adbTcpPolicy,
                activeTcpPort = activeTcpPort,
                managedWirelessAdbEnabled = store.managedWirelessAdbEnabledForStart(),
                managedWirelessAdbStatus = store.state.value.managedWirelessAdbStatus,
            )
            val serverInfo = withManagedWirelessDebuggingPrompt(options) {
                PrivilegeRuntimeStartCoordinator.startAdb(
                    launch = requireRuntimeClientLaunch(),
                    options = options,
                    timeoutMillis = store.config.startTimeoutMillis,
                    adbDeviceName = adbDeviceName,
                    startupLogListener = startupLogListener,
                )
            }
            updateTcpModeAfterWirelessAdbStart(
                activeTcpPort = activeTcpPort,
            )
            serverInfo
        }
    }

    private fun staticTcpAdbThroughWirelessStartAttempt(): PrivilegeUiRuntimeStartAttempt.Connect {
        return PrivilegeUiRuntimeStartAttempt.Connect(
            progressText = store.resourceText(R.string.priv_ui_wireless_adb_starting),
            startupSource = store.text(R.string.priv_ui_auth_method_adb),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP,
        ) {
            val tcpPort = store.config.tcpPort
            val manager = Privilege.createAdbManager(
                adbDeviceName = store.currentAdbDeviceNameOverride(),
            )
            appendStartupLog(store.text(R.string.priv_ui_tcp_enabling))
            val options = privilegeUiStaticTcpSwitchOptions(
                managedWirelessAdbEnabled = store.managedWirelessAdbEnabledForStart(),
                managedWirelessAdbStatus = store.state.value.managedWirelessAdbStatus,
            )
            withManagedWirelessDebuggingPrompt(options) {
                manager.switchToTcp(
                    tcpPort = tcpPort,
                    options = options,
                )
            }
            delay(PRIVILEGE_UI_TCP_START_DELAY_MILLIS.milliseconds)
            val activeTcpPort = tcpActions.requireStaticTcpReady(manager, this)
            appendStartupLog(store.text(R.string.priv_ui_tcp_enabled))
            appendStartupLog(store.text(R.string.priv_ui_tcp_starting))
            tcpActions.startTcpAdbNow(activeTcpPort, this)
        }
    }

    private fun handleWirelessAdbStartFailure(
        throwable: Throwable,
    ): PrivilegeUiRuntimeStartFailureDisposition? {
        if (generateSequence(throwable) { it.cause }.any {
                it is SecurityException && it.message.orEmpty().contains("android.permission.MANAGE_DEBUGGING")
            }
        ) {
            val text = store.resourceText(R.string.priv_ui_wireless_debugging_control_restricted)
            return PrivilegeUiRuntimeStartFailureDisposition(
                stateTransform = { current ->
                    current.copy(managedWirelessAdbStatus = PrivilegeUiManagedWirelessAdbStatus.FAILED)
                },
                snackbarText = text,
                startupLogLines = listOf(store.resolveText(text), throwable.toPrivilegeUiDiagnosticString()),
            )
        }
        if (!throwable.isAdbKeyNotAuthorizedFailure()) return null
        val text = store.resourceText(R.string.priv_ui_wireless_pair_required_for_wireless_adb_start)
        val message = store.resolveText(text)
        val wirelessDebuggingStatus = currentWirelessDebuggingStatus()
        val notificationPairingRunning = PrivilegeAdbPairingService.isRunning(
            store.notificationPairingOwnerId,
        )
        return PrivilegeUiRuntimeStartFailureDisposition(
            stateTransform = { current ->
                current.copy(
                    wirelessDebuggingStatus = wirelessDebuggingStatus,
                    wirelessPairingServiceStatus = if (wirelessDebuggingStatus == PrivilegeUiWirelessAdbStatus.ON) {
                        current.wirelessPairingServiceStatus
                    } else {
                        PrivilegeUiWirelessAdbStatus.OFF
                    },
                    wirelessPairingCheckStatus = PrivilegeUiWirelessAdbStatus.OFF,
                    notificationPairingRunning = notificationPairingRunning,
                )
            },
            snackbarText = text,
            startupLogLines = listOf(
                message,
                throwable.toPrivilegeUiDiagnosticString(),
            ),
            afterCommit = {
                statusActions.resetWirelessPairingSession()
            },
        )
    }

    private suspend fun <T> withManagedWirelessDebuggingPrompt(
        options: PrivilegeAdbConnectionOptions,
        action: suspend () -> T,
    ): T = if (
        options.port == null &&
        options.wirelessDebuggingControl != PrivilegeAdbWirelessDebuggingControl.NEVER
    ) {
        systemPromptCoordinator.withPrompt(
            prompt = privilegeUiWirelessDebuggingPrompt(),
            action = action,
        )
    } else {
        action()
    }

    private fun staticTcpControlOptions(): PrivilegeAdbConnectionOptions? =
        if (isPrivilegeUiWirelessAdbSupported()) {
            privilegeUiStaticTcpSwitchOptions(
                managedWirelessAdbEnabled = store.managedWirelessAdbEnabledForStart(),
                managedWirelessAdbStatus = store.state.value.managedWirelessAdbStatus,
            )
        } else {
            null
        }

    private suspend fun <T> withTcpControlPrompt(
        options: PrivilegeAdbConnectionOptions?,
        action: suspend () -> T,
    ): T = if (options == null) action() else withManagedWirelessDebuggingPrompt(options, action)

    private fun currentWirelessDebuggingStatus(): PrivilegeUiWirelessAdbStatus {
        if (!isPrivilegeUiWirelessAdbSupported()) return PrivilegeUiWirelessAdbStatus.OFF
        return privilegeUiWirelessDebuggingStatus(
            Privilege.createAdbManager(
                adbDeviceName = store.currentAdbDeviceNameOverride(),
            ).getWirelessDebuggingControlStatus().wirelessDebuggingEnabled,
        )
    }

    private fun activeTcpPortForWirelessAdbStart(adbDeviceName: String?): Int? {
        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return null
        return runCatching {
            Privilege.createAdbManager(adbDeviceName = adbDeviceName).getActiveTcpPort()
        }.getOrNull()
    }

    private fun updateTcpModeAfterWirelessAdbStart(activeTcpPort: Int?) {
        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        store.updateStaticTcp {
            it.copy(
                activePort = activeTcpPort,
                configuredPort = activeTcpPort ?: it.configuredPort,
                authorizationStatus = if (activeTcpPort != null) {
                    PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZED
                } else {
                    PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN
                },
                loaded = true,
            )
        }
    }

    private fun showStaticTcpUnavailable(
        session: PrivilegeUiRuntimeStartSession,
        messageRes: Int,
    ) {
        val text = store.resourceText(messageRes)
        if (session.showAttemptFeedback) {
            store.showSnackbar(text)
        }
        session.appendStartupLog(store.resolveText(text))
    }

}

private const val PRIVILEGE_UI_TCP_START_DELAY_MILLIS = 1_000L

internal fun shouldRequestStaticTcpAuthorizationForStart(
    authorizationStatus: PrivilegeUiAdbTcpAuthorizationStatus,
    showAttemptFeedback: Boolean,
): Boolean =
    showAttemptFeedback && authorizationStatus in setOf(
        PrivilegeUiAdbTcpAuthorizationStatus.UNAUTHORIZED,
        PrivilegeUiAdbTcpAuthorizationStatus.FAILED,
        PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN,
        PrivilegeUiAdbTcpAuthorizationStatus.CHECKING,
        PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZING,
    )
