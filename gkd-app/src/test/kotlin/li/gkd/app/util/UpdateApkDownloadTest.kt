package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateApkDownloadTest {
    @get:Rule val temporary = TemporaryFolder()
    private val url = "https://example.invalid/update.apk"

    @Test fun validResponseWritesExactBytesAndVerifiesDigest() = runBlocking {
        val bytes = ByteArray(200_000) { (it % 251).toByte() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        HttpClient(MockEngine { respond(bytes, headers = headersOf(HttpHeaders.ContentLength, bytes.size.toString())) }).use { client ->
            var progress = 0f
            val file = downloadUpdateApk(client, url, temporary.root, bytes.size.toLong(), expectedSha256 = digest) { progress = it }
            assertArrayEquals(bytes, file.readBytes())
            assertEquals(1f, progress, 0f)
        }
    }

    @Test fun unknownReleaseSizeStillHonorsHardByteLimit() = runBlocking {
        HttpClient(MockEngine { respond("abc") }).use { client ->
            val file = downloadUpdateApk(client, url, temporary.root, 0, maxBytes = 3)
            assertEquals("abc", file.readText())
        }
    }

    @Test fun rejectsOversizeMismatchEmptyAndHttpFailuresAndCleansPartialFiles() = runBlocking {
        for ((body, declared, status, expected) in listOf(
            Case("abcd", null, HttpStatusCode.OK, 0),
            Case("abc", "1000", HttpStatusCode.OK, 0),
            Case("abc", null, HttpStatusCode.OK, 2),
            Case("ab", null, HttpStatusCode.OK, 3),
            Case("abc", "2", HttpStatusCode.OK, 0),
            Case("", null, HttpStatusCode.OK, 0),
            Case("abc", null, HttpStatusCode.BadGateway, 0),
        )) {
            val headers = declared?.let { headersOf(HttpHeaders.ContentLength, it) } ?: headersOf()
            HttpClient(MockEngine { respond(body, status, headers) }).use { client ->
                try { downloadUpdateApk(client, url, temporary.root, expected, maxBytes = 3); fail("Invalid download accepted") }
                catch (_: IOException) { }
            }
            assertTrue("Partial APK left behind", temporary.root.listFiles().orEmpty().isEmpty())
        }
    }

    @Test fun sha256MismatchRemovesDownloadedFile() = runBlocking {
        HttpClient(MockEngine { respond("abc") }).use { client ->
            try { downloadUpdateApk(client, url, temporary.root, 3, expectedSha256 = "00".repeat(32)); fail("Wrong digest accepted") }
            catch (_: IOException) { }
        }
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
    }

    @Test fun requestTimeoutRemovesPartialFile() = runBlocking {
        HttpClient(MockEngine { delay(5_000); respond("abc") }).use { client ->
            try { downloadUpdateApk(client, url, temporary.root, 3, timeoutMillis = 30); fail("No deadline") }
            catch (_: TimeoutCancellationException) { }
        }
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
    }

    @Test fun externalCancellationCleansFileImmediately() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        HttpClient(MockEngine { entered.complete(Unit); delay(5_000); respond("abc") }).use { client ->
            val download = async { downloadUpdateApk(client, url, temporary.root, 3) }
            entered.await()
            download.cancelAndJoin()
            assertTrue(download.isCancelled)
        }
        assertTrue(temporary.root.listFiles().orEmpty().isEmpty())
    }

    private data class Case(val body: String, val declared: String?, val status: HttpStatusCode, val expected: Long)
}
