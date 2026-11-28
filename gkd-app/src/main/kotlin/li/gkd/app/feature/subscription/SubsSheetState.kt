package li.gkd.app.feature.subscription

import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.MainViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.core.state.Loadable
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.mtimeStr
import li.gkd.app.data.subscription.SubscriptionRepository
import li.gkd.app.feature.log.ActionLogRoute
import li.gkd.app.ui.component.GkPreferenceArrow
import li.gkd.app.ui.component.GkTooltipBox
import li.gkd.app.ui.style.GkShapes
import li.gkd.db.LOCAL_SUBS_ID
import li.gkd.db.SubsItem
import li.gkd.app.ui.share.DeletionTarget
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.share.message
import li.gkd.app.data.subscription.SubscriptionState
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.TimeUtils.formatTimeAgo
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkModalBottomSheet
import li.gkd.app.ui.component.GkRetainedSheet
import li.gkd.app.ui.component.SheetRequest

private data class SubsSheetSnapshot(
    val item: SubsItem,
    val subscription: RawSubscription?,
    val loading: Boolean,
)

class SubsSheetState {
    private val subsIdFlow = MutableStateFlow<SheetRequest<Long>?>(null)

    fun show(subsId: Long) {
        subsIdFlow.value = SheetRequest(subsId)
    }

    private fun dismiss(request: SheetRequest<Long>) {
        if (subsIdFlow.value === request) subsIdFlow.value = null
    }

    fun dismissForDeletion(targets: Set<DeletionTarget>) {
        val request = subsIdFlow.value ?: return
        if (DeletionTarget.Subscription(request.key) in targets) dismiss(request)
    }

