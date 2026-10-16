package li.gkd.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 截断判定的取值名来自各家 API 参考，混用会把「内容被过滤」当成「预算不够」去翻倍 max_tokens，
 * 所以每个分支的接受集与拒绝集都要钉住。
 */
class AiTruncationTest {

    @Test
    fun chatCompletionsOnlyTreatsLengthAsTruncation() {
        assertTrue(AiTruncation.chatCompletions("length"))
        assertFalse(AiTruncation.chatCompletions("stop"))
        assertFalse(AiTruncation.chatCompletions("tool_calls"))
        assertFalse(AiTruncation.chatCompletions("content_filter"))
        assertFalse(AiTruncation.chatCompletions(null))
    }

    @Test
    fun anthropicTreatsBothLimitHitAndContextOverflowAsTruncation() {
        assertTrue(AiTruncation.anthropic("max_tokens"))
        assertTrue(AiTruncation.anthropic("model_context_window_exceeded"))
        assertFalse(AiTruncation.anthropic("end_turn"))
        assertFalse(AiTruncation.anthropic("tool_use"))
        assertFalse(AiTruncation.anthropic("stop_sequence"))
        assertFalse(AiTruncation.anthropic("refusal"))
        assertFalse(AiTruncation.anthropic(null))
    }

    @Test
    fun responsesRequiresBothIncompleteStatusAndTokenReason() {
        assertTrue(AiTruncation.responses("incomplete", "max_output_tokens"))
        // content_filter 不是预算问题，翻倍 max_tokens 只会白花一次钱
        assertFalse(AiTruncation.responses("incomplete", "content_filter"))
        assertFalse(AiTruncation.responses("incomplete", null))
        assertFalse(AiTruncation.responses("completed", "max_output_tokens"))
        assertFalse(AiTruncation.responses("failed", "max_output_tokens"))
        assertFalse(AiTruncation.responses(null, null))
    }

    @Test
    fun streamAndNonStreamAgreeOnWhatLengthTruncationMeans() {
        val truncated = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"content":"x"},"finish_reason":"length"}]}
            data: [DONE]
            """.trimIndent().lines(),
        )
        assertTrue(truncated.isTruncated)
        assertTrue(AiTruncation.chatCompletions(truncated.finishReason))
    }
}
