// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.PrivilegeUiStartupMode
import priv.kit.ui.R
import priv.kit.ui.miuix.PrivilegeIconButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivilegeTopBar(onBack: () -> Unit, backEnabled: Boolean = true) {
    SmallTopAppBar(
        title = stringResource(R.string.priv_ui_title),
        navigationIcon = {
            val backDescription = stringResource(R.string.priv_ui_nav_back)
            PrivilegeIconTooltip(text = backDescription, modifier = Modifier) {
                PrivilegeIconButton(enabled = backEnabled, onClick = onBack) {
                    Icon(
                        imageVector = PrivilegeUiIcons.ArrowBack,
                        contentDescription = backDescription,
                    )
                }
            }
        },
    )
}

@Composable
internal fun PrivilegeUiScreenScope.AuthorizationModeTabs() {
    val items = privilegeUiAuthorizationModeItems(
        modes = state.startupModes,
        selectedMode = state.selectedStartupMode,
        busy = state.busy,
        interactionEnabled = interactionEnabled,
    )
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier.widthIn(min = 88.dp, max = 180.dp)
                    .heightIn(min = 48.dp)
                    .squircleBackground(
                        color = if (item.selected) colors.surfaceContainer else colors.surface,
                        cornerRadius = TabRowDefaults.TabRowCornerRadius)
                    .squircleClip(TabRowDefaults.TabRowCornerRadius)
                    .selectable(selected = item.selected, enabled = item.enabled,
                        role = Role.Tab, onClick = {
                            if (item.enabled && actions.canInteract()) actions.selectStartupMode(item.mode)
                        })
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(item.mode.labelRes()), style = MiuixTheme.textStyles.body1,
                    fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (!item.enabled) colors.disabledOnSurface
                        else if (item.selected) colors.onBackground else colors.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

internal data class PrivilegeUiAuthorizationModeItem(
    val mode: PrivilegeUiStartupMode,
    val selected: Boolean,
    val enabled: Boolean,
)

internal fun privilegeUiAuthorizationModeItems(
    modes: List<PrivilegeUiStartupMode>,
    selectedMode: PrivilegeUiStartupMode,
    busy: Boolean,
    interactionEnabled: Boolean,
): List<PrivilegeUiAuthorizationModeItem> {
    val resolvedMode = selectedMode.takeIf { it in modes } ?: modes.firstOrNull()
    return modes.map { mode ->
        PrivilegeUiAuthorizationModeItem(
            mode = mode,
            selected = mode == resolvedMode,
            enabled = interactionEnabled && (!busy || mode == resolvedMode),
        )
    }
}

internal fun PrivilegeUiStartupMode.labelRes(): Int =
    when (this) {
        PrivilegeUiStartupMode.ROOT -> R.string.priv_ui_auth_method_root
        PrivilegeUiStartupMode.MANUAL_SHELL -> R.string.priv_ui_auth_method_manual_shell
        PrivilegeUiStartupMode.ADB -> R.string.priv_ui_auth_method_adb
        PrivilegeUiStartupMode.EXTERNAL -> R.string.priv_ui_auth_method_external
    }
