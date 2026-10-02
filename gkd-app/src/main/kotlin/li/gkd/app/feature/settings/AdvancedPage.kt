package li.gkd.app.feature.settings

import li.gkd.app.MainViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.gkd.app.text.UiStrings
import li.gkd.app.feature.log.A11yEventLogRoute
import li.gkd.app.feature.log.ActivityLogRoute
import li.gkd.app.feature.snapshot.SnapshotPageRoute
import li.gkd.app.permission.PermissionStates
import li.gkd.app.platform.service.ServiceController
import li.gkd.app.service.ActivityService
import li.gkd.app.service.ButtonService
import li.gkd.app.service.EventService
import li.gkd.app.service.HttpService
import li.gkd.app.service.TrackService
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.feature.settings.ai.AiProvidersPageRoute
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.ui.style.EmptyHeight
import li.gkd.app.ui.style.itemHorizontalPadding
import li.gkd.app.ui.style.itemVerticalPadding
import li.gkd.app.ui.style.surfaceCardColors
import li.gkd.app.ui.style.TABULAR_NUMBERS_FONT_FEATURE
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkSizedIconButton
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.PerfAlertDialog
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfIconButton
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.ui.component.SettingItem
import li.gkd.app.ui.component.TextSwitch
import li.gkd.app.ui.component.autoFocus
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data object AdvancedPageRoute : NavKey

@Composable
fun AdvancedPage() {
    AdvancedContent()
}

