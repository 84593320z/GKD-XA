package li.gkd.app.util

import kotlinx.serialization.json.JsonPrimitive
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import li.gkd.app.data.settings.AiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 移植自 gkd-plus 的 AiThinkingUtilsTest，按 XA 的「每模型 reasoning 标志」语义改写：
 * plus 按档位（auto/off/low/medium/high/custom）驱动，XA 只有开/关，开启时固定中等强度。
 */
class AiThinkingUtilsTest {

    private fun config(
        model: String,
        reasoning: Boolean? = true,
        protocol: String = "openai",
        endpointMode: String = AiEndpointMode.CHAT,
        thinkingRejected: Boolean = false,
    ) = AiConfig(
        protocol = protocol,
        endpointMode = endpointMode,
        model = model,
        thinkingRejected = thinkingRejected,
        models = listOf(AiModel(modelId = model, reasoning = reasoning)),
    )

    @Test
    fun flagOffOrUnknownSendsNothing() {
        // 标志不是 true（false 或没标记过）一律不发：XA 的保守原则
        assertTrue(AiThinkingUtils.buildParams(config("gpt-5.1", reasoning = false)).isEmpty())
        assertTrue(AiThinkingUtils.buildParams(config("gpt-5.1", reasoning = null)).isEmpty())
        assertTrue(AiThinkingUtils.buildParams(config("")).isEmpty())
    }

    @Test
    fun recognizedFamiliesGetTheirFieldsAtMediumStrength() {
        assertEquals(
            mapOf("reasoning_effort" to JsonPrimitive("medium")),
            AiThinkingUtils.buildParams(config("gpt-5.1")),
        )
        // o 系同样有 medium 档
        assertEquals(
            mapOf("reasoning_effort" to JsonPrimitive("medium")),
            AiThinkingUtils.buildParams(config("o3-mini")),
        )
        // 千问没有档位，用开关 + token 预算表达
        assertEquals(
            mapOf(
                "enable_thinking" to JsonPrimitive(true),
                "thinking_budget" to JsonPrimitive(4096),
            ),
            AiThinkingUtils.buildParams(config("qwen3-max")),
        )
    }

    @Test
    fun unrecognizedModelSendsNothing() {
        // 认不出来时宁可不发：发错字段会让请求直接 400
        assertTrue(AiThinkingUtils.buildParams(config("deepseek-reasoner")).isEmpty())
        assertTrue(AiThinkingUtils.buildParams(config("some-model")).isEmpty())
    }

    @Test
    fun unsupportedLevelFallsBackInsteadOfSendingInvalidValue() {
        // grok 没有 medium 档，退到 low 而不是发一个会被拒绝的值
        assertEquals(
            mapOf("reasoning_effort" to JsonPrimitive("low")),
            AiThinkingUtils.buildParams(config("grok-4")),
        )
    }

    @Test
    fun anthropicEnabledCarriesBudget() {
        assertEquals(
            """{"type":"enabled","budget_tokens":4096}""",
            AiThinkingUtils.buildParams(config("claude-sonnet-4", protocol = "anthropic"))["thinking"]
                .toString(),
        )
        // 中转常把别的模型挂在 Anthropic 兼容端点上：XA 没有「关闭」档位，
        // 标志开启时同样发 enabled + 预算（plus 原版此处测的是关闭档发 disabled）
        assertEquals(
            """{"type":"enabled","budget_tokens":4096}""",
            AiThinkingUtils.buildParams(config("DeepSeek-V4.1-Flash", protocol = "anthropic"))["thinking"]
                .toString(),
        )
    }

    @Test
    fun newGptAndGlmVersionsAreRecognizedWithoutEditingTheTable() {
        // 门槛匹配的意义：新版本发布后不用改代码也能识别出来
        assertEquals(AiThinkingUtils.Family.Gpt5, AiThinkingUtils.familyOf("gpt-6", "openai"))
        assertEquals(AiThinkingUtils.Family.Gpt5, AiThinkingUtils.familyOf("openai/gpt-7.2-codex", "openai"))
        assertEquals(AiThinkingUtils.Family.Zhipu, AiThinkingUtils.familyOf("glm-5.1", "openai"))
        assertTrue(AiThinkingUtils.familyOf("zhipu/glm-6", "openai") != AiThinkingUtils.Family.Unknown)
    }

