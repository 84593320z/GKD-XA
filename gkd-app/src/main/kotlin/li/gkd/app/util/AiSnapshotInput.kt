package li.gkd.app.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.File

/** The queue owns these immutable values, never a file that can be deleted or replaced later. */
internal data class AiSnapshotInput(val appId: String, val content: String) {
    companion object {
        const val MAX_BYTES = 8 * 1024 * 1024

        fun readText(file: File): String {
            val out = ByteArrayOutputStream()
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= MAX_BYTES) { "Snapshot exceeds AI size limit" }
                    out.write(buffer, 0, count)
                }
            }
            return out.toByteArray().decodeToString(throwOnInvalidSequence = true)
        }

        fun read(file: File, selectedNodeIds: Set<Int>, includeAllNodes: Boolean): AiSnapshotInput {
            val snapshot = json.parseToJsonElement(readText(file)) as? JsonObject
                ?: error("Invalid snapshot")
            val appId = (snapshot["appId"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?.takeIf { it.isNotBlank() } ?: error("Missing snapshot application")
            val selected = selectedNodeIds.toSet()
            val cropped = AiSnapshotSelection.crop(snapshot, selected, includeAllNodes)
            val selection = if (selected.isEmpty()) "" else
                "\n仅对选中节点生成规则；选中节点 ID：" + selected.sorted().joinToString(",") +
                    "。节点树可能已裁剪，只含目标子树及祖先；不要假设缺失节点不存在。"
            return AiSnapshotInput(appId, cropped.toString() + selection)
        }
    }
}
