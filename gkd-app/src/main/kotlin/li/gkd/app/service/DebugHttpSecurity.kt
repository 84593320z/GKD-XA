package li.gkd.app.service

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond

internal fun debugHttpSecurity(session: DebugHttpSession) = createApplicationPlugin("DebugHttpSecurity") {
    onCall { call ->
        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.response.header("Referrer-Policy", "no-referrer")
        call.response.header("X-Content-Type-Options", "nosniff")
        call.response.header("X-Frame-Options", "DENY")
        val host = call.request.headers[HttpHeaders.Host]
        val origin = call.request.headers[HttpHeaders.Origin]
        if (!session.acceptsHost(host) || !session.acceptsOrigin(origin, host)) {
            call.respond(HttpStatusCode.Forbidden)
            return@onCall
        }
        if (origin != null) {
            call.response.header(HttpHeaders.AccessControlAllowOrigin, origin)
            call.response.header(HttpHeaders.Vary, "Origin")
            call.response.header(HttpHeaders.AccessControlAllowMethods, "POST, OPTIONS")
            call.response.header(HttpHeaders.AccessControlAllowHeaders, "Authorization, Content-Type")
            // A browser still has to pass the exact origin and bearer checks.
            call.response.header("Access-Control-Allow-Private-Network", "true")
        }
        if (call.request.httpMethod == HttpMethod.Options) {
            call.respond(HttpStatusCode.NoContent)
            return@onCall
        }
        val path = call.request.path()
        if (path == "/" && call.request.httpMethod == HttpMethod.Get) return@onCall
        val header = call.request.headers[HttpHeaders.Authorization]
        val bearer = header?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)
        // Cookies are accepted only for same-origin requests. Cross-origin clients must send Bearer.
        val sameOrigin = origin == null || origin == "http://" + host
        val cookie = if (sameOrigin && path != "/session") call.request.cookies[DebugHttpSession.COOKIE] else null
        if (!session.acceptsToken(bearer ?: cookie)) {
            call.response.header(HttpHeaders.WWWAuthenticate, "Bearer realm=GKD-debug")
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
        val length = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (length != null && (length < 0 || length > DebugHttpSession.MAX_REQUEST_BYTES)) {
            call.respond(HttpStatusCode.PayloadTooLarge)
            return@onCall
        }
    }
}
