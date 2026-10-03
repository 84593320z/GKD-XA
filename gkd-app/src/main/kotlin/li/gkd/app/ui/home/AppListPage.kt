package li.gkd.app.ui.home

import li.gkd.app.ui.component.GkPageBottomSpaceDefaults
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.MainViewModel

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.text.UiStrings
import li.gkd.app.MainActivity
import li.gkd.app.data.AppInfo
import li.gkd.app.permission.PermissionStates
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.AppConfigRoute
import li.gkd.app.ui.EditBlockAppListRoute
import li.gkd.app.ui.share.ListPlaceholder
import li.gkd.app.util.AppGroupOption
import li.gkd.app.util.AppSortOption
import li.gkd.app.util.findOption
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.ui.component.GkAnimatedFloatingActionButton
import li.gkd.app.ui.icon.GkSearchCloseIconButton
import li.gkd.app.ui.component.GkRow
import li.gkd.app.ui.component.GkRowDefaults
import li.gkd.app.ui.component.GkRowText
import li.gkd.app.ui.component.GkRuleStatsData
import li.gkd.app.ui.component.GkEmptyState
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkQueryPkgAuthCard
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfTopAppBar
import li.gkd.app.ui.component.PerfIconButton
import li.gkd.app.ui.component.PerfDropdownMenu
import li.gkd.app.ui.component.GkCheckbox
import li.gkd.app.ui.component.AppIcon
import androidx.compose.foundation.layout.fillMaxWidth
import li.gkd.app.ui.component.AppBarTextField
import li.gkd.app.ui.component.MenuGroupCard
import li.gkd.app.ui.component.MenuItemCheckbox
import li.gkd.app.ui.component.MenuItemRadioButton
import li.gkd.app.ui.component.perfDefaultIconTint
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import androidx.compose.foundation.layout.PaddingValues
import li.gkd.app.ui.component.rememberListScrollState

