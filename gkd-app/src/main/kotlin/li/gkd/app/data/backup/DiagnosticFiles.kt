package li.gkd.app.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.nio.file.Files

/** Fail closed for recovery files and arbitrary binaries; diagnostic text never carries credentials. */
internal object DiagnosticFiles {
    private const val MASK = "********"
    private val parser = Json
    private val secretKey = Regex("(?i)(api.?key|authorization|cookie|token|password|secret|credential)")
    private val jsonSecret = Regex("""(?i)("[^"]*(?:api.?key|authorization|cookie|token|password|secret|credential)[^"]*"\s*:\s*)"(?:\\.|[^"\\])*"""")
    private val headerValue = Regex("""(?i)("value"\s*:\s*)"(?:\\.|[^"\\])*"""")
    private val plainSecret = Regex("""(?i)\b((?:api[-_ ]?key|authorization|cookie|token|password|secret|credential)\s*[=:]\s*)(?:bearer\s+)?[^\r\n,}\]]+""")
    private val bearer = Regex("""(?i)\bbearer\s+[^\s,}"']+""")

    fun redact(text: String): String {
        val structured = runCatching { parser.parseToJsonElement(text) }.getOrNull()
        if (structured is JsonObject || structured is JsonArray) return sanitize(structured).toString()
        return text.replace(jsonSecret) { it.groupValues[1] + "\"$MASK\"" }
            .replace(headerValue) { it.groupValues[1] + "\"$MASK\"" }
            .replace(plainSecret) { it.groupValues[1] + MASK }
            .replace(bearer, "Bearer $MASK")
    }

    private fun sanitize(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.mapValues { (key, child) ->
            when {
                secretKey.containsMatchIn(key) -> JsonPrimitive(MASK)
                key.equals("value", true) && value.keys.any { it.equals("name", true) || it.equals("key", true) } -> JsonPrimitive(MASK)
                child is JsonPrimitive && child.isString -> JsonPrimitive(redact(child.content))
                else -> sanitize(child)
            }
        })
        is JsonArray -> JsonArray(value.map(::sanitize))
        is JsonPrimitive -> if (value.isString) JsonPrimitive(redact(value.content)) else value
        else -> value
    }

    fun allowed(file: File): Boolean {
        val name = file.name.lowercase()
        return !name.contains(".corrupt") && !name.contains(".tmp") && !name.endsWith(".bak") &&
            !name.endsWith(".backup") && !name.startsWith(".") &&
            !listOf("cookie", "credential", "token", "secret").any(name::contains)
    }

    fun copySnapshots(source: File, target: File, since: Long?, capturedAt: Long, budget: DiagnosticBudget) {
        if (since == null) return
        source.listFiles()?.forEach { directory ->
            val id = directory.name.toLongOrNull()
            if (id != null && id.toString() == directory.name && id in since..capturedAt && directory.isDirectory) {
                copyTree(directory, target.resolve(directory.name), allowImages = true, budget = budget)
            }
        }
    }

    fun copyTree(source: File, target: File, allowImages: Boolean = false, budget: DiagnosticBudget = DiagnosticBudget()) {
        if (!allowed(source) || Files.isSymbolicLink(source.toPath())) { budget.omit(); return }
        if (source.isDirectory) {
            check(target.isDirectory || target.mkdirs()) { "Cannot create diagnostic directory" }
            val children = source.listFiles() ?: error("Cannot read diagnostic directory")
            children.sortedByDescending { it.lastModified() }.forEach { copyTree(it, target.resolve(it.name), allowImages, budget) }
            if (target.list()?.isEmpty() == true) target.delete()
        } else if (source.isFile) {
            val extension = source.extension.lowercase()
            if (extension in setOf("json", "txt", "log")) {
                if (source.length() > budget.availableTextBytes) { budget.omit(); return }
                val bytes = source.inputStream().use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= budget.availableTextBytes) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                if (bytes.size > budget.availableTextBytes) { budget.omit(); return }
                val text = bytes.toString(Charsets.UTF_8)
                // A malformed JSON document can hide credentials beyond a regex's reach.
                if (extension == "json") {
                    val parsed = runCatching { parser.parseToJsonElement(text) }.getOrNull()
                    if (parsed !is JsonObject && parsed !is JsonArray) { budget.omit(); return }
                }
                budget.writeText(target, text)
            } else if (allowImages && extension in setOf("png", "jpg", "jpeg", "webp")) {
                budget.copyImage(source, target)
            } else {
                budget.omit()
            }
        }
    }
}
