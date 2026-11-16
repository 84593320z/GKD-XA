package li.gkd.app.data.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer

/** Shared budget checked before querying, reading or encoding diagnostic content. */
internal class DiagnosticBudget(
    val maxTextBytes: Long = 8L * 1024 * 1024,
    private val maxTotalBytes: Long = 64L * 1024 * 1024,
) {
    private var writtenBytes = 0L
    var omittedFiles = 0
        private set
    val availableTextBytes: Long get() = minOf(maxTextBytes, maxTotalBytes - writtenBytes).coerceAtLeast(0)

    fun omit() { omittedFiles++ }

    private fun reserve(bytes: Long): Boolean {
        if (bytes < 0 || bytes > maxTotalBytes - writtenBytes) { omit(); return false }
        writtenBytes += bytes
        return true
    }

    /** The producer cannot allocate an unbounded encoded document before this limit applies. */
    fun writeProducedText(target: File, produce: (OutputStream) -> Unit) {
        val limit = availableTextBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (limit == 0) { omit(); return }
        val output = ByteArrayOutputStream(minOf(limit, 8192))
        val bounded = object : OutputStream() {
            override fun write(value: Int) {
                if (output.size() == limit) throw DiagnosticLimitExceeded()
                output.write(value)
            }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                if (length > limit - output.size()) throw DiagnosticLimitExceeded()
                output.write(bytes, offset, length)
            }
        }
        try { produce(bounded) } catch (_: DiagnosticLimitExceeded) { omit(); return }
        writeText(target, output.toString(Charsets.UTF_8.name()))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun <T> writeJson(target: File, json: Json, serializer: SerializationStrategy<T>, value: T) {
        // Check scalars/collections before JSON internals can grow an escaping buffer for a huge string.
        try { serializer.serialize(DiagnosticSizeEncoder(availableTextBytes, json.serializersModule), value) }
        catch (_: DiagnosticLimitExceeded) { omit(); return }
        writeProducedText(target) { json.encodeToStream(serializer, value, it) }
    }

    inline fun <reified T> writeJson(target: File, json: Json, value: T) =
        writeJson(target, json, serializer<T>(), value)

    fun writeText(target: File, text: String) {
        val limit = availableTextBytes
        if (text.length > limit) { omit(); return }
        val bytes = DiagnosticFiles.redact(text).toByteArray(Charsets.UTF_8)
        if (bytes.size > limit) { omit(); return }
        if (!reserve(bytes.size.toLong())) return
        check(target.parentFile?.let { it.isDirectory || it.mkdirs() } != false)
        target.writeBytes(bytes)
    }

    fun copyImage(source: File, target: File) {
        if (!reserve(source.length())) return
        check(target.parentFile?.let { it.isDirectory || it.mkdirs() } != false)
        source.copyTo(target, overwrite = true)
    }

    fun summary(): String = "Diagnostics: at most 500 log rows, pages of 50; config at most 50 rows per table. " +
        "SQL text fields over 1024 characters are replaced by an omission marker, never sliced JSON. " +
        "Rows beyond these limits and oversized entries are omitted; this is not a restorable backup. " +
        "Text limit: $maxTextBytes bytes; total limit: $maxTotalBytes bytes. " +
        "Excluded or over-limit files: $omittedFiles; written bytes: $writtenBytes. " +
        "Recovery files, credentials and arbitrary binary files are excluded."
}

private class DiagnosticLimitExceeded : RuntimeException()

/** Conservative JSON size bound, without constructing strings or JSON trees. */
@OptIn(ExperimentalSerializationApi::class)
private class DiagnosticSizeEncoder(
    private var remaining: Long,
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {
    private fun charge(bytes: Long) {
        if (bytes > remaining) throw DiagnosticLimitExceeded()
        remaining -= bytes
    }
    override fun beginStructure(descriptor: SerialDescriptor) = apply { charge(2) }
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        charge(4L + descriptor.getElementName(index).length.toLong() * 6)
        return true
    }
    override fun encodeString(value: String) { charge(2L + value.length.toLong() * 6) }
    override fun encodeValue(value: Any) { charge(value.toString().length.toLong() + 2) }
    override fun encodeNull() { charge(4) }
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }
}
