package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.GkSizedIconButton
import li.gkd.app.util.AiProtocolOption
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.ToastUtils.toast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data object AiProvidersPageRoute : NavKey

/** id 为 null 表示新建，protocol 决定新建时的默认协议。 */
@Serializable
data class AiProviderDetailRoute(
    val id: String? = null,
    val protocol: String = "openai",
) : NavKey

@Composable
fun AiProvidersPage() {
    val mainVm = MainViewModel.requireCurrent()
    val scope = rememberCoroutineScope()
    val store by storeFlow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val providers = store.aiProviders
    val activeId = store.activeAiProvider()?.id
    val listState = rememberLazyListState()
    var wasEmpty by rememberSaveable { mutableStateOf(providers.isEmpty()) }
    LaunchedEffect(providers.isEmpty()) {
        if (providers.isEmpty()) {
            query = ""
            wasEmpty = true
        } else if (wasEmpty) {
            // The stable "add" item moves down when the first provider appears. Return to the
            // newly inserted list rather than preserving that old first-item scroll anchor.
            listState.scrollToItem(0)
            wasEmpty = false
        }
    }
    val filtered = remember(providers, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            providers
        } else {
            providers.filter {
                it.name.contains(keyword, true) ||
                        it.apiUrl.contains(keyword, true) ||
                        it.model.contains(keyword, true)
            }
        }
    }

    fun confirmDelete(provider: AiConfig) = scope.launch {
        if (!mainVm.dialogRequests.confirm(
                title = "移除服务商",
                text = "确定移除「${provider.name.ifBlank { "未命名" }}」？它的模型列表会一并删除。",
                confirmText = "移除",
                error = true,
            )
        ) return@launch
        AiProviders.remove(provider.id)
        toast("已移除 ${provider.name.ifBlank { "未命名" }}")
    }

    GkPageScaffold(
        title = "AI 服务商",
        navigationIcon = {
            GkIconButton(
                imageVector = GkIcons.ArrowBack,
                onClick = { mainVm.popPage() },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(contentPadding).consumeWindowInsets(contentPadding).imePadding(),
        ) {
            if (providers.isNotEmpty()) {
                item(key = "list") {
                    AiSection(title = "已配置 ${providers.size} 个") {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            label = "搜索名称 / 地址 / 模型",
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )
                        if (filtered.isEmpty()) {
                            AiHint(
                                text = "没有匹配的服务商",
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        } else {
                            Column(modifier = Modifier.selectableGroup()) {
                                filtered.forEach { provider ->
                                    AiRowDivider(hasLeading = false)
                                    val isActive = provider.id == activeId
                                    val activate = throttle { AiProviders.setActive(provider.id) }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable(
                                                        onClickLabel = "查看服务商详情",
                                                        onClick = throttle {
                                                            mainVm.navigatePage(
                                                                AiProviderDetailRoute(id = provider.id),
                                                            )
                                                        },
                                                    ),
                                            ) {
                                                Text(
                                                    text = provider.name.ifBlank { "未命名" },
                                                    style = MiuixTheme.textStyles.headline1,
                                                    color = MiuixTheme.colorScheme.onSurface,
                                                    maxLines = 2,
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
                                            }
                                            AiProviderSelection(
                                                selected = isActive,
                                                enabled = provider.enabled,
                                                label = "设为当前服务商：${provider.name.ifBlank { "未命名" }}",
                                                onClick = activate,
                                            )
                                            var menuExpanded by remember { mutableStateOf(false) }
                                            li.gkd.app.ui.component.PerfDropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false },
                                                anchor = {
                                                    GkSizedIconButton(
                                                        size = 48.dp,
                                                        iconSize = 24.dp,
                                                        onClickLabel = "更多操作",
                                                        onClick = { menuExpanded = true },
                                                        imageVector = GkIcons.MoreVert,
                                                        contentDescription = "更多操作",
                                                    )
                                                },
                                            ) {
                                                li.gkd.app.ui.component.PerfDropdownMenuItem(
                                                    text = "编辑配置",
                                                    onClick = throttle {
                                                        menuExpanded = false
                                                        mainVm.navigatePage(AiProviderDetailRoute(id = provider.id))
                                                    },
                                                )
                                                li.gkd.app.ui.component.PerfDropdownMenuItem(
                                                    text = "移除服务商",
                                                    titleColor = MiuixTheme.colorScheme.error,
                                                    onClick = throttle {
                                                        menuExpanded = false
                                                        confirmDelete(provider)
                                                    },
                                                )
                                            }
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 8.dp),
                                        ) {
                                            if (isActive) {
                                                AiTagChip(text = "当前", emphasized = true)
                                            }
                                            AiTagChip(text = provider.protocolLabel)
                                            AiTagChip(text = "模型 ${provider.models.size}")
                                            if (provider.model.isNotBlank()) {
                                                AiTagChip(text = provider.model)
                                            }
                                            if (!provider.enabled) {
                                                AiTagChip(text = "已停用")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "add") {
                AiSection(title = "添加服务商") {
                    if (providers.isEmpty()) {
                        AiHint(text = "还没有服务商，选择下方协议添加第一个配置")
                        AiRowDivider(hasLeading = false)
                    }
                    AiProtocolOption.objects.forEachIndexed { index, option ->
                        if (index > 0) {
                            AiRowDivider(hasLeading = false)
                        }
                        BasicComponent(
                            title = option.newTitle,
                            summary = option.newSummary,
                            startAction = { AiRowIcon(imageVector = GkIcons.Link) },
                            endActions = { GkIcon(imageVector = GkIcons.Add) },
                            onClick = throttle {
                                mainVm.navigatePage(AiProviderDetailRoute(protocol = option.value))
                            },
                        )
                    }
                }
            }

            item(key = "help") {
                AiSection(title = "关于") {
                    BasicComponent(
                        title = "使用说明",
                        summary = "快照与规则生成说明",
                        startAction = { AiRowIcon(imageVector = GkIcons.HelpOutline) },
                        onClick = throttle { mainVm.navigatePage(AiHelpPageRoute) },
                    )
                }
            }
            item(key = "bottom") { GkPageBottomSpace(height = 40.dp) }
        }
    }
}
