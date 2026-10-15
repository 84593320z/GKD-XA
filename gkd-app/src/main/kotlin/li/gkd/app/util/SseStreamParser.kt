package li.gkd.app.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class SseStreamResult(
    val content: String,
    val finishReason: String?,
    val done: Boolean,
) {
    val isTruncated: Boolean get() = finishReason == SseStreamParser.FINISH_REASON_LENGTH
}

object SseStreamParser {

    const val DONE_MARKER = "[DONE]"
    const val FINISH_REASON_LENGTH = "length"

    fun parse(lines: Sequence<String>): SseStreamResult {
        val text = StringBuilder()
        var finishReason: String? = null
        var done = false
        for (raw in lines) {
            val line = raw.trimEnd('\r')
            if (line.isEmpty() || line.startsWith(":")) continue
            val colon = line.indexOf(':')
            if (colon < 0 || line.substring(0, colon) != "data") continue
            val payload = line.substring(colon + 1).trim()
            if (payload == DONE_MARKER) {
                done = true
                break
            }
            val chunk = runCatching { Json.parseToJsonElement(payload) as? JsonObject }.getOrNull()
                ?: continue
            val choice = chunk["choices"].arrayOrNull()?.firstOrNull() as? JsonObject ?: continue
            choice["delta"].objectOrNull()?.get("content").stringOrNull()?.let { text.append(it) }
            finishReason = choice["finish_reason"].stringOrNull() ?: finishReason
        }
        return SseStreamResult(text.toString(), finishReason, done)
    }

    fun parse(lines: List<String>): SseStreamResult = parse(lines.asSequence())
}

private fun JsonElement?.arrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.stringOrNull(): String? = when (this) {
    null, JsonNull -> null
    is JsonPrimitive -> content
    else -> null
}
