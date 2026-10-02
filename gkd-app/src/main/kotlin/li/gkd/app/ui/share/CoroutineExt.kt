package li.gkd.app.ui.share

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import li.gkd.app.data.RpcError
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ToastUtils.toast
import li.songe.codeorigin.CallSite
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchUi(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context, start) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogUtils.d(e, loc = loc)
        toast(
            e.message ?: e.stackTraceToString(),
            forced = e is RpcError,
            loc = "",
        )
    }
}

fun CoroutineScope.launchUiAction(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.() -> Unit,
): () -> Unit = {
    launchUi(context = context, start = start, loc = loc, block = block)
}

fun <T> CoroutineScope.launchUiAction(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.(T) -> Unit,
): (T) -> Unit = { value ->
    launchUi(context = context, start = start, loc = loc) { block(value) }
}
