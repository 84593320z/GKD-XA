package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Count the decoded bytes as well as checking Content-Length; chunked bodies have no length. */
internal suspend fun ByteReadChannel.readBoundedBytes(maxBytes: Int): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
    val buffer = ByteArray(minOf(maxBytes + 1L, 8192L).toInt())
    while (true) {
        val count = readAvailable(buffer, 0, minOf(buffer.size, maxBytes - output.size() + 1))
        if (count == -1) break
        if (count == 0) continue
        if (count > maxBytes - output.size()) throw IOException("HTTP response exceeds $maxBytes bytes")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

internal suspend fun HttpClient.downloadTextBounded(
    url: String,
    maxBytes: Int = 8 * 1024 * 1024,
    timeoutMillis: Long = 60_000,
): String = withTimeout(timeoutMillis) {
    prepareGet(url).execute { response ->
        if (response.status.value !in 200..299) throw IOException("HTTP "+response.status.value)
        val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declared != null && (declared < 0 || declared > maxBytes)) {
            throw IOException("HTTP response exceeds $maxBytes bytes")
        }
        response.bodyAsChannel().readBoundedBytes(maxBytes).toString(Charsets.UTF_8)
    }
}
