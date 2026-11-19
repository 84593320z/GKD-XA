package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Each opening has its own identity, including reopening the same item during exit.
class SheetRequest<K : Any>(val key: K)

private data class SheetPresentation<K : Any, T : Any>(
    val request: SheetRequest<K>,
    val snapshot: T,
)

@Composable
fun <K : Any, T : Any> GkRetainedSheet(
    request: SheetRequest<K>?,
    snapshot: T?,
    missing: Boolean,
    onDismissRequest: (SheetRequest<K>) -> Unit,
    retainContent: Boolean = false,
    content: @Composable (
        SheetRequest<K>,
        T,
        Boolean,
        () -> Unit,
        () -> Unit,
    ) -> Unit,
) {
    var retained by remember { mutableStateOf<SheetPresentation<K, T>?>(null) }
    val live = if (request != null && snapshot != null && !missing && !retainContent) {
        SheetPresentation(request, snapshot)
    } else {
        null
    }
    if (live != null) {
        SideEffect { retained = live }
    }
    LaunchedEffect(request, missing) {
        if (request != null && missing) onDismissRequest(request)
    }

    val presentation = live ?: retained
    if (presentation != null) {
        key(presentation.request) {
            // Keep the last valid snapshot mounted while the Miuix sheet runs its exit
            // animation. A newly-created request object reopens immediately and is protected
            // from an old request's onDismissFinished callback by identity comparison.
            val show = request === presentation.request && !missing
            content(
                presentation.request,
                presentation.snapshot,
                show,
                { onDismissRequest(presentation.request) },
                {
                    if (!show && retained?.request === presentation.request) {
                        retained = null
                    }
                },
            )
        }
    }
}
