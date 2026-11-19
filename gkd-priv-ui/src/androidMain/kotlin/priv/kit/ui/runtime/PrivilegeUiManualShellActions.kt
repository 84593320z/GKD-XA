package priv.kit.ui.runtime

import priv.kit.ui.*
import priv.kit.ui.state.*

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import priv.kit.core.Privilege

internal suspend fun PrivilegeUiViewModelStore.loadManualShellCommand() {
    val ownerUserId = Process.myUserHandle().hashCode()
    val commandLine = withContext(Dispatchers.IO) {
        runCatching { Privilege.nativeStarterCommand }.map { nativeCommand ->
            val scriptPath = config.manualShellScriptPath
            val script = preparePrivilegeUiExternalShellScript(
                nativeStarterCommand = { nativeCommand },
                ownerUserId = ownerUserId,
                scriptPath = scriptPath,
                externalFilesDirectory = { requireContext().getExternalFilesDir(null) },
                onScriptFailure = { appendStartupLog(it.toPrivilegeUiDiagnosticString()) },
            )
            if (script != null) {
                privilegeUiManualShellScriptCommand(requireContext().packageName, checkNotNull(scriptPath))
            } else {
                privilegeUiManualShellCommand(nativeCommand)
            }
        }
    }.getOrElse { throwable ->
        appendStartupLog(throwable.toPrivilegeUiDiagnosticString())
        updateState { it.copy(manualShellCommandLine = null) }
        return
    }
    updateState { it.copy(manualShellCommandLine = commandLine) }
}

internal fun PrivilegeUiViewModelStore.copyManualShellCommand() {
    val commandLine = state.value.manualShellCommandLine ?: return
    requireContext().copyToClipboard(
        label = text(R.string.priv_ui_manual_command_clip_label),
        text = commandLine,
    )
}
