package li.gkd.app.util

import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode

/**
 * AI 服务地址处理与配置预检：纯函数集合，不依赖 Android/应用状态，可直接跑 JVM 单元测试
 * （原先放在 AiRuleGenerator 里，其 object 初始化会拉起应用协程作用域，单测无法加载）。
 * AiRuleGenerator 保留同名委托，调用方无需改动。
 */
object AiApiUrls {

const val PROTOCOL_ANTHROPIC = "anthropic"

/**
 * 各家兼容接口的默认路径。用户只填域名（或少了版本段）时据此补全，
 * 避免一律补 `/v1` —— 智谱是 `/api/paas/v4`、火山是 `/api/v3`、千问是 `/compatible-mode/v1`。
 */
private class ApiProviderPreset(
    val host: String,
    val openAiPath: String,
    val anthropicPath: String? = null,
)

private val apiProviderPresets = listOf(
    ApiProviderPreset("open.bigmodel.cn", "/api/paas/v4", "/api/anthropic"),
    ApiProviderPreset("bigmodel.cn", "/api/paas/v4", "/api/anthropic"),
    ApiProviderPreset("api.z.ai", "/api/paas/v4", "/api/anthropic"),
    ApiProviderPreset("dashscope.aliyuncs.com", "/compatible-mode/v1"),
    ApiProviderPreset("dashscope-intl.aliyuncs.com", "/compatible-mode/v1"),
    ApiProviderPreset("ark.cn-beijing.volces.com", "/api/v3"),
    ApiProviderPreset("generativelanguage.googleapis.com", "/v1beta/openai"),
    ApiProviderPreset("api.deepseek.com", "/v1"),
    ApiProviderPreset("api.moonshot.cn", "/v1"),
    ApiProviderPreset("api.moonshot.ai", "/v1"),
    ApiProviderPreset("api.siliconflow.cn", "/v1"),
    ApiProviderPreset("api.siliconflow.com", "/v1"),
    ApiProviderPreset("openrouter.ai", "/api/v1"),
    ApiProviderPreset("api.groq.com", "/openai/v1"),
    ApiProviderPreset("api.mistral.ai", "/v1"),
    ApiProviderPreset("api.x.ai", "/v1"),
    ApiProviderPreset("api.minimaxi.com", "/v1"),
    ApiProviderPreset("api.minimax.chat", "/v1"),
    ApiProviderPreset("api.anthropic.com", "/v1", "/v1"),
    ApiProviderPreset("api.openai.com", "/v1"),
)

/** 路径里的版本段：`/v1`、`/api/v3`、`/api/paas/v4`、`/v1beta`、`/compatible-mode/v1` */
private val apiVersionSegment = Regex("""(?:^|/)v\d+(?:beta|alpha)?(?=/|$)""", RegexOption.IGNORE_CASE)

/** 完整的聊天端点后缀；出现这些后缀时不再拼接 */
private val endpointSuffixes = listOf("/chat/completions", "/messages", "/completions", "/responses")

private val apiUrlScheme = Regex("""^https?:/*""", RegexOption.IGNORE_CASE)

/** 粘贴地址时常见的全角符号与包裹字符 */
private fun cleanApiUrlInput(raw: String): String = raw
    .replace("\uFEFF", "")
    .replace("\u200B", "")
    .filterNot { it.isWhitespace() }
    .replace('：', ':')
    .replace('／', '/')
    .replace('．', '.')
    // 不能 trim 方括号：IPv6 的 [::1]:8080 会被破坏
    .trim('"', '\'', '`', '“', '”', '‘', '’', '<', '>', ',', '，', '、', '(', ')', '（', '）')
    .trimEnd('/', '.', '。', ',', '，', ';', '；', '、')

fun apiHostOf(url: String): String {
    val rest = url.substringAfter("://", url).trimStart('/')
    // authority 只到路径/查询/锚点为止，`host?key=abc` 这种写法也要能取到域名
    val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
    return if (authority.startsWith("[")) {
        authority.substringBefore("]") + "]"
    } else {
        authority.substringBefore(':').lowercase()
    }
}

/** 本机 / 局域网的地址默认用 http，其余默认 https */
private fun isLocalApiHost(host: String): Boolean {
    val name = host.trim('[', ']').lowercase()
    if (name == "localhost" || name == "::1" || name == "0.0.0.0") return true
    if (name.endsWith(".local") || name.endsWith(".lan")) return true
    val octets = name.split('.')
    if (octets.size != 4) return false
    val parts = octets.map { it.toIntOrNull() ?: return false }
    return parts[0] == 10 || parts[0] == 127 ||
        (parts[0] == 192 && parts[1] == 168) ||
        (parts[0] == 172 && parts[1] in 16..31)
}

private fun findProviderPreset(host: String): ApiProviderPreset? = apiProviderPresets
    .filter { host == it.host || host.endsWith(".${it.host}") }
    .maxByOrNull { it.host.length }

/** Azure OpenAI 走 `/openai/deployments/<部署名>/...`，路径不能按通用规则补 `/v1` */
fun isAzureApiHost(host: String): Boolean =
    host.endsWith("openai.azure.com") || host.endsWith("openai.azure.cn")

private fun providerDefaultPath(host: String, protocol: String): String? {
    val preset = findProviderPreset(host) ?: return null
    return if (protocol == PROTOCOL_ANTHROPIC) preset.anthropicPath else preset.openAiPath
}

private fun isFullApiEndpoint(path: String): Boolean = endpointSuffixes.any { path.endsWith(it) }

/** 路径与查询串分开，拼接端点时要把 `?api-version=` 这类参数保留在后面 */
private fun String.splitQuery(): Pair<String, String> {
    val index = indexOfFirst { it == '?' || it == '#' }
    return if (index < 0) this to "" else substring(0, index) to substring(index)
}

/** 只合并完全相同的相邻版本段（`/v1/v1`），`/v3/v1` 这种有意义的不同版本保持原样 */
private fun collapseRepeatedVersionSegments(path: String): String {
    if (path.isEmpty()) return path
    val result = ArrayList<String>()
    path.split('/').forEach { segment ->
        val previous = result.lastOrNull()
        val duplicated = previous != null &&
            previous.equals(segment, ignoreCase = true) &&
            apiVersionSegment.matches(segment)
        if (!duplicated) result += segment
    }
    return result.joinToString("/")
}

/** 选了 anthropic 协议，但地址其实是服务方的 OpenAI 路径（或该服务方根本没有 Anthropic 端点） */
private fun anthropicProtocolHint(url: String, protocol: String): String? {
    if (protocol != PROTOCOL_ANTHROPIC) return null
    val host = apiHostOf(url)
    val preset = findProviderPreset(host) ?: return null
    val anthropicPath = preset.anthropicPath
        ?: return "${preset.host} 没有 Anthropic 兼容端点，请把协议切换为 openai"
    if (anthropicPath == preset.openAiPath) return null
    if (url.substringAfter("://").contains(preset.openAiPath)) {
        return "该地址是 OpenAI 兼容路径，Anthropic 协议应填 https://${preset.host}$anthropicPath"
    }
    return null
}

