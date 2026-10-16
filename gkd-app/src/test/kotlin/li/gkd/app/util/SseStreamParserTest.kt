package li.gkd.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseStreamParserTest {

    @Test
    fun deltaChunksAreConcatenatedUntilDoneMarker() {
        val result = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"role":"assistant","content":null}}]}
            data: {"choices":[{"index":0,"delta":{"content":"Hello"}}]}
            data: {"choices":[{"index":0,"delta":{"content":", world"}}]}
            data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}
            data: [DONE]
            """.trimIndent().lines(),
        )

        assertEquals("Hello, world", result.content)
        assertEquals("stop", result.finishReason)
        assertFalse(result.isTruncated)
        assertTrue(result.done)
    }

    @Test
    fun finishReasonLengthIsReportedAsTruncatedOutput() {
        val result = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"content":"{"}}]}
            data: {"choices":[{"index":0,"delta":{"content":"\"name\": \"a\""}}]}
            data: {"choices":[{"index":0,"delta":{},"finish_reason":"length"}]}
            data: [DONE]
            """.trimIndent().lines(),
        )

        assertEquals("{\"name\": \"a\"", result.content)
        assertEquals("length", result.finishReason)
        assertTrue(result.isTruncated)
    }

    @Test
    fun chunkWithEmptyChoicesArrayIsSkippedWithoutBreakingParsing() {
        val result = SseStreamParser.parse(
            """
            data: {"id":"chatcmpl-1","choices":[]}
            data: {"choices":[{"index":0,"delta":{"content":"kept"}}]}
            data: [DONE]
            """.trimIndent().lines(),
        )

        assertEquals("kept", result.content)
        assertTrue(result.done)
    }

    @Test
    fun blankLinesCommentsNonDataFieldsAndDirtyJsonAreAllSkipped() {
        val result = SseStreamParser.parse(
            """
            : keep-alive

            event: message
            id: 42
            retry: 1000
            data: {not json at all
            data:
            {"choices":[{"index":0,"delta":{"content":"missed"}}]}
            data: {"choices":[{"index":0,"delta":{"content":"ok"},"finish_reason":null}]}
            data: [DONE]
            """.trimIndent().lines(),
        )

        assertEquals("ok", result.content)
        assertNull(result.finishReason)
        assertTrue(result.done)
    }

    @Test
    fun streamEndingWithoutDoneMarkerStillReturnsTheAccumulatedContent() {
        val result = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"content":"half"}}]}
            data: {"choices":[{"index":0,"delta":{"content":" written"}}]}
            """.trimIndent().lines(),
        )

        assertEquals("half written", result.content)
        assertNull(result.finishReason)
        assertFalse(result.isTruncated)
        assertFalse(result.done)
    }

    @Test
    fun reasoningContentIsNeverAppendedToTheAnswer() {
        val result = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"reasoning_content":"先想一步","content":"answer"}}]}
            data: {"choices":[{"index":0,"delta":{"thinking":"再想一步"}}]}
            data: {"choices":[{"index":0,"delta":{"content":" now"},"finish_reason":"stop"}]}
            data: [DONE]
            """.trimIndent().lineSequence(),
        )

        assertEquals("answer now", result.content)
        assertEquals("stop", result.finishReason)
        assertTrue(result.done)
    }

    @Test
    fun finishReasonChunkWithoutDoneMarkerCountsAsCompleteStream() {
        val result = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"content":"{\"a\": 1}"},"finish_reason":"stop"}]}
            """.trimIndent().lines(),
        )

        assertEquals("{\"a\": 1}", result.content)
        assertTrue(
            "有些服务商只发 finish_reason 收尾，不能因为缺 [DONE] 就判成断流",
            result.isComplete,
        )
    }

    @Test
    fun silentCloseIsTheOnlyIncompleteCase() {
        val cut = SseStreamParser.parse(
            """
            data: {"choices":[{"index":0,"delta":{"content":"半"}}]}
            """.trimIndent().lines(),
        )
        assertFalse("既无 [DONE] 又无 finish_reason 时必须识别成残缺", cut.isComplete)
        assertFalse(cut.isTruncated)

        assertTrue(SseStreamParser.parse(listOf("data: [DONE]")).isComplete)
    }
}
