package li.gkd.app.util

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import li.gkd.app.a11y.A11yContext
import li.gkd.app.a11y.A11yRuntime
import li.gkd.app.a11y.currentTopActivity
import li.gkd.app.a11y.ruleNow
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.data.NodeInfo
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.info2nodeList
import li.gkd.app.data.appinfo.AppInfoRepository
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import li.gkd.app.data.settings.AiModel
import li.gkd.app.data.snapshot.SnapshotRepository
import li.gkd.app.data.subscription.SubscriptionRepository
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.db.LOCAL_SUBS_ID

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0f,
    val top_p: Float = 1f,
    val max_tokens: Int = 4096,
    /** 只有需要 SSE 时才写 true；null 时该 key 不出现（json 的 explicitNulls = false），保持旧请求体不变 */
    val stream: Boolean? = null,
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val temperature: Float = 0f,
    val top_p: Float = 1f,
    val max_tokens: Int = 4096,
    val system: String? = null,
)

/** Responses 端点不接受 top_p，只带 temperature 与 max_output_tokens。 */
@Serializable
data class ResponsesInputMessage(
    val role: String = "user",
    val content: String,
)

@Serializable
data class ResponsesRequest(
    val model: String,
    val input: List<ResponsesInputMessage>,
    val instructions: String? = null,
    val temperature: Float = 0f,
    val max_output_tokens: Int = 4096,
)

object AiRuleGenerator {
    private data class AiGenerateTask(
        val config: AiConfig,
        val snapshot: AiSnapshotInput? = null,
        val requestedContext: AiScreenContext? = null,
    )

    private val scheduler = AiTaskScheduler<AiGenerateTask>(
        appScope,
        onFailure = { e ->
            ToastUtils.toast("AI 任务失败：" + analyzeConnectionError(e))
            LogUtils.d("AI task failed (" + e.javaClass.simpleName + ")")
        },
    ) { task ->
        withContext(Dispatchers.Default) {
            if (task.snapshot == null) processEnhanced(task) else processRule(task)
        }
    }
    val isGenerating = scheduler.isGenerating
    val pendingCount = scheduler.pendingCount
    private val capabilities = AiCapabilities()
    private var cachedPrompt: String? = null
    private suspend fun loadPrompt(): String = withContext(Dispatchers.IO) {
        cachedPrompt ?: app.assets.open("gkd-rule-generator-prompt.md").bufferedReader().use { it.readText() }
            .also { cachedPrompt = it }
    }

    fun fixApiUrl(url: String, protocol: String): String = AiApiUrls.fixApiUrl(url, protocol)

    fun resolveEndpoint(
        apiUrl: String,
        protocol: String,
        endpointMode: String = AiEndpointMode.CHAT,
    ): String = AiApiUrls.resolveEndpoint(apiUrl, protocol, endpointMode)

    fun resolveModelsEndpoint(apiUrl: String, protocol: String): String =
        AiApiUrls.resolveModelsEndpoint(apiUrl, protocol)

    private fun buildApiEndpoint(config: AiConfig): String = AiApiUrls.buildApiEndpoint(config)

    const val PUBLIC_HTTP_HINT = AiApiUrls.PUBLIC_HTTP_HINT

    fun validateAiConfig(config: AiConfig, requireModel: Boolean = true): String? =
        AiApiUrls.validateAiConfig(config, requireModel)

    /** 只有 Chat Completions 接 SSE：Responses / anthropic 的流式写法不同，一律保持现在的非流式请求。 */
    private val AiConfig.streamChatCompletions: Boolean
        get() = !isAnthropic && endpointMode == AiEndpointMode.CHAT

