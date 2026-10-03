package li.gkd.app.feature.settings.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import li.gkd.app.MainViewModel
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.TextSearchListDialog
import li.gkd.app.util.AiProtocolOption
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.findOption
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.launchLogged
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 服务商详情：配置 / 模型两个页签。
 *
 * 配置页分层与列表页一致：连接配置（输入项 + 端点模式 + 测试连接）→ 自定义请求头 →
 * 偏好与提示词 → 生成参数 → 主操作按钮 → 危险操作卡片。
 */
@Composable
fun AiProviderDetailPage(route: AiProviderDetailRoute) {
    val mainVm = MainViewModel.requireCurrent()
    val store by storeFlow.collectAsStateWithLifecycle()
    var createdId by remember { mutableStateOf<String?>(null) }

    val providerId = route.id ?: createdId
    val provider = providerId?.let { id -> store.aiProviders.firstOrNull { it.id == id } }

    val isNew = provider == null
    if (providerId != null && provider == null) {
        MissingProviderPage(onBack = { mainVm.popPage() })
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    var draft by remember(providerId) {
        mutableStateOf(provider?.let(AiProviderDraft::of) ?: AiProviderDraft.new(route.protocol))
    }

    AiPageScaffold(
        title = if (isNew) "新建服务商" else draft.name.ifBlank { "服务商" },
        onBack = { mainVm.popPage() },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            if (!isNew) {
                TabRow(
                    tabs = listOf("配置", "模型"),
                    selectedTabIndex = tab,
                    onTabSelected = { tab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AiUiDefaults.SidePadding + 12.dp,
                            vertical = 8.dp,
                        ),
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isNew || tab == 0) {
                    AiProviderConfigTab(
                        provider = provider ?: AiConfig(),
                        draft = draft,
                        isNew = isNew,
                        onDraftChange = { draft = it },
                        onCreated = { createdId = it },
                        onRemoved = { mainVm.popPage() },
                    )
                } else {
                    AiProviderModelsTab(provider!!)
                }
            }
        }
    }
}

@Composable
private fun MissingProviderPage(onBack: () -> Unit) {
    AiPageScaffold(title = "AI 服务商", onBack = onBack) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "该服务商已被移除",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AiTextButton(text = "返回", onClick = throttle(fn = onBack))
        }
    }
}

