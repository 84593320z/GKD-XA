package li.gkd.app.util

import kotlinx.coroutines.CancellationException

/** Input preparation happens before the scheduler takes ownership of failures. */
internal suspend fun <T> admitAiInput(
    onFailure: (Exception) -> Unit,
    load: suspend () -> T,
): T? = try {
    load()
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    onFailure(error)
    null
}
