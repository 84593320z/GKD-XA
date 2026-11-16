package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.*
import li.gkd.app.data.settings.AiConfig
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AiTransportTest {
    @Test fun doneCompletesBeforeServerClosesAndCancelsUnreadBody() = runBlocking {
        StreamingServer("""data: {"choices":[{"delta":{"content":"streamed"}}]}""" + "\n\ndata: [DONE]\n\n").use { server ->
            HttpClient(OkHttp).use { client ->
                val reply = withTimeout(5000) { AiTransport(client).post(server.config, "{}") }
                assertEquals("streamed", reply.content)
                assertFalse(reply.interrupted)
                assertTrue("response channel must close at DONE", server.disconnected.await(3, TimeUnit.SECONDS))
            }
        }
    }

    @Test fun cancellationClosesLiveResponseAndLeavesClientReusable() = runBlocking {
        HttpClient(OkHttp).use { client ->
            StreamingServer(": waiting\n\n").use { server ->
                val request = async(Dispatchers.IO) { AiTransport(client).post(server.config, "{}") }
                assertTrue(server.sent.await(3, TimeUnit.SECONDS))
                request.cancelAndJoin()
                assertTrue(server.disconnected.await(3, TimeUnit.SECONDS))
            }
            StreamingServer("data: [DONE]\n\n").use { server ->
                assertFalse(withTimeout(5000) { AiTransport(client).post(server.config, "{}") }.interrupted)
            }
        }
    }

    @Test fun totalDeadlineStopsLiveStream() = runBlocking {
        StreamingServer(": waiting\n\n").use { server ->
            HttpClient(OkHttp).use { client ->
                try {
                    AiTransport(client, AiResponseLimits(deadlineMillis = 2000, idleMillis = 5000)).post(server.config, "{}")
                    fail("deadline must expire")
                } catch (_: TimeoutCancellationException) {
                    assertTrue(server.disconnected.await(3, TimeUnit.SECONDS))
                }
            }
        }
    }

    @Test fun oversizedLineClosesConnectionWithoutBufferingWholeBody() = runBlocking {
        StreamingServer("data: " + "x".repeat(256) + "\n\n").use { server ->
            HttpClient(OkHttp).use { client ->
                try {
                    AiTransport(client, AiResponseLimits(lineBytes = 32)).post(server.config, "{}")
                    fail("oversized line must fail")
                } catch (e: IllegalStateException) {
                    assertEquals("AI SSE line exceeds size limit", e.message)
                    assertTrue(server.disconnected.await(3, TimeUnit.SECONDS))
                }
            }
        }
    }

    /** Never sends the final chunk: a buffering HTTP client will hang. */
    private class StreamingServer(payload: String) : AutoCloseable {
        private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        @Volatile private var socket: Socket? = null
        val sent = CountDownLatch(1)
        val disconnected = CountDownLatch(1)
        val config = AiConfig(apiUrl = "http://127.0.0.1:" + server.localPort + "/v1", apiKey = "test", model = "test")
        private val worker = thread(isDaemon = true, name = "ai-stream-fixture") {
            try {
                server.accept().use { connection ->
                    socket = connection
                    connection.soTimeout = 10000
                    val input = connection.getInputStream().bufferedReader(Charsets.US_ASCII)
                    var bodySize = 0
                    while (true) {
                        val line = input.readLine() ?: return@thread
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) bodySize = line.substringAfter(':').trim().toInt()
                    }
                    repeat(bodySize) { input.read() }
                    val output = connection.getOutputStream()
                    output.write("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nTransfer-Encoding: chunked\r\n\r\n".toByteArray())
                    val bytes = payload.toByteArray()
                    output.write((bytes.size.toString(16) + "\r\n").toByteArray())
                    output.write(bytes)
                    output.write("\r\n".toByteArray())
                    output.flush()
                    sent.countDown()
                    try {
                        if (input.read() == -1) disconnected.countDown()
                    } catch (_: SocketTimeoutException) {
                        // Timeout is not evidence of response cleanup.
                    } catch (_: SocketException) {
                        disconnected.countDown()
                    }
                }
            } catch (_: SocketException) {
                // Fixture disposal unblocks accept/read on failed assertions.
            }
        }
        override fun close() {
            socket?.close()
            server.close()
            worker.join(1000)
        }
    }
}
