package li.gkd.app.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class AiSnapshotSelectionTest {
    private val snapshot = Json.parseToJsonElement(
        """{
          "appId":"example.app", "screenWidth":1080, "screenHeight":2400,
          "nodes":[
            {"id":10,"pid":-1,"attr":{"text":"root","left":0,"top":0}},
            {"id":30,"pid":10,"attr":{"text":"selected","left":20,"top":40}},
            {"id":50,"pid":30,"attr":{"text":"descendant","left":25,"top":50}},
            {"id":70,"pid":50,"attr":{"text":"grandchild","left":30,"top":60}},
            {"id":90,"pid":10,"attr":{"text":"unrelated sibling","left":400,"top":200}},
            {"id":110,"pid":90,"attr":{"text":"unrelated child","left":420,"top":240}}
          ]
        }""",
    ).jsonObject

    @Test fun selectedSubtreeKeepsAncestorsStableIdsCoordinatesAndMetadata() {
        val cropped = AiSnapshotSelection.crop(snapshot, setOf(30))
        assertEquals(listOf(10, 30, 50, 70), ids(cropped))
        assertEquals(snapshot.filterKeys { it != "nodes" }, cropped.filterKeys { it != "nodes" })
        assertEquals(nodes(snapshot).take(4), nodes(cropped))
        assertEquals(listOf(10, 30, 50, 70, 90, 110), ids(snapshot))
    }

    @Test fun selectedLeafDoesNotIncludeSiblingsOfItsAncestors() {
        assertEquals(listOf(10, 30, 50, 70), ids(AiSnapshotSelection.crop(snapshot, setOf(70))))
        assertEquals(listOf(10, 30, 50, 70, 90, 110), ids(AiSnapshotSelection.crop(snapshot, setOf(70, 90))))
    }

    @Test fun emptySelectionAndExplicitFullModePreserveWholeSnapshot() {
        assertSame(snapshot, AiSnapshotSelection.crop(snapshot, emptySet()))
        assertSame(snapshot, AiSnapshotSelection.crop(snapshot, setOf(30), includeAllNodes = true))
        assertSame(snapshot, AiSnapshotSelection.crop(snapshot, setOf(999), includeAllNodes = true))
    }

    @Test fun staleSelectionFailsInsteadOfSilentlySendingWholeTree() {
        try {
            AiSnapshotSelection.crop(snapshot, setOf(999))
            fail("unknown selected nodes must be rejected")
        } catch (e: IllegalArgumentException) {
            assertEquals("Selected snapshot nodes no longer exist", e.message)
        }
        assertEquals(listOf(10, 30, 50, 70), ids(AiSnapshotSelection.crop(snapshot, setOf(30, 999))))
    }

    @Test(timeout = 1000) fun malformedParentCycleTerminatesAndExcludesUnrelatedNodes() {
        val cyclic = Json.parseToJsonElement(
            """{"nodes":[{"id":1,"pid":2},{"id":2,"pid":1},{"id":3,"pid":-1}]}""",
        ).jsonObject
        assertEquals(listOf(1, 2), ids(AiSnapshotSelection.crop(cyclic, setOf(1))))
        val orphan = Json.parseToJsonElement(
            """{"nodes":[{"id":7,"pid":999},{"id":8,"pid":7},{"id":9,"pid":-1}]}""",
        ).jsonObject
        assertEquals(listOf(7, 8), ids(AiSnapshotSelection.crop(orphan, setOf(7))))
    }

    private fun nodes(value: JsonObject): JsonArray = value["nodes"] as JsonArray
    private fun ids(value: JsonObject): List<Int> = nodes(value).map { it.jsonObject["id"]!!.jsonPrimitive.int }
}