    /**
     * 把用户输入整理成可用的 API 地址：补 scheme、去多余斜杠、去重复版本段，
     * 并按服务方补默认路径（智谱 `/api/paas/v4`、火山 `/api/v3` 等）。
     * 已经粘了完整端点（`/chat/completions`、`/messages`）时只做清洗，不再改动路径。
     * Azure 的 deployment 路径原样保留，补 `/v1` 反而会 404。
     */
    fun fixApiUrl(url: String, protocol: String): String {
        val cleaned = cleanApiUrlInput(url)
        if (cleaned.isEmpty()) return ""
        val hasScheme = apiUrlScheme.containsMatchIn(cleaned)
        val explicitScheme = when {
            cleaned.startsWith("https:", ignoreCase = true) -> "https"
            cleaned.startsWith("http:", ignoreCase = true) -> "http"
            else -> null
        }
        val rest = if (hasScheme) {
            apiUrlScheme.replaceFirst(cleaned, "").trimStart('/')
        } else {
            cleaned.trimStart('/')
        }
        if (rest.isEmpty()) return ""
        // 先把查询串/锚点从地址里切出来：`host?key=abc` 这种没有路径的写法
        // 否则整串会被当成 authority，域名解析和 Azure 判断都会失效
        val queryIndex = rest.indexOfFirst { it == '?' || it == '#' }
        val address = if (queryIndex < 0) rest else rest.substring(0, queryIndex)
        val tail = if (queryIndex < 0) "" else rest.substring(queryIndex)
        val authority = address.substringBefore('/')
        if (authority.isEmpty()) return ""
        val host = apiHostOf("https://$authority")
        val scheme = explicitScheme ?: if (isLocalApiHost(host)) "http" else "https"

        val pathOnly = address.substringAfter('/', "")
        val collapsed = if (pathOnly.isEmpty()) {
            ""
        } else {
            "/" + pathOnly.split('/').filter { it.isNotEmpty() }.joinToString("/")
        }
        val deduped = collapseRepeatedVersionSegments(collapsed)
        val azure = isAzureApiHost(host)

        val defaultPath = providerDefaultPath(host, protocol)
        val path = when {
            // Azure 的 deployment 路径原样保留，补 /v1 反而会 404
            azure -> deduped
            deduped.isEmpty() -> defaultPath ?: "/v1"
            defaultPath != null && !apiVersionSegment.containsMatchIn(deduped) &&
                (deduped == defaultPath || defaultPath.startsWith("$deduped/")) -> defaultPath
            else -> deduped
        }
        val finalPath = when {
            azure -> path
            isFullApiEndpoint(path) -> path
            apiVersionSegment.containsMatchIn(path) -> path
            else -> "$path/v1"
        }
        return "$scheme://$authority$finalPath$tail"
    }

