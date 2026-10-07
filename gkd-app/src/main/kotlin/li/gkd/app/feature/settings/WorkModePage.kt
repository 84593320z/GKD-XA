package li.gkd.app.feature.settings

import li.gkd.app.MainViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.Card
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yukonga.miuix.kmp.nav.core.NavKey
import kotlinx.serialization.Serializable
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.service.A11yService
import li.gkd.app.ui.PrivilegeServiceRoute
import li.gkd.app.ui.A11YScopeAppListRoute
import li.gkd.app.ui.style.cardHorizontalPadding
import li.gkd.app.ui.style.EmptyHeight
import li.gkd.app.ui.style.lineHeightDp
import li.gkd.app.ui.style.surfaceCardColors
import li.gkd.app.util.AutomatorModeOption
import li.gkd.app.util.ShortUrlSet
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.IntentUtils
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.ui.component.GkAnimatedBooleanContent
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfIconButton

@Serializable
data object WorkModeRoute : NavKey

@Composable
fun WorkModePage() {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel<WorkModeVm>()
    val writeSecureSettings by PermissionStates.writeSecureSettings.stateFlow.collectAsStateWithLifecycle()
    val a11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
    val privilegeContext by privilegeContextFlow.collectAsStateWithLifecycle()
    val automatorMode by mainVm.automatorModeFlow.collectAsStateWithLifecycle()
    GkPageScaffold(
        title = UiStrings.work_mode_title,
        navigationIcon = {
            PerfIconButton(
                imageVector = PerfIcon.ArrowBack,
                onClick = {
                    mainVm.popPage()
                })
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = cardHorizontalPadding)
                    .fillMaxWidth(),
                onClick = throttle { mainVm.updateAutomatorMode(AutomatorModeOption.A11yMode) },
                colors = surfaceCardColors,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = automatorMode == AutomatorModeOption.A11yMode,
                        onClick = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = AutomatorModeOption.A11yMode.label,
                        style = MiuixTheme.textStyles.title3,
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(start = 4.dp),
                    text = UiStrings.work_mode_basic,
                    style = MiuixTheme.textStyles.subtitle,
                )
                TextListItem(
                    modifier = Modifier
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(start = 8.dp, top = 4.dp),
                    style = MiuixTheme.textStyles.body1,
                    list = listOf(
                        UiStrings.a11y_permission_grant,
                        UiStrings.a11y_permission_regrant_description
                    ),
                )
                GkAnimatedBooleanContent(
                    targetState = writeSecureSettings || a11yRunning,
                    contentTrue = {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = cardHorizontalPadding)
                                .padding(start = 8.dp, top = 4.dp),
                            text = UiStrings.a11y_permission_ready,
                            style = MiuixTheme.textStyles.body2,
                        )
                    },
                    contentFalse = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = cardHorizontalPadding)
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                text = UiStrings.a11y_enable,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                onClick = throttle { IntentUtils.openA11ySettings() },
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(onClick = throttle {
                                        mainVm.navigateWebPage(ShortUrlSet.URL2)
                                    })
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                text = UiStrings.help_view,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(start = 4.dp, top = 8.dp),
                    text = UiStrings.work_mode_enhanced,
                    style = MiuixTheme.textStyles.subtitle,
                )
                TextListItem(
                    modifier = Modifier
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(start = 8.dp, top = 4.dp),
                    style = MiuixTheme.textStyles.body1,
                    list = listOf(
                        UiStrings.secure_settings_permission_grant,
                        UiStrings.secure_settings_permission_description,
                    ),
                )
                GkAnimatedBooleanContent(
                    targetState = writeSecureSettings,
                    contentTrue = {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = cardHorizontalPadding)
                                .padding(start = 8.dp, top = 4.dp),
                            text = UiStrings.secure_settings_permission_granted,
                            style = MiuixTheme.textStyles.body2,
                        )
                    },
                    contentFalse = {
                        PrivilegeAuthButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = cardHorizontalPadding)
                                .padding(top = 8.dp),
                        )
                    }
                )
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(top = 8.dp),
                    text = UiStrings.keep_alive_title,
                    onClick = throttle(vm.scope.launchUiAction {
                        val tutorialText = UiStrings.keep_alive_tile_description(META.appName) +
                                UiStrings.keep_alive_setup_heading +
                                UiStrings.keep_alive_setup_open_tiles +
                                UiStrings.keep_alive_setup_add_tile(META.appName) +
                                UiStrings.keep_alive_setup_place_tile
                        if (writeSecureSettings) {
                            mainVm.dialogRequests.showMessage(
                                title = UiStrings.keep_alive_title,
                                text = tutorialText,
                            )
                        } else if (mainVm.dialogRequests.confirm(
                                title = UiStrings.keep_alive_title,
                                text = tutorialText + UiStrings.keep_alive_permission_missing +
                                        UiStrings.keep_alive_permission_description,
                                confirmText = UiStrings.settings_go_to,
                                dismissText = UiStrings.action_close,
                                dismissOnRequest = true,
                            )
                        ) {
                            mainVm.navigatePage(PrivilegeServiceRoute)
                        }
                    }),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .padding(horizontal = cardHorizontalPadding)
                    .fillMaxWidth(),
                onClick = vm.scope.launchUiAction {
                    if (privilegeContext == null) {
                        if (mainVm.dialogRequests.confirm(
                                title = UiStrings.privilege_service_required,
                                text = UiStrings.automation_privilege_required_description,
                                confirmText = UiStrings.settings_go_to,
                                dismissOnRequest = true,
                            )
                        ) {
                            mainVm.navigatePage(PrivilegeServiceRoute)
                        }
                    } else {
                        mainVm.updateAutomatorMode(AutomatorModeOption.AutomationMode)
                    }
                },
                colors = surfaceCardColors,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = automatorMode == AutomatorModeOption.AutomationMode,
                        onClick = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = AutomatorModeOption.AutomationMode.label,
                        style = MiuixTheme.textStyles.title3,
                    )
                }
                TextListItem(
                    modifier = Modifier
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(start = 8.dp),
                    style = MiuixTheme.textStyles.body1,
                    list = listOf(
                        UiStrings.automation_a11y_description,
                        UiStrings.automation_no_display_issues,
                        UiStrings.automation_undetectable_a11y,
                        UiStrings.automation_scope_compatibility_hint,
                    ),
                )
                GkAnimatedBooleanContent(
                    targetState = privilegeContext != null,
                    contentTrue = {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = cardHorizontalPadding)
                                .padding(start = 8.dp, top = 8.dp),
                            text = UiStrings.privilege_service_connected,
                            style = MiuixTheme.textStyles.body2,
                        )
                    },
                    contentFalse = {},
                )
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = cardHorizontalPadding)
                        .padding(top = 8.dp),
                    text = UiStrings.a11y_scoped,
                    onClick = throttle {
                        mainVm.navigatePage(A11YScopeAppListRoute)
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }

}

@Composable
private fun PrivilegeAuthButton(
    modifier: Modifier = Modifier,
) {
    val mainVm = MainViewModel.requireCurrent()
    TextButton(
        modifier = modifier,
        text = UiStrings.permission_grant,
        onClick = throttle {
            mainVm.navigatePage(PrivilegeServiceRoute)
        },
    )
}

@Composable
private fun TextListItem(
    list: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    // MIUIX 部分 TextStyle 的 lineHeight 不是 Sp，直接 toDp() 会抛
    // IllegalStateException: Only Sp can convert to Px，必须走带 fallback 的工具
    val lineHeightDp = style.lineHeightDp(LocalDensity.current)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        list.forEach { text ->
            Row {
                Spacer(
                    modifier = Modifier
                        .padding(vertical = (lineHeightDp - 4.dp) / 2)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary)
                        .size(4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = style)
            }
        }
    }
}
