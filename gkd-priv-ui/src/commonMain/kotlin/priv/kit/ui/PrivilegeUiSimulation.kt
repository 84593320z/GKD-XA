package priv.kit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import priv.kit.ui.adb.PrivilegeUiStaticTcpSwitchAction
import priv.kit.ui.adb.pairing.isPrivilegeUiPairingCode
import kotlin.io.encoding.Base64
import kotlin.random.Random

/** In-memory preview data. No Core, process, network, permission, or settings access. */
internal class PrivilegeUiSimulation(
    private val scope: CoroutineScope,
    externalLabel: String,
    private val startingText: Map<PrivilegeUiRuntimeStartSource, String>,
    private val pairingText: String,
    private val requestBatteryOptimization: () -> Unit = {},
    private val requestLocalNetworkPermission: () -> Unit = {},
    private val copyText: (String) -> Unit,
) {
    private val installationDirectory = "/data/app/~~${randomInstallToken()}/priv.kit.sample-${randomInstallToken()}"

    private fun manualCommand(useLegacyPackaging: Boolean): String = if (useLegacyPackaging) {
        "adb shell $installationDirectory/lib/arm64/libprivkitstarter.so"
    } else {
        "adb shell /system/bin/linker64 '$installationDirectory/base.apk!/lib/arm64-v8a/libprivkitstarter.so'"
    }

    private fun randomInstallToken(): String = Base64.UrlSafe.encode(Random.nextBytes(16))

    var state by mutableStateOf(
        PrivilegeUiScreenState(
            startupModes = PrivilegeUiStartupMode.entries.toList(),
            wirelessAdbStatusLoaded = true,
            staticTcp = PrivilegeUiStaticTcpState(loaded = true),
            wifiConnected = true,
            wirelessDebuggingStatus = PrivilegeUiWirelessAdbStatus.ON,
            wirelessPairingServiceStatus = PrivilegeUiWirelessAdbStatus.ON,
            wirelessPairingCheckStatus = PrivilegeUiWirelessAdbStatus.OFF,
            managedWirelessAdbStatus = PrivilegeUiManagedWirelessAdbStatus.READY,
            adbKeyFingerprint = "D3:50:9A:42:70:1B:8C:EE:41:20:7F:68:34:AD:09:55",
            manualShellCommandLine = manualCommand(useLegacyPackaging = true),
            permissionRestrictionStatus = PrivilegeUiPermissionRestrictionStatus.NOT_RESTRICTED,
            externalStartItems = listOf(PrivilegeUiExternalStartItemState(
                id = "simulation",
                label = externalLabel,
                snapshot = PrivilegeUiExternalStartSnapshot(available = true, uid = 2000, version = 1),
                statusLoaded = true,
            )),
        ),
    )
        private set

    var externalAuthorizationRequested by mutableStateOf(false)
        private set

    private var operation: Job? = null
    private var pendingStart: PrivilegeUiServerRestartRequest? = null
    private var adbRestricted = true

    fun setAdbRestricted(value: Boolean) {
        adbRestricted = value
        if (state.serverUid == 2000) {
            state = state.copy(
                permissionRestrictionStatus = if (value) PrivilegeUiPermissionRestrictionStatus.RESTRICTED
                    else PrivilegeUiPermissionRestrictionStatus.NOT_RESTRICTED,
                deniedServerPermissions = if (value) simulatedDeniedPermissions else emptyList(),
            )
        }
    }

    private val simulatedDeniedPermissions = listOf(
        "android.permission.GRANT_RUNTIME_PERMISSIONS",
        "android.permission.INJECT_EVENTS",
        "android.permission.WRITE_SECURE_SETTINGS",
    )

    fun setUseLegacyPackaging(value: Boolean) {
        state = state.copy(manualShellCommandLine = manualCommand(value))
    }

    fun setPermissions(batteryExempt: Boolean, networkGranted: Boolean) {
        state = state.copy(batteryOptimizationPromptVisible = !batteryExempt,
            localNetworkPermissionMissing = !networkGranted)
    }

    val actions = PrivilegeUiActions(
        continuePairingWithoutNotification = { state = state.copy(pairingNotificationPermissionWarningVisible = false) },
        dismissTcpAuthorizationFailureDialog = { state = state.copy(tcpAuthorizationFailureDialogVisible = false) },
        // The preview has no system notification settings; dismiss its simulated warning.
        openNotificationSettings = { state = state.copy(pairingNotificationPermissionWarningVisible = false) },
        requestBatteryOptimization = { requestBatteryOptimization(); true },
        requestLocalNetworkPermission = requestLocalNetworkPermission,
        // Cancellation remains available while an operation is busy.
        canInteract = { true },
        selectStartupMode = { if (!state.busy) state = state.copy(selectedStartupMode = it) },
        startRoot = { requestStart(PrivilegeUiServerRestartRequest.Root) },
        startWirelessAdb = { requestStart(PrivilegeUiServerRestartRequest.WirelessAdb) },
        startStaticTcpAdb = { requestStart(PrivilegeUiServerRestartRequest.StaticTcpAdb) },
        authorizeOrStartExternal = { id ->
            if (state.externalStartItems.any { it.id == id }) {
                requestStart(PrivilegeUiServerRestartRequest.External(id))
            }
        },
        startInteractive = ::startSelected,
        confirmServerRestart = {
            state.restartConfirmationTarget?.let { request ->
                state = state.copy(restartConfirmationTarget = null, runtimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED, serverUid = null, deniedServerPermissions = emptyList())
                prepareStart(request)
            }
        },
        cancelServerRestart = { state = state.copy(restartConfirmationTarget = null) },
        stopCurrentStart = ::cancelOperation,
        stopServer = ::stopServer,
        disableAutoRecovery = { state = state.copy(desiredEnabled = false) },
        startNotificationPairing = { if (!state.busy) openPairing() },
        stopNotificationPairing = ::cancelOperation,
        cancelPendingPairingStart = ::cancelOperation,
        updatePairingCode = { code ->
            if (state.pairingStatus != PrivilegeUiAdbPairingStatus.PAIRING) {
                state = state.copy(pairingCode = code.filter { it in '0'..'9' }.take(6))
            }
        },
        submitNotificationPairingCode = ::submitPairingCode,
        confirmStaticTcpSwitch = ::confirmTcpSwitch,
        cancelStaticTcpSwitch = ::cancelOperation,
        enableTcpMode = ::requestTcpSwitch,
        restartTcpMode = ::requestTcpSwitch,
        disableTcpMode = {
            if (!state.busy) {
                disconnectAdbRuntime()
                state = state.copy(staticTcp = PrivilegeUiStaticTcpState(loaded = true))
                log("Static TCP port stopped")
            }
        },
        copyManualCommand = { state.manualShellCommandLine?.let(copyText) },
        copyStaticTcpCommand = { copyText("# Simulation only\nadb tcpip ${state.tcpPort}") },
        copyStartupLog = { copyText(state.startupLogLines.joinToString("\n")) },
        clearStartupLog = { state = state.copy(startupLogLines = emptyList()) },
    )

    private fun startSelected() {
        when (state.selectedStartupMode) {
            PrivilegeUiStartupMode.ROOT -> requestStart(PrivilegeUiServerRestartRequest.Root)
            PrivilegeUiStartupMode.ADB -> requestStart(PrivilegeUiServerRestartRequest.WirelessAdb)
            PrivilegeUiStartupMode.EXTERNAL -> requestStart(PrivilegeUiServerRestartRequest.External("simulation"))
            PrivilegeUiStartupMode.MANUAL_SHELL -> if (!state.busy && state.runtimeStatus != PrivilegeUiRuntimeStatus.CONNECTED) beginStart(null)
        }
    }

    private fun requestStart(request: PrivilegeUiServerRestartRequest) {
        if (state.busy || state.restartConfirmationTarget != null) return
        if (state.runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED) {
            state = state.copy(restartConfirmationTarget = request)
        } else {
            prepareStart(request)
        }
    }

    private fun prepareStart(request: PrivilegeUiServerRestartRequest) {
        when {
            request == PrivilegeUiServerRestartRequest.WirelessAdb && state.wirelessPairingCheckStatus != PrivilegeUiWirelessAdbStatus.ON -> {
                pendingStart = request
                openPairing()
            }
            request == PrivilegeUiServerRestartRequest.StaticTcpAdb && state.staticTcp.activePort == null -> {
                pendingStart = request
                state = state.copy(busy = true, staticTcpSwitchConfirmation = PrivilegeUiStaticTcpSwitchAction.START_SERVICE)
            }
            request is PrivilegeUiServerRestartRequest.External && !state.externalStartItems.first { it.id == request.providerId }.snapshot.authorized -> {
                pendingStart = request
                state = state.copy(busy = true)
                externalAuthorizationRequested = true
            }
            else -> beginStart(request)
        }
    }

    private fun beginStart(request: PrivilegeUiServerRestartRequest?) {
        val source = when (request) {
            PrivilegeUiServerRestartRequest.Root -> PrivilegeUiRuntimeStartSource.ROOT
            PrivilegeUiServerRestartRequest.Adb, PrivilegeUiServerRestartRequest.WirelessAdb -> PrivilegeUiRuntimeStartSource.ADB_WIRELESS
            PrivilegeUiServerRestartRequest.StaticTcpAdb -> PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP
            is PrivilegeUiServerRestartRequest.External -> PrivilegeUiRuntimeStartSource.EXTERNAL
            null -> null // A manual command has no automatic recovery method.
        }
        pendingStart = null
        state = state.copy(busy = true, runtimeStatus = PrivilegeUiRuntimeStatus.STARTING,
            runtimeStartSource = source, runtimeStartProviderId = (request as? PrivilegeUiServerRestartRequest.External)?.providerId,
            runtimeStartPhase = PrivilegeUiRuntimeStartPhase.RUNNING, runtimeProgressText = startingText[source], serverUid = null, deniedServerPermissions = emptyList())
        log("Starting ${source?.name ?: "MANUAL_SHELL"}")
        operation = scope.launch {
            delay(1_200)
            log("Connected; uid=${if (source == PrivilegeUiRuntimeStartSource.ROOT) 0 else 2000}")
            state = state.copy(busy = false, runtimeStatus = PrivilegeUiRuntimeStatus.CONNECTED,
                runtimeStartPhase = PrivilegeUiRuntimeStartPhase.IDLE, runtimeProgressText = null,
                serverUid = if (source == PrivilegeUiRuntimeStartSource.ROOT) 0 else 2000,
                connectionSerial = state.connectionSerial + 1,
                permissionRestrictionStatus = if (source == PrivilegeUiRuntimeStartSource.ROOT || !adbRestricted)
                    PrivilegeUiPermissionRestrictionStatus.NOT_RESTRICTED else PrivilegeUiPermissionRestrictionStatus.RESTRICTED,
                deniedServerPermissions = if (source == PrivilegeUiRuntimeStartSource.ROOT || !adbRestricted)
                    emptyList() else simulatedDeniedPermissions,
                desiredEnabled = request != null)
        }
    }

    private fun openPairing() {
        state = state.copy(busy = true, pairingDialogVisible = true, pairingCode = "",
            pairingStatus = PrivilegeUiAdbPairingStatus.FOUND, pairingText = pairingText)
    }

    private fun submitPairingCode() {
        if (!state.pairingDialogVisible || state.pairingStatus != PrivilegeUiAdbPairingStatus.FOUND || !state.pairingCode.isPrivilegeUiPairingCode()) return
        state = state.copy(pairingStatus = PrivilegeUiAdbPairingStatus.PAIRING)
        operation = scope.launch {
            delay(700)
            val next = pendingStart
            pendingStart = null
            state = state.copy(busy = false, pairingDialogVisible = false, pairingCode = "", pairingText = null,
                pairingStatus = PrivilegeUiAdbPairingStatus.PAIRED, wirelessPairingCheckStatus = PrivilegeUiWirelessAdbStatus.ON)
            log("Wireless ADB paired")
            if (next != null) beginStart(next)
        }
    }

    private fun requestTcpSwitch() {
        if (state.busy) return
        state = state.copy(busy = true, staticTcpSwitchConfirmation = PrivilegeUiStaticTcpSwitchAction.ENABLE_PORT)
    }

    private fun confirmTcpSwitch() {
        if (state.staticTcpSwitchConfirmation == null) return
        state = state.copy(staticTcpSwitchConfirmation = null)
        disconnectAdbRuntime()
        operation = scope.launch {
            delay(700)
            val next = pendingStart
            pendingStart = null
            state = state.copy(busy = false, staticTcp = PrivilegeUiStaticTcpState(
                loaded = true, activePort = state.tcpPort, configuredPort = state.tcpPort,
                authorizationStatus = PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZED))
            log("Static TCP port ready: ${state.tcpPort}")
            if (next != null) beginStart(next)
        }
    }

    fun confirmExternalAuthorization() {
        if (!externalAuthorizationRequested) return
        val request = pendingStart ?: return
        externalAuthorizationRequested = false
        state = state.copy(externalStartItems = state.externalStartItems.map { it.copy(snapshot = it.snapshot.copy(authorized = true)) })
        beginStart(request)
    }

    fun cancelOperation() {
        val wasPending = state.busy || state.restartConfirmationTarget != null
        operation?.cancel()
        operation = null
        pendingStart = null
        externalAuthorizationRequested = false
        state = state.copy(busy = false, pairingDialogVisible = false, pairingCode = "", pairingText = null,
            pairingStatus = if (state.wirelessPairingCheckStatus == PrivilegeUiWirelessAdbStatus.ON) PrivilegeUiAdbPairingStatus.PAIRED else PrivilegeUiAdbPairingStatus.NOT_PAIRED,
            staticTcpSwitchConfirmation = null, restartConfirmationTarget = null,
            runtimeStatus = if (state.runtimeStatus == PrivilegeUiRuntimeStatus.STARTING) PrivilegeUiRuntimeStatus.DISCONNECTED else state.runtimeStatus,
            runtimeStartPhase = PrivilegeUiRuntimeStartPhase.IDLE, runtimeProgressText = null)
        if (wasPending) log("Operation cancelled")
    }

    private fun stopServer() {
        cancelOperation()
        state = state.copy(runtimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED, serverUid = null, deniedServerPermissions = emptyList(), desiredEnabled = false,
            runtimeStartSource = null, runtimeStartProviderId = null)
        log("Server stopped")
    }

    private fun disconnectAdbRuntime() {
        if (state.serverUid == 2000 && state.runtimeStatus == PrivilegeUiRuntimeStatus.CONNECTED) {
            state = state.copy(runtimeStatus = PrivilegeUiRuntimeStatus.DISCONNECTED, serverUid = null, deniedServerPermissions = emptyList())
        }
    }

    private fun log(message: String) {
        state = state.copy(startupLogLines = (state.startupLogLines + "[simulation] $message").takeLast(80))
    }
}
