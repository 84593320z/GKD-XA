package li.gkd.app.util

import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 地址补全与预检的用例移植自 gkd-plus 的 AiApiUrlTest（e22fadb/df2045b/385969d 三轮边界修复）。
 * 与 plus 的差异：XA 支持 Responses 端点模式，plus 的「/responses 不支持」断言改为
 * XA 语义——地址是 /responses 但端点模式是 Chat 时才报错，模式对上则通过。
 */
class AiApiUrlTest {

    private fun config(
        url: String,
        protocol: String = "openai",
        model: String = "glm-4.7",
        endpointMode: String = AiEndpointMode.CHAT,
    ) = AiConfig(
        protocol = protocol,
        endpointMode = endpointMode,
        apiUrl = url,
        apiKey = "test-key",
        model = model,
    )

    @Test
    fun providerVersionPathsAreNotRewrittenToV1() {
        // 各家路径版本不同：一律补 /v1 会把智谱、火山、千问的地址拼坏
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4",
            AiApiUrls.fixApiUrl("https://open.bigmodel.cn/api/paas/v4", "openai"),
        )
        assertEquals(
            "https://ark.cn-beijing.volces.com/api/v3",
            AiApiUrls.fixApiUrl("https://ark.cn-beijing.volces.com/api/v3", "openai"),
        )
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            AiApiUrls.fixApiUrl("https://dashscope.aliyuncs.com/compatible-mode/v1", "openai"),
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai",
            AiApiUrls.fixApiUrl("https://generativelanguage.googleapis.com/v1beta/openai", "openai"),
        )
    }

    @Test
    fun hostOnlyInputUsesProviderDefaultPath() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4",
            AiApiUrls.fixApiUrl("open.bigmodel.cn", "openai"),
        )
        assertEquals(
            "https://open.bigmodel.cn/api/coding/paas/v4",
            AiApiUrls.fixApiUrl("https://open.bigmodel.cn/api/coding/paas/v4", "openai"),
        )
    }

    @Test
    fun fullEndpointIsNotAppendedTwice() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            AiApiUrls.resolveEndpoint("https://open.bigmodel.cn/api/paas/v4/chat/completions", "openai"),
        )
    }

    @Test
    fun baseUrlGetsChatCompletionsOrMessages() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            AiApiUrls.resolveEndpoint("https://open.bigmodel.cn/api/paas/v4", "openai"),
        )
        // 智谱的 Anthropic 兼容端点和 Claude Code 一样是 /api/anthropic/v1/messages
        assertEquals(
            "https://open.bigmodel.cn/api/anthropic/v1/messages",
            AiApiUrls.resolveEndpoint("https://open.bigmodel.cn/api/anthropic", "anthropic"),
        )
    }

    @Test
    fun responsesModeAppendsResponsesSuffix() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            AiApiUrls.resolveEndpoint(
                "https://api.openai.com/v1",
                "openai",
                AiEndpointMode.RESPONSES,
            ),
        )
    }

    @Test
    fun messyInputIsRepaired() {
        // 少了 scheme、多个斜杠、全角符号、结尾句号都是常见粘贴错误
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4",
            AiApiUrls.fixApiUrl("open.bigmodel.cn//api//paas//v4/", "openai"),
        )
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4",
            AiApiUrls.fixApiUrl("https：//open.bigmodel.cn／api／paas／v4。", "openai"),
        )
        assertEquals(
            "https://api.openai.com/v1",
            AiApiUrls.fixApiUrl("  \"api.openai.com/v1\"  ", "openai"),
        )
    }

    @Test
    fun repeatedVersionSegmentIsCollapsed() {
        assertEquals(
            "https://api.openai.com/v1",
            AiApiUrls.fixApiUrl("https://api.openai.com/v1/v1", "openai"),
        )
    }

    @Test
    fun differentVersionSegmentsAreKept() {
        // /v3/v1 是两个有意义的路径段，不能像 /v1/v1 那样合并
        assertEquals(
            "https://gw.example.com/v3/v1",
            AiApiUrls.fixApiUrl("https://gw.example.com/v3/v1", "openai"),
        )
    }

    @Test
    fun queryStringIsKeptAfterAppendingEndpoint() {
        assertEquals(
            "https://gw.example.com/v1?key=abc",
            AiApiUrls.fixApiUrl("https://gw.example.com/v1?key=abc", "openai"),
        )
        assertEquals(
            "https://gw.example.com/v1/chat/completions?key=abc",
            AiApiUrls.resolveEndpoint("https://gw.example.com/v1?key=abc", "openai"),
        )
    }

    @Test
    fun ipv6LoopbackKeepsBracketsAndUsesHttp() {
        assertEquals(
            "http://[::1]:8080/v1",
            AiApiUrls.fixApiUrl("[::1]:8080", "openai"),
        )
    }

    @Test
    fun azureDeploymentPathIsNotRewritten() {
        val endpoint =
            "https://r.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-10-21"
        assertEquals(endpoint, AiApiUrls.fixApiUrl(endpoint, "openai"))
        assertEquals(endpoint, AiApiUrls.resolveEndpoint(endpoint, "openai"))
        assertNull(AiApiUrls.validateAiConfig(config(endpoint)))
    }

    @Test
    fun azureDeploymentWithoutVersionSegmentGetsChatCompletions() {
        assertEquals(
            "https://r.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-10-21",
            AiApiUrls.resolveEndpoint(
                "https://r.openai.azure.com/openai/deployments/gpt-4o?api-version=2024-10-21",
                "openai",
            ),
        )
    }

    @Test
    fun azureConfigWithoutApiVersionOrDeploymentIsReported() {
        assertNotNull(
            AiApiUrls.validateAiConfig(
                config("https://r.openai.azure.com/openai/deployments/gpt-4o/chat/completions"),
            ),
        )
        assertNotNull(
            AiApiUrls.validateAiConfig(
                config("https://r.openai.azure.com/chat/completions?api-version=2024-10-21"),
            ),
        )
    }

    @Test
    fun queryOnHostWithoutPathIsNotTreatedAsAuthority() {
        // 没有路径时 `?` 之后不能并进域名，否则地址会被拼成 ...?key=abc/v1
        assertEquals(
            "https://gw.example.com/v1?key=abc",
            AiApiUrls.fixApiUrl("gw.example.com?key=abc", "openai"),
        )
        assertEquals(
            "https://gw.example.com/v1/chat/completions?key=abc",
            AiApiUrls.resolveEndpoint("gw.example.com?key=abc", "openai"),
        )
    }

    @Test
    fun queryOnBareAzureHostIsStillValidated() {
        // 域名里混入查询串会让 Azure 判断失效，这里要求仍能识别出地址不完整
        assertNotNull(
            AiApiUrls.validateAiConfig(
                config("https://r.openai.azure.com?api-version=2024-10-21"),
            ),
        )
    }

    @Test
    fun azureModelsEndpointUsesOpenAiModelsPath() {
        assertEquals(
            "https://r.openai.azure.com/openai/models?api-version=2024-10-21",
            AiApiUrls.resolveModelsEndpoint(
                "https://r.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-10-21",
                "openai",
            ),
        )
    }

    @Test
    fun gatewayQueryIsKeptForModelList() {
        assertEquals(
            "https://gw.example.com/v1/models?key=abc",
            AiApiUrls.resolveModelsEndpoint("https://gw.example.com/v1?key=abc", "openai"),
        )
    }

    @Test
    fun anthropicMismatchIsDetectedEvenWithQuery() {
        assertNotNull(
            AiApiUrls.validateAiConfig(
                config("https://api.openai.com/v1/chat/completions?key=abc", "anthropic"),
            ),
        )
    }

    @Test
    fun azureChinaDeploymentPathIsNotRewritten() {
        assertEquals(
            "https://r.openai.azure.cn/openai/deployments/gpt-4o",
            AiApiUrls.fixApiUrl("https://r.openai.azure.cn/openai/deployments/gpt-4o", "openai"),
        )
    }

    @Test
    fun localServiceDefaultsToPlainHttp() {
        // Ollama 这类本机服务通常没有 TLS，默认 https 会直接连不上
        assertEquals(
            "http://localhost:11434/v1",
            AiApiUrls.fixApiUrl("localhost:11434", "openai"),
        )
        assertEquals(
            "http://192.168.1.10:8000/v1",
            AiApiUrls.fixApiUrl("192.168.1.10:8000/v1", "openai"),
        )
    }

    @Test
    fun modelListEndpointFallsBackToBaseUrl() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4/models",
            AiApiUrls.resolveModelsEndpoint("https://open.bigmodel.cn/api/paas/v4/chat/completions", "openai"),
        )
    }

    @Test
    fun zhipuConfigPassesValidationAfterNormalization() {
        assertNull(AiApiUrls.validateAiConfig(config("open.bigmodel.cn")))
    }

    @Test
    fun mismatchedEndpointsAreReported() {
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://open.bigmodel.cn/api/anthropic", "openai")),
        )
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://api.openai.com/v1/chat/completions", "anthropic")),
        )
        // 智谱有 Anthropic 端点，但填的是 OpenAI 路径
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://open.bigmodel.cn/api/paas/v4", "anthropic")),
        )
        // DeepSeek 没有 Anthropic 兼容端点
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://api.deepseek.com/v1", "anthropic")),
        )
        // 智谱旧版 /v3 不是 OpenAI 兼容接口
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://open.bigmodel.cn/api/paas/v3")),
        )
    }

    @Test
    fun responsesUrlRequiresResponsesMode() {
        // XA 支持 Responses：地址已经是 /responses 时，端点模式也得是 Responses
        assertNotNull(
            AiApiUrls.validateAiConfig(config("https://api.openai.com/v1/responses")),
        )
        assertNull(
            AiApiUrls.validateAiConfig(
                config(
                    "https://api.openai.com/v1/responses",
                    endpointMode = AiEndpointMode.RESPONSES,
                ),
            ),
        )
    }
}