@Composable
private fun AdvancedContent() {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel<AdvancedVm>()
    val scope = vm.scope
    var showEditPortDialog by rememberSaveable { mutableStateOf(false) }
    var showHttpSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val store by storeFlow.collectAsStateWithLifecycle()
    val httpServer by HttpService.httpServerFlow.collectAsStateWithLifecycle()
    val localNetworkIps by HttpService.localNetworkIpsFlow.collectAsStateWithLifecycle()
    val buttonServiceRunning by ButtonService.isRunning.collectAsStateWithLifecycle()
    val activityServiceRunning by ActivityService.isRunning.collectAsStateWithLifecycle()
    val eventServiceRunning by EventService.isRunning.collectAsStateWithLifecycle()
    val trackServiceRunning by TrackService.isRunning.collectAsStateWithLifecycle()

    if (showEditPortDialog) {
        EditHttpPortDialog(
            currentPort = store.httpServerPort,
            onDismissRequest = { showEditPortDialog = false },
            onConfirm = {
                if (vm.saveHttpServerPort(it)) {
                    showEditPortDialog = false
                }
            },
        )
    }

    GkPageScaffold(
        title = UiStrings.advanced_settings,
        navigationIcon = {
            PerfIconButton(
                imageVector = PerfIcon.ArrowBack,
                onClick = mainVm::popPage,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            PreferenceGroup(showTop = false) {
                SettingItem(
                    title = UiStrings.snapshot_records,
                    subtitle = UiStrings.snapshot_records_description,
                    onClick = { mainVm.navigatePage(SnapshotPageRoute) },
                )
                TextSwitch(
                    title = UiStrings.snapshot_button_label,
                    subtitle = UiStrings.snapshot_button_description,
                    checked = buttonServiceRunning,
                    onCheckedChange = scope.launchUiAction { enabled ->
                        if (!enabled || mainVm.permissionRequests.ensurePermissions(
                                PermissionStates.foregroundServiceSpecialUse,
                                PermissionStates.notification,
                                PermissionStates.drawOverlays,
                            )
                        ) {
                            ServiceController.setSnapshotButtonEnabled(enabled)
                        }
                    },
                )
                TextSwitch(
                    title = "启用 AI 规则",
                    subtitle = "保存快照后按当前服务商自动生成规则",
                    checked = store.aiEnable,
                    onCheckedChange = { enabled ->
                        if (enabled && store.activeAiProvider()?.usable != true) {
                            toast("请先配置并启用一个 AI 服务商")
                            mainVm.navigatePage(AiProvidersPageRoute)
                            return@TextSwitch
                        }
                        vm.setAiEnable(enabled)
                    },
                )
                SettingItem(
                    title = "AI 服务商",
                    subtitle = "管理多个服务商，配置模型、请求头与生成参数",
                    imageVector = GkIcons.Edit,
                    onClick = { mainVm.navigatePage(AiProvidersPageRoute) },
                )
            }

            PreferenceGroup {
                HttpServiceItem(
                    running = httpServer != null,
                    settingsSelected = showHttpSettingsDialog,
                    port = store.httpServerPort,
                    localNetworkIps = localNetworkIps,
                    onSettingsClick = { showHttpSettingsDialog = !showHttpSettingsDialog },
                    onRunningChange = throttle(fn = scope.launchUiAction { enabled ->
                        if (!enabled || mainVm.permissionRequests.ensurePermissions(
                                PermissionStates.foregroundServiceSpecialUse,
                                PermissionStates.notification,
                                PermissionStates.localNetwork,
                            )
                        ) {
                            ServiceController.setHttpEnabled(enabled)
                        }
                    }),
                    onAddressClick = mainVm::openUrl,
                )
                androidx.compose.animation.AnimatedVisibility(visible = showHttpSettingsDialog) {
                    Column {
                        SettingItem(
                            title = UiStrings.http_port,
                            subtitle = store.httpServerPort.toString(),
                            imageVector = GkIcons.Edit,
                            onClickLabel = UiStrings.http_port_edit,
                            onClick = {
                                showHttpSettingsDialog = false
                                showEditPortDialog = true
                            },
                        )
                        TextSwitch(
                            title = UiStrings.http_clear_subscription,
                            subtitle = UiStrings.http_clear_subscription_description,
                            checked = store.autoClearMemorySubs,
                            onCheckedChange = vm::setAutoClearMemorySubs,
                        )
                    }
                }
            }

            PreferenceGroup {
                TextSwitch(
                    title = UiStrings.activity_service_label,
                    subtitle = UiStrings.activity_service_description,
                    checked = activityServiceRunning,
                    onCheckedChange = scope.launchUiAction { enabled ->
                        if (!enabled || mainVm.permissionRequests.ensurePermissions(
                                PermissionStates.foregroundServiceSpecialUse,
                                PermissionStates.notification,
                                PermissionStates.drawOverlays,
                            )
                        ) {
                            ServiceController.setActivityMonitorEnabled(enabled)
                        }
                    },
                )
                TextSwitch(
                    title = UiStrings.event_service_label,
                    subtitle = UiStrings.event_service_description,
                    checked = eventServiceRunning,
                    onCheckedChange = scope.launchUiAction { enabled ->
                        if (!enabled || mainVm.permissionRequests.ensurePermissions(
                                PermissionStates.foregroundServiceSpecialUse,
                                PermissionStates.notification,
                                PermissionStates.drawOverlays,
                            )
                        ) {
                            ServiceController.setEventMonitorEnabled(enabled)
                        }
                    },
                )
                TextSwitch(
                    title = UiStrings.track_overlay,
                    subtitle = UiStrings.track_overlay_description,
                    checked = trackServiceRunning,
                    onCheckedChange = { enabled ->
                        scope.launchUi {
                            if (enabled) {
                                if (!mainVm.dialogRequests.confirm(
                                        title = UiStrings.usage_notice,
                                        text = UiStrings.track_overlay_usage_description,
                                        confirmText = UiStrings.action_continue,
                                    )
                                ) return@launchUi
                                if (
                                    !mainVm.permissionRequests.ensurePermissions(
                                        PermissionStates.foregroundServiceSpecialUse,
                                        PermissionStates.notification,
                                        PermissionStates.drawOverlays,
                                    )
                                ) {
                                    return@launchUi
                                }
                            }
                            vm.setTrackServiceEnabled(enabled)
                        }
                    },
                )
            }

            PreferenceGroup {
                SettingItem(
                    title = UiStrings.activity_log_title,
                    subtitle = UiStrings.activity_switch_logs,
                    onClick = { mainVm.navigatePage(ActivityLogRoute) },
                )
                SettingItem(
                    title = UiStrings.event_log_title,
                    subtitle = UiStrings.a11y_event_logs,
                    onClick = { mainVm.navigatePage(A11yEventLogRoute) },
                )
                SettingItem(
                    title = UiStrings.github_cookie_label,
                    subtitle = UiStrings.upload_links_description,
                    suffix = UiStrings.tutorial_view,
                    suffixUnderline = true,
                    onSuffixClick = mainVm.githubUpload::openCookieHelp,
                    imageVector = GkIcons.Edit,
                    onClick = mainVm.githubUpload::editCookie,
                )
            }

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}

@Composable
private fun HttpServiceItem(
    running: Boolean,
    settingsSelected: Boolean,
    port: Int,
    localNetworkIps: List<String>,
    onSettingsClick: () -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onAddressClick: (String) -> Unit,
) {
    val addressStyle = MiuixTheme.textStyles.footnote1.copy(
        fontFeatureSettings = TABULAR_NUMBERS_FONT_FEATURE,
    )
    val addressItem: @Composable (String, String) -> Unit = { host, type ->
        Text(
            text = UiStrings.http_address_description(host, port, type),
            style = addressStyle,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = UiStrings.http_view_addresses(type),
                    onClick = throttle { onAddressClick("http://${host}:${port}") },
                )
                .padding(vertical = 2.dp),
        )
    }

    TextSwitch(
        modifier = Modifier.fillMaxWidth(),
        title = UiStrings.http_service_label,
        subtitle = UiStrings.http_service_description,
        suffixIcon = {
            GkSizedIconButton(
                size = 32.dp,
                iconSize = 20.dp,
                onClickLabel = UiStrings.http_settings_open,
                onClick = onSettingsClick,
                imageVector = GkIcons.PageInfo,
                contentDescription = UiStrings.http_settings_button,
                tint = if (settingsSelected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        },
        checked = running,
        onCheckedChange = onRunningChange,
        onClick = null,
    )
    androidx.compose.animation.AnimatedVisibility(visible = running) {
        CompositionLocalProvider(
            LocalTextStyle provides MiuixTheme.textStyles.body2
        ) {
            Column(
                modifier = Modifier.padding(
                    start = itemHorizontalPadding,
                    top = 0.dp,
                    end = itemHorizontalPadding,
                    bottom = itemVerticalPadding,
                ),
            ) {
                Text(text = "点击下方链接即可连接")
                addressItem("127.0.0.1", UiStrings.network_local_device)
                localNetworkIps.forEach { host ->
                    addressItem(host, UiStrings.network_lan)
                }
            }
        }
    }
}

@Composable
private fun EditHttpPortDialog(
    currentPort: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(currentPort.toString()) }
    PerfAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(text = UiStrings.http_port) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(5) },
                    label = UiStrings.http_port_input_hint,
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoFocus(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = UiStrings.port_input_length(value.length),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                text = UiStrings.action_confirm,
                enabled = value.isNotEmpty(),
                onClick = { onConfirm(value) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        },
        dismissButton = {
            TextButton(
                text = UiStrings.action_cancel,
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            )
        },
    )
}
