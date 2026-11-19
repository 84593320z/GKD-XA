// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import priv.kit.ui.PrivilegeUiRuntimeStartPhase
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiScreenState
import priv.kit.ui.R

internal enum class PrivilegeUiStartAction {
    START,
    CANCEL,
    CANCELLING,
    NONE,
}

internal fun privilegeUiStartAction(
    phase: PrivilegeUiRuntimeStartPhase,
    ownsRuntimeStart: Boolean,
): PrivilegeUiStartAction =
    when (phase) {
        PrivilegeUiRuntimeStartPhase.IDLE -> PrivilegeUiStartAction.START
        PrivilegeUiRuntimeStartPhase.RUNNING -> if (ownsRuntimeStart) {
            PrivilegeUiStartAction.CANCEL
        } else {
            PrivilegeUiStartAction.NONE
        }
        PrivilegeUiRuntimeStartPhase.CANCELLING -> if (ownsRuntimeStart) {
            PrivilegeUiStartAction.CANCELLING
        } else {
            PrivilegeUiStartAction.NONE
        }
    }

internal fun PrivilegeUiScreenState.startActionFor(
    source: PrivilegeUiRuntimeStartSource,
    providerId: String?,
): PrivilegeUiStartAction {
    val providerMatches = source != PrivilegeUiRuntimeStartSource.EXTERNAL ||
        (providerId != null && runtimeStartProviderId == providerId)
    val ownsRuntimeStart = runtimeStartSource == source && providerMatches
    return privilegeUiStartAction(runtimeStartPhase, ownsRuntimeStart)
}

internal fun privilegeUiStartActionEnabled(
    action: PrivilegeUiStartAction,
    startEnabled: Boolean,
): Boolean =
    when (action) {
        PrivilegeUiStartAction.START -> startEnabled
        PrivilegeUiStartAction.CANCEL -> true
        PrivilegeUiStartAction.CANCELLING,
        PrivilegeUiStartAction.NONE,
        -> false
    }

internal fun PrivilegeUiScreenState.startActionEnabled(
    action: PrivilegeUiStartAction,
    startAvailable: Boolean,
): Boolean =
    privilegeUiStartActionEnabled(
        action = action,
        startEnabled = startAvailable && !busy,
    )

internal fun privilegeUiStartActionLabel(
    action: PrivilegeUiStartAction,
    startLabel: Int,
): Int =
    when (action) {
        PrivilegeUiStartAction.CANCEL -> R.string.priv_ui_start_cancel_action
        PrivilegeUiStartAction.CANCELLING -> R.string.priv_ui_start_cancelling_action
        PrivilegeUiStartAction.START,
        PrivilegeUiStartAction.NONE,
        -> startLabel
    }
