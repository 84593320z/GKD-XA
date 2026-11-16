package li.gkd.app.domain.rule

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Submission acceptance is not completion. Late/duplicate callbacks are harmless. */
internal suspend fun awaitSubmittedAction(
    timeoutMillis: Long,
    submit: ((completed: Boolean) -> Unit) -> Boolean,
): Boolean = withTimeoutOrNull(timeoutMillis) {
    val completion = CompletableDeferred<Boolean>()
    try {
        if (!submit { completion.complete(it) }) false else completion.await()
    } finally {
        completion.cancel()
    }
} ?: false
