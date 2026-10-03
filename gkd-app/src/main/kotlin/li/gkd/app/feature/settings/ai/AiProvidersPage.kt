package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.util.AiProtocolOption
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.ToastUtils.toast
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data object AiProvidersPageRoute : NavKey

/** id 为 null 表示新建，protocol 决定新建时的默认协议。 */
@Serializable
data class AiProviderDetailRoute(
    val id: String? = null,
    val protocol: String = "openai",
) : NavKey

/**
 * AI 服务商列表：顶部搜索 → 新增入口分组 → 已配置分组。
 *
 * 行排版沿用统一规格：行首 24dp 图标、标题 body1 Medium、地址 body2、
 * 元信息 footnote1；点击进入详情，长按删除，行尾圆点选择当前服务商。
 */
@Composable
fun AiProvidersPage() {
    val mainVm = MainViewModel.requireCurrent()
    val store by storeFlow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var providerToDelete by remember { mutableStateOf<AiConfig?>(null) }

    val providers = store.aiProviders
    val activeId = store.activeAiProvider()?.id
    val filtered = remember(providers, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            providers
        } else {
            providers.filter {
                it.name.contains(keyword, true) ||
                        it.apiUrl.contains(keyword, true) ||
                        it.model.contains(keyword, true) ||
                        it.protocolLabel.contains(keyword, true)
            }
        }
    }

    AiPageScaffold(
        title = "AI 服务商",
        onBack = { mainVm.popPage() },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = contentPadding.calculateTopPadding()),
        ) {
            item(key = "search") {
                AiSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    label = "搜索服务商",
                    modifier = Modifier
                        .padding(horizontal = AiUiDefaults.SidePadding)
                        .padding(top = 12.dp, bottom = 8.dp),
                )
            }

            item(key = "create_section") {
                AiGroup(title = "添加服务商") {
                    AiProtocolOption.objects.forEachIndexed { index, option ->
                        if (index > 0) AiDivider()
                        AiArrowRow(
                            title = option.newTitle,
                            summary = option.newSummary,
                            startAction = { AiRowIcon(imageVector = GkIcons.Link) },
                            onClick = throttle {
                                mainVm.navigatePage(AiProviderDetailRoute(protocol = option.value))
                            },
                        )
                    }
                    AiDivider()
                    AiArrowRow(
                        title = "使用说明",
                        summary = "快照生成规则的流程与加强模式",
                        startAction = { AiRowIcon(imageVector = GkIcons.HelpOutline) },
                        onClick = throttle { mainVm.navigatePage(AiHelpPageRoute) },
                    )
                }
            }

            item(key = "list_section") {
                AiGroup(title = "已配置 ${providers.size} 个") {
                    if (filtered.isEmpty()) {
                        AiEmptyText(
                            text = if (providers.isEmpty()) {
                                "还没有服务商，用上面的入口新建一个"
                            } else {
                                "没有匹配的服务商"
                            },
                        )
                    } else {
                        filtered.forEachIndexed { index, provider ->
                            if (index > 0) AiDivider()
                            AiProviderListRow(
                                provider = provider,
                                isActive = provider.id == activeId,
                                onOpen = {
                                    mainVm.navigatePage(AiProviderDetailRoute(id = provider.id))
                                },
                                onSelect = { AiProviders.setActive(provider.id) },
                                onDelete = { providerToDelete = provider },
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                AiPageBottomSpacer()
            }
        }
    }

    val deleting = providerToDelete
    AiDialog(
        show = deleting != null,
        title = "移除服务商",
        summary = deleting?.let { "确定移除「${it.displayName}」？它的模型列表会一并删除。" },
        onDismissRequest = { providerToDelete = null },
    ) {
        AiDialogActions(
            confirmText = "移除",
            destructive = true,
            onCancel = { providerToDelete = null },
            onConfirm = {
                deleting?.let { provider ->
                    AiProviders.remove(provider.id)
                    toast("已移除 ${provider.displayName}")
                }
                providerToDelete = null
            },
        )
    }
}

/** 列表行：整行点击进详情、长按删除，行尾圆点切换「当前使用」。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AiProviderListRow(
    provider: AiConfig,
    isActive: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val enabledOpacity = if (provider.enabled) 1f else 0.6f
    AiRow(
        modifier = Modifier.graphicsLayer { alpha = enabledOpacity },
        startAction = {
            AiRowIcon(
                imageVector = GkIcons.Link,
                tint = MiuixTheme.colorScheme.primary,
                enabled = provider.enabled,
            )
        },
        endActions = {
            AiSelectionIcon(selected = isActive)
        },
        interaction = Modifier.combinedClickable(
            onClick = throttle(fn = onOpen),
            onLongClick = throttle(fn = onDelete),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = provider.displayName,
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = provider.apiUrl.ifBlank { "未填写地址" },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = listOfNotNull(
                    provider.protocolLabel,
                    "模型 ${provider.models.size} 个",
                ).joinToString(" · "),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!provider.enabled) {
                Text(
                    text = "已停用",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
