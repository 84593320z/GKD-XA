package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BoundedHttpDownloadsTest {
    @Test fun decodedBytesAreBoundedWithoutContentLength() = runBlocking {
        assertArrayEquals(byteArrayOf(1, 2, 3), ByteReadChannel(byteArrayOf(1, 2, 3)).readBoundedBytes(3))
        try {
            ByteReadChannel(ByteArray(4)).readBoundedBytes(3)
            fail("Oversized body accepted")
        } catch (_: IOException) { }
    }

    @Test fun rejectsOversizedDeclaredAndChunkedBodiesAndNonSuccess() = runBlocking {
        for ((body, headers, status) in listOf(
            Triple("abcd", headersOf(), HttpStatusCode.OK),
            Triple("abc", headersOf(HttpHeaders.ContentLength, "1000"), HttpStatusCode.OK),
            Triple("abc", headersOf(), HttpStatusCode.BadGateway),
        )) {
            HttpClient(MockEngine { respond(body, status, headers) }).use { client ->
                try { client.downloadTextBounded("https://example.invalid", 3); fail("Invalid response accepted") }
                catch (_: IOException) { }
            }
        }
    }

    @Test fun readsValidResponseAndCancelsTimedOutRequest() = runBlocking {
        HttpClient(MockEngine { respond("abc") }).use { client ->
            assertEquals("abc", client.downloadTextBounded("https://example.invalid", 3))
        }
        HttpClient(MockEngine { delay(5_000); respond("late") }).use { client ->
            try { client.downloadTextBounded("https://example.invalid", timeoutMillis = 30); fail("No timeout") }
            catch (_: TimeoutCancellationException) { }
        }
    }
}