    @Composable
    fun Render() {
        val requested by subsIdFlow.collectAsStateWithLifecycle()
        val subsItems by SubscriptionState.subsItemsFlow.collectAsStateWithLifecycle()
        val subscriptions by SubscriptionRepository.snapshotFlow.collectAsStateWithLifecycle()
        val loading by SubscriptionRepository.updating.collectAsStateWithLifecycle()
        val item = subsItems.find { it.id == requested?.key }
        val data = subscriptions.value
        val subscription = data?.subscriptions?.get(requested?.key)
        GkRetainedSheet(
            request = requested,
            snapshot = if (item != null && subscriptions is Loadable.Ready) SubsSheetSnapshot(item, subscription, loading) else null,
            missing = requested != null && subscriptions is Loadable.Ready && subscription == null &&
                data?.loadErrors?.containsKey(requested?.key) != true && data?.updateErrors?.containsKey(requested?.key) != true,
            onDismissRequest = ::dismiss,
        ) { _, displayed, show, dismiss, onDismissFinished ->
            val subsItem = displayed.item
            val subscription = displayed.subscription
            val mainVm = MainViewModel.requireCurrent()
            val scope = mainVm.scope
            val scrollState = rememberScrollState()
            GkModalBottomSheet(
                onDismissRequest = dismiss,
                show = show,
                onDismissFinished = onDismissFinished,
            ) {
                val showName = subscription?.name ?: UiStrings.subscription_id_description(subsItem.id)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(state = scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = showName,
                            style = MiuixTheme.textStyles.title3,
                        )
                        if (subscription != null) {
                            val timeStr = formatTimeAgo(subsItem.mtime)
                            val timeTooltipState = rememberTooltipState()
                            val timeTooltipScope = rememberCoroutineScope()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription =
                                            UiStrings.subscription_metadata_description(
                                                if (subsItem.isLocal) META.appName else subscription.author ?: UiStrings.unknown,
                                                subscription.version,
                                                timeStr,
                                            )
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = if (subsItem.isLocal) META.appName else subscription.author ?: UiStrings.unknown,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = when {
                                        subsItem.isLocal -> MiuixTheme.colorScheme.onSurfaceSecondary
                                        subscription.author == null -> MiuixTheme.colorScheme.onSurfaceVariantActions
                                        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = UiStrings.version_prefixed(subscription.version),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .clip(GkShapes.compact)
                                        .background(MiuixTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                GkTooltipBox(
                                    text = subsItem.mtimeStr,
                                    state = timeTooltipState,
                                    positioning = TooltipAnchorPosition.Above,
                                ) {
                                    Text(
                                        text = timeStr,
                                        modifier = Modifier
                                            .clip(GkShapes.compact)
                                            .clickable {
                                                timeTooltipScope.launch { timeTooltipState.show() }
                                            }
                                            .semantics {
                                                contentDescription = UiStrings.update_time_description(subsItem.mtimeStr)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }

                    if (subscription != null) {
                        val showGlobal = subscription.globalGroups.isNotEmpty() || subsItem.isLocal
                        val showApp = subscription.appGroups.isNotEmpty() || subsItem.isLocal
                        val showCategory = subscription.categories.isNotEmpty() || subsItem.isLocal
                        val updateUrl = subsItem.updateUrl
                        val showLink = !subsItem.isLocal && updateUrl != null

                        if (showGlobal || showApp || showCategory) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                if (showGlobal) {
                                    SubsSheetNavRow(
                                        title = UiStrings.global_rules,
                                        summary = if (subscription.globalGroups.isNotEmpty()) UiStrings.subscription_global_rules_count(subscription.globalGroups.size) else UiStrings.none_available,
                                        mutedSummary = subscription.globalGroups.isEmpty(),
                                        onClickLabel = UiStrings.global_rules_view_list,
                                        onClick = throttle {
                                            dismiss()
                                            mainVm.navigatePage(SubsGlobalGroupListRoute(subsItem.id))
                                        },
                                    )
                                }
                                if (showApp) {
                                    SubsSheetNavRow(
                                        title = UiStrings.app_rules,
                                        summary = if (subscription.appGroups.isNotEmpty()) UiStrings.subscription_apps_rules_count(subscription.apps.size, subscription.appGroups.size) else UiStrings.none_available,
                                        mutedSummary = subscription.appGroups.isEmpty(),
                                        onClickLabel = UiStrings.app_rules_view_list,
                                        onClick = throttle {
                                            dismiss()
                                            mainVm.navigatePage(SubsAppListRoute(subsItem.id))
                                        },
                                    )
                                }
                                if (showCategory) {
                                    SubsSheetNavRow(
                                        title = UiStrings.rule_categories,
                                        summary = if (subscription.categories.isNotEmpty()) UiStrings.subscription_categories_count(subscription.categories.size) else UiStrings.none_available,
                                        mutedSummary = subscription.categories.isEmpty(),
                                        onClickLabel = UiStrings.rule_categories_view_list,
                                        onClick = throttle {
                                            dismiss()
                                            mainVm.navigatePage(SubsCategoryRoute(subsItem.id))
                                        },
                                    )
                                }
                            }
                        }
                        if (showLink) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                BasicComponent(
                                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    onClickLabel = UiStrings.subscription_link_edit,
                                    onClick = throttle {
                                        if (SubscriptionRepository.isBusy) {
                                            toast(UiStrings.subscription_refresh_wait_compact)
                                            return@throttle
                                        }
                                        scope.launchUi {
                                            val url = mainVm.subsLinkDialog.request(
                                                initialValue = updateUrl,
                                            ) ?: return@launchUi
                                            SubscriptionRepository.addOrModifyRemote(url, subsItem).message?.let {
                                                toast(it)
                                            }
                                        }
                                    },
                                    endActions = {
                                        GkIcon(
                                            imageVector = GkIcons.Edit,
                                            contentDescription = null,
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = UiStrings.subscription_link,
                                            style = MiuixTheme.textStyles.headline1,
                                            color = MiuixTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = updateUrl,
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            softWrap = false,
                                            overflow = TextOverflow.MiddleEllipsis,
                                            modifier = Modifier
                                                .clearAndSetSemantics {}
                                                .clickable(
                                                    onClickLabel = UiStrings.subscription_link_view,
                                                    onClick = { mainVm.openUrl(updateUrl) },
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                            ) {
                                if (displayed.loading) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        text = UiStrings.file_load_failed_or_missing,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.error,
                                    )
                                    GkActionButton(onClick = throttle {
                                        scope.launchUi {
                                            SubscriptionRepository.refresh().message?.let { toast(it) }
                                        }
                                    }) {
                                        Text(text = UiStrings.action_reload)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!subsItem.isLocal && subscription?.supportUri != null) {
                            GkIconButton(
                                imageVector = GkIcons.HelpOutline,
                                onClick = throttle {
                                    mainVm.openUrl(subscription.supportUri)
                                },
                            )
                        }
                        GkIconButton(imageVector = GkIcons.History, onClick = throttle {
                            dismiss()
                            mainVm.navigatePage(ActionLogRoute(subsId = subsItem.id))
                        })
                        if (subsItem.id != LOCAL_SUBS_ID) {
                            GkIconButton(
                                imageVector = GkIcons.Delete,
                                onClick = throttle {
                                    mainVm.confirmDelete(
                                        title = UiStrings.subscription_delete,
                                        text = UiStrings.delete_named_confirmation(subscription?.name ?: subsItem.id),
                                        targets = { setOf(DeletionTarget.Subscription(subsItem.id)) },
                                        dismiss = dismiss,
                                    ) {
                                        val result = SubscriptionRepository.delete(subsItem.id)
                                        result.message?.let { toast(it) }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsSheetNavRow(
    title: String,
    summary: String,
    mutedSummary: Boolean,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    BasicComponent(
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClickLabel = onClickLabel,
        onClick = onClick,
        endActions = { GkPreferenceArrow() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.let {
                    if (mutedSummary) it.copy(alpha = 0.5f) else it
                },
            )
        }
    }
}