@Composable
private fun AiProviderConfigTab(
    provider: AiConfig,
    draft: AiProviderDraft,
    isNew: Boolean,
    onDraftChange: (AiProviderDraft) -> Unit,
    onCreated: (String) -> Unit,
    onRemoved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var apiKeyVisible by remember { mutableStateOf(false) }
    var headersExpanded by remember { mutableStateOf(false) }
    var showEndpointDlg by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var createdAt by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val protocolOption = AiProtocolOption.objects.findOption(draft.protocol)
    val baseUrl = draft.apiUrl.ifBlank { protocolOption.placeholder }

    fun buildDraftProvider(): AiConfig = draft.toProvider(provider)

    fun update(transform: (AiProviderDraft) -> AiProviderDraft) {
        onDraftChange(transform(draft))
        // 任何一次改动都让上一次测试结果失效
        testResult = null
    }

    if (showEndpointDlg) {
        TextSearchListDialog(
            onDismiss = { showEndpointDlg = false },
            title = "端点模式",
            selectedText = AiEndpointMode.labels[draft.endpointMode],
            textList = listOf(AiEndpointMode.CHAT, AiEndpointMode.RESPONSES).map { mode ->
                AiEndpointMode.labels[mode]!! to { update { it.copy(endpointMode = mode) } }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
    ) {
        item(key = "connection") {
            AiGroup(title = "连接配置") {
                AiFieldBlock {
                    AiTextField(
                        value = draft.name,
                        onValueChange = { v -> update { it.copy(name = v) } },
                        label = "名称",
                    )
                    AiTextField(
                        value = draft.apiUrl,
                        onValueChange = { v -> update { it.copy(apiUrl = v) } },
                        label = protocolOption.placeholder,
                        keyboardType = KeyboardType.Uri,
                    )
                    AiTextField(
                        value = draft.apiKey,
                        onValueChange = { v -> update { it.copy(apiKey = v) } },
                        label = "API Key",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (apiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            top.yukonga.miuix.kmp.basic.IconButton(
                                onClick = { apiKeyVisible = !apiKeyVisible },
                            ) {
                                GkIcon(
                                    imageVector = if (apiKeyVisible) {
                                        GkIcons.ToggleOn
                                    } else {
                                        GkIcons.ToggleOff
                                    },
                                    contentDescription = if (apiKeyVisible) "隐藏 API Key" else "显示 API Key",
                                )
                            }
                        },
                    )
                    if (draft.isAnthropic) {
                        AiTextField(
                            value = draft.anthropicVersion,
                            onValueChange = { v -> update { it.copy(anthropicVersion = v) } },
                            label = "anthropic-version",
                        )
                    }
                }
                if (!draft.isAnthropic) {
                    AiDivider()
                    AiDropdownRow(
                        title = "端点模式",
                        value = AiEndpointMode.labels[draft.endpointMode] ?: draft.endpointMode,
                        summary = if (draft.endpointMode == AiEndpointMode.RESPONSES) {
                            "调用 /responses，只带 temperature 与 max_output_tokens"
                        } else {
                            "调用 /chat/completions，支持 top_p 与 system 消息"
                        },
                        onClick = { showEndpointDlg = true },
                    )
                }
                AiDivider()
                AiPreferenceRow(
                    title = if (testing) "测试连接中…" else "测试连接",
                    summary = testResult
                        ?: "读取 $baseUrl/models，并把返回的模型合并进模型列表",
                    enabled = !testing,
                    startAction = { AiRowIcon(imageVector = GkIcons.Autorenew) },
                    onClick = {
                        val error = draft.validationError()
                        if (error != null) {
                            testResult = "校验未通过：$error"
                            return@AiPreferenceRow
                        }
                        testing = true
                        testResult = null
                        val config = buildDraftProvider()
                        scope.launchLogged {
                            AiRuleGenerator.fetchModels(config)
                                .onSuccess { remote ->
                                    val (merged, added) = mergeAiModels(config.models, remote)
                                    if (!isNew) {
                                        AiProviders.mutate(provider.id) { it.copy(models = merged) }
                                    }
                                    testResult = if (remote.isEmpty()) {
                                        "接口未返回模型列表"
                                    } else {
                                        "连接成功：远端 ${remote.size} 个，新增 $added"
                                    }
                                }.onFailure { e ->
                                    testResult = "连接失败：${e.message}"
                                }
                            testing = false
                        }
                    },
                )
            }
        }

        item(key = "headers") {
            AiGroup(title = "自定义请求头") {
                val rotation by animateFloatAsState(if (headersExpanded) 180f else 0f)
                AiPreferenceRow(
                    title = if (draft.headers.isEmpty()) "未设置" else "已设置 ${draft.headers.size} 项",
                    summary = "会先于认证头写入，因此 Authorization / x-api-key 仍以 API Key 为准。",
                    endActions = {
                        GkIcon(
                            imageVector = GkIcons.ExpandMore,
                            modifier = Modifier.rotate(rotation),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                    onClick = throttle { headersExpanded = !headersExpanded },
                )
                if (headersExpanded) {
                    draft.headers.forEach { header ->
                        AiDivider()
                        HeaderRow(
                            header = header,
                            onNameChange = { name ->
                                update {
                                    it.copy(
                                        headers = it.headers.map { h ->
                                            if (h.key == header.key) h.copy(name = name) else h
                                        },
                                    )
                                }
                            },
                            onValueChange = { value ->
                                update {
                                    it.copy(
                                        headers = it.headers.map { h ->
                                            if (h.key == header.key) h.copy(value = value) else h
                                        },
                                    )
                                }
                            },
                            onRemove = {
                                update { it.copy(headers = it.headers.filterNot { h -> h.key == header.key }) }
                            },
                        )
                    }
                    AiDivider()
                    AiPreferenceRow(
                        title = "添加请求头",
                        startAction = { AiRowIcon(imageVector = GkIcons.Add) },
                        endActions = { GkIcon(imageVector = GkIcons.Add) },
                        onClick = throttle { update { it.copy(headers = it.headers + AiHeaderDraft()) } },
                    )
                }
            }
        }

        item(key = "preferences") {
            AiGroup(title = "偏好与提示词") {
                AiSwitchRow(
                    title = "启用此服务商",
                    summary = "停用后不参与快照自动生成",
                    checked = draft.enabled,
                    onCheckedChange = { v -> update { it.copy(enabled = v) } },
                )
                AiDivider()
                AiFieldBlock {
                    AiTextField(
                        value = draft.systemPrompt,
                        onValueChange = { v -> update { it.copy(systemPrompt = v) } },
                        label = "系统提示词（追加在内置提示词之前）",
                        singleLine = false,
                        modifier = Modifier.height(120.dp),
                    )
                    AiHint(text = "留空则只使用内置的 gkd-rule-generator-prompt.md。")
                }
            }
        }

        item(key = "params") {
            AiGroup(title = "生成参数") {
                AiFieldBlock {
                    AiTextField(
                        value = draft.temperature,
                        onValueChange = { v -> update { it.copy(temperature = v) } },
                        label = "Temperature（0 ~ 2，越大输出越随机）",
                        keyboardType = KeyboardType.Decimal,
                    )
                    AiTextField(
                        value = draft.topP,
                        onValueChange = { v -> update { it.copy(topP = v) } },
                        label = "Top P（0 ~ 1，通常与 Temperature 二选一）",
                        keyboardType = KeyboardType.Decimal,
                    )
                    AiTextField(
                        value = draft.maxTokens,
                        onValueChange = { v -> update { it.copy(maxTokens = v) } },
                        label = "Max Tokens（1 ~ ${AiProviderDraft.MAX_TOKENS_LIMIT}，规则 JSON 建议 4096）",
                        keyboardType = KeyboardType.Number,
                    )
                    AiHint(text = "快照规则输出是结构化 JSON，Temperature 建议保持 0，可减少选择器漂移。")
                }
            }
        }

        item(key = "actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AiUiDefaults.SidePadding)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AiTextButton(
                    text = when {
                        saving -> "保存中…"
                        createdAt -> "已创建"
                        isNew -> "创建服务商"
                        else -> "保存配置"
                    },
                    enabled = !saving && !createdAt,
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val error = draft.validationError()
                        if (error != null) {
                            status = "保存失败：$error"
                            return@AiTextButton
                        }
                        val config = buildDraftProvider()
                        saving = true
                        if (isNew) {
                            onCreated(AiProviders.add(config))
                            status = "已创建，切到「模型」页签拉取模型"
                            createdAt = true
                            toast("已创建服务商 ${config.displayName}")
                        } else {
                            AiProviders.save(config)
                            status = if (config.enabled) {
                                "已保存"
                            } else {
                                "已保存，但该服务商处于停用状态"
                            }
                            toast("AI 配置已保存")
                        }
                        saving = false
                    },
                )
                status?.let { message ->
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.footnote2,
                        color = if (message.startsWith("保存失败")) {
                            MiuixTheme.colorScheme.error
                        } else {
                            MiuixTheme.colorScheme.primary
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (!isNew) {
            item(key = "danger") {
                AiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AiUiDefaults.SidePadding)
                        .padding(top = 12.dp),
                    showIndication = true,
                    onClick = if (saving) null else ({ showDeleteDialog = true }),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "移除服务商",
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            AiPageBottomSpacer()
        }
    }

    AiDialog(
        show = showDeleteDialog,
        title = "移除服务商",
        summary = "确定移除「${provider.displayName}」？它的模型列表会一并删除。",
        onDismissRequest = { if (!saving) showDeleteDialog = false },
    ) {
        AiDialogActions(
            confirmText = "移除",
            destructive = true,
            confirmEnabled = !saving,
            onCancel = { showDeleteDialog = false },
            onConfirm = {
                AiProviders.remove(provider.id)
                toast("已移除 ${provider.displayName}")
                showDeleteDialog = false
                onRemoved()
            },
        )
    }
}

/** 单条自定义请求头：名称 + 值 + 删除。 */
@Composable
private fun HeaderRow(
    header: AiHeaderDraft,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AiUiDefaults.SidePadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AiTextField(
                value = header.name,
                onValueChange = onNameChange,
                label = "请求头名称",
            )
            Spacer(modifier = Modifier.height(8.dp))
            AiTextField(
                value = header.value,
                onValueChange = onValueChange,
                label = "请求头值",
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        top.yukonga.miuix.kmp.basic.IconButton(onClick = onRemove) {
            GkIcon(
                imageVector = GkIcons.Delete,
                contentDescription = "删除该请求头",
                tint = MiuixTheme.colorScheme.error,
            )
        }
    }
}