    private fun buildRequestBody(
        config: AiConfig,
        content: String,
        maxTokensOverride: Int? = null,
        stream: Boolean = false,
    ): String {
        val maxTokens = maxTokensOverride ?: config.maxTokens
        val systemPrompt = config.systemPrompt.trim().takeIf { it.isNotEmpty() }
        // 思考参数由 AiThinkingUtils 按模型名翻译，识别不出或被服务端拒绝过就不发
        val thinkingParams = AiThinkingUtils.buildParams(config, maxTokens)
        val base = when {
            config.isAnthropic -> json.encodeToString(
                AnthropicRequest(
                    model = config.model,
                    messages = listOf(AnthropicMessage("user", content)),
                    // Anthropic 协议思考开启时 temperature 必须为 1，其它值会被拒绝
                    temperature = if (thinkingEnabled(thinkingParams)) 1f else config.temperature,
                    top_p = config.topP,
                    max_tokens = maxTokens,
                    system = systemPrompt,
                )
            )
            config.endpointMode == AiEndpointMode.RESPONSES -> json.encodeToString(
                ResponsesRequest(
                    model = config.model,
                    input = listOf(ResponsesInputMessage(content = content)),
                    instructions = systemPrompt,
                    temperature = config.temperature,
                    max_output_tokens = maxTokens,
                )
            )
            else -> json.encodeToString(
                ChatRequest(
                    model = config.model,
                    messages = listOfNotNull(
                        systemPrompt?.let { ChatMessage("system", it) },
                        ChatMessage("user", content),
                    ),
                    temperature = config.temperature,
                    top_p = config.topP,
                    max_tokens = maxTokens,
                    stream = stream.takeIf { it },
                )
            )
        }
        if (thinkingParams.isEmpty()) return base
        val merged = JsonObject(json.parseToJsonElement(base).jsonObject + thinkingParams)
        return json.encodeToString(JsonObject.serializer(), merged)
    }

    /** 思考是否真的开启：合并的参数里 thinking.type == enabled */
    private fun thinkingEnabled(params: Map<String, JsonElement>): Boolean =
        (params["thinking"] as? JsonObject)?.get("type")?.jsonPrimitive?.content == "enabled"

    fun analyzeConnectionError(e: Throwable): String {
        val msg = e.message.orEmpty()
        val lower = msg.lowercase()
        return when {
            lower.contains("model_not_found") || lower.contains("no available") ||
                msg.contains("无可用") || msg.contains("模型不存在") ->
                "模型不可用：当前 Key 没有这个模型的权限，或服务商还没上架它。"

            lower.contains("401") || lower.contains("403") ||
                lower.contains("unauthorized") || lower.contains("invalid api key") ||
                lower.contains("invalid_api_key") || lower.contains("authentication") ->
                "鉴权失败：API Key 无效、已过期，或没有这个模型的权限。"

            lower.contains("402") || lower.contains("insufficient") ||
                msg.contains("余额不足") || msg.contains("欠费") ->
                "余额不足：请到服务商控制台确认账户余额。"

            lower.contains("429") || lower.contains("rate limit") || lower.contains("rate_limit") ||
                msg.contains("限流") || msg.contains("请求过于频繁") ->
                "请求太频繁被限流了，等一会儿再试。"

            lower.contains("404") || lower.contains("not found") ->
                "接口不存在：多半是 API 地址填错了，检查地址与路径是否完整。"

            lower.contains("max_tokens") || lower.contains("max tokens") ->
                "输出上限被拒绝：请在服务商配置里调小 Max Tokens 再试。"

            lower.contains("timeout") || lower.contains("timed out") || msg.contains("超时") ->
                "连接超时：网络太慢或服务商无响应，稍后再试。"

            lower.contains("unable to resolve host") || lower.contains("no address associated") ||
                lower.contains("unknownhost") ->
                "域名解析失败：设备当前连不上这个地址，检查网络或地址拼写。"

            lower.contains("ssl") || lower.contains("certificate") || lower.contains("handshake") ->
                "证书错误：多半是地址协议写错（http/https），或服务商证书有问题。"

            lower.contains("http 5") ->
                "服务方错误：接口在服务商那边出错了，稍后再试。"

            else -> "连接失败：检查 API 地址、Key 和网络后再试。"
        }
    }

