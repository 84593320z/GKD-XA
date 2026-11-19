// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.FabPosition
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import priv.kit.ui.miuix.PrivilegeScaffoldLayout
import priv.kit.ui.miuix.PrivilegeScaffoldDefaults
import priv.kit.ui.miuix.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import priv.kit.ui.miuix.privilegeContentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import priv.kit.ui.component.AdbPanel
import priv.kit.ui.component.PermissionRestrictionWarning
import priv.kit.ui.component.AuthorizationModeTabs
import priv.kit.ui.component.AutoRecoveryWarning
import priv.kit.ui.component.ExternalStartPanel
import priv.kit.ui.component.ManualShellPanel
import priv.kit.ui.component.PrivilegeUiSpacing
import priv.kit.ui.component.RootPanel
import priv.kit.ui.component.RestartConfirmationDialog
import priv.kit.ui.component.ServiceStatusPanel
import priv.kit.ui.component.StartupLogPanel
import priv.kit.ui.component.privilegeUiAutoRecoveryWarningVisible
import priv.kit.ui.component.privilegeUiPermissionRestrictionWarningVisible

@Composable
internal fun PrivilegeScaffoldContent(
    screenScope: PrivilegeUiScreenScope,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = PrivilegeMiuixTheme.colorScheme.background,
    contentColor: Color = privilegeContentColorFor(containerColor),
    contentWindowInsets: WindowInsets = PrivilegeScaffoldDefaults.contentWindowInsets,
) {
    val state = screenScope.state
    val interactionEnabled = screenScope.interactionEnabled
    screenScope.RestartConfirmationDialog()
    PrivilegeScaffoldLayout(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            snackbarHost(snackbarHostState)
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PrivilegeUiSpacing.large,
                    top = PrivilegeUiSpacing.medium,
                    end = PrivilegeUiSpacing.large,
                    bottom = PrivilegeUiSpacing.extraLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.large),
        ) {
            Column {
                AnimatedVisibility(
                    visible = privilegeUiAutoRecoveryWarningVisible(
                        state = state,
                        interactionEnabled = interactionEnabled,
                    ),
                ) {
                    Column {
                        screenScope.AutoRecoveryWarning()
                        Spacer(Modifier.height(PrivilegeUiSpacing.large))
                    }
                }
                screenScope.ServiceStatusPanel()
                AnimatedVisibility(
                    visible = privilegeUiPermissionRestrictionWarningVisible(
                        runtimeStatus = state.runtimeStatus,
                        restrictionStatus = state.permissionRestrictionStatus,
                    ),
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    Column {
                        Spacer(Modifier.height(PrivilegeUiSpacing.large))
                        screenScope.PermissionRestrictionWarning()
                    }
                }
            }
            screenScope.AuthorizationModeTabs()
            screenScope.AuthorizationModePanel()
            if (state.startupLogLines.isNotEmpty()) {
                screenScope.StartupLogPanel()
            }
        }
    }
}

internal fun privilegeUiAutoRecoveryWarningVisible(
    state: PrivilegeUiScreenState,
    interactionEnabled: Boolean,
): Boolean = interactionEnabled &&
    privilegeUiAutoRecoveryWarningVisible(
        desiredEnabled = state.desiredEnabled,
        runtimeStatus = state.runtimeStatus,
        runtimeStartPhase = state.runtimeStartPhase,
    )

internal class PrivilegeUiScreenScope(
    val state: PrivilegeUiScreenState,
    val actions: PrivilegeUiActions,
    val interactionEnabled: Boolean,
    val showFeedback: (String) -> Unit,
    val onViewPermissionSolutions: (() -> Unit)? = null,
    val startupTutorialUrl: String? = null,
    val onViewStartupTutorial: ((PrivilegeUiTutorialTopic) -> Unit)? = null,
)

@Composable
private fun PrivilegeUiScreenScope.AuthorizationModePanel() {
    val mode = state.selectedStartupMode.takeIf { it in state.startupModes }
        ?: state.startupModes.first()
    when (mode) {
        PrivilegeUiStartupMode.ROOT -> RootPanel()
        PrivilegeUiStartupMode.MANUAL_SHELL -> ManualShellPanel()
        PrivilegeUiStartupMode.ADB -> AdbPanel()
        PrivilegeUiStartupMode.EXTERNAL -> ExternalStartPanel()
    }
}
