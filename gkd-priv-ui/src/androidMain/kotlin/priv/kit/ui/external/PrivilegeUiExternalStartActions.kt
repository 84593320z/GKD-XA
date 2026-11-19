package priv.kit.ui.external

import priv.kit.ui.*
import priv.kit.ui.runtime.*
import priv.kit.ui.state.*

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import priv.kit.core.internal.runtime.PrivilegeRuntimeClientLaunch
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import kotlin.time.Duration.Companion.milliseconds

internal class PrivilegeUiExternalStartActions(
    private val store: PrivilegeUiViewModelStore,
    private val runtimeActions: PrivilegeUiRuntimeActions,
    private val createNativeStarterCommand: suspend (PrivilegeRuntimeClientLaunch) -> String =
        PrivilegeRuntimeStartCoordinator::createNativeStarterCommand,
    private val acquireInteractivePermit: () -> AutoCloseable? =
        PrivilegeUiStartGate.newInteractivePermitAcquirer(),
    private val systemPromptCoordinator: PrivilegeUiSystemPromptCoordinator =
        PrivilegeUiSystemPromptCoordinator(),
) {
    private val statusRefreshes = store.config.externalStartProviders.associate {
        it.id to PrivilegeUiRefreshTask()
    }

    suspend fun pollExternalStartStatus() {
        if (store.config.externalStartProviders.isEmpty()) return
        while (currentCoroutineContext().isActive) {
            delay(store.config.externalStartStatusPollIntervalMillis.milliseconds)
            refreshExternalStartStatusNow(providerId = null)
        }
    }

    suspend fun refreshExternalStartStatusNow(providerId: String?): Boolean {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return false
        val context = store.applicationContext ?: return false
        store.config.externalStartProviders.filter { providerId == null || it.id == providerId }
            .forEach { provider ->
                statusRefreshes.getValue(provider.id).run {
                    val snapshot = provider.snapshotOrFailure(context)
                    store.setExternalStartSnapshot(provider.id, snapshot)
                    if (snapshot.exceptionText.isNotBlank()) store.appendStartupLog(snapshot.exceptionText)
                }
            }
        return true
    }

    suspend fun authorizeOrStartExternal(
        providerId: String,
        replaceConnectedServer: Boolean = false,
    ) {
        if (store.state.value.busy) return
        val interactionPermit = acquireInteractivePermit() ?: return
        try {
            if (store.state.value.busy) return
            val context = store.requireContext()
            val provider = store.config.externalStartProviders.firstOrNull { it.id == providerId } ?: return
            val snapshot = provider.snapshotOrFailure(context)
            store.setExternalStartSnapshot(provider.id, snapshot)
            if (!snapshot.canStart) {
                val requested = try {
                    systemPromptCoordinator.withPrompt(
                        privilegeUiExternalAuthorizationPrompt(provider.label),
                    ) {
                        provider.requestAuthorization(context)
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Throwable) {
                    PrivilegeUiExternalStartSnapshot(
                        message = throwable.failureMessage(),
                        exceptionText = throwable.toPrivilegeUiDiagnosticString(),
                    )
                }
                store.setExternalStartSnapshot(provider.id, requested)
                if (!requested.canStart) {
                    if (requested.message.isNotBlank()) store.appendStartupLog(requested.message.toString())
                    if (requested.exceptionText.isNotBlank()) store.appendStartupLog(requested.exceptionText)
                    return
                }
            }

            startExternal(provider, context, replaceConnectedServer)
        } finally {
            interactionPermit.close()
        }
    }

    suspend fun directStartAttempt(providerId: String): PrivilegeUiRuntimeStartAttempt.Request? {
        if (PrivilegeUiStartGate.isSilentStartInProgress) return null
        val context = store.requireContext()
        val provider = store.config.externalStartProviders.firstOrNull { it.id == providerId } ?: return null
        val snapshot = provider.snapshotOrFailure(context)
        store.setExternalStartSnapshot(provider.id, snapshot)
        if (snapshot.exceptionText.isNotBlank()) store.appendStartupLog(snapshot.exceptionText)
        if (!snapshot.canStart) return null
        return externalStartAttempt(provider, context)
    }

    private fun startExternal(
        provider: PrivilegeUiExternalStartProvider,
        context: Context,
        replaceConnectedServer: Boolean,
    ): Boolean = runtimeActions.runServerStartRequest(
        externalStartAttempt(provider, context),
        replaceConnectedServer,
    )

    private fun externalStartAttempt(
        provider: PrivilegeUiExternalStartProvider,
        context: Context,
    ): PrivilegeUiRuntimeStartAttempt.Request =
        PrivilegeUiRuntimeStartAttempt.Request(
            progressText = store.resourceText(R.string.priv_ui_external_starting),
            startedText = store.resourceText(R.string.priv_ui_external_start_requested),
            startupSource = provider.label.toString(),
            runtimeStartSource = PrivilegeUiRuntimeStartSource.EXTERNAL,
            runtimeStartProviderId = provider.id,
        ) {
            val commandLine = createNativeStarterCommand(requireRuntimeClientLaunch())
            if (provider is PrivilegeUiStreamingExternalStartProvider) {
                provider.start(
                    context = context,
                    commandLine = commandLine,
                    startupLogListener = startupLogListener,
                )
            } else {
                provider.start(context, commandLine)
            }
        }

    private suspend fun PrivilegeUiExternalStartProvider.snapshotOrFailure(
        context: Context,
    ): PrivilegeUiExternalStartSnapshot = try {
        snapshot(context)
    } catch (exception: CancellationException) {
        throw exception
    } catch (throwable: Throwable) {
        PrivilegeUiExternalStartSnapshot(
            message = throwable.failureMessage(),
            exceptionText = throwable.toPrivilegeUiDiagnosticString(),
        )
    }

}
