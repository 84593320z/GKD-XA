package li.gkd.app.util

import io.ktor.client.request.HttpRequestBuilder
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import li.gkd.app.data.settings.AiHeader
import org.junit.Assert.*
import org.junit.Test

class AiResponseTest {
    @Test fun protocolsPreserveAllTextBlocksAndIgnoreThinking() {
        val response = AiResponseText.parse(
            """{"error":null,"status":"completed","output":[{"type":"reasoning","summary":[]},{"type":"message","content":[{"type":"output_text","text":"first"},{"type":"output_text","text":"second"}]}]}""",
            AiConfig(endpointMode = AiEndpointMode.RESPONSES),
        )
        assertEquals("firstsecond", response.content)
        assertFalse(response.interrupted)
        assertEquals("ab", AiResponseText.parse(
            """{"content":[{"type":"thinking","thinking":"private"},{"type":"text","text":"a"},{"type":"text","text":"b"}],"stop_reason":"end_turn"}""",
            AiConfig(protocol = "anthropic"),
        ).content)
        assertEquals("ab", AiResponseText.parse(
            """{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"text","text":"b"}]},"finish_reason":"stop"}]}""",
            AiConfig(),
        ).content)
    }

    @Test fun thinkingOnlyTruncationCanTriggerBudgetRetry() {
        val reply = AiResponseText.parse(
            """{"content":[{"type":"thinking","thinking":"private"}],"stop_reason":"max_tokens"}""",
            AiConfig(protocol = "anthropic"),
        )
        assertEquals("", reply.content)
        assertTrue(reply.truncated)
    }

    @Test fun multilineEventsDoneAndErrorsUseActualDecoder() {
        val decoder = AiSseDecoder()
        listOf(": heartbeat", """data: {"choices":[""", """data: {"delta":{"content":"one"},"finish_reason":null}]}""", "", "data: [DONE]", "").forEach(decoder::line)
        decoder.line("data: {bad trailing payload}")
        assertEquals("one", decoder.finish().content)
        assertFalse(decoder.finish().interrupted)
        val error = AiSseDecoder()
        error.line("event: error")
        error.line("data: secret response body")
        val exception = assertThrows(IllegalStateException::class.java) { error.line("") }
        assertFalse(exception.message.orEmpty().contains("secret"))
        val incomplete = AiSseDecoder()
        incomplete.line("""data: {"choices":[{"delta":{"content":"partial"}}]}""")
        incomplete.line("")
        assertTrue(incomplete.finish().interrupted)
    }

    @Test fun allLineEndingsAndChunkBoundariesUseIncrementalFraming() {
        for (ending in listOf("\n", "\r", "\r\n")) {
            val bytes = ("data: {\"choices\":[{\"delta\":{\"content\":\"文字\"}}]}" + ending + ending +
                "data: [DONE]" + ending + ending).encodeToByteArray()
            for (split in 0..bytes.size) {
                val framer = AiSseFramer()
                framer.feed(bytes.copyOfRange(0, split))
                assertFalse(framer.feed(bytes.copyOfRange(split, bytes.size)))
                assertEquals(AiReply("文字"), framer.finish())
            }
            val bytewise = AiSseFramer()
            ("\uFEFF".encodeToByteArray() + bytes).forEach { bytewise.feed(byteArrayOf(it)) }
            assertEquals(AiReply("文字"), bytewise.finish())
        }
    }

    @Test fun eventAndOutputBoundsFailClosed() {
        val event = AiSseDecoder(AiResponseLimits(eventChars = 8))
        assertThrows(IllegalStateException::class.java) { event.line("data: 123456789") }
        val output = AiSseDecoder(AiResponseLimits(outputChars = 2))
        output.line("""data: {"choices":[{"delta":{"content":"long"}}]}""")
        assertThrows(IllegalStateException::class.java) { output.line("") }
        val count = AiSseDecoder(AiResponseLimits(events = 1))
        count.line("data: {}"); count.line("")
        count.line("data: {}")
        assertThrows(IllegalStateException::class.java) { count.line("") }
    }

    @Test fun canonicalCredentialsReplaceAllCustomAuthHeaders() {
        val custom = listOf(AiHeader("aUthOrIzAtIoN", "old"), AiHeader("API-key", "old"), AiHeader("X-API-Key", "old"))
        val openai = HttpRequestBuilder().apply { applyAiHeaders(AiConfig(apiKey = "new", headers = custom)) }.headers
        assertEquals(listOf("Bearer new"), openai.getAll("Authorization"))
        assertNull(openai["api-key"])
        assertNull(openai["x-api-key"])
        val anthropic = HttpRequestBuilder().apply {
            applyAiHeaders(AiConfig(protocol = "anthropic", apiKey = "new", headers = custom + AiHeader("anthropic-version", "old")))
        }.headers
        assertNull(anthropic["Authorization"])
        assertEquals(listOf("new"), anthropic.getAll("x-api-key"))
        assertEquals(listOf(AiConfig.DEFAULT_ANTHROPIC_VERSION), anthropic.getAll("anthropic-version"))
    }
}
