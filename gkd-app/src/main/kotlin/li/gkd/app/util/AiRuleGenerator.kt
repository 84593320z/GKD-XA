package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
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
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.data.ActionPerformer
import li.gkd.app.data.GkdAction
import li.gkd.app.data.NodeInfo
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.info2nodeList
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import li.gkd.app.data.settings.AiModel
import li.gkd.app.data.snapshot.SnapshotRepository
import li.gkd.app.data.subscription.SubscriptionRepository
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.store.AppStore.updateSettings
import li.gkd.app.text.UiStrings
import li.gkd.db.LOCAL_SUBS_ID
import li.gkd.selector.MatchOptions
import li.gkd.selector.Selector
import li.gkd.selector.SelectorCompileResult
import java.util.concurrent.TimeUnit

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

    val isGenerating = MutableStateFlow(false)
    val pendingCount = MutableStateFlow(0)

    private val taskQueue = Channel<Long>(Channel.UNLIMITED)

    init {
        appScope.launchLogged {
            for (snapshotId in taskQueue) {
                processRule(snapshotId)
            }
        }
    }

    private var cachedPrompt: String? = null

    private suspend fun loadPrompt(): String = withContext(Dispatchers.IO) {
        cachedPrompt?.let { return@withContext it }
        val text = app.assets.open("gkd-rule-generator-prompt.md").bufferedReader().readText()
        cachedPrompt = text
        text
    }

    fun fixApiUrl(url: String, protocol: String): String {
        var fixed = url.trim().trimEnd('/')
        // If URL already contains a full path, don't modify
        if (fixed.contains("/chat/completions") || fixed.contains("/messages")) {
            return fixed
        }
        // If URL already ends with /v1, keep it
        if (fixed.endsWith("/v1")) {
            return fixed
        }
        // If URL already contains /v1 somewhere, don't add it again
        if (fixed.contains("/v1/")) {
            return fixed
        }
        return "$fixed/v1"
    }

    private fun buildApiEndpoint(config: AiConfig): String {
        val baseUrl = fixApiUrl(config.apiUrl, config.protocol)
        return when (config.protocol) {
            "anthropic" -> "$baseUrl/messages"
            else -> if (config.endpointMode == AiEndpointMode.RESPONSES) {
                "$baseUrl/responses"
            } else {
                "$baseUrl/chat/completions"
            }
        }
    }

    /** 认证头最后写入，保证 API Key 不被自定义请求头覆盖。 */
    private fun HttpRequestBuilder.applyAiHeaders(config: AiConfig) {
        config.headers.forEach { header ->
            val name = header.name.trim()
            if (name.isNotEmpty()) header(name, header.value)
        }
        when (config.protocol) {
            "anthropic" -> {
                header("x-api-key", config.apiKey)
                header("anthropic-version", config.anthropicVersion.ifBlank { AiConfig.DEFAULT_ANTHROPIC_VERSION })
            }
            else -> header("Authorization", "Bearer ${config.apiKey}")
        }
    }

    private fun createHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(0, TimeUnit.SECONDS) // no timeout for AI response
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
        }
    }

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
        return when {
            config.isAnthropic -> json.encodeToString(
                AnthropicRequest(
                    model = config.model,
                    messages = listOf(AnthropicMessage("user", content)),
                    temperature = config.temperature,
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
    }

    /** 只做一次 max_tokens=1 的最小对话，返回错误摘要；无错误时返回 "ok"。 */
    suspend fun testConnection(config: AiConfig): Result<String> = runCatching {
        val httpClient = createHttpClient()
        try {
            val response = httpClient.post(buildApiEndpoint(config)) {
                applyAiHeaders(config)
                contentType(ContentType.Application.Json)
                setBody(buildRequestBody(config, "hi", maxTokensOverride = 1))
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw Exception("HTTP ${response.status.value} ${errorHint(body)}".trim())
            }
            errorHint(body).takeIf { it.isNotEmpty() }?.let { throw Exception(it) }
            "ok"
        } finally {
            httpClient.close()
        }
    }

    private fun errorHint(body: String): String {
        val error = runCatching { json.parseToJsonElement(body).jsonObject["error"] }.getOrNull()
            ?: return ""
        val message = runCatching { error.jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
            ?: runCatching { error.jsonPrimitive.content }.getOrNull()
            ?: return ""
        return message.trim().take(200)
    }

    /** 拉取远端模型：各家字段名不统一，能解析出上下文长度与推理能力就一并记录。 */
    suspend fun fetchModels(config: AiConfig): Result<List<AiModel>> = runCatching {
        val baseUrl = fixApiUrl(config.apiUrl, config.protocol)
        val httpClient = createHttpClient()
        try {
            val response = httpClient.get("$baseUrl/models") {
                applyAiHeaders(config)
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw Exception("HTTP ${response.status.value} ${errorHint(body)}".trim())
            }
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
        } finally {
            httpClient.close()
        }
    }

    suspend fun generateRule(snapshotId: Long) {
        val config = storeFlow.value.activeAiProvider()
        if (config == null || !config.usable) {
            ToastUtils.toast("请先在 AI 设置中选择并配置服务商")
            return
        }
        pendingCount.value++
        taskQueue.send(snapshotId)
        if (isGenerating.value) {
            ToastUtils.toast("AI 任务已加入队列（排队中：${pendingCount.value}）")
        }
    }

    private suspend fun processRule(snapshotId: Long) {
        val config = storeFlow.value.activeAiProvider()
            ?: run {
                ToastUtils.toast("请先在 AI 设置中选择并配置服务商")
                return
            }
        isGenerating.value = true
        try {
            ToastUtils.toast("AI 正在生成规则...", forced = true)
            val prompt = loadPrompt()
            val snapshotJson = withContext(Dispatchers.IO) {
                SnapshotRepository.snapshotFile(snapshotId).readText()
            }
            val userContent = "$prompt\n$snapshotJson"
            LogUtils.d("AI generateRule: prompt length=${prompt.length}, snapshot length=${snapshotJson.length}, total=${userContent.length}")

            val result = callAiApi(config, userContent)
            LogUtils.d("AI extracted content (first 2000 chars): ${result.take(2000)}")
            val ruleText = extractContent(result)
            LogUtils.d("AI after extractContent (first 2000 chars): ${ruleText.take(2000)}")

            val currentRule = parseAndValidateRule(ruleText)
            if (currentRule == null) {
                ToastUtils.toast("AI 生成的规则 JSON 不合法，重试中...")
                val retryResult = callAiApi(config, userContent)
                val retryText = extractContent(retryResult)
                val retryParsed = parseAndValidateRule(retryText)
                if (retryParsed == null) {
                    ToastUtils.toast("AI 生成规则失败：JSON 解析错误")
                    return
                }
                insertRuleToSubscription(retryParsed)
            } else {
                insertRuleToSubscription(currentRule)
            }
            ToastUtils.toast("AI 规则生成成功并已添加到本地规则")
        } catch (e: Exception) {
            ToastUtils.toast("AI 生成规则失败：${e.message}")
            LogUtils.d("AI rule generation failed", e)
        } finally {
            pendingCount.value--
            isGenerating.value = pendingCount.value > 0
        }
    }

    /**
     * 加强模式：双击快照按钮触发
     * 1. 捕获快照 → AI 生成规则
     * 2. 执行规则点击 → 等待界面变化 → 再次捕获节点树
     * 3. 对比两次节点树判断规则是否生效
     * 4. 未生效则将失败规则加入 prompt，让 AI 重新生成
     */
    suspend fun enhancedGenerate() {
        if (!storeFlow.value.aiEnable) {
            ToastUtils.toast("请先启用 AI 规则")
            return
        }
        val config = storeFlow.value.activeAiProvider()
        if (config == null || !config.usable) {
            ToastUtils.toast("请先在 AI 设置中选择并配置服务商")
            return
        }
        if (isGenerating.value) {
            ToastUtils.toast("AI 正在生成中，请稍后再试")
            return
        }
        isGenerating.value = true
        // 异常路径原来只 toast，已生成好的规则直接丢了；留一份引用给 catch 用。
        var latestRule: RawSubscription? = null
        try {
            // Step 1: 捕获快照
            val snapshot = SnapshotCapture.capture()
            val snapshotJson = withContext(Dispatchers.IO) {
                SnapshotRepository.snapshotFile(snapshot.id).readText()
            }

            // Step 2: AI 生成规则
            ToastUtils.toast("加强模式：AI 正在生成规则...\n 请勿离开当前界面", forced = true)
            val prompt = loadPrompt()
            val userContent = "$prompt\n$snapshotJson"
            val result = callAiApi(config, userContent)
            val ruleText = extractContent(result)
            var currentRule = parseAndValidateRule(ruleText) ?: run {
                ToastUtils.toast("AI 生成规则失败：JSON 解析错误")
                return
            }

            // Step 3: 尝试执行规则并验证
            var lastRuleText = ruleText
            var matchedSelectorText: String? = null
            val maxRetries = 2
            for (attempt in 0 until maxRetries) {
                latestRule = currentRule
                val actions = extractActions(currentRule)
                if (actions.isEmpty()) {
                    ToastUtils.toast("加强模式：未找到有效选择器")
                    insertRuleToSubscription(currentRule)
                    return
                }

                // 获取当前节点树，尝试匹配并执行
                val rootNode = A11yRuntime.getRoot()
                if (rootNode == null) {
                    ToastUtils.toast("无法获取当前界面节点")
                    insertRuleToSubscription(currentRule)
                    return
                }

                // 记录执行前的节点树特征
                val beforeNodes = info2nodeList(rootNode)
                val beforeSignature = snapshotSignature(beforeNodes)

                // 尝试用生成规则匹配并执行动作
                var actionExecuted = false
                var unmatchedSelector: String? = null
                var anyFastQueryable = false
                val a11yContext = A11yContext(getRoot = { A11yRuntime.getRoot() }, interruptable = false)
                for (action in actions) {
                    val selector = (Selector.compile(action.selector) as? SelectorCompileResult.Success)?.value
                        ?: continue
                    val targetNode = a11yContext.querySelfOrSelector(
                        rootNode, selector, MatchOptions(fastQuery = action.fastQuery)
                    )
                    if (targetNode != null) {
                        val actionResult = ActionPerformer
                            .getAction(action.action ?: ActionPerformer.Click.action)
                            .perform(targetNode, action)
                        if (actionResult.result) {
                            actionExecuted = true
                            matchedSelectorText = action.selector
                            LogUtils.d("加强模式：执行动作成功, action=${action.action}, selector=${action.selector}")
                            break
                        }
                    } else {
                        unmatchedSelector = action.selector
                        anyFastQueryable = anyFastQueryable || !selector.fastQueryList.isEmpty()
                    }
                }

                if (!actionExecuted) {
                    val noFastQuery = unmatchedSelector != null && !anyFastQueryable
                    LogUtils.d("加强模式：选择器未匹配到任何节点，attempt=$attempt, 可快速匹配=$anyFastQueryable, selector=$unmatchedSelector")
                    if (attempt == maxRetries - 1) {
                        ToastUtils.toast("加强模式：规则未能匹配节点，已保存供手动调整")
                        insertRuleToSubscription(currentRule)
                        return
                    }
                    // 未匹配到，重新生成
                    ToastUtils.toast("加强模式：规则未匹配，正在重新生成...")
                    val retryContent = buildRetryPrompt(
                        prompt, snapshotJson, lastRuleText,
                        if (noFastQuery) UiStrings.selector_no_fast_query_retry else "选择器未匹配到任何节点",
                    )
                    val retryResult = callAiApi(config, retryContent)
                    val retryRuleText = extractContent(retryResult)
                    currentRule = parseAndValidateRule(retryRuleText) ?: run {
                        ToastUtils.toast("加强模式：重试失败")
                        return
                    }
                    lastRuleText = retryRuleText
                    continue
                }

                // Step 4: 等待界面变化
                delay(1500)

                // 再次捕获节点树（不保存）
                val afterRootNode = A11yRuntime.getRoot()
                if (afterRootNode == null) {
                    // 界面消失，规则很可能生效了（弹窗关闭）
                    ToastUtils.toast("加强模式：规则验证成功（界面已变化）")
                    insertRuleToSubscription(currentRule)
                    return
                }
                val afterNodes = info2nodeList(afterRootNode)
                val afterSignature = snapshotSignature(afterNodes)

                // Step 5: 判据先看「刚才命中的那个选择器还在不在」，界面签名只作辅助。
                // 原来只比签名：误点了别的元素、页面滚动、跳到别的界面都会让签名变化，
                // 于是「点错了」被判成「规则生效」—— 而这正是加强模式存在的意义所在。
                val selectorGone = matchedSelectorText?.let { text ->
                    val compiled = (Selector.compile(text) as? SelectorCompileResult.Success)?.value
                    compiled == null || a11yContext.querySelfOrSelector(
                        afterRootNode, compiled, MatchOptions(),
                    ) == null
                } ?: false
                if (selectorGone) {
                    ToastUtils.toast("加强模式：规则验证成功（目标已不再命中）")
                    insertRuleToSubscription(currentRule)
                    return
                }
                if (beforeSignature != afterSignature) {
                    LogUtils.d("加强模式：界面有变化但目标仍在，判为未生效, selector=$matchedSelectorText")
                }

                // 界面未变化，规则可能未生效
                if (attempt < maxRetries - 1) {
                    ToastUtils.toast("加强模式：规则未生效，正在重新生成...")
                    val retryContent = buildRetryPrompt(prompt, snapshotJson, lastRuleText, "点击执行后界面未发生变化，规则可能不正确")
                    val retryResult = callAiApi(config, retryContent)
                    val retryRuleText = extractContent(retryResult)
                    currentRule = parseAndValidateRule(retryRuleText) ?: run {
                        ToastUtils.toast("加强模式：重试解析失败，保存当前规则")
                        return
                    }
                    lastRuleText = retryRuleText
                } else {
                    ToastUtils.toast("加强模式：多次尝试后规则仍未生效，已保存最新版本")
                    insertRuleToSubscription(currentRule)
                }
            }
        } catch (e: Exception) {
            // 取节点、执行动作都可能抛异常；别把已经生成好的规则一起丢掉。
            // runCatching 是为了让保存失败不掩盖原始异常。
            latestRule?.let { runCatching { insertRuleToSubscription(it) } }
            ToastUtils.toast("加强模式失败：${e.message}")
            LogUtils.d("Enhanced generate failed", e)
        } finally {
            isGenerating.value = false
        }
    }

    private fun extractActions(subs: RawSubscription): List<GkdAction> {
        return subs.apps.flatMap { app ->
            app.groups.flatMap { group ->
                group.rules.flatMap { rule ->
                    rule.matches.orEmpty().map { selector ->
                        GkdAction(
                            selector = selector,
                            fastQuery = rule.fastQuery ?: group.fastQuery ?: false,
                            action = rule.action,
                            position = rule.position,
                            swipeArg = rule.swipeArg,
                        )
                    }
                }
            }
        }
    }

    private fun snapshotSignature(nodes: List<NodeInfo>): String {
        // 用节点数量 + 可见节点的 text/desc 拼接作为简单签名
        val visibleTexts = nodes.mapNotNull { node ->
            val text = node.attr.text ?: node.attr.desc
            if (node.attr.visibleToUser && text != null) text else null
        }.sorted()
        return "${nodes.size}:${visibleTexts.hashCode()}"
    }

    private fun buildRetryPrompt(
        originalPrompt: String,
        snapshotJson: String,
        previousRule: String,
        failureReason: String,
    ): String {
        return """$originalPrompt

$snapshotJson

---
注意：上一次生成的规则未能生效。
失败原因：$failureReason
上次生成的规则（请勿再使用相同的选择器）：
$previousRule

请分析失败原因，重新选择更准确的目标节点和选择器。"""
    }

    /** 该服务商记下的上限：以 store 里的最新值为准，取不到才用调用方传进来的 config 副本。 */
    private fun ceilingOf(config: AiConfig): Int? =
        storeFlow.value.aiProviders.firstOrNull { it.id == config.id }?.maxTokenCeiling
            ?: config.maxTokenCeiling

    /** 记住这道墙，返回收窄后的上限；写盘走服务商自己的 id，多个服务商互不影响。 */
    private fun recordCeiling(config: AiConfig, rejectedBudget: Int): Int {
        val newCeiling = AiTokenBudget.narrowedCeiling(ceilingOf(config), rejectedBudget)
        if (ceilingOf(config) == newCeiling) return newCeiling
        updateSettings { store ->
            store.copy(
                aiProviders = store.aiProviders.map {
                    if (it.id == config.id) it.copy(maxTokenCeiling = newCeiling) else it
                },
            )
        }
        return newCeiling
    }

    /**
     * 一次 AI 调用：先按自适应预算发请求，
     * 截断（finishReason == length）就翻倍重试一次，被服务端拒绝就记下上限并回退重试一次。
     */
    private suspend fun callAiApi(config: AiConfig, userContent: String): String {
        val budget = AiTokenBudget.budgetForRequest(config.maxTokens, ceilingOf(config))
        LogUtils.d("AI max tokens budget: configured=${config.maxTokens}, ceiling=${ceilingOf(config)}, use=$budget")
        return try {
            val first = postAiRequest(config, userContent, budget)
            if (!first.truncated) {
                return first.content
            }
            val retryBudget = AiTokenBudget.budgetForTruncatedRetry(budget, ceilingOf(config))
            if (retryBudget == null) {
                LogUtils.d("AI 输出被截断，但已达该服务商已知上限（ceiling=${ceilingOf(config)}），不再翻倍重试")
                first.content
            } else {
                ToastUtils.toast(UiStrings.ai_max_tokens_retry(retryBudget))
                postAiRequest(config, userContent, retryBudget).content
            }
        } catch (e: AiTokenLimitReject) {
            val newCeiling = recordCeiling(config, e.budget)
            val fallbackBudget = AiTokenBudget.budgetForRejection(newCeiling)
            ToastUtils.toast(UiStrings.ai_max_tokens_rejected(e.budget, fallbackBudget))
            postAiRequest(config, userContent, fallbackBudget).content
        }
    }

    private suspend fun postAiRequest(config: AiConfig, userContent: String, budget: Int): AiReply {
        val endpoint = buildApiEndpoint(config)
        val stream = config.streamChatCompletions
        val requestBody = buildRequestBody(config, userContent, maxTokensOverride = budget, stream = stream)
        LogUtils.d("AI Request endpoint: $endpoint")
        LogUtils.d("AI Request body (first 2000 chars): ${requestBody.take(2000)}")
        val httpClient = createHttpClient()
        try {
            val response = httpClient.post(endpoint) {
                applyAiHeaders(config)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                LogUtils.d("AI Response (first 3000 chars): ${body.take(3000)}")
                val hint = errorHint(body)
                val message = "HTTP ${response.status.value} $hint".trim()
                // 只有「max_tokens 过大 / 参数非法」这一类错误才触发回退，其它错误维持原样抛出
                if (AiTokenBudget.isTokenLimitRejection(hint.ifBlank { body })) {
                    throw AiTokenLimitReject(budget, message)
                }
                throw Exception(message)
            }
            // 服务端可能忽略 stream 参数：content-type 不是 event-stream 就原样走非流式解析
            val eventStream = stream &&
                    response.headers[HttpHeaders.ContentType]?.contains(SSE_CONTENT_TYPE, ignoreCase = true) == true
            if (eventStream) {
                return readSseReply(response.bodyAsChannel())
            }
            val body = response.bodyAsText()
            LogUtils.d("AI Response (first 3000 chars): ${body.take(3000)}")
            val jsonElement = json.parseToJsonElement(body)

            val content = when {
                config.isAnthropic -> {
                    val contentArr = jsonElement.jsonObject["content"]
                        ?: throw Exception(errorHint(body).ifBlank { "Anthropic 响应缺少 content" })
                    contentArr.jsonArray.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                        ?: throw Exception("No text in Anthropic content")
                }
                config.endpointMode == AiEndpointMode.RESPONSES -> {
                    // Responses 的正文在 output[] 的 message 项里，逐段拼 output_text
                    val text = jsonElement.jsonObject["output"].jsonArrayOrNull
                        ?.flatMap { item ->
                            val msg = item.objectOrNull() ?: return@flatMap emptyList()
                            msg["content"].jsonArrayOrNull.orEmpty().mapNotNull { part ->
                                val obj = part.objectOrNull() ?: return@mapNotNull null
                                obj["output_text"]?.jsonPrimitive?.content
                                    ?: obj["text"]?.jsonPrimitive?.content
                                        .takeIf { obj["type"]?.jsonPrimitive?.content == "text" }
                            }
                        }?.joinToString("\n").orEmpty()
                    text.ifBlank { errorHint(body) }.takeIf { it.isNotBlank() }
                        ?: throw Exception("Responses 响应中没有文本内容")
                }
                else -> {
                    val choices = jsonElement.jsonObject["choices"]
                        ?: throw Exception("No choices in OpenAI response")
                    choices.jsonArray.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                        ?: throw Exception("No content in OpenAI response")
                }
            }
            // 非流式路径不读 finish_reason，预算自适应在这里只靠服务端拒绝那一半
            return AiReply(content, truncated = false)
        } finally {
            httpClient.close()
        }
    }

    /** 边收边攒 SSE 行，收到 [DONE] 就收尾，不用等连接关闭。 */
    private suspend fun readSseReply(channel: ByteReadChannel): AiReply {
        val lines = mutableListOf<String>()
        while (true) {
            val line = channel.readLine() ?: break
            lines.add(line)
            if (isSseDoneLine(line)) break
        }
        val result = SseStreamParser.parse(lines)
        LogUtils.d("AI SSE Response: chars=${result.content.length}, finishReason=${result.finishReason}, done=${result.done}")
        return AiReply(result.content, result.isTruncated)
    }

    private fun isSseDoneLine(line: String): Boolean {
        val trimmed = line.trimEnd('\r').trim()
        return trimmed.startsWith("data:") && trimmed.substring(5).trim() == SseStreamParser.DONE_MARKER
    }

    private fun extractContent(text: String): String {
        var content = text.trim()
        // Try to extract JSON from markdown code block anywhere in the text
        val codeBlockRegex = Regex("```(?:json5?)?\\s*\\n?([\\s\\S]*?)\\n?```")
        val match = codeBlockRegex.find(content)
        if (match != null) {
            content = match.groupValues[1].trim()
        } else {
            // Remove markdown code block markers at start/end
            if (content.startsWith("```json5") || content.startsWith("```json") || content.startsWith("```")) {
                content = content.removePrefix("```json5").removePrefix("```json").removePrefix("```")
            }
            if (content.endsWith("```")) {
                content = content.removeSuffix("```")
            }
            content = content.trim()
        }
        // If content doesn't start with '{', try to find the first '{' and last '}'
        if (!content.startsWith("{")) {
            val firstBrace = content.indexOf('{')
            val lastBrace = content.lastIndexOf('}')
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                content = content.substring(firstBrace, lastBrace + 1)
            }
        }
        return content
    }

    private fun parseAndValidateRule(text: String): RawSubscription? {
        // First try parsing as full subscription
        try {
            return RawSubscription.parse(text, json5 = true)
        } catch (_: Exception) {}
        try {
            return RawSubscription.parse(text, json5 = false)
        } catch (_: Exception) {}

        // AI typically outputs an App-level JSON (id=packageName, groups=[...])
        // Wrap it into a subscription structure
        try {
            val jsonElement = json.parseToJsonElement(text)
            val app = RawSubscription.parseApp(jsonElement.jsonObject)
            LogUtils.d("AI parsed as RawApp: id=${app.id}, groups=${app.groups.size}")
            // Build a minimal subscription containing this app
            return RawSubscription(
                id = -1L,
                name = "AI Generated",
                version = 1,
                apps = listOf(app),
            )
        } catch (e: Exception) {
            LogUtils.d("AI parse as app failed: ${e.message}")
            LogUtils.d("AI failed text (first 500 chars): ${text.take(500)}")
            return null
        }
    }

    private suspend fun insertRuleToSubscription(newSubs: RawSubscription) {
        val newApp = newSubs.apps.firstOrNull() ?: return
        SubscriptionRepository.update(LOCAL_SUBS_ID) { currentSubs ->
            val existingApp = currentSubs.apps.find { it.id == newApp.id }

            // Reassign group keys to avoid conflicts with existing groups
            val existingKeys = existingApp?.groups?.map { it.key }?.toSet() ?: emptySet()
            var nextKey = (existingKeys.maxOrNull() ?: -1) + 1
            val fixedGroups = newApp.groups.map { group ->
                val newKey = nextKey++
                group.copy(key = newKey)
            }
            val fixedApp = newApp.copy(groups = fixedGroups)

            if (existingApp != null) {
                val updatedApp = existingApp.copy(groups = existingApp.groups + fixedGroups)
                currentSubs.copy(
                    apps = currentSubs.apps.map { if (it.id == updatedApp.id) updatedApp else it }
                )
            } else {
                currentSubs.copy(
                    apps = (currentSubs.apps + fixedApp).filterIfNotAll { it.groups.isNotEmpty() }
                )
            }
        }
    }
}

/** 一次请求的正文与「是否被 max_tokens 截断」的结论。 */
private class AiReply(val content: String, val truncated: Boolean)

/** 服务端判定本次的 max_tokens 过大 / 非法：带上那个值才能记住上限并回退。 */
private class AiTokenLimitReject(val budget: Int, message: String) : Exception(message)

private val SSE_CONTENT_TYPE = "text/event-stream"

private val JsonElement?.jsonArrayOrNull: JsonArray? get() = this as? JsonArray

private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.intOrNullValue(): Int? = (this as? JsonPrimitive)?.intOrNull

private fun JsonElement?.boolOrNullValue(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

private fun JsonObject.intOf(key: String): Int? = this[key].intOrNullValue()
