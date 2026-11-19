// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import priv.kit.ui.PrivilegeUiAdbTcpPolicy
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.R

@Composable
internal fun PrivilegeUiScreenScope.AdbPanel() {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = state.localNetworkPermissionMissing,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                LocalNetworkPermissionPanel()
                Spacer(Modifier.height(PrivilegeUiSpacing.large))
            }
        }
        AnimatedVisibility(visible = state.batteryOptimizationPromptVisible) {
            Column {
                BatteryOptimizationPromptPanel()
                Spacer(Modifier.height(PrivilegeUiSpacing.large))
            }
        }
        Panel(topPadding = PrivilegeUiSpacing.extraSmall) {
            val wirelessAdbVisible = state.wirelessAdbSupported
            val staticTcpVisible = state.adbTcpPolicy != PrivilegeUiAdbTcpPolicy.DISABLED
            AdbFingerprintRow(
                fingerprint = state.adbKeyFingerprint,
                trailingContent = { TutorialButton(priv.kit.ui.PrivilegeUiTutorialTopic.ADB) },
            )
            if (!wirelessAdbVisible && !staticTcpVisible) {
                StatusText(stringResource(R.string.priv_ui_adb_unavailable))
            } else {
                HorizontalDivider(color = PrivilegeMiuixTheme.colorScheme.outlineVariant)
                if (wirelessAdbVisible) {
                    WirelessAdbSection()
                }
                if (wirelessAdbVisible && staticTcpVisible) {
                    HorizontalDivider(color = PrivilegeMiuixTheme.colorScheme.outlineVariant)
                }
                if (staticTcpVisible) {
                    StaticTcpAdbSection()
                }
            }
        }
    }
    state.staticTcpSwitchConfirmation?.let { action ->
        StaticTcpSwitchConfirmationDialog(action)
    }
    if (state.tcpAuthorizationFailureDialogVisible) {
        TcpAuthorizationFailureDialog()
    }
}
