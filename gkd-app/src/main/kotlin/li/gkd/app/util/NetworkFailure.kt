package li.gkd.app.util

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.utils.io.ClosedByteChannelException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * 只归纳传输层失败（对齐上游 70f89350 的 networkFailureSummary）：这类失败打全栈没有
 * 信息量，一行摘要足够排障；识别不了的失败返回 null，调用方照常打全栈。
 */
fun networkFailureSummary(error: Throwable): String? =
    when (error) {
        is ResponseException -> "HTTP ${error.response.status.value}"
        is HttpRequestTimeoutException,
        is SocketTimeoutException -> "network timeout"
        is UnknownHostException -> "DNS resolution failed"
        is ConnectException,
        is NoRouteToHostException -> "connection unavailable"
        is SocketException -> "socket connection failed"
        is SSLException -> "TLS ${error.javaClass.simpleName}"
        is ClosedByteChannelException -> error.cause?.let(::networkFailureSummary)
        else -> null
    }

/** 传输失败记一行摘要，其他失败记全栈。 */
fun logNetworkFailure(operation: String, error: Exception) {
    val summary = networkFailureSummary(error)
    if (summary != null) {
        LogUtils.d("$operation: $summary")
    } else {
        LogUtils.d(operation, error)
    }
}
