package li.gkd.app.ui.home

import li.gkd.app.ui.component.gkPageBottomSpace
import li.gkd.app.MainViewModel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.text.UiStrings
import li.gkd.app.core.state.Loadable
import li.gkd.app.data.subscription.SubscriptionResult
import li.gkd.app.feature.subscription.UpsertRuleGroupRoute
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.WebViewRoute
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.share.message
import li.gkd.app.util.ShortUrlSet
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.UpdateTimeOption
import li.gkd.app.util.findOption
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.db.LOCAL_SUBS_ID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkAnimatedFloatingActionButton
import li.gkd.app.ui.component.GkBatchActionMenuItem
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkMultiSelectionActions
import li.gkd.app.ui.component.GkMultiSelectionTopAppBar
import li.gkd.app.ui.component.GkSettingsDialog
import li.gkd.app.ui.component.SubsItemCard
import li.gkd.app.ui.component.PerfTopAppBar
import li.gkd.app.ui.component.PerfIconButton
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfDropdownMenu
import li.gkd.app.ui.component.PerfDropdownMenuItem
import li.gkd.app.ui.component.GkTextMenu
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.rememberMultiSelectionState
import li.gkd.app.ui.component.rememberPinnedListScrollState
import li.gkd.app.ui.component.rememberReorderSession

@Composable
fun useSubsManagePage(): ScaffoldExt {
    val vm = viewModel<SubsManageVm>()
    val loadableState by vm.uiState.collectAsStateWithLifecycle()
    val state = loadableState.value
    return if (state == null) {
        subsManageStatePage(loadableState)
    } else {
        useLoadedSubsManagePage(vm, state)
    }
}

private fun subsManageStatePage(
    state: Loadable<SubsManageUiState>,
) = ScaffoldExt(
    navItem = BottomNavItem.SubsManage,
    content = { contentPadding ->
        val error = (state as? Loadable.Failure)?.cause
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = error?.message ?: if (error == null) UiStrings.loading_progress else UiStrings.data_load_failed,
                color = if (error == null) {
                    LocalContentColor.current
                } else {
                    MiuixTheme.colorScheme.error
                },
            )
        }
    },
)

