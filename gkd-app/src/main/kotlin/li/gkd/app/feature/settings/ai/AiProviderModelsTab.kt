package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import li.gkd.app.MainViewModel
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiModel
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkCheckbox
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkSizedIconButton
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.ui.component.defaultIconTint
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.launchLogged
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun AiProviderModelsTab(provider: AiConfig) {
    val mainVm = MainViewModel.requireCurrent()
    val scope = rememberCoroutineScope()

    var fetching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AiModel?>(null) }
    var creating by remember { mutableStateOf(false) }
    var selectionMode by remember(provider.id) { mutableStateOf(false) }
    var selected by remember(provider.id) { mutableStateOf(setOf<String>()) }

    val keyword = query.trim()
    val filtered = remember(provider.models, keyword) {
        if (keyword.isEmpty()) {
            provider.models
        } else {
            provider.models.filter {
                it.displayName.contains(keyword, true) || it.modelId.contains(keyword, true)
            }
        }
    }

    fun saveModels(models: List<AiModel>) {
        AiProviders.mutate(provider.id) { it.copy(models = models) }
    }

    fun setModelId(modelId: String) {
        AiProviders.mutate(provider.id) { it.copy(model = modelId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item(key = "actions") {
                AiSection(title = "模型管理") {
                    BasicComponent(
                        title = if (fetching) "拉取中…" else "从远端拉取模型",
                        summary = "读取 ${
                            if (provider.apiUrl.isBlank()) "（未填写地址）"
                            else AiRuleGenerator.resolveModelsEndpoint(provider.apiUrl, provider.protocol)
                        }",
                        enabled = !fetching,
                        startAction = { AiRowIcon(imageVector = GkIcons.ArrowDownward) },
                        onClick = throttle {
                            val error = AiRuleGenerator.validateAiConfig(provider, requireModel = false)
                            if (error != null && error != AiRuleGenerator.PUBLIC_HTTP_HINT) {
                                toast(error)
                                return@throttle
                            }
                            fetching = true
                            message = null
                            scope.launchLogged {
                                AiRuleGenerator.fetchModels(provider)
                                    .onSuccess { remote ->
                                        AiProviders.mutate(provider.id) { latest ->
                                            latest.copy(models = mergeAiModels(latest.models, remote).first)
                                        }
                                        message = if (remote.isEmpty()) {
                                            "接口未返回模型列表"
                                        } else {
                                            "拉取成功：已合并远端 ${remote.size} 个模型"
                                        }
                                    }.onFailure { e ->
                                        message = "拉取失败：${AiRuleGenerator.analyzeConnectionError(e)}（${e.message}）"
                                    }
                                fetching = false
                            }
                        },
                    )
                    AiRowDivider(hasLeading = false)
                    BasicComponent(
                        title = "添加自定义模型",
                        summary = "手动填写 Model ID，用于接口不开放模型列表的服务商",
                        enabled = !fetching,
                        startAction = { AiRowIcon(imageVector = GkIcons.Add) },
                        onClick = throttle {
                            creating = true
                            editing = AiModel(modelId = "", displayName = "")
                        },
                    )
                    AiRowDivider(hasLeading = false)
                    BasicComponent(
                        title = if (selectionMode) "退出多选" else "批量管理模型",
                        summary = "多选后可一次删除；退出多选可用左侧关闭按钮",
                        enabled = !fetching,
                        startAction = { AiRowIcon(imageVector = GkIcons.Check) },
                        onClick = throttle {
                            selectionMode = !selectionMode
                            selected = emptySet()
                        },
                    )
                    message?.let { text ->
                        AiRowDivider(hasLeading = false)
                        AiHint(
                            text = text,
                            error = text.startsWith("拉取失败"),
                        )
                    }
                }
            }

            item(key = "search") {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = "搜索模型",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }

            val listTitle = if (keyword.isEmpty()) {
                "已保存 ${provider.models.size} 个模型"
            } else {
                "匹配 ${filtered.size} / ${provider.models.size} 个模型"
            }

            if (filtered.isEmpty()) {
                item(key = "models_empty") {
                    AiSection(title = listTitle) {
                        AiHint(
                            text = if (provider.models.isEmpty()) {
                                "还没有模型，先「从远端拉取模型」或手动添加"
                            } else {
                                "没有匹配的模型"
                            },
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    }
                }
            } else {
                val visible = if (selectionMode) provider.models else filtered
                item(key = "models_title") {
                    SmallTitle(
                        text = listTitle,
                        modifier = Modifier.semantics { heading() },
                        insideMargin = PaddingValues(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 8.dp),
                    )
                }
                items(visible, key = { "model:${it.modelId}" }) { model ->
                    PreferenceGroup {
                        ModelRow(
                            provider = provider,
                            model = model,
                            selectionMode = selectionMode,
                            checked = model.modelId in selected,
                            onToggleChecked = {
                                selected = if (model.modelId in selected) {
                                    selected - model.modelId
                                } else {
                                    selected + model.modelId
                                }
                            },
                            onEdit = {
                                creating = false
                                editing = model
                            },
                            onSetCurrent = { setModelId(model.modelId) },
                        )
                    }
                }
            }

            item(key = "bottom") {
                GkPageBottomSpace(height = 40.dp)
            }
        }

        if (selectionMode) {
            PreferenceGroup {
                Column {
                    Text(
                        text = "已选 ${selected.size} 个",
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GkSizedIconButton(
                            size = 48.dp,
                            iconSize = 24.dp,
                            onClickLabel = "退出多选",
                            onClick = throttle {
                                selectionMode = false
                                selected = emptySet()
                            },
                            imageVector = GkIcons.Close,
                            contentDescription = "退出多选",
                        )
                        TextButton(
                            text = if (selected.size == provider.models.size) "取消全选" else "全选",
                            enabled = !fetching,
                            modifier = Modifier.weight(1f),
                            onClick = throttle {
                                selected = if (selected.size == provider.models.size) {
                                    emptySet()
                                } else {
                                    provider.models.mapTo(mutableSetOf()) { it.modelId }
                                }
                            },
                        )
                        TextButton(
                            text = "删除",
                            enabled = selected.isNotEmpty() && !fetching,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(
                                color = colorScheme.error,
                                textColor = colorScheme.onError,
                            ),
                            onClick = throttle {
                                scope.launch {
                                    if (!mainVm.dialogRequests.confirm(
                                            title = "删除模型",
                                            text = "确定删除选中的 ${selected.size} 个模型？",
                                            confirmText = "删除",
                                            error = true,
                                        )
                                    ) return@launch
                                    saveModels(provider.models.filterNot { it.modelId in selected })
                                    toast("已删除 ${selected.size} 个模型")
                                    selected = emptySet()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    editing?.let { model ->
        ModelEditDialog(
            model = model,
            isNew = creating,
            onDismiss = { editing = null },
            onSubmit = { saved ->
                val ids = provider.models.map { it.modelId }.filter { it != model.modelId }
                if (saved.modelId in ids) {
                    toast("模型 ${saved.modelId} 已存在")
                    return@ModelEditDialog
                }
                val rest = provider.models.filterNot { it.modelId == model.modelId }
                val models = if (creating) rest + saved else rest.replaceFirst(saved)
                saveModels(models)
                editing = null
                message = if (creating) "已添加 ${saved.displayName}" else "已更新 ${saved.displayName}"
            },
            onDelete = if (creating) null else {
                {
                    saveModels(provider.models.filterNot { it.modelId == model.modelId })
                    editing = null
                    message = "已删除 ${model.displayName}"
                }
            },
        )
    }
}

@Composable
private fun ModelRow(
    provider: AiConfig,
    model: AiModel,
    selectionMode: Boolean,
    checked: Boolean,
    onToggleChecked: () -> Unit,
    onEdit: () -> Unit,
    onSetCurrent: () -> Unit,
) {
    val isCurrent = provider.model == model.modelId
    BasicComponent(
        modifier = Modifier.semantics {
            if (selectionMode) {
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            } else {
                selected = isCurrent
            }
        },
        role = if (selectionMode) Role.Checkbox else Role.RadioButton,
        onClick = if (selectionMode) onToggleChecked else onSetCurrent,
        endActions = {
            if (selectionMode) {
                GkCheckbox(checked = checked, key = model.modelId)
            } else {
                GkSizedIconButton(
                    size = 48.dp,
                    iconSize = 24.dp,
                    onClickLabel = "编辑模型参数",
                    onClick = throttle(fn = onEdit),
                    imageVector = GkIcons.Edit,
                    contentDescription = "编辑",
                )
                GkSizedIconButton(
                    size = 48.dp,
                    iconSize = 24.dp,
                    onClickLabel = if (isCurrent) "当前模型" else "设为当前模型",
                    onClick = throttle(fn = onSetCurrent),
                    imageVector = GkIcons.Check,
                    contentDescription = "设为当前",
                    tint = if (isCurrent) colorScheme.primary else defaultIconTint(),
                )
            }
        },
        bottomAction = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AiTagChip(text = "上下文 ${formatTokens(model.contextWindow)}")
                if (model.reasoning == true) AiTagChip(text = "支持思考")
                if (isCurrent) AiTagChip(text = "当前", emphasized = true)
            }
        },
    ) {
        Text(
            text = model.displayName.ifBlank { model.modelId },
            style = MiuixTheme.textStyles.headline1,
            color = colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = model.modelId,
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ModelEditDialog(
    model: AiModel,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AiModel) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var displayName by remember(model.modelId, isNew) { mutableStateOf(model.displayName) }
    var modelId by remember(model.modelId, isNew) { mutableStateOf(model.modelId) }
    var contextWindow by remember(model.modelId, isNew) {
        mutableStateOf(model.contextWindow.takeIf { it > 0 }?.toString().orEmpty())
    }
    var reasoning by remember(model.modelId, isNew) { mutableStateOf(model.reasoning == true) }
    val contextError = contextWindow.takeIf { it.isNotBlank() && it.toIntOrNull() == null }
    val canSave = displayName.isNotBlank() && modelId.isNotBlank() && contextError == null

    GkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isNew) "添加模型" else "编辑模型") },
        text = {
            Column {
                FieldLabel("显示名称")
                TextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "显示名称",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldLabel("Model ID")
                TextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Model ID",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldLabel("上下文长度（tokens，可留空）")
                TextField(
                    value = contextWindow,
                    onValueChange = { contextWindow = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "上下文长度（tokens，可留空）",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                contextError?.let {
                    AiHint(text = "上下文长度需为正整数", error = true, contentPadding = PaddingValues(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                GkTextSwitch(
                    title = "支持思考",
                    subtitle = "仅用于列表标记，生成时仍按服务商参数请求",
                    checked = reasoning,
                    onCheckedChange = { reasoning = it },
                )
                onDelete?.let { delete ->
                    TextButton(
                        text = "删除该模型",
                        onClick = throttle(fn = delete),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            textColor = colorScheme.error,
                        ),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                text = "取消",
                onClick = throttle(fn = onDismiss),
            )
        },
        confirmButton = {
            TextButton(
                text = "保存",
                enabled = canSave,
                onClick = throttle {
                    onSubmit(
                        AiModel(
                            modelId = modelId.trim(),
                            displayName = displayName.trim(),
                            contextWindow = contextWindow.trim().toIntOrNull() ?: 0,
                            reasoning = reasoning,
                        ),
                    )
                },
            )
        },
    )
}

/** 保持原有顺序替换模型；模型不在列表里时追加（远端拉取的编辑场景）。 */
private fun List<AiModel>.replaceFirst(model: AiModel): List<AiModel> {
    val index = indexOfFirst { it.modelId == model.modelId }
    return if (index < 0) this + model else toMutableList().also { it[index] = model }
}
