package li.gkd.app.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal object AiSnapshotSelection {
    /** Keep selected subtrees and their ancestor chains, with stable ids and original coordinates. */
    fun crop(snapshot: JsonObject, selected: Set<Int>, includeAllNodes: Boolean = false): JsonObject {
        if (includeAllNodes || selected.isEmpty()) return snapshot
        val nodes = snapshot["nodes"] as? JsonArray ?: error("Snapshot has no nodes")
        val byId = nodes.associateBy { (it as? JsonObject)?.int("id") }
        val selectedIds = selected.filter { byId.containsKey(it) }
        require(selectedIds.isNotEmpty()) { "Selected snapshot nodes no longer exist" }
        val keep = selectedIds.toMutableSet()
        val children = nodes.mapNotNull { it as? JsonObject }.groupBy { it.int("pid") }
        val todo = ArrayDeque(selectedIds)
        while (todo.isNotEmpty()) {
            for (child in children[todo.removeFirst()].orEmpty()) {
                val id = child.int("id") ?: continue
                if (keep.add(id)) todo.addLast(id)
            }
        }
        for (selectedId in selectedIds) {
            var id: Int? = selectedId
            val seen = mutableSetOf<Int>()
            while (id != null && seen.add(id)) {
                keep.add(id)
                id = (byId[id] as? JsonObject)?.int("pid")?.takeIf { it in byId }
            }
        }
        return JsonObject(snapshot + ("nodes" to JsonArray(nodes.filter { (it as? JsonObject)?.int("id") in keep })))
    }

    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
}
