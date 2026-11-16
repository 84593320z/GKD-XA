package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

internal data class AiReply(val content: String, val truncated: Boolean = false, val interrupted: Boolean = false)

/** Error bodies can contain the entire prompt or credentials; only classifications escape. */
internal class AiHttpFailure(val status: Int, val tokenLimit: Boolean, val thinking: Boolean) :
    Exception("HTTP $status")

internal data class AiResponseLimits(
    val bodyBytes: Int = 4 * 1024 * 1024,
    val lineBytes: Int = 64 * 1024,
    val eventChars: Int = 256 * 1024,
    val outputChars: Int = 1024 * 1024,
    val events: Int = 20_000,
    val deadlineMillis: Long = 120_000,
    val idleMillis: Long = 30_000,
)

internal fun HttpRequestBuilder.applyAiHeaders(config: AiConfig) {
    config.headers.forEach { item ->
        val name = item.name.trim()
        if (name.isNotEmpty()) headers[name] = item.value
    }
    // Replace case-insensitively; append/header would send the user-supplied secret as well.
    listOf(HttpHeaders.Authorization, "api-key", "x-api-key").forEach(headers::remove)
    when {
        config.isAnthropic -> {
            headers["x-api-key"] = config.apiKey
            headers["anthropic-version"] = config.anthropicVersion.ifBlank { AiConfig.DEFAULT_ANTHROPIC_VERSION }
        }
        AiApiUrls.isAzureApiHost(AiApiUrls.apiHostOf(config.apiUrl)) -> headers["api-key"] = config.apiKey
        else -> headers[HttpHeaders.Authorization] = "Bearer "+config.apiKey
    }
}

internal class AiTransport(
    private val client: HttpClient,
    private val limits: AiResponseLimits = AiResponseLimits(),
) {
    companion object {
        /** Application lifetime pool. Individual calls close/cancel their response, not the pool. */
        val shared: AiTransport by lazy {
            AiTransport(HttpClient(OkHttp) {
                followRedirects = false // never forward API credentials to a redirect target
                engine { config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                } }
            })
        }
    }

    suspend fun post(config: AiConfig, body: String): AiReply = withTimeout(limits.deadlineMillis) {
        client.preparePost(AiApiUrls.buildApiEndpoint(config)) {
            applyAiHeaders(config)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            try {
                if (!response.status.isSuccess()) fail(response.status.value, readText(channel))
                if (response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()
                        .equals("text/event-stream", ignoreCase = true)) {
                    readSse(channel)
                } else {
                    AiResponseText.parse(readText(channel), config, limits.outputChars)
                }
            } finally {
                channel.cancel(null)
            }
        }
    }

    suspend fun models(config: AiConfig): String = withTimeout(limits.deadlineMillis) {
        client.prepareGet(AiApiUrls.resolveModelsEndpoint(config.apiUrl, config.protocol)) {
            applyAiHeaders(config)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            try {
                val body = readText(channel)
                if (!response.status.isSuccess()) fail(response.status.value, body)
                body
            } finally {
                channel.cancel(null)
            }
        }
    }

    private fun fail(status: Int, body: String): Nothing = throw AiHttpFailure(
        status,
        status in setOf(400, 422) && AiTokenBudget.isTokenLimitRejection(body),
        status in setOf(400, 422) && AiThinkingUtils.isThinkingRejection(Exception(body)),
    )

    private suspend fun readChunks(channel: ByteReadChannel, consume: (ByteArray, Int) -> Boolean) {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val size = withTimeout(limits.idleMillis) { channel.readAvailable(buffer) }
            if (size < 0) break
            total += size
            check(total <= limits.bodyBytes) { "AI response exceeds size limit" }
            if (!consume(buffer, size)) return
        }
    }

    private suspend fun readText(channel: ByteReadChannel): String {
        val out = ByteArrayOutputStream()
        readChunks(channel) { bytes, count -> out.write(bytes, 0, count); true }
        return out.toByteArray().decodeToString(throwOnInvalidSequence = true)
    }

    private suspend fun readSse(channel: ByteReadChannel): AiReply {
        val framer = AiSseFramer(limits)
        readChunks(channel, framer::feed)
        return framer.finish()
    }
}

/** SSE allows CR, LF and CRLF, including delimiters and UTF-8 characters split across reads. */
internal class AiSseFramer(private val limits: AiResponseLimits = AiResponseLimits()) {
    private val parser = AiSseDecoder(limits)
    private val line = ByteArrayOutputStream()
    private var afterCr = false
    private var firstLine = true

    private fun dispatchLine() {
        val text = line.toByteArray().decodeToString(throwOnInvalidSequence = true)
        parser.line(if (firstLine) text.removePrefix("\uFEFF") else text)
        firstLine = false
        line.reset()
    }

