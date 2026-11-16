package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

internal const val MAX_UPDATE_APK_BYTES = 256L * 1024 * 1024
internal const val UPDATE_DOWNLOAD_TIMEOUT_MILLIS = 5 * 60 * 1000L

/** execute keeps the response streaming; get() would save the entire body before returning. */
internal suspend fun downloadUpdateApk(
    httpClient: HttpClient,
    url: String,
    directory: File,
    expectedSize: Long,
    expectedSha256: String? = null,
    maxBytes: Long = MAX_UPDATE_APK_BYTES,
    timeoutMillis: Long = UPDATE_DOWNLOAD_TIMEOUT_MILLIS,
    onProgress: (Float) -> Unit = {},
): File {
    require(maxBytes > 0 && timeoutMillis > 0)
    require(expectedSize >= 0 && expectedSize <= maxBytes) { "更新包大小超出允许范围" }
    require(expectedSha256 == null || expectedSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "更新包 SHA-256 信息无效" }
    val digest = expectedSha256?.let { MessageDigest.getInstance("SHA-256") }
    val file = File.createTempFile("gkd-update-", ".apk", directory)
    try {
        withTimeout(timeoutMillis) {
            httpClient.prepareGet(url).execute { response ->
                if (response.status.value !in 200..299) throw IOException("下载失败：HTTP "+response.status.value)
                val length = response.contentLength()?.takeIf { it >= 0 }
                if (length != null && (length > maxBytes || expectedSize > 0 && length != expectedSize)) {
                    throw IOException("更新包响应大小与发布信息不符")
                }
                val channel = response.bodyAsChannel()
                var total = 0L
                val buffer = ByteArray(64 * 1024)
                FileOutputStream(file).use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = channel.readAvailable(buffer, 0, buffer.size)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > maxBytes || expectedSize > 0 && total > expectedSize) {
                            throw IOException("更新包下载大小超出限制")
                        }
                        digest?.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        val progressSize = expectedSize.takeIf { it > 0 } ?: length ?: 0L
                        onProgress(if (progressSize > 0) (total.toFloat() / progressSize).coerceIn(0f, 1f) else 0f)
                    }
                    currentCoroutineContext().ensureActive()
                    if (total == 0L || expectedSize > 0 && total != expectedSize || length != null && total != length) {
                        throw IOException("更新包下载不完整")
                    }
                    if (digest != null) {
                        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                        if (!actual.equals(expectedSha256, ignoreCase = true)) throw IOException("更新包 SHA-256 校验失败")
                    }
                    output.fd.sync()
                }
            }
        }
        return file
    } catch (error: Throwable) {
        file.delete()
        throw error
    }
}
