package li.gkd.app.util

/**
 * 「输出被长度上限截断」的判定。三种端点形态的字段名和取值都不一样，之前只有流式路径认得这个信号
 * （SseStreamParser 从 delta 分片里读 finish_reason），非流式一律按「没截断」把残缺 JSON 交回去，
 * 于是开着 Max Tokens 自适应但不走流式的用户，只能等下游 JSON 解析报错才知道出事了。
 *
 * 取值名按各家 API 参考：
 * - Chat Completions：choices[].finish_reason == "length"
 * - Anthropic Messages：顶层 stop_reason == "max_tokens"；"model_context_window_exceeded"
 *   是填满上下文窗口，同样是截断，翻倍预算也可能救不回来，但重试一次比拿残缺正文强
 * - OpenAI Responses：status == "incomplete" 且 incomplete_details.reason == "max_output_tokens"；
 *   reason 还可能是 content_filter，那不是预算问题，不能拿它去翻倍 max_tokens
 */
object AiTruncation {

    fun chatCompletions(finishReason: String?): Boolean =
        finishReason == SseStreamParser.FINISH_REASON_LENGTH

    fun anthropic(stopReason: String?): Boolean =
        stopReason == ANTHROPIC_MAX_TOKENS || stopReason == ANTHROPIC_CONTEXT_WINDOW_EXCEEDED

    fun responses(status: String?, incompleteReason: String?): Boolean =
        status == RESPONSES_STATUS_INCOMPLETE && incompleteReason == RESPONSES_MAX_OUTPUT_TOKENS

    const val ANTHROPIC_MAX_TOKENS = "max_tokens"
    const val ANTHROPIC_CONTEXT_WINDOW_EXCEEDED = "model_context_window_exceeded"
    const val RESPONSES_STATUS_INCOMPLETE = "incomplete"
    const val RESPONSES_MAX_OUTPUT_TOKENS = "max_output_tokens"
}
