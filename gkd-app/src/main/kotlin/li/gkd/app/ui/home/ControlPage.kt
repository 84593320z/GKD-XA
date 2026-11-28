package li.gkd.app.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.MainActivity
import li.gkd.app.MainViewModel
import li.gkd.app.R
import li.gkd.app.data.subscription.SubscriptionState
import li.gkd.app.data.appinfo.AppInfoRepository
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
import li.gkd.app.ui.component.GkTooltipIconButtonBox
import li.gkd.app.ui.component.GroupNameText
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfTopAppBar
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.ui.component.SettingItem
import li.gkd.app.ui.component.TextSwitch
import li.gkd.app.ui.component.perfDefaultIconTint
import li.gkd.app.ui.component.textSize
import li.gkd.app.ui.component.useScrollBehaviorState
import li.gkd.app.ui.icon.GkPrivilegeConnectionIcon
import li.gkd.app.ui.share.statusText
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.ui.style.EmptyHeight
import li.gkd.app.ui.style.GkUi
import li.gkd.app.ui.style.StatusColors
import li.gkd.app.util.HOME_PAGE_URL
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.db.RuleGroupType
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
                    // 状态读取放在 actions 内部：无障碍/自动化状态一变只重组顶栏这一小块，
                    // 不会顺着 useControlPage 把整页（状态卡片、应用列表）一起重组。
                    val topBarStore by storeFlow.collectAsStateWithLifecycle()
                    val topBarA11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
                    val topBarUiAutomation by uiAutomationFlow.collectAsStateWithLifecycle()
                    val topBarTopAppId by topAppIdFlow.collectAsStateWithLifecycle()
                    // 口径与下方状态卡片的 serviceRunning 保持一致
                    val serviceConnected =
                        if (topBarStore.useA11y || actualA11yScopeAppList.contains(topBarTopAppId)) {
                            topBarA11yRunning
                        } else {
                            topBarUiAutomation != null
                        }
                    GkTooltipIconButtonBox(contentDescription = "工作模式") {
                        MiuixIconButton(
                            minWidth = 48.dp,
                            minHeight = 48.dp,
                            modifier = Modifier.semantics {
                                onClick(label = "前往工作模式页面", action = null)
                            },
                            onClick = throttle {
                                mainVm.navigatePage(li.gkd.app.feature.settings.WorkModeRoute)
                            },
                        ) {
                            GkPrivilegeConnectionIcon(
                                connected = serviceConnected,
                                // Tooltip 只说入口，读屏要能听出当前状态
                                contentDescription = if (serviceConnected) {
                                    "工作模式，运行中"
                                } else {
                                    "工作模式，未运行"
                                },
                                tint = perfDefaultIconTint(),
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        val store by storeFlow.collectAsStateWithLifecycle()

        val a11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
        val manageRunning by StatusService.isRunning.collectAsStateWithLifecycle()
        val writeSecureSettings by PermissionStates.writeSecureSettings.stateFlow.collectAsStateWithLifecycle()
        val appOpsRestricted by PermissionStates.appOpsRestrictedFlow.collectAsStateWithLifecycle()
        val topAppId by topAppIdFlow.collectAsStateWithLifecycle()
        val a11yServiceEnabled by mainVm.a11yServiceEnabledFlow.collectAsStateWithLifecycle()
        val a11yPartDisabled by a11yPartDisabledFlow.collectAsStateWithLifecycle()
        val uiAutomation by uiAutomationFlow.collectAsStateWithLifecycle()
        val privilegeContext by privilegeContextFlow.collectAsStateWithLifecycle()
        val activityServiceRunning by ActivityService.isRunning.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding),
        ) {
            if (appOpsRestricted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                if (store.useA11y || actualA11yScopeAppList.contains(topAppId)) {
                    TextSwitch(
                        title = "无障碍服务",
                        subtitle = if (a11yRunning) {
                            null
                        } else if (a11yServiceEnabled) {
                            "无障碍发生故障"
                        } else if (writeSecureSettings) {
                            if (store.enableAutomator && a11yPartDisabled) {
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
                    TextSwitch(
                        title = "自动化服务",
                        subtitle = if (uiAutomation != null) {
                            null
                        } else if (privilegeContext == null) {
                            "自动化未授权"
                        } else {
                            if (store.enableAutomator && a11yPartDisabled) {
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

            PreferenceGroup(title = "快捷入口") {
                SettingItem(
                    title = "触发记录",
                    subtitle = "规则误触可定位关闭",
                    onClickLabel = "打开触发记录页面",
                    onClick = {
                        mainVm.navigatePage(ActionLogRoute())
                    },
                )

                if (activityServiceRunning) {
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

            ServerStatusSection()

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
    val topAppId by topAppIdFlow.collectAsStateWithLifecycle()
    val privilegeContext by privilegeContextFlow.collectAsStateWithLifecycle()
    val colorScheme = MiuixTheme.colorScheme
    val subsCount = SubscriptionState.subsItemsFlow.collectAsStateWithLifecycle().value.size
    // 口径必须与「应用」Tab 一致：AppListPage 用 visibleAppInfosFlow 建列表。
    // 旧代码误用 SubscriptionState.subsMapFlow（该表以 subsId 为键、存的是订阅快照），
    // size 等于订阅数量，所以「应用」卡片显示的一直是订阅数。
    val appCount = AppInfoRepository.visibleAppInfosFlow.collectAsStateWithLifecycle().value.size

    val useA11y = store.useA11y || actualA11yScopeAppList.contains(topAppId)
    val uiAutomation by uiAutomationFlow.collectAsStateWithLifecycle()
    val serviceRunning = if (useA11y) a11yRunning else uiAutomation != null
    val statusColors = StatusColors.serviceStatus(running = serviceRunning)
    val animatedContainerColor by animateColorAsState(targetValue = statusColors.container, animationSpec = tween(250), label = "StatusCardContainer")
    val animatedIconColor by animateColorAsState(targetValue = statusColors.icon, animationSpec = tween(250), label = "StatusCardIcon")

    val statusTitle = if (serviceRunning) {
        "运行中"
    } else if (useA11y) {
        if (writeSecureSettings) "未运行" else "未授权"
    } else {
        if (privilegeContext != null) "未运行" else "未授权"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = if (appOpsRestricted) 4.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick(label = "前往工作模式与权限页面", action = null)
                },
            colors = CardDefaults.defaultColors(color = animatedContainerColor, contentColor = colorScheme.onSurface),
            onClick = throttle { mainVm.navigatePage(PrivilegeServiceRoute) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(animatedIconColor.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        imageVector = if (serviceRunning) PerfIcon.Check else PerfIcon.WarningAmber,
                        tint = animatedIconColor,
                        contentDescription = null,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(if (useA11y) "无障碍服务" else "自动化服务", style = MiuixTheme.textStyles.body2)
                    Text(statusTitle, style = MiuixTheme.textStyles.title2)
                }
                PerfIcon(imageVector = PerfIcon.KeyboardArrowRight, tint = colorScheme.onSurfaceSecondary)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetric(
                label = "订阅",
                count = subsCount,
                icon = PerfIcon.Layers,
                modifier = Modifier.weight(1f),
                onClick = throttle { mainVm.handleClickTab(BottomNavItem.SubsManage) },
            )
            DashboardMetric(
                label = "应用",
                count = appCount,
                icon = PerfIcon.Apps,
                modifier = Modifier.weight(1f),
                onClick = throttle { mainVm.handleClickTab(BottomNavItem.AppList) },
            )
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            onClick(label = "切换到${label}页面", action = null)
        },
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = count.toString(), style = MiuixTheme.textStyles.title2)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PerfIcon(
                        imageVector = icon,
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        contentDescription = null,
                    )
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            PerfIcon(
                imageVector = PerfIcon.KeyboardArrowRight,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                contentDescription = null,
            )
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
    val subsStatus = ruleSummary.statusText(actionCount)

    PreferenceGroup(title = "数据概览") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                        .fillMaxWidth()
                        .heightIn(min = GkUi.MinTouchTarget)
                        .squircleClip(cornerRadius = 10.dp)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupNameText(
                        modifier = Modifier.weight(1f),
                        preText = "最近触发：",
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