    suspend fun testConnection(config: AiConfig): Result<String> = cancellableResult {
        val effective = config.copy(thinkingRejected = true)
        AiTransport.shared.post(effective, buildRequestBody(effective, "hi", maxTokensOverride = 1))
        "ok"
    }

    suspend fun fetchModels(config: AiConfig): Result<List<AiModel>> = cancellableResult {
        val body = AiTransport.shared.models(config)
            val data = json.parseToJsonElement(body).jsonObject["data"]
                ?: throw Exception("接口未返回 data 字段")
            data.jsonArray.mapNotNull { element ->
                runCatching {
                    val item = element.jsonObject
                    val id = item["id"]?.jsonPrimitive?.content ?: return@runCatching null
                    val label = (item["display_name"] ?: item["name"])?.jsonPrimitive?.content
                    val capabilities = item["capabilities"].objectOrNull()
                    val reasoning = item["supports_reasoning"].boolOrNullValue()
                        ?: item["supported_parameters"].jsonArrayOrNull
                            ?.any { it.jsonPrimitive.content == "reasoning_effort" }
                        ?: capabilities?.get("supports_reasoning").boolOrNullValue()
                    AiModel(
                        modelId = id,
                        displayName = label?.ifBlank { null } ?: id,
                        contextWindow = item.intOf("context_length")
                            ?: item["context_window"].intOrNullValue()
                            ?: item["context_window"].objectOrNull()?.intOf("max_input_tokens")
                            ?: capabilities?.intOf("max_input_tokens")
                            ?: 0,
                        reasoning = reasoning,
                    )
                }.getOrNull()
            }
    }

    private suspend fun <T> cancellableResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun generationConfig(): AiConfig? {
        val settings = storeFlow.value
        if (!settings.aiEnable) {
            ToastUtils.toast("请先启用 AI 规则")
            return null
        }
        val config = settings.activeAiProvider()
        if (config == null || !config.usable || !config.enabled ||
            validateAiConfig(config)?.let { it != PUBLIC_HTTP_HINT } == true) {
            ToastUtils.toast("请先在 AI 设置中选择并配置服务商")
            return null
        }
        // Freeze settings at admission: later edits/deletion cannot corrupt queued work.
        return config.copy(headers = config.headers.toList(), models = config.models.toList())
    }

    suspend fun generateRule(
        snapshotId: Long,
        selectedNodeIds: Set<Int> = emptySet(),
        includeAllNodes: Boolean = false,
    ): Boolean {
        currentCoroutineContext().ensureActive()
        val config = generationConfig() ?: return false
        val selection = selectedNodeIds.toSet()
        val snapshot = admitAiInput(onFailure = { error ->
            LogUtils.d("AI input preparation failed (" + error.javaClass.simpleName + ")")
            ToastUtils.toast("无法读取有效快照，请重新捕获后再试")
        }) {
            withContext(Dispatchers.IO) {
                AiSnapshotInput.read(SnapshotRepository.snapshotFile(snapshotId), selection, includeAllNodes)
            }
        } ?: return false
        currentCoroutineContext().ensureActive()
        return enqueue(AiGenerateTask(config, snapshot = snapshot))
    }

    suspend fun enhancedGenerate() {
        currentCoroutineContext().ensureActive()
        val config = generationConfig() ?: return
        val root = A11yRuntime.getRoot() ?: return ToastUtils.toast("无法获取当前界面节点")
        val context = withContext(Dispatchers.IO) { AiScreenContext.capture(root) }
        val accepted = scheduler.enqueueAndAwait(AiGenerateTask(config, requestedContext = context)) {
            ToastUtils.toast("AI 任务已加入队列")
        }
        if (!accepted) ToastUtils.toast("AI 任务队列已满，请稍后再试")
    }

