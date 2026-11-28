package li.gkd.app.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HttpServiceSafetyTest {
    @Test
    fun snapshotSummaryBudgetStopsLoadingAtExactUtf8WireLimit() = runTest {
        val item = JsonObject(mapOf("label" to JsonPrimitive("测试🙂")))
        val limit = item.toString().encodeToByteArray().size * 2 + 3
        val loaded = mutableListOf<Long>()
        val result = boundedSnapshotSummaryJson((1L..10L).toList(), maxBytes = limit) { id ->
            loaded += id
            item
        }
        assertEquals(limit, result.size)
        assertEquals(JsonArray(listOf(item, item)), Json.parseToJsonElement(result.decodeToString()))
        assertEquals(listOf(1L, 2L), loaded)
    }

    @Test
    fun snapshotSummaryCountBoundsReadsIncludingMissingEntries() = runTest {
        val loaded = mutableListOf<Long>()
        val result = boundedSnapshotSummaryJson((1L..10L).toList(), maxCount = 2) { id ->
            loaded += id
            if (id == 1L) null else JsonObject(mapOf("id" to JsonPrimitive(id)))
        }
        assertEquals("[{\"id\":2}]", result.decodeToString())
        assertEquals(listOf(1L, 2L), loaded)
    }

    @Test
    fun snapshotSummarySkipsUnreadableEntriesWithoutLosingHealthyOnes() = runTest {
        val skipped = mutableListOf<Long>()
        val result = boundedSnapshotSummaryJson(listOf(1L, 2L, 3L), onUnreadable = { id, _ -> skipped += id }) { id ->
            when (id) {
                1L -> Json.decodeFromString<JsonObject>("{")
                2L -> throw IOException("file unavailable")
                else -> JsonObject(mapOf("id" to JsonPrimitive(id)))
            }
        }
        assertEquals("[{\"id\":3}]", result.decodeToString())
        assertEquals(listOf(1L, 2L), skipped)
    }

    @Test
    fun snapshotSummaryDoesNotSwallowCancellation() = runTest {
        val result = runCatching {
            boundedSnapshotSummaryJson(listOf(1L)) { throw CancellationException("request ended") }
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun unknownHttpFailuresUseStableRedactedMessage() {
        val error = genericDebugRpcError()
        assertEquals("设备操作失败", error.message)
        assertTrue(error.unknown)
    }
}