    fun feed(bytes: ByteArray, count: Int = bytes.size): Boolean {
        if (parser.done) return false
        for (index in 0 until count) {
            val byte = bytes[index].toInt() and 0xff
            if (byte == 10 && afterCr) { afterCr = false; continue }
            afterCr = false
            if (byte == 10 || byte == 13) {
                dispatchLine()
                afterCr = byte == 13
                if (parser.done) return false
            } else {
                check(line.size() < limits.lineBytes) { "AI SSE line exceeds size limit" }
                line.write(byte)
            }
        }
        return true
    }

    fun finish(): AiReply {
        if (!parser.done && line.size() > 0) {
            dispatchLine()
        }
        return parser.finish()
    }
}

/** SSE framing and Chat payload decoding are incremental; no list of raw response lines. */
internal class AiSseDecoder(private val limits: AiResponseLimits = AiResponseLimits()) {
    private val data = StringBuilder()
    private val content = StringBuilder()
    private var event = ""
    private var count = 0
    private var reason: String? = null
    var done = false
        private set

    fun line(line: String) {
        if (done) return
        if (line.isEmpty()) { dispatch(); return }
        if (line.startsWith(':')) return
        val name = line.substringBefore(':')
        val value = line.substringAfter(':', "").removePrefix(" ")
        when (name) {
            "event" -> event = value
            "data" -> {
                check(data.length + value.length + 1 <= limits.eventChars) { "AI SSE event exceeds size limit" }
                if (data.isNotEmpty()) data.append('\n')
                data.append(value)
            }
        }
    }

    private fun dispatch() {
        if (event == "error") error("AI service returned a stream error")
        if (data.isEmpty()) { event = ""; return }
        check(++count <= limits.events) { "AI SSE event count exceeds limit" }
        val payload = data.toString()
        data.clear()
        event = ""
        if (payload.trim() == "[DONE]") { done = true; return }
        val chunk = Json.parseToJsonElement(payload) as? JsonObject ?: error("Invalid AI SSE payload")
        if (chunk["error"] != null && chunk["error"] != JsonNull) error("AI service returned a stream error")
        val choice = (chunk["choices"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return
        val delta = choice["delta"] as? JsonObject
        val text = AiResponseText.blocks(delta?.get("content"))
        check(content.length + text.length <= limits.outputChars) { "AI output exceeds size limit" }
        content.append(text)
        reason = (choice["finish_reason"] as? JsonPrimitive)?.contentOrNull ?: reason
    }

    fun finish(): AiReply {
        if (!done) dispatch()
        return AiReply(content.toString(), AiTruncation.chatCompletions(reason), !done && reason == null)
    }
}

internal object AiResponseText {
    fun blocks(element: JsonElement?): String = when (element) {
        is JsonPrimitive -> if (element.isString) element.content else ""
        is JsonArray -> element.mapNotNull { part ->
            val block = part as? JsonObject ?: return@mapNotNull null
            val type = (block["type"] as? JsonPrimitive)?.content
            if (type == "text" || type == "output_text") (block["text"] as? JsonPrimitive)?.contentOrNull else null
        }.joinToString("")
        else -> ""
    }

    fun parse(body: String, config: AiConfig, maxChars: Int = 1024 * 1024): AiReply {
        val root = Json.parseToJsonElement(body) as? JsonObject ?: error("Invalid AI response")
        check(root["error"] == null || root["error"] == JsonNull) { "AI service returned an error" }
        val choice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
        val content = when {
            config.isAnthropic -> blocks(root["content"])
            config.endpointMode == AiEndpointMode.RESPONSES -> {
                val output = root["output"] as? JsonArray
                output.orEmpty().mapNotNull { item ->
                    val message = item as? JsonObject ?: return@mapNotNull null
                    if ((message["type"] as? JsonPrimitive)?.content == "message") blocks(message["content"]) else null
                }.joinToString("").ifEmpty { blocks(root["output_text"]) }
            }
            else -> blocks((choice?.get("message") as? JsonObject)?.get("content"))
        }
        check(content.length <= maxChars) { "AI output exceeds size limit" }
        fun value(objectValue: JsonObject?, key: String) = (objectValue?.get(key) as? JsonPrimitive)?.contentOrNull
        val truncated = when {
            config.isAnthropic -> AiTruncation.anthropic(value(root, "stop_reason"))
            config.endpointMode == AiEndpointMode.RESPONSES -> AiTruncation.responses(
                value(root, "status"), value(root["incomplete_details"] as? JsonObject, "reason"),
            )
            else -> AiTruncation.chatCompletions(value(choice, "finish_reason"))
        }
        val incomplete = !config.isAnthropic && config.endpointMode == AiEndpointMode.RESPONSES &&
            value(root, "status") in setOf("incomplete", "failed", "cancelled", "in_progress", "queued") && !truncated
        check(content.isNotBlank() || truncated || incomplete) { "AI response has no text content" }
        return AiReply(content, truncated, incomplete)
    }
}