    /**
     * 实际会请求的地址；已是完整端点就原样返回。
     * 查询串（如 Azure 的 api-version）保留在拼接的端点后缀之后。
     */
    fun resolveEndpoint(
        apiUrl: String,
        protocol: String,
        endpointMode: String = AiEndpointMode.CHAT,
    ): String {
        val baseUrl = fixApiUrl(apiUrl, protocol)
        if (baseUrl.isEmpty()) return baseUrl
        val (path, query) = baseUrl.splitQuery()
        if (isFullApiEndpoint(path)) return baseUrl
        val suffix = when {
            protocol == PROTOCOL_ANTHROPIC -> "/messages"
            endpointMode == AiEndpointMode.RESPONSES -> "/responses"
            else -> "/chat/completions"
        }
        return "$path$suffix$query"
    }

    /** 模型列表接口；用户粘完整端点时先退回到它的 base。Azure 走 `/openai/models`。 */
    fun resolveModelsEndpoint(apiUrl: String, protocol: String): String {
        val (path, query) = fixApiUrl(apiUrl, protocol).splitQuery()
        val host = apiHostOf(path)
        if (isAzureApiHost(host)) {
            val scheme = path.substringBefore("://", "https")
            val authority = path.substringAfter("://", path).substringBefore('/')
            return "$scheme://$authority/openai/models$query"
        }
        val modelsBase = endpointSuffixes.fold(path) { acc, suffix -> acc.removeSuffix(suffix) }
        return "$modelsBase/models$query"
    }

    fun buildApiEndpoint(config: AiConfig): String =
        resolveEndpoint(config.apiUrl, config.protocol, config.endpointMode)

    /** 公网明文 http 的提示文案；[validateAiConfig] 会返回它，保存/测试时不当作硬错误拦截。 */
    const val PUBLIC_HTTP_HINT = "API 地址建议使用 https（公网明文 http 可能被服务方拒绝）"

    /**
     * 常见配置错误的本地预检，返回错误提示；null 表示通过。
     * 地址的格式问题（缺 http、多余斜杠、版本段重复）已经由 [fixApiUrl] 自动修正，这里只提示改不了的部分。
     * [requireModel] 为 false 时不查模型名（新建服务商还没拉模型列表时用）。
     */
    fun validateAiConfig(config: AiConfig, requireModel: Boolean = true): String? {
        val raw = config.apiUrl.trim()
        val url = fixApiUrl(config.apiUrl, config.protocol)
        val endpoint = resolveEndpoint(config.apiUrl, config.protocol, config.endpointMode)
        val protocolHint = anthropicProtocolHint(url, config.protocol)
        val endpointPath = endpoint.splitQuery().first
        val azure = isAzureApiHost(apiHostOf(url))
        val key = config.apiKey
        return when {
            raw.isEmpty() -> "请填写 API 地址"
            url.isEmpty() || !url.startsWith("http") -> "API 地址无效，请检查域名和端口"
            url.startsWith("http://") && !isLocalApiHost(apiHostOf(url)) -> PUBLIC_HTTP_HINT
            // Azure 的路径是 /openai/deployments/<部署名>/chat/completions?api-version=...
            azure && (!endpointPath.contains("/openai/deployments/") ||
                !endpointPath.endsWith("/chat/completions")) ->
                "Azure OpenAI 地址需为 https://<资源名>.openai.azure.com/openai/deployments/<部署名>/chat/completions"
            azure && !endpoint.contains("api-version=") ->
                "Azure OpenAI 地址需带 ?api-version=2024-xx-xx"
            endpointPath.endsWith("/responses") && config.endpointMode != AiEndpointMode.RESPONSES ->
                "该地址是 /responses 端点，请把端点模式切换为 Responses API"
            apiHostOf(url).endsWith("bigmodel.cn") && url.contains("/api/paas/v3") ->
                "智谱 OpenAI 兼容接口是 /api/paas/v4，/v3 是旧版非兼容接口"
            config.protocol != PROTOCOL_ANTHROPIC && url.contains("/anthropic", ignoreCase = true) ->
                "该地址是 Anthropic 兼容端点，请把协议切换为 anthropic"
            protocolHint != null -> protocolHint
            config.protocol == PROTOCOL_ANTHROPIC && endpointPath.endsWith("/chat/completions") ->
                "该地址是 OpenAI 兼容端点，请把协议切换为 openai"
            key.contains(' ') -> "API Key 中含空格，绝大多数服务方 Key 不含空格"
            requireModel && config.model.isBlank() -> "模型名称为空"
            else -> null
        }
    }
}
