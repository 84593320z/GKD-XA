package priv.kit.ui.adb

import priv.kit.core.Privilege
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.adb.PrivilegeAdbAuthorizationEndReason
import priv.kit.core.adb.PrivilegeAdbAuthorizationRequestResult
import priv.kit.core.adb.PrivilegeAdbAuthorizationStatus
import priv.kit.core.adb.PrivilegeAdbConnectionOptions
import priv.kit.core.adb.PrivilegeAdbManager
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import priv.kit.ui.PrivilegeUiAdbTcpAuthorizationStatus
import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiStaticTcpState
import priv.kit.ui.PrivilegeUiSystemPromptCoordinator
import priv.kit.ui.R
import priv.kit.ui.privilegeUiTcpAuthorizationPrompt
import priv.kit.ui.runtime.PrivilegeUiRuntimeStartAttempt
import priv.kit.ui.runtime.PrivilegeUiRuntimeStartSession
import priv.kit.ui.state.PrivilegeUiFailureKind
import priv.kit.ui.state.PrivilegeUiViewModelStore

internal class PrivilegeUiAdbTcpActions(
    private val store: PrivilegeUiViewModelStore,
    private val systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator,
) {
    suspend fun enableTcpMode() {

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        val tcpPort = store.config.tcpPort
        val result = adbManager().switchToTcp(tcpPort = tcpPort)
        store.updateStaticTcp {
            it.copy(
                activePort = result.port,
                configuredPort = result.port,
                loaded = true,
            )
        }
    }

    suspend fun disableTcpMode(
        tcpPort: Int,
        options: PrivilegeAdbConnectionOptions?,
    ) {

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        adbManager().stopTcp(tcpPort, options)
        store.updateStaticTcp {
            PrivilegeUiStaticTcpState(loaded = true)
        }
    }

    suspend fun restartTcpMode(
        tcpPort: Int,
        options: PrivilegeAdbConnectionOptions?,
    ) {

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return
        val restartedPort = adbManager().restartTcp(tcpPort, options).port
        store.updateStaticTcp {
            it.copy(
                activePort = restartedPort,
                configuredPort = restartedPort,
                authorizationStatus = PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN,
                loaded = true,
            )
        }
    }

    suspend fun requestTcpAuthorizationForStart(
        session: PrivilegeUiRuntimeStartSession,
        tcpPort: Int,
    ): Boolean {

        if (store.config.adbTcpPolicy == PrivilegeUiAdbTcpPolicy.DISABLED) return false
        session.appendStartupLog(store.text(R.string.priv_ui_adb_static_authorize_action))
        session.appendStartupLog(store.text(R.string.priv_ui_tcp_authorization_prompt_waiting_help))
        store.updateState { current ->
            current.copy(
                staticTcp = current.staticTcp.copy(
                    authorizationStatus = PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZING,
                ),
                tcpAuthorizationFailureDialogVisible = false,
            )
        }
        val result = try {
            systemPromptCoordinator.withPrompt(privilegeUiTcpAuthorizationPrompt()) {
                adbManager().requestTcpAuthorization(
                    tcpPort = tcpPort,
                    timeoutMillis = store.config.adbAuthorizationTimeoutMillis,
                )
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            markTcpAuthorizationUnauthorizedIfAuthorizing()
            throw exception
        }
        session.checkActive()
        return applyTcpAuthorizationResult(
            session = session,
            result = result,
            showFailure = session.showAttemptFeedback && !session.cancellationRequested,
        )
    }

    fun tcpAdbStartAttempt(tcpPort: Int): PrivilegeUiRuntimeStartAttempt.Connect {
        return PrivilegeUiRuntimeStartAttempt.Connect(
            progressText = store.resourceText(R.string.priv_ui_tcp_starting),
            startupSource = store.text(R.string.priv_ui_auth_method_adb),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP,
        ) {
            startTcpAdbNow(tcpPort, this)
        }
    }

    suspend fun prepareStaticTcpForStart(
        tcpPort: Int,
        session: PrivilegeUiRuntimeStartSession,
    ): PrivilegeUiStaticTcpPreparationResult {

        val manager = adbManager()
        val initialAuthorization = manager.prepareTcpForStart(tcpPort = tcpPort)
        val configuredTcpPort = manager.getConfiguredTcpPort()
        val activeTcpPort = manager.getActiveTcpPort()
        val authorization = when {
            configuredTcpPort == null -> null
            configuredTcpPort == tcpPort -> initialAuthorization
            else -> manager.prepareTcpForStart(tcpPort = configuredTcpPort)
        }
        val authorizationStatus = authorization
            ?.status
            ?.toUiTcpAuthorizationStatus()
            ?: PrivilegeUiAdbTcpAuthorizationStatus.UNKNOWN
        store.updateStaticTcp {
            PrivilegeUiStaticTcpState(
                activePort = activeTcpPort,
                configuredPort = configuredTcpPort,
                authorizationStatus = authorizationStatus,
                loaded = true,
            )
        }
        authorization
            ?.failureMessage
            ?.takeIf { it.startsWith("Failed to prepare ADB for static TCP") }
            ?.let(session::appendStartupLog)
        return PrivilegeUiStaticTcpPreparationResult(
            tcpPort = configuredTcpPort,
            authorizationStatus = authorizationStatus,
        )
    }

    suspend fun requireStaticTcpReady(
        manager: PrivilegeAdbManager,
        session: PrivilegeUiRuntimeStartSession,
    ): Int {

        val configuredTcpPort = manager.getConfiguredTcpPort()
        if (configuredTcpPort == null) {
            store.updateStaticTcp {
                PrivilegeUiStaticTcpState(loaded = true)
            }
            throwStaticTcpStartFailed(session)
        }
        val authorization = manager.prepareTcpForStart(tcpPort = configuredTcpPort)
        val activeTcpPort = manager.getActiveTcpPort()
        store.updateStaticTcp {
            PrivilegeUiStaticTcpState(
                activePort = activeTcpPort,
                configuredPort = configuredTcpPort,
                authorizationStatus = authorization.status.toUiTcpAuthorizationStatus(),
                loaded = true,
            )
        }
        if (
            activeTcpPort != null &&
            authorization.status == PrivilegeAdbAuthorizationStatus.AUTHORIZED
        ) {
            return activeTcpPort
        }
        authorization.failureMessage
            ?.takeIf { it.isNotBlank() }
            ?.let(session::appendStartupLog)
        throwStaticTcpStartFailed(session)
    }

    suspend fun startTcpAdbNow(
        tcpPort: Int,
        session: PrivilegeUiRuntimeStartSession,
    ): PrivilegeServerInfo {

        val serverInfo = PrivilegeRuntimeStartCoordinator.startAdb(
            launch = session.requireRuntimeClientLaunch(),
            options = PrivilegeAdbConnectionOptions(
                port = tcpPort,
            ),
            timeoutMillis = store.config.startTimeoutMillis,
            adbDeviceName = store.currentAdbDeviceNameOverride(),
            startupLogListener = session.startupLogListener,
        )
        store.updateStaticTcp {
            it.copy(
                activePort = tcpPort,
                configuredPort = tcpPort,
                loaded = true,
            )
        }
        return serverInfo
    }

    private fun throwStaticTcpStartFailed(session: PrivilegeUiRuntimeStartSession): Nothing {
        val message = store.text(R.string.priv_ui_adb_static_start_failed)
        session.appendStartupLog(message)
        throw IllegalStateException(message)
    }

    private fun applyTcpAuthorizationResult(
        session: PrivilegeUiRuntimeStartSession,
        result: PrivilegeAdbAuthorizationRequestResult,
        showFailure: Boolean,
    ): Boolean {
        val status = if (result.authorized) {
            PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZED
        } else {
            result.endReason.toUiTcpAuthorizationStatus()
        }
        store.updateStaticTcp { current ->
            current.copy(authorizationStatus = status)
        }
        if (result.authorized) {
            session.appendStartupLog(store.text(R.string.priv_ui_tcp_authorization_allowed))
        } else {
            result.failureMessage
                ?.takeIf { it.isNotBlank() }
                ?.let(session::appendStartupLog)
            if (showFailure) {
                if (result.endReason == PrivilegeAdbAuthorizationEndReason.AUTOMATIC_TIMEOUT) {
                    store.updateState { current ->
                        current.copy(tcpAuthorizationFailureDialogVisible = true)
                    }
                    session.appendStartupLog(
                        store.text(R.string.priv_ui_tcp_authorization_timeout_message),
                    )
                } else {
                    store.showFailure(PrivilegeUiFailureKind.TCP_AUTHORIZATION_FAILED)
                    session.appendStartupLog(
                        store.text(PrivilegeUiFailureKind.TCP_AUTHORIZATION_FAILED.messageResId),
                    )
                }
            }
        }
        return result.authorized
    }

    private fun markTcpAuthorizationUnauthorizedIfAuthorizing() {
        store.updateStaticTcp { current ->
            if (current.authorizationStatus == PrivilegeUiAdbTcpAuthorizationStatus.AUTHORIZING) {
                current.copy(authorizationStatus = PrivilegeUiAdbTcpAuthorizationStatus.UNAUTHORIZED)
            } else {
                current
            }
        }
    }

    private fun adbManager(): PrivilegeAdbManager = Privilege.createAdbManager(
        adbDeviceName = store.currentAdbDeviceNameOverride(),
    )
}

internal data class PrivilegeUiStaticTcpPreparationResult(
    val tcpPort: Int?,
    val authorizationStatus: PrivilegeUiAdbTcpAuthorizationStatus,
)

private fun PrivilegeAdbAuthorizationEndReason?.toUiTcpAuthorizationStatus(): PrivilegeUiAdbTcpAuthorizationStatus =
    when (this) {
        PrivilegeAdbAuthorizationEndReason.FAILED -> PrivilegeUiAdbTcpAuthorizationStatus.FAILED
        PrivilegeAdbAuthorizationEndReason.AUTOMATIC_TIMEOUT,
        null,
        -> PrivilegeUiAdbTcpAuthorizationStatus.UNAUTHORIZED
    }
