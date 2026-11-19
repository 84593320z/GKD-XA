// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import priv.kit.ui.miuix.PrivilegeSecondaryButton
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.R

@Composable
internal fun PrivilegeUiScreenScope.LocalNetworkPermissionPanel() {
    Panel {
        Text(
            text = stringResource(R.string.priv_ui_local_network_permission_required),
            style = PrivilegeMiuixTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.priv_ui_local_network_permission_message),
            style = PrivilegeMiuixTheme.typography.bodyMedium,
            color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
        )
        PrivilegeSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = interactionEnabled,
            onClick = actions.requestLocalNetworkPermission,
        ) {
            Text(stringResource(
                if (state.localNetworkPermissionSettingsRequired) {
                    R.string.priv_ui_local_network_permission_settings_action
                } else {
                    R.string.priv_ui_local_network_permission_grant_action
                },
            ))
        }
    }
}

@Composable
internal fun PrivilegeUiScreenScope.BatteryOptimizationPromptPanel() {
    val settingsUnavailable = stringResource(
        R.string.priv_ui_battery_optimization_settings_unavailable,
    )
    Panel {
        Text(
            text = stringResource(R.string.priv_ui_battery_optimization_title),
            style = PrivilegeMiuixTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.priv_ui_battery_optimization_message),
            style = PrivilegeMiuixTheme.typography.bodyMedium,
            color = PrivilegeMiuixTheme.colorScheme.onSurfaceVariant,
        )
        PrivilegeSecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = interactionEnabled,
            onClick = {
                if (
                    actions.canInteract() &&
                    !actions.requestBatteryOptimization()
                ) {
                    showFeedback(settingsUnavailable)
                }
            },
        ) {
            Text(stringResource(R.string.priv_ui_battery_optimization_settings_action))
        }
    }
}