    private fun enqueue(task: AiGenerateTask): Boolean {
        val accepted = scheduler.enqueue(task)
        ToastUtils.toast(if (accepted) "AI 任务已加入队列" else "AI 任务队列已满，请稍后再试")
        return accepted
    }

    private suspend fun snapshotText(id: Long): String = withContext(Dispatchers.IO) {
        AiSnapshotInput.readText(SnapshotRepository.snapshotFile(id))
    }

    private suspend fun processRule(task: AiGenerateTask) {
        val snapshot = requireNotNull(task.snapshot)
        val content = loadPrompt() + "\n" + snapshot.content
        ToastUtils.toast("AI 正在生成规则...", forced = true)
        val candidate = AiRuleCandidate(snapshot.appId)
        repeat(2) {
            val generated = candidate.accept(callAiApi(task.config, content))
            if (generated != null) {
                val inserted = insertRuleToSubscription(generated)
                ToastUtils.toast("AI 规则已添加到本地规则（" + inserted + " 组）")
                return
            }
        }
        ToastUtils.toast("AI 生成规则未通过应用范围或规则校验")
    }

    private suspend fun processEnhanced(task: AiGenerateTask) {
        val initial = task.requestedContext ?: return
        val beforeCapture = A11yRuntime.getRoot()
        if (beforeCapture == null || !initial.matches(beforeCapture)) {
            ToastUtils.toast("界面已变化，已取消增强任务")
            return
        }
        val snapshot = SnapshotCapture.capture(triggerAiRule = false)
        if (snapshot.appId != initial.appId || snapshot.activityId != initial.activityId) return
        val candidate = AiRuleCandidate(snapshot.appId)
        var status = "规则未通过自动验证，已保存供手动调整"
        try {
            val snapshotJson = snapshotText(snapshot.id)
            val prompt = loadPrompt()
            var content = prompt + "\n" + snapshotJson
            for (attempt in 0 until 2) {
                val response = withStableContext(initial) { callAiApi(task.config, content) }
                val generated = candidate.accept(response)
                if (generated == null) {
                    status = "重试规则无效，已保存最近一次有效规则供手动调整"
                    break
                }
                when (verifyEnhanced(initial, generated)) {
                    AiEnhancedVerification.Result.Verified -> {
                        status = "目标已不再命中，规则已保存"
                        break
                    }
                    AiEnhancedVerification.Result.ContextChanged -> {
                        status = "界面已变化，已停止自动验证并保存规则供手动调整"
                        break
                    }
                    AiEnhancedVerification.Result.Unmatched -> {
                        if (attempt == 0) content = prompt + "\n" + snapshotJson +
                            "\n上次规则未通过完整条件匹配或执行后目标仍在，请修正规则：\n" + response
                    }
                }
            }
        } catch (e: AiContextChanged) {
            status = "界面已变化，已停止自动验证并保存规则供手动调整"
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            status = "增强请求已超时，已保存最近一次有效规则供手动调整"
        } catch (_: Exception) {
            status = "增强验证失败，已保存最近一次有效规则供手动调整"
        }
        val generated = candidate.latest
        if (generated == null) {
            ToastUtils.toast("增强任务已结束，没有通过校验的规则")
        } else {
            insertRuleToSubscription(generated)
            ToastUtils.toast(status)
        }
    }

    private suspend fun verifyEnhanced(initial: AiScreenContext, generated: RawSubscription): AiEnhancedVerification.Result {
        val resolvedAt = ruleNow()
        val rules = AiEnhancedRules.resolve(generated, AppInfoRepository.appInfoMapFlow.value[initial.appId], resolvedAt)
        return withTimeout(30_000) {
            AiEnhancedVerification(
                getRoot = { A11yRuntime.getRoot() },
                matchesContext = initial::matches,
                sameWindow = initial::sameWindow,
                refresh = { it.refresh() },
                eligible = { AiEnhancedRules.eligible(it, initial.appId, initial.activityId) },
                query = { rule, root ->
                    A11yContext(getRoot = { A11yRuntime.getRoot() }, interruptable = false).queryRule(rule, root)
                },
                perform = { rule, target -> rule.performAction(target).result },
            ).run(rules, resolvedAt)
        }
    }