    @Test
    fun modelsThatRejectTheThinkingFieldStayUnknown() {
        // gpt-4o / gpt-4.1 不支持 reasoning_effort，误判会让请求 400
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("gpt-4o", "openai"))
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("gpt-4.1-mini", "openai"))
        // glm-4-flash 不支持 thinking
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("glm-4-flash", "openai"))
        // 关键词后面不是版本号的也认不出
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("gpt-oss-120b", "openai"))
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("glm-z1-air", "openai"))
    }

    @Test
    fun oSeriesIsDetectedWithProviderPrefixButNotFromGpt4o() {
        assertEquals(AiThinkingUtils.Family.OpenAiReasoning, AiThinkingUtils.familyOf("o3-mini", "openai"))
        // 带厂商前缀时 startsWith 会漏掉，这里必须还能认出来
        assertEquals(AiThinkingUtils.Family.OpenAiReasoning, AiThinkingUtils.familyOf("openai/o5-preview", "openai"))
        // gpt-4o 里的 "4o" 不能被当成 o 系
        assertTrue(AiThinkingUtils.familyOf("gpt-4o", "openai") != AiThinkingUtils.Family.OpenAiReasoning)
    }

    @Test
    fun deepSeekV4ThinksByDefaultButEarlierVersionsAreLeftAlone() {
        assertEquals(
            mapOf("thinking" to """{"type":"enabled"}"""),
            AiThinkingUtils.buildParams(config("deepseek-v4-flash"))
                .mapValues { it.value.toString() },
        )
        assertEquals(
            AiThinkingUtils.Family.DeepSeek,
            AiThinkingUtils.familyOf("deepseek/deepseek-v4.1-pro", "openai"),
        )
        // V3 及更早用模型名区分思考模式，deepseek-reasoner / r1 更不能被当成可控
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("deepseek-v3.2", "openai"))
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("deepseek-reasoner", "openai"))
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("deepseek-r1", "openai"))
    }

    @Test
    fun miniMaxVariantsAreSplitByThinkingSupport() {
        // M3.1-Flash-Preview 发 disabled 会 400，只能降 effort
        assertEquals(
            mapOf("reasoning_effort" to JsonPrimitive("medium")),
            AiThinkingUtils.buildParams(config("MiniMax-M3.1-Flash-Preview")),
        )
        // M3 可以真的开关
        assertEquals(
            mapOf("thinking" to """{"type":"enabled"}"""),
            AiThinkingUtils.buildParams(config("MiniMax-M3"))
                .mapValues { it.value.toString() },
        )
        // M2.x 接受但忽略这个参数，没必要发
        assertEquals(AiThinkingUtils.Family.Unknown, AiThinkingUtils.familyOf("MiniMax-M2.7", "openai"))
    }

    @Test
    fun miMoUsesDifferentFieldDependingOnHost() {
        // 小米自家接口
        assertEquals(
            mapOf("thinking" to """{"type":"enabled"}"""),
            AiThinkingUtils.buildParams(config("mimo-v2.5-pro"))
                .mapValues { it.value.toString() },
        )
        // 百炼托管的同一个模型用 enable_thinking
        assertEquals(
            mapOf(
                "enable_thinking" to JsonPrimitive(true),
                "thinking_budget" to JsonPrimitive(4096),
            ),
            AiThinkingUtils.buildParams(config("xiaomi/mimo-v2.5-pro")),
        )
    }

    @Test
    fun onlyThinkingParameterErrorsCountAsRejection() {
        // 各家报错文案不统一，这里列出实际会遇到的几种
        assertTrue(AiThinkingUtils.isThinkingRejection(Exception("Unrecognized request argument supplied: reasoning_effort")))
        assertTrue(AiThinkingUtils.isThinkingRejection(Exception("""unknown field "thinking"""")))
        assertTrue(AiThinkingUtils.isThinkingRejection(Exception("unsupported parameter: enable_thinking")))
        assertTrue(
            AiThinkingUtils.isThinkingRejection(
                Exception("""model "MiniMax-M3.1-Flash-Preview" requires adaptive thinking"""),
            ),
        )
        // 智谱强制思考模型的原文报错
        assertTrue(
            AiThinkingUtils.isThinkingRejection(
                Exception("该模型始终思考，不支持关闭思考；请使用 low、high 或 max。"),
            ),
        )
        // 别的 400 不能被误判成思考参数问题，否则会把好配置改掉
        assertTrue(!AiThinkingUtils.isThinkingRejection(Exception("HTTP 401 invalid api key")))
        assertTrue(!AiThinkingUtils.isThinkingRejection(Exception("HTTP 429 rate limit exceeded")))
        assertTrue(!AiThinkingUtils.isThinkingRejection(Exception("model not found: gpt-9")))
        assertTrue(!AiThinkingUtils.isThinkingRejection(Exception("invalid max_tokens value")))
    }

    @Test
    fun glmVersionsAreSplitByThinkingCapability() {
        // 5.3 起强制思考：XA 中档映射到 high（智谱只认 low/high/max）
        assertEquals(
            mapOf(
                "thinking" to """{"type":"enabled"}""",
                "reasoning_effort" to """"high"""",
            ),
            AiThinkingUtils.buildParams(config("glm-5.3-flash"))
                .mapValues { it.value.toString() },
        )
        // 新版本同样按强制思考处理，不用改代码
        assertEquals(
            AiThinkingUtils.Family.ZhipuForced,
            AiThinkingUtils.familyOf("glm-6", "openai"),
        )
        // 4.7 强制思考且不可调档，发什么都没用
        assertEquals(AiThinkingUtils.Family.ZhipuForcedFixed, AiThinkingUtils.familyOf("glm-4.7", "openai"))
        assertTrue(AiThinkingUtils.buildParams(config("glm-4.7")).isEmpty())
        // 5.2 可切换思考，且支持 reasoning_effort（中档 -> high）
        assertEquals(
            mapOf(
                "thinking" to """{"type":"enabled"}""",
                "reasoning_effort" to """"high"""",
            ),
            AiThinkingUtils.buildParams(config("glm-5.2"))
                .mapValues { it.value.toString() },
        )
        // 4.5–5.1 只能切 thinking.type
        assertEquals(
            mapOf("thinking" to """{"type":"enabled"}"""),
            AiThinkingUtils.buildParams(config("glm-5.1"))
                .mapValues { it.value.toString() },
        )
        assertEquals(
            mapOf("thinking" to """{"type":"enabled"}"""),
            AiThinkingUtils.buildParams(config("glm-4.6"))
                .mapValues { it.value.toString() },
        )
    }

    @Test
    fun forcedThinkingModelsAreMarkedAsNotSupportingDisable() {
        assertTrue(!AiThinkingUtils.familyOf("glm-5.3-flash", "openai").supportsDisable)
        assertTrue(!AiThinkingUtils.familyOf("glm-4.7", "openai").supportsDisable)
        assertTrue(!AiThinkingUtils.familyOf("MiniMax-M3.1-Flash-Preview", "openai").supportsDisable)
        assertTrue(AiThinkingUtils.familyOf("glm-5.2", "openai").supportsDisable)
        assertTrue(AiThinkingUtils.familyOf("glm-5.1", "openai").supportsDisable)
    }

    @Test
    fun responsesModeOnlySendsOpenAiReasoningShape() {
        // Responses 端点的思考结构是 reasoning 对象
        assertEquals(
            """{"effort":"medium"}""",
            AiThinkingUtils.buildParams(config("gpt-5.1", endpointMode = AiEndpointMode.RESPONSES))["reasoning"]
                .toString(),
        )
        // 非 OpenAI 家族在 Responses 模式下不发，避免把 Chat 风格字段塞过去
        assertTrue(
            AiThinkingUtils.buildParams(config("qwen3-max", endpointMode = AiEndpointMode.RESPONSES)).isEmpty(),
        )
    }

    @Test
    fun rejectedProviderNeverSendsAgain() {
        // 被服务端拒绝过一次后记住：同一个配置再也不发思考参数
        assertTrue(
            AiThinkingUtils.buildParams(config("gpt-5.1", thinkingRejected = true)).isEmpty(),
        )
    }
}