@Composable
fun useAppListPage(): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val context = LocalActivity.current as MainActivity

    val vm = viewModel { AppListVm(mainVm) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val store by storeFlow.collectAsStateWithLifecycle()
    val appInfos = state.appInfos
    val searchStr = state.searchText
    val ruleSummary = state.ruleSummary

    val showSearchBar = state.showSearchBar
    val refreshing = state.refreshing
    val pullToRefreshState = rememberPullToRefreshState()
    val editWhiteListMode = state.editWhiteListMode
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    pageScrollState.ResetOnListChange(
        appInfos,
        key = { it.id },
        leadingItemKey = if (state.canQueryPackages) null else 1,
    )
    ResetPageScrollOnRequest(BottomNavItem.AppList, pageScrollState::resetScrollAndAwait)
    return ScaffoldExt(
        navItem = BottomNavItem.AppList,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DisposableEffect(null) {
                onDispose {
                    vm.onLeaveScreen()
                }
            }
            PerfTopAppBar(
                titleText = if (editWhiteListMode) UiStrings.app_whitelist else BottomNavItem.AppList.label,
                miuixScrollBehavior = scrollBehavior,
                bottomContent = {
                    if (showSearchBar) {
                        BackHandler {
                            if (!context.imeController.requestHide()) {
                                vm.closeSearch()
                            }
                        }
                        AppBarTextField(
                            value = searchStr,
                            onValueChange = vm::setSearchText,
                            hint = UiStrings.app_name_id_input_hint,
                            modifier = Modifier,
                        )
                    }
                },
                actions = {
                if (state.queryPackagesAbnormal) {
                    CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.error) {
                        PerfIconButton(
                            imageVector = PerfIcon.WarningAmber,
                            contentDescription = PermissionStates.queryPackages.name + UiStrings.error_label,
                            onClick = throttle(vm.scope.launchUiAction {
                                mainVm.dialogRequests.showMessage(
                                    title = UiStrings.permission_error,
                                    text = UiStrings.app_list_permission_error_description(PermissionStates.queryPackages.name)
                                )
                            }),
                        )
                    }
                }
                GkSearchCloseIconButton(
                    onClick = throttle(vm::toggleSearch),
                    isSearchOpen = showSearchBar,
                    contentDescription = if (showSearchBar) UiStrings.search_close else UiStrings.app_list_search,
                )
                var expanded by remember { mutableStateOf(false) }
                PerfDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    anchor = {
                        PerfIconButton(
                            imageVector = PerfIcon.Sort,
                            contentDescription = UiStrings.sort_filter,
                            tint = if (!state.showAllApps) MiuixTheme.colorScheme.primary else perfDefaultIconTint(),
                            onClick = {
                                expanded = true
                            },
                        )
                    },
                ) {
                        MenuGroupCard(inTop = true, title = UiStrings.sort_title) {
                            AppSortOption.objects.forEach { option ->
                                MenuItemRadioButton(
                                    text = option.label,
                                    selected = AppSortOption.objects.findOption(store.appSort) == option,
                                    onClick = { vm.setSortType(option) },
                                )
                            }
                        }
                        MenuGroupCard(title = UiStrings.group_title) {
                            AppGroupOption.normalObjects.forEach { option ->
                                val newValue = option.invert(store.appGroupType)
                                MenuItemCheckbox(
                                    enabled = newValue != 0,
                                    text = option.label,
                                    checked = option.include(store.appGroupType),
                                    onClick = { vm.setAppGroupType(newValue) },
                                )
                            }
                        }
                        MenuGroupCard(title = UiStrings.filter_title) {
                            MenuItemCheckbox(
                                text = UiStrings.whitelist_title,
                                checked = store.showBlockApp,
                                onClick = {
                                    vm.setShowBlockApp(!store.showBlockApp)
                                },
                            )
                        }
                }
                PerfIconButton(
                    imageVector = GkIcons.Block,
                    contentDescription = UiStrings.whitelist_edit_mode_toggle,
                    onClickLabel = if (editWhiteListMode) UiStrings.edit_exit else UiStrings.edit_enter,
                    tint = if (editWhiteListMode) MiuixTheme.colorScheme.primary else perfDefaultIconTint(),
                    onClick = vm::toggleEditWhiteListMode,
                )
            })
        },
        floatingActionButton = {
            GkAnimatedFloatingActionButton(
                visible = editWhiteListMode,
                contentDescription = UiStrings.whitelist_edit,
                onClick = {
                    mainVm.navigatePage(EditBlockAppListRoute)
                },
                imageVector = GkIcons.Edit,
            )
        }
    ) { contentPadding ->
        PullToRefreshBox(
            // 不在此处 padding：会压缩整个容器高度，导致列表提前结束、底部出现死白且吃不到底栏模糊。
            // 改由 LazyColumn 的 contentPadding 承担，容器继续铺满全屏，内容延伸到底栏下方。
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (!state.canQueryPackages) {
                    item(key = 1, contentType = 1) {
                        GkQueryPkgAuthCard()
                    }
                }
                items(appInfos, { it.id }) { appInfo ->
                    val stats = if (editWhiteListMode) null else GkRuleStatsData(
                        globalGroups = ruleSummary.appIdToGlobalGroupCount[appInfo.id] ?: 0,
                        appGroups = ruleSummary.appIdToAllGroups[appInfo.id]?.count { it.enable } ?: 0,
                        enabledOnly = true,
                    )
                    AppItemCard(
                        appInfo = appInfo,
                        stats = stats,
                        editWhiteListMode = editWhiteListMode,
                        inWhiteList = appInfo.id in state.whiteListAppIds,
                        onClick = {
                            if (editWhiteListMode) {
                                vm.toggleWhiteList(appInfo.id)
                            } else {
                                context.imeController.requestHide()
                                mainVm.navigatePage(AppConfigRoute(appInfo.id))
                            }
                        },
                    )
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    if (appInfos.isEmpty() && searchStr.isNotEmpty()) {
                        GkEmptyState(text = if (state.showAllApps) UiStrings.search_no_results else UiStrings.search_no_results_filter_hint)
                        // 底部空间已由 contentPadding.bottom（HomePage 的 listBottomSpace）统一提供，
                        // 这里只补一点紧凑间距，避免小屏上「空状态」离底栏太远
                        GkPageBottomSpace(height = GkPageBottomSpaceDefaults.CompactHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItemCard(
    appInfo: AppInfo,
    stats: GkRuleStatsData?,
    editWhiteListMode: Boolean,
    inWhiteList: Boolean,
    onClick: () -> Unit,
) {
    val summary = stats?.takeIf { it.hasRules }?.description ?: appInfo.id
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GkRowDefaults.SidePadding)
            .padding(bottom = 8.dp)
            .clearAndSetSemantics {
                contentDescription = if (editWhiteListMode) {
                    appInfo.name
                } else {
                    UiStrings.app_whitelist_state_description(appInfo.name, summary)
                }
                if (inWhiteList) {
                    stateDescription = UiStrings.whitelist_member
                } else if (editWhiteListMode) {
                    stateDescription = UiStrings.whitelist_not_member
                }
                onClick(
                    label = if (editWhiteListMode) if (inWhiteList) UiStrings.whitelist_remove else UiStrings.whitelist_add else UiStrings.rule_summary_open,
                    action = null
                )
            },
        cornerRadius = GkRowDefaults.CardCornerRadius,
        insideMargin = PaddingValues(0.dp),
    ) {
        GkRow(
            onClick = throttle(onClick),
            startAction = {
                AppIcon(appId = appInfo.id, size = 34.dp)
            },
            endActions = {
                if (editWhiteListMode) {
                    GkCheckbox(
                        key = appInfo.id,
                        checked = inWhiteList,
                    )
                } else if (inWhiteList) {
                    PerfIcon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(20.dp),
                        imageVector = PerfIcon.WhiteList,
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            },
        ) {
            GkRowText(title = appInfo.name, summary = summary)
        }
    }
}
