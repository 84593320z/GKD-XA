// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import top.yukonga.miuix.kmp.basic.FabPosition
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import priv.kit.ui.miuix.PrivilegeScaffoldDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import priv.kit.ui.miuix.privilegeContentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import priv.kit.ui.component.PrivilegeSystemPromptOverlay
import priv.kit.ui.component.PrivilegeTopBar
import priv.kit.ui.component.PrivilegeUiSpacing

/**
 * Controlled authorization page shared by Android, JVM and WasmJS.
 * The host owns [state], implements every [actions] handler, and supplies [showFeedback].
 * This entry point creates no runtime, permission launcher, polling, or simulated operations.
 * [interactionEnabled] controls presentation; [PrivilegeUiActions.canInteract] rechecks clicks.
 * [systemPrompt] is display-only; platform prompt ownership stays with the host.
 * [systemPromptWindowInsets] positions the prompt independently of [contentWindowInsets],
 * allowing simulation hosts to supply their own status bar or cutout insets.
 * [startupTutorialUrl] replaces the localized guide URL for every help topic.
 * [onViewStartupTutorial] overrides guide navigation; [onViewPermissionSolutions] takes
 * precedence for permission help. Topic anchors are stable across guide languages.
 */
@Composable
public fun PrivilegeScreen(
    state: PrivilegeUiScreenState,
    actions: PrivilegeUiActions,
    showFeedback: (String) -> Unit,
    interactionEnabled: Boolean = true,
    systemPrompt: PrivilegeUiPromptState? = null,
    onViewPermissionSolutions: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = { PrivilegeTopBar(onBack = {}, backEnabled = false) },
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = PrivilegeMiuixTheme.colorScheme.background,
    contentColor: Color = privilegeContentColorFor(containerColor),
    contentWindowInsets: WindowInsets = PrivilegeScaffoldDefaults.contentWindowInsets,
    systemPromptWindowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    startupTutorialUrl: String? = null,
    onViewStartupTutorial: ((PrivilegeUiTutorialTopic) -> Unit)? = null,
) {
    require(state.startupModes.isNotEmpty()) { "startupModes must not be empty" }
    Box(modifier = modifier) {
        PrivilegeScaffoldContent(
            screenScope = PrivilegeUiScreenScope(
                state = state,
                actions = actions,
                interactionEnabled = interactionEnabled,
                showFeedback = showFeedback,
                onViewPermissionSolutions = onViewPermissionSolutions,
                startupTutorialUrl = startupTutorialUrl,
                onViewStartupTutorial = onViewStartupTutorial,
            ),
            modifier = Modifier.fillMaxSize(),
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            snackbarHostState = snackbarHostState,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
        )
        PrivilegeSystemPromptOverlay(
            prompt = systemPrompt,
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
                .windowInsetsPadding(systemPromptWindowInsets)
                .padding(horizontal = PrivilegeUiSpacing.large, vertical = PrivilegeUiSpacing.medium),
        )
    }
}
