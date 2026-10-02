package li.gkd.app.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import li.gkd.app.META
import li.gkd.app.MainActivity
import li.gkd.app.MainViewModel
import li.gkd.app.R
import li.gkd.app.data.subscription.SubscriptionState
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.uiAutomationFlow
import li.gkd.app.service.A11yService
import li.gkd.app.service.ActivityService
import li.gkd.app.service.StatusService
import li.gkd.app.service.a11yPartDisabledFlow
import li.gkd.app.service.switchAutomatorService
import li.gkd.app.service.topAppIdFlow
import li.gkd.app.store.AppStore.actionCountFlow
import li.gkd.app.store.AppStore.actualA11yScopeAppList
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.feature.log.ActionLogRoute
import li.gkd.app.feature.log.ActivityLogRoute
import li.gkd.app.ui.AppConfigRoute
import li.gkd.app.ui.PrivilegeServiceRoute
import li.gkd.app.ui.WebViewRoute
import li.gkd.app.ui.component.GroupNameText
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfIconButton
import li.gkd.app.ui.component.PerfTopAppBar
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.ui.component.SettingItem
import li.gkd.app.ui.component.TextSwitch
import li.gkd.app.ui.component.textSize
import li.gkd.app.ui.component.useScrollBehaviorState
import li.gkd.app.ui.share.statusText
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.ui.style.EmptyHeight
import li.gkd.app.ui.style.StatusColors
import li.gkd.app.util.HOME_PAGE_URL
import li.gkd.app.util.ShortUrlSet
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.db.RuleGroupType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun useControlPage(): ScaffoldExt {
    val context = LocalActivity.current as MainActivity
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel<DashboardVm>()
    val scrollKey = rememberSaveable { mutableIntStateOf(0) }
    val scrollState = useScrollBehaviorState(scrollKey)
    val miuixScrollBehavior = MiuixScrollBehavior()
    val appTitle = stringResource(R.string.app_name)
    ResetPageScrollOnRequest(BottomNavItem.Dashboard) {
        scrollKey.intValue++
    }
    return ScaffoldExt(
        navItem = BottomNavItem.Dashboard,
        modifier = Modifier.nestedScroll(miuixScrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                titleText = appTitle,
                miuixScrollBehavior = miuixScrollBehavior,
                actions = {
                    PerfIconButton(
                        imageVector = PerfIcon.RocketLaunch,
                        onClickLabel = "前往工作模式页面",
                        contentDescription = "工作模式",
                        onClick = throttle {
                            mainVm.navigatePage(li.gkd.app.feature.settings.WorkModeRoute)
                        },
                    )
                },
            )
        },
    ) { contentPadding ->
        val store by storeFlow.collectAsStateWithLifecycle()

        val a11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
        val manageRunning by StatusService.isRunning.collectAsStateWithLifecycle()
        val writeSecureSettings by PermissionStates.writeSecureSettings.stateFlow.collectAsStateWithLifecycle()
        val appOpsRestricted by PermissionStates.appOpsRestrictedFlow.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (appOpsRestricted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .semantics(mergeDescendants = true) {
                            this.onClick(label = "前往解除限制页面", action = null)
                        },
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.errorContainer,
                        contentColor = MiuixTheme.colorScheme.onErrorContainer,
                    ),
                    onClick = throttle {
                        mainVm.navigatePage(PrivilegeServiceRoute)
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PerfIcon(
                            imageVector = PerfIcon.WarningAmber,
                            tint = MiuixTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "检测到权限受限制，请前往解除",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onErrorContainer,
                        )
                        PerfIcon(
                            imageVector = PerfIcon.KeyboardArrowRight,
                            tint = MiuixTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            StatusOverviewSection()

            PreferenceGroup(title = "服务", showTop = true) {
                if (store.useA11y || actualA11yScopeAppList.contains(topAppIdFlow.collectAsStateWithLifecycle().value)) {
                    TextSwitch(
                        title = "服务状态",
                        subtitle = if (a11yRunning) {
                            "无障碍正在运行"
                        } else if (mainVm.a11yServiceEnabledFlow.collectAsStateWithLifecycle().value) {
                            "无障碍发生故障"
                        } else if (writeSecureSettings) {
                            if (store.enableAutomator && a11yPartDisabledFlow.collectAsStateWithLifecycle().value) {
                                "无障碍局部关闭"
                            } else {
                                "无障碍已关闭"
                            }
                        } else {
                            "无障碍未授权"
                        },
                        checked = a11yRunning,
                        onCheckedChange = { newEnabled ->
                            if (newEnabled && !PermissionStates.writeSecureSettings.value) {
                                mainVm.navigatePage(li.gkd.app.feature.settings.WorkModeRoute)
                            } else {
                                switchAutomatorService()
                            }
                        },
                    )
                } else {
                    val uiAutomation by uiAutomationFlow.collectAsStateWithLifecycle()
                    val privilegeContext by privilegeContextFlow.collectAsStateWithLifecycle()
                    TextSwitch(
                        title = "服务状态",
                        subtitle = if (uiAutomation != null) {
                            "自动化正在运行"
                        } else if (privilegeContext == null) {
                            "自动化未授权"
                        } else {
                            if (store.enableAutomator && a11yPartDisabledFlow.collectAsStateWithLifecycle().value) {
                                "自动化局部关闭"
                            } else {
                                "自动化已关闭"
                            }
                        },
                        checked = uiAutomation != null,
                        onCheckedChange = vm.scope.launchUiAction<Boolean> { newEnabled ->
                            if (newEnabled) {
                                mainVm.navigatePage(PrivilegeServiceRoute)
                            } else {
                                switchAutomatorService()
                            }
                        },
                    )
                }

                TextSwitch(
                    title = "常驻通知",
                    subtitle = "显示运行状态及统计数据",
                    checked = manageRunning && store.enableStatusService,
                    onCheckedChange = vm.scope.launchUiAction<Boolean> { enabled ->
                        if (enabled) {
                            mainVm.enableStatusService()
                        } else {
                            vm.stopStatusService()
                        }
                    },
                )
            }

            ServerStatusSection()

            PreferenceGroup(title = "快捷入口") {
                SettingItem(
                    title = "触发记录",
                    subtitle = "规则误触可定位关闭",
                    onClickLabel = "打开触发记录页面",
                    onClick = {
                        mainVm.navigatePage(ActionLogRoute())
                    },
                )

                if (ActivityService.isRunning.collectAsStateWithLifecycle().value) {
                    SettingItem(
                        title = "界面日志",
                        subtitle = "记录打开的应用及界面",
                        onClickLabel = "打开界面日志页面",
                        onClick = {
                            mainVm.navigatePage(ActivityLogRoute)
                        },
                    )
                }

                SettingItem(
                    title = "了解 GKD",
                    subtitle = "查阅规则文档和常见问题",
                    onClickLabel = "打开规则文档页面",
                    onClick = {
                        mainVm.navigatePage(WebViewRoute(initUrl = HOME_PAGE_URL))
                    },
                )
            }

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}

@Composable
private fun StatusOverviewSection() {
    val mainVm = MainViewModel.requireCurrent()
    val store by storeFlow.collectAsStateWithLifecycle()
    val a11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
    val writeSecureSettings by PermissionStates.writeSecureSettings.stateFlow.collectAsStateWithLifecycle()
    val appOpsRestricted by PermissionStates.appOpsRestrictedFlow.collectAsStateWithLifecycle()
    val colorScheme = MiuixTheme.colorScheme
    val subsCount = SubscriptionState.subsItemsFlow.collectAsStateWithLifecycle().value.size
    val appCount = SubscriptionState.subsMapFlow.collectAsStateWithLifecycle().value.size

    val useA11y = store.useA11y || actualA11yScopeAppList.contains(topAppIdFlow.collectAsStateWithLifecycle().value)
    val uiAutomation by uiAutomationFlow.collectAsStateWithLifecycle()
    val serviceRunning = if (useA11y) a11yRunning else uiAutomation != null
    val statusColors = StatusColors.serviceStatus(running = serviceRunning)

    val statusTitle = if (serviceRunning) {
        "运行中"
    } else if (useA11y) {
        if (writeSecureSettings) "未运行" else "未授权"
    } else {
        val privilegeContext by privilegeContextFlow.collectAsStateWithLifecycle()
        if (privilegeContext != null) "未运行" else "未授权"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp)
            .padding(top = if (appOpsRestricted) 4.dp else 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.defaultColors(
                color = statusColors.container,
            ),
            onClick = throttle {
                mainVm.navigatePage(PrivilegeServiceRoute)
            },
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = if (serviceRunning) {
                            Icons.Rounded.CheckCircleOutline
                        } else {
                            Icons.Rounded.ErrorOutline
                        },
                        tint = statusColors.icon,
                        contentDescription = null,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = statusTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "版本: ${META.versionName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = throttle {
                    mainVm.handleClickTab(BottomNavItem.SubsManage)
                },
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "订阅",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = subsCount.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = throttle {
                    mainVm.handleClickTab(BottomNavItem.AppList)
                },
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "应用",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = appCount.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStatusSection() {
    val mainVm = MainViewModel.requireCurrent()
    val ruleSummary by SubscriptionState.ruleSummaryFlow.collectAsStateWithLifecycle()
    val actionCount by actionCountFlow.collectAsStateWithLifecycle()
    val latestRecordDesc by SubscriptionState.latestRecordDescFlow.collectAsStateWithLifecycle()
    val latestRecord by SubscriptionState.latestRecordFlow.collectAsStateWithLifecycle()
    val usedSubsItemCount = SubscriptionState.subsItemsFlow.collectAsStateWithLifecycle().value.size
    val subsStatus = ruleSummary.statusText(actionCount)

    PreferenceGroup(title = "数据概览") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(usedSubsItemCount > 0) {
                Text(
                    text = "已开启 $usedSubsItemCount 条订阅",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            AnimatedVisibility(subsStatus.isNotEmpty()) {
                Text(
                    text = subsStatus,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (latestRecordDesc != null) {
                Row(
                    modifier = Modifier
                        .squircleClip(cornerRadius = 8.dp)
                        .clickable(onClickLabel = "前往应用的规则汇总页面", onClick = throttle {
                            latestRecord?.let {
                                mainVm.navigatePage(
                                    AppConfigRoute(
                                        appId = it.appId,
                                        focusLog = it,
                                    )
                                )
                            }
                        })
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupNameText(
                        modifier = Modifier.weight(1f),
                        preText = "最近触发: ",
                        isGlobal = latestRecord?.groupType == RuleGroupType.Global,
                        text = latestRecordDesc ?: "",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    PerfIcon(
                        imageVector = PerfIcon.KeyboardArrowRight,
                        modifier = Modifier.textSize(style = MiuixTheme.textStyles.body2),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