@Composable
private fun useLoadedSubsManagePage(
    vm: SubsManageVm,
    state: SubsManageUiState,
): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val settingsDialogVisible by vm.settingsDialogVisibleFlow.collectAsStateWithLifecycle()
    val powerWarningItem by vm.powerWarningItemFlow.collectAsStateWithLifecycle()
    val store by storeFlow.collectAsStateWithLifecycle()
    val subItems = state.subItems
    val subsIdToRaw = state.subscriptions
    val scope = vm.scope
    val batchBusy by vm.batchBusyFlow.collectAsStateWithLifecycle()

    val refreshing = state.refreshing
    val pullToRefreshState = rememberPullToRefreshState()
    // 多选仅属于当前订阅 Tab 的临时交互状态，切换 Tab 后按设计清空，不要改为可保存状态。
    val selectionState = rememberMultiSelectionState<Long>()
    val allIds = remember(subItems) { subItems.mapTo(mutableSetOf()) { it.id } }
    val selectedIds = selectionState.selectedKeys intersect allIds
    val isSelectedMode = selectionState.active
    val reorderSession = rememberReorderSession(subItems) { it.id }
    val orderSubItems = reorderSession.items
    BackHandler(isSelectedMode) {
        selectionState.clear()
    }
    LaunchedEffect(allIds) {
        selectionState.retain(allIds)
    }

    if (settingsDialogVisible) {
        GkSettingsDialog(
            onDismissRequest = { vm.setSettingsDialogVisible(false) },
            title = UiStrings.subscription_settings,
        ) {
            GkTextMenu(
                title = UiStrings.subscriptions_update,
                option = UpdateTimeOption.objects.findOption(store.updateSubsInterval),
                onOptionChange = { vm.setUpdateInterval(it.value) },
            )
            GkTextSwitch(
                title = UiStrings.subscription_battery_warning,
                subtitle = UiStrings.subscription_battery_warning_setting,
                checked = store.subsPowerWarn,
                onCheckedChange = throttle(fn = vm::setPowerWarningEnabled),
            )
        }
    }

    powerWarningItem?.let { item ->
        GkAlertDialog(
            title = { Text(text = UiStrings.subscription_battery_warning) },
            text = {
                Column {
                    Text(text = UiStrings.subscription_multiple_enabled_warning)
                    Text(
                        text = UiStrings.subscription_battery_help,
                        modifier = Modifier.clickable(onClick = throttle {
                            vm.dismissPowerWarning()
                            mainVm.navigatePage(WebViewRoute(initUrl = ShortUrlSet.URL6))
                        }),
                        textDecoration = TextDecoration.Underline,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            },
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = throttle(vm::confirmPowerWarning),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MiuixTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = UiStrings.enable_anyway)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPowerWarning) {
                    Text(text = UiStrings.action_cancel)
                }
            },
        )
    }

    val pageScrollState = rememberPinnedListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val lazyListState = pageScrollState.listState
    ResetPageScrollOnRequest(BottomNavItem.SubsManage, pageScrollState::resetScrollAndAwait)
    return ScaffoldExt(
        navItem = BottomNavItem.SubsManage,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PerfTopAppBar(
                titleText = if (isSelectedMode) {
                    if (selectedIds.isNotEmpty()) selectedIds.size.toString() else ""
                } else {
                    BottomNavItem.SubsManage.label
                },
                miuixScrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (isSelectedMode) {
                        PerfIconButton(
                            imageVector = PerfIcon.Close,
                            contentDescription = "取消选择",
                            onClick = { selectionState.clear() },
                        )
                    }
                },
                actions = {
                    if (isSelectedMode) {
                        val canDeleteIds = selectedIds - LOCAL_SUBS_ID
                        if (canDeleteIds.isNotEmpty()) {
                            val text = UiStrings.subscriptions_delete_confirmation(canDeleteIds.size) +
                                if (LOCAL_SUBS_ID in selectedIds) UiStrings.subscriptions_local_excluded_suffix else ""
                            PerfIconButton(
                                imageVector = PerfIcon.Delete,
                                contentDescription = "删除选中订阅",
                                onClick = {
                                    scope.launchUi {
                                        vm.runBatchAction {
                                            if (!mainVm.dialogRequests.confirm(
                                                title = UiStrings.subscription_delete,
                                                text = text,
                                                error = true,
                                            )) return@runBatchAction
                                            val result = vm.deleteSubscriptions(canDeleteIds)
                                            if (result is SubscriptionResult.Success) {
                                                selectionState.removeDeleted(canDeleteIds)
                                                toast(if (result.count > 0) UiStrings.subscriptions_deleted_count(result.count) else UiStrings.selected_subscriptions_changed)
                                            } else {
                                                result.message?.let { toast(it) }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        PerfIconButton(
                            imageVector = PerfIcon.Autorenew,
                            contentDescription = if (refreshing) "正在刷新订阅" else "刷新订阅",
                            onClickLabel = "刷新订阅",
                            enabled = !refreshing,
                            onClick = throttle {
                                if (refreshing) {
                                    toast(UiStrings.subscription_refresh_wait)
                                } else {
                                    vm.refresh()
                                }
                            },
                        )
                        PerfIconButton(
                            imageVector = if (store.enableMatch) PerfIcon.ToggleOn else PerfIcon.ToggleOff,
                            tint = if (!store.enableMatch) MiuixTheme.colorScheme.primary else LocalContentColor.current,
                            contentDescription = UiStrings.rule_matching_label + if (store.enableMatch) UiStrings.enabled else UiStrings.disabled,
                            onClickLabel = UiStrings.switch_toggle,
                            onClick = throttle(vm::toggleMatching),
                        )
                        PerfIconButton(
                            imageVector = PerfIcon.PageInfo,
                            contentDescription = UiStrings.subscription_settings,
                            onClickLabel = UiStrings.settings_dialog_open,
                            onClick = { vm.setSettingsDialogVisible(true) },
                        )
                        PerfDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            anchor = {
                                PerfIconButton(
                                    imageVector = PerfIcon.MoreVert,
                                    contentDescription = UiStrings.more_actions,
                                    onClick = {
                                        if (refreshing) {
                                            toast(UiStrings.subscription_refresh_wait)
                                        } else {
                                            expanded = true
                                        }
                                    },
                                )
                            },
                        ) {
                            PerfDropdownMenuItem(
                                text = UiStrings.app_rule_add,
                                onClick = throttle {
                                    expanded = false
                                    mainVm.navigatePage(
                                        UpsertRuleGroupRoute(
                                            subsId = LOCAL_SUBS_ID,
                                            groupKey = null,
                                            appId = "",
                                            forward = true,
                                        )
                                    )
                                },
                            )
                            PerfDropdownMenuItem(
                                text = UiStrings.global_rule_add,
                                onClick = throttle {
                                    expanded = false
                                    mainVm.navigatePage(
                                        UpsertRuleGroupRoute(
                                            subsId = LOCAL_SUBS_ID,
                                            groupKey = null,
                                            appId = null,
                                            forward = true,
                                        )
                                    )
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            GkAnimatedFloatingActionButton(
                contentDescription = UiStrings.subscription_add,
                onClickLabel = UiStrings.subscription_add_dialog_open,
                visible = !isSelectedMode,
                onClick = {
                    if (refreshing) {
                        toast(UiStrings.subscription_refresh_wait_compact)
                    } else {
                        scope.launchUi {
                            val url = mainVm.subsLinkDialog.request() ?: return@launchUi
                            vm.addOrModifySubscription(url).message?.let { toast(it) }
                        }
                    }
                },
                imageVector = GkIcons.Add,
            )
        },
    ) { contentPadding ->
        val reorderableLazyColumnState =
            rememberReorderableLazyListState(lazyListState) { from, to ->
                reorderSession.move(from.index, to.index)
            }
        Column(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
            AnimatedVisibility(
                visible = !store.enableMatch,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.onSurface,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                                    append(UiStrings.rule_matching_paused)
                                }
                                withStyle(SpanStyle(color = MiuixTheme.colorScheme.onSurfaceVariantSummary)) {
                                    append(UiStrings.rule_matching_resume_suffix)
                                }
                            },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MiuixTheme.textStyles.body2,
                        )
                        GkIconButton(
                            imageVector = GkIcons.FlashOn,
                            contentDescription = UiStrings.rule_matching_enable,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MiuixTheme.colorScheme.primaryContainer,
                                contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                            ),
                            onClickLabel = UiStrings.rule_matching_enable,
                            onClick = vm::enableMatching,
                        )
                    }
                }
            }
            PullToRefreshBox(
                modifier = Modifier.weight(1f),
                state = pullToRefreshState,
                isRefreshing = refreshing,
                onRefresh = vm::refresh,
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(orderSubItems, { _, subItem -> subItem.id }) { index, subItem ->
                        // Keep the initial gesture alive after it enters selection mode.
                        val canDrag = reorderSession.dragging ||
                            (!refreshing && !batchBusy && !isSelectedMode && orderSubItems.size > 1)
                        ReorderableItem(
                            state = reorderableLazyColumnState,
                            key = subItem.id,
                            enabled = canDrag,
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            SubsItemCard(
                                modifier = Modifier.longPressDraggableHandle(
                                    enabled = canDrag,
                                    interactionSource = interactionSource,
                                    onDragStarted = {
                                        reorderSession.startDragging()
                                        if (!isSelectedMode) {
                                            selectionState.select(subItem.id)
                                        }
                                    },
                                    onDragStopped = {
                                        val result = reorderSession.finishDragging()
                                        if (result.moved) {
                                            selectionState.clear()
                                        }
                                        result.reorderedItems?.let { reorderedItems ->
                                            val changedItems = reorderedItems.mapIndexedNotNull { index, item ->
                                                item.copy(order = index).takeIf { it.order != item.order }
                                            }
                                            if (changedItems.isNotEmpty()) {
                                                vm.updateOrder(changedItems)
                                            }
                                        }
                                    },
                                ),
                                interactionSource = interactionSource,
                                subsItem = subItem,
                                subscription = subsIdToRaw[subItem.id],
                                index = index + 1,
                                isSelectedMode = isSelectedMode,
                                isSelected = selectedIds.contains(subItem.id),
                                loadError = state.loadErrors[subItem.id],
                                refreshError = state.refreshErrors[subItem.id],
                                refreshing = refreshing,
                                onCheckedChange = { checked ->
                                    vm.requestSubscriptionEnabled(subItem, checked)
                                },
                                onSelectedChange = { selectionState.toggle(subItem.id) },
                            )
                        }
                    }
                    gkPageBottomSpace()
                }
            }
        }
    }
}
