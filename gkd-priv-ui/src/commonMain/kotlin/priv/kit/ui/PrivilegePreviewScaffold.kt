// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import priv.kit.ui.miuix.PrivilegeDialog
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import priv.kit.ui.miuix.PrivilegeTextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import priv.kit.ui.component.PrivilegeTopBar
import priv.kit.ui.R

/**
 * Displays the shared page with an in-memory simulation; no runtime is created.
 * [useLegacyPackaging] selects the simulated manual command's native library packaging format.
 * [adbRestricted] controls permission restrictions for simulated Shell connections.
 * [batteryOptimizationExempt] and [localNetworkPermissionGranted] control simulated permission cards.
 * [onPermissionsChanged] reports successful simulated requests to the host controls.
 * [onViewPermissionSolutions] overrides opening the localized permission troubleshooting page.
 * [startupTutorialUrl] replaces the localized guide URL for every help topic.
 * [onViewStartupTutorial] overrides guide navigation; [onViewPermissionSolutions] takes
 * precedence for permission help. Topic anchors are stable across guide languages.
 */
@Composable
@Suppress("DEPRECATION")
public fun PrivilegePreviewScaffold(
    modifier: Modifier = Modifier,
    useLegacyPackaging: Boolean = true,
    adbRestricted: Boolean = true,
    batteryOptimizationExempt: Boolean = true,
    localNetworkPermissionGranted: Boolean = true,
    onPermissionsChanged: ((batteryOptimizationExempt: Boolean, localNetworkPermissionGranted: Boolean) -> Unit)? = null,
    onViewPermissionSolutions: (() -> Unit)? = null,
    startupTutorialUrl: String? = null,
    onViewStartupTutorial: ((PrivilegeUiTutorialTopic) -> Unit)? = null,
) {
    var batteryExempt by remember(batteryOptimizationExempt) { mutableStateOf(batteryOptimizationExempt) }
    var networkGranted by remember(localNetworkPermissionGranted) { mutableStateOf(localNetworkPermissionGranted) }
    val permissionsChanged by rememberUpdatedState(onPermissionsChanged)
    val grantBatteryExemption by rememberUpdatedState<() -> Unit>({
        batteryExempt = true
        permissionsChanged?.invoke(true, networkGranted)
    })
    val grantNetworkPermission by rememberUpdatedState<() -> Unit>({
        networkGranted = true
        permissionsChanged?.invoke(batteryExempt, true)
    })
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val externalLabel = stringResource(R.string.priv_ui_simulation_external)
    val startingText = mapOf(
        PrivilegeUiRuntimeStartSource.ROOT to stringResource(R.string.priv_ui_starting_root),
        PrivilegeUiRuntimeStartSource.ADB_WIRELESS to stringResource(R.string.priv_ui_wireless_adb_starting),
        PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP to stringResource(R.string.priv_ui_tcp_starting),
        PrivilegeUiRuntimeStartSource.EXTERNAL to stringResource(R.string.priv_ui_external_starting),
    )
    val pairingText = stringResource(R.string.priv_ui_pairing_service_found_text)
    val simulation = remember(externalLabel, startingText, pairingText) {
        PrivilegeUiSimulation(scope, externalLabel, startingText, pairingText,
            requestBatteryOptimization = { grantBatteryExemption() },
            requestLocalNetworkPermission = { grantNetworkPermission() },
        ) {
            clipboard.setText(AnnotatedString(it))
        }
    }
    SideEffect {
        simulation.setUseLegacyPackaging(useLegacyPackaging)
        simulation.setAdbRestricted(adbRestricted)
        simulation.setPermissions(batteryExempt, networkGranted)
    }
    if (simulation.externalAuthorizationRequested) {
        PrivilegeDialog(
            onDismissRequest = simulation::cancelOperation,
            title = { Text(stringResource(R.string.priv_ui_external_authorize_start)) },
            text = { Text(stringResource(R.string.priv_ui_simulation_external_confirmation)) },
            confirmButton = {
                PrivilegeTextButton(onClick = simulation::confirmExternalAuthorization) {
                    Text(stringResource(R.string.priv_ui_ok))
                }
            },
            dismissButton = {
                PrivilegeTextButton(onClick = simulation::cancelOperation) {
                    Text(stringResource(R.string.priv_ui_pairing_cancel_action))
                }
            },
        )
    }
    PrivilegeScreen(
        modifier = modifier,
        state = simulation.state,
        actions = simulation.actions,
        interactionEnabled = true,
        showFeedback = { scope.launch { snackbar.showSnackbar(it) } },
        onViewPermissionSolutions = onViewPermissionSolutions,
        startupTutorialUrl = startupTutorialUrl,
        onViewStartupTutorial = onViewStartupTutorial,
        snackbarHostState = snackbar,
        topBar = { PrivilegeTopBar(onBack = {}, backEnabled = false) },
    )
}