    private suspend fun <T> withStableContext(context: AiScreenContext, block: suspend () -> T): T = coroutineScope {
        val request = async { block() }
        val watcher = launch {
            while (request.isActive) {
                delay(1000)
                val root = A11yRuntime.getRoot()
                if (root == null || !context.matches(root)) {
                    request.cancel(AiContextChanged())
                    return@launch
                }
            }
        }
        try { request.await() } finally { watcher.cancel() }
    }

    private suspend fun callAiApi(config: AiConfig, userContent: String): String = withTimeout(180_000) {
        var budget = capabilities.initialBudget(config)
        var incompleteRetry = false
        var truncatedRetry = false
        repeat(8) {
            val effective = config.copy(thinkingRejected = capabilities.get(config).thinkingRejected)
            val reply = try {
                AiTransport.shared.post(effective, buildRequestBody(effective, userContent, budget, effective.streamChatCompletions))
            } catch (e: AiHttpFailure) {
                if (e.thinking && AiThinkingUtils.buildParams(effective, budget).isNotEmpty()) {
                    capabilities.rejectThinking(config)
                    return@repeat
                }
                if (e.tokenLimit && budget > 1) {
                    capabilities.rejectBudget(config, budget)
                    budget = AiTokenBudget.budgetForRejection(budget)
                    return@repeat
                }
                throw e
            }
            capabilities.acceptBudget(config, budget)
            if (reply.interrupted) {
                check(!incompleteRetry) { "AI response interrupted twice" }
                incompleteRetry = true
                return@repeat
            }
            if (reply.truncated) {
                check(!truncatedRetry) { "AI output remains truncated" }
                truncatedRetry = true
                budget = AiTokenBudget.budgetForTruncatedRetry(budget, capabilities.get(config).rejectedBudget)
                    ?: error("AI output exceeds model budget")
                return@repeat
            }
            check(reply.content.isNotBlank()) { "AI response has no text" }
            return@withTimeout reply.content
        }
        error("AI request exceeded retry limit")
    }

    private suspend fun insertRuleToSubscription(generated: RawSubscription): Int {
        var inserted = 0
        val updated = SubscriptionRepository.update(LOCAL_SUBS_ID) { current ->
            val next = AiRuleImport.merge(current, generated)
            val id = generated.apps.single().id
            inserted = next.apps.single { it.id == id }.groups.size -
                (current.apps.find { it.id == id }?.groups?.size ?: 0)
            next
        }
        check(updated && inserted > 0) { "AI rules were not saved" }
        return inserted
    }
}

private class AiContextChanged : CancellationException("AI screen context changed")

private data class AiScreenContext(
    val appId: String,
    val activityId: String?,
    val windowId: Int,
    val top: li.gkd.app.a11y.TopActivity,
    val nodes: List<NodeInfo>,
) {
    fun sameWindow(root: AccessibilityNodeInfo): Boolean = root.packageName?.toString() == appId &&
        root.windowId == windowId && currentTopActivity == top
    fun matches(root: AccessibilityNodeInfo): Boolean = sameWindow(root) && info2nodeList(root) == nodes
    companion object {
        fun capture(root: AccessibilityNodeInfo): AiScreenContext {
            val top = currentTopActivity
            return AiScreenContext(root.packageName.toString(), top.activityId, root.windowId, top, info2nodeList(root))
        }
    }
}

private val JsonElement?.jsonArrayOrNull: JsonArray? get() = this as? JsonArray
private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement?.intOrNullValue(): Int? = (this as? JsonPrimitive)?.intOrNull
private fun JsonElement?.boolOrNullValue(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
private fun JsonObject.intOf(key: String): Int? = this[key].intOrNullValue()
