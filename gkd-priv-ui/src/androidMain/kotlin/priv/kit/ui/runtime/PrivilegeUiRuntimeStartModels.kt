package priv.kit.ui.runtime

import priv.kit.core.PrivilegeServerInfo
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiStartMethod
import priv.kit.ui.PrivilegeUiState
import priv.kit.ui.PrivilegeUiText
import priv.kit.ui.state.PrivilegeUiFailureKind

internal sealed interface PrivilegeUiRuntimeStartAttempt {
    val progressText: PrivilegeUiText
    val startupSource: String?
    val runtimeStartSource: PrivilegeUiRuntimeStartSource?
    val runtimeStartProviderId: String?

    class Connect(
        override val progressText: PrivilegeUiText,
        override val startupSource: String?,
        override val runtimeStartSource: PrivilegeUiRuntimeStartSource? = null,
        val onFailure: ((Throwable) -> PrivilegeUiRuntimeStartFailureDisposition?)? = null,
        override val runtimeStartProviderId: String? = null,
        val start: suspend PrivilegeUiRuntimeStartSession.() -> PrivilegeServerInfo,
    ) : PrivilegeUiRuntimeStartAttempt

    class Request(
        override val progressText: PrivilegeUiText,
        val startedText: PrivilegeUiText,
        override val startupSource: String?,
        override val runtimeStartSource: PrivilegeUiRuntimeStartSource? = null,
        override val runtimeStartProviderId: String? = null,
        val start: suspend PrivilegeUiRuntimeStartSession.() -> Unit,
    ) : PrivilegeUiRuntimeStartAttempt

    class Workflow(
        override val progressText: PrivilegeUiText,
        override val startupSource: String?,
        override val runtimeStartSource: PrivilegeUiRuntimeStartSource? = null,
        val onFailure: ((Throwable) -> PrivilegeUiRuntimeStartFailureDisposition?)? = null,
        override val runtimeStartProviderId: String? = null,
        val start: suspend PrivilegeUiRuntimeStartSession.() -> PrivilegeUiRuntimeStartResult,
    ) : PrivilegeUiRuntimeStartAttempt
}

internal class PrivilegeUiRuntimeStartFailureDisposition(
    val stateTransform: (PrivilegeUiState) -> PrivilegeUiState = { it },
    val snackbarText: PrivilegeUiText? = null,
    val startupLogLines: List<String> = emptyList(),
    val afterCommit: (() -> Unit)? = null,
)

internal sealed interface PrivilegeUiRuntimeStartResult {
    class Connected(val serverInfo: PrivilegeServerInfo) : PrivilegeUiRuntimeStartResult
    class RequestSent(val text: PrivilegeUiText) : PrivilegeUiRuntimeStartResult
    data object Finished : PrivilegeUiRuntimeStartResult
}

internal sealed interface RuntimeStartCompletion {
    class Connected(
        val serverInfo: PrivilegeServerInfo,
        val successfulMethod: PrivilegeUiStartMethod?,
    ) : RuntimeStartCompletion
    class Failure(
        val throwable: Throwable,
        val failureKind: PrivilegeUiFailureKind,
        val appendDiagnostic: Boolean = true,
    ) : RuntimeStartCompletion

    data object Cancelled : RuntimeStartCompletion
    data object Finished : RuntimeStartCompletion
    class HandledFailure(
        val disposition: PrivilegeUiRuntimeStartFailureDisposition,
    ) : RuntimeStartCompletion
    data object Superseded : RuntimeStartCompletion
}
