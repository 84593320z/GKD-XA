package li.gkd.app.service

import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/** A credential belongs to one service instance and never survives a restart. */
internal class DebugHttpSession(
    val port: Int,
    hosts: Set<String>,
    private val nanoTime: () -> Long,
    private val lifetimeNanos: Long = TimeUnit.MINUTES.toNanos(30),
) {
    val token: String = ByteArray(32).also(SecureRandom()::nextBytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private val startedAt = nanoTime()
    private val allowedHosts = hosts.map { it.lowercase().removePrefix("[").removeSuffix("]") }.toSet()

    fun acceptsToken(value: String?): Boolean =
        nanoTime() - startedAt < lifetimeNanos && value?.length == token.length &&
            MessageDigest.isEqual(token.toByteArray(Charsets.US_ASCII), value.toByteArray(Charsets.US_ASCII))

    fun acceptsHost(authority: String?): Boolean = parseOrigin("http://" + (authority ?: ""))?.let {
        it.host.lowercase().removePrefix("[").removeSuffix("]") in allowedHosts && effectivePort(it) == port
    } == true

    fun acceptsOrigin(origin: String?, authority: String?): Boolean {
        if (origin == null) return true // Non-browser clients still require the token.
        val uri = parseOrigin(origin) ?: return false
        if (origin in TRUSTED_ORIGINS) return true
        return uri.scheme == "http" && acceptsHost(authority) &&
            origin == "http://" + authority
    }

    companion object {
        const val COOKIE = "gkd_debug_session"
        const val MAX_REQUEST_BYTES = 4 * 1024 * 1024
        val TRUSTED_ORIGINS = setOf("https://inspect.gkd.li", "https://i.gkd.li")
        private fun effectivePort(uri: URI) = if (uri.port == -1) 80 else uri.port
        private fun parseOrigin(value: String): URI? = runCatching {
            URI(value).takeIf {
                it.scheme in setOf("http", "https") && it.host != null &&
                    it.rawUserInfo == null && it.rawQuery == null && it.rawFragment == null &&
                    it.rawPath.orEmpty().isEmpty()
            }
        }.getOrNull()
    }
}
