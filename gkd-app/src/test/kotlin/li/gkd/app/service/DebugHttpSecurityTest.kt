package li.gkd.app.service

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.Assert.*
import org.junit.Test

class DebugHttpSecurityTest {
    @Test fun tokenIsUniqueConstantLengthAndExpires() {
        var now = 100L
        val session = DebugHttpSession(8888, setOf("127.0.0.1"), { now }, 50)
        assertEquals(64, session.token.length)
        assertNotEquals(session.token, DebugHttpSession(8888, setOf("127.0.0.1"), { now }).token)
        assertFalse(session.acceptsToken(null))
        assertFalse(session.acceptsToken("0".repeat(64)))
        assertTrue(session.acceptsToken(session.token))
        now = 150
        assertFalse(session.acceptsToken(session.token))
    }

    @Test fun rejectsRebindingAndUntrustedOrigins() {
        val session = DebugHttpSession(8888, setOf("127.0.0.1", "192.168.1.2"), { 0L })
        assertTrue(session.acceptsHost("127.0.0.1:8888"))
        assertFalse(session.acceptsHost("evil.example:8888"))
        assertFalse(session.acceptsHost("127.0.0.1:8889"))
        assertFalse(session.acceptsHost("u@127.0.0.1:8888"))
        assertFalse(session.acceptsOrigin("null", "127.0.0.1:8888"))
        assertFalse(session.acceptsOrigin("https://inspect.gkd.li.evil", "127.0.0.1:8888"))
        assertTrue(session.acceptsOrigin("http://127.0.0.1:8888", "127.0.0.1:8888"))
    }

    @Test fun unauthorizedRequestsNeverReachPrivilegedHandler() = testApplication {
        // The test engine does not synthesize the Host header sent by real HTTP clients.
        val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
        val session = DebugHttpSession(80, setOf("localhost"), { 0L })
        var calls = 0
        application {
            install(debugHttpSecurity(session))
            routing { post("/api/action") { calls++; call.respondText("ok") } }
        }
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/action").status)
        assertEquals(0, calls)
        assertEquals(HttpStatusCode.Forbidden, client.post("/api/action") {
            header(HttpHeaders.Authorization, "Bearer " + session.token)
            header(HttpHeaders.Origin, "https://untrusted.example")
        }.status)
        assertEquals(0, calls)
        val authorized = client.post("/api/action") {
            header(HttpHeaders.Authorization, "Bearer " + session.token)
        }
        assertEquals(HttpStatusCode.OK, authorized.status)
        assertEquals(1, calls)
        assertEquals("no-store", authorized.headers[HttpHeaders.CacheControl])
        assertNull(authorized.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test fun preflightAndCookieStillRequireTrustedOrigin() = testApplication {
        // The test engine does not synthesize the Host header sent by real HTTP clients.
        val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
        val session = DebugHttpSession(80, setOf("localhost"), { 0L })
        var calls = 0
        application {
            install(debugHttpSecurity(session))
            routing { post("/api/action") { calls++; call.respondText("ok") } }
        }
        val preflight = client.options("/api/action") { header(HttpHeaders.Origin, "https://inspect.gkd.li") }
        assertEquals(HttpStatusCode.NoContent, preflight.status)
        assertEquals("https://inspect.gkd.li", preflight.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/action") {
            header(HttpHeaders.Origin, "https://inspect.gkd.li")
            header(HttpHeaders.Cookie, DebugHttpSession.COOKIE + "=" + session.token)
        }.status)
        assertEquals(0, calls)
        assertEquals(HttpStatusCode.OK, client.post("/api/action") {
            header(HttpHeaders.Cookie, DebugHttpSession.COOKIE + "=" + session.token)
        }.status)
        assertEquals(1, calls)
    }

    @Test fun oversizedAuthorizedRequestsNeverReachPrivilegedHandler() = testApplication {
        // The test engine does not synthesize the Host header sent by real HTTP clients.
        val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
        val session = DebugHttpSession(80, setOf("localhost"), { 0L })
        var calls = 0
        application {
            install(debugHttpSecurity(session))
            routing { post("/api/action") { calls++; call.respondText("ok") } }
        }
        val response = client.post("/api/action") {
            header(HttpHeaders.Authorization, "Bearer " + session.token)
            setBody(ByteArray(DebugHttpSession.MAX_REQUEST_BYTES + 1))
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals(0, calls)
    }

    @Test fun consoleUsesOnlyLocalSessionAndResumesCookieOnRefresh() {
        val module = generateSequence(java.io.File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { java.io.File(it, "gkd-app/src/main/assets/debug-console.html").isFile }
        val page = java.io.File(module, "gkd-app/src/main/assets/debug-console.html").readText()
        assertTrue(page.indexOf("history.replaceState") < page.indexOf("fetch('/session'"))
        assertFalse(page.contains("await import"))
        assertFalse(page.contains("location.href"))
        assertFalse(page.contains("innerHTML"))
        assertTrue(page.contains("if (token)"))
        assertTrue(page.contains("api('getServerInfo')"))
        assertTrue(page.contains("credentials:'same-origin'"))
    }
}
