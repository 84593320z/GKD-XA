package li.gkd.app.ui.component

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.lineHeightDp
import li.gkd.app.ui.style.toSpDpOr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import li.gkd.app.data.RawSubscription
import li.gkd.app.util.mapState
import li.gkd.app.data.subscription.SubscriptionState
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarState

@Composable
fun useSubs(subsId: Long?): RawSubscription? {
    val scope = rememberCoroutineScope()
    return remember(subsId) {
        SubscriptionState.subsMapFlow.mapState(scope) { it[subsId] }
    }.collectAsStateWithLifecycle().value
}

@Composable
fun useSubsGroup(
    subs: RawSubscription?,
    groupKey: Int?,
    appId: String?,
): RawSubscription.RawGroupProps? {
    return remember(subs, groupKey, appId) {
        if (subs != null && groupKey != null) {
            if (appId != null) {
                subs.apps.find { it.id == appId }?.groups?.find { it.key == groupKey }
            } else {
                subs.globalGroups.find { it.key == groupKey }
            }
        } else {
            null
        }
    }
}

@Composable
fun Modifier.autoFocus(immediateFocus: Boolean = false): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(null) {
        if (!immediateFocus) {
            delay(DefaultDurationMillis.toLong())
        }
        focusRequester.requestFocus()
    }
    return focusRequester(focusRequester)
}

private fun ScrollBehavior.resetScroll() {
    state.heightOffset = 0f
    state.contentOffset = 0f
}

private class ListChangeMarker<T>(
    var list: List<T>,
    var leadingItemKey: Any?,
)
@Composable
fun <T> GkResetOnItemKeysChange(
    list: List<T>,
    key: (T) -> Any,
    leadingItemKey: Any? = null,
    onChange: () -> Unit,
) {
    // Immutable list snapshots let us skip the key comparison on unrelated recompositions.
    val previous = remember { ListChangeMarker(list, leadingItemKey) }
    SideEffect {
        val changed = previous.leadingItemKey != leadingItemKey ||
            (previous.list !== list && (
                previous.list.size != list.size ||
                list.indices.any { index -> key(previous.list[index]) != key(list[index]) }
            ))
        previous.list = list
        previous.leadingItemKey = leadingItemKey
        if (changed) onChange()
    }
}

@Stable
class ListScrollState(
    val scrollBehavior: ScrollBehavior,
    val listState: LazyListState,
    private val coroutineScope: CoroutineScope,
) {
    private var resetJob: Job? = null

    private fun requestScrollReset() {
        resetJob?.cancel()
        resetJob = null
        scrollBehavior.resetScroll()
        listState.requestScrollToItem(0)
    }

    private suspend fun performScrollReset() {
        scrollBehavior.resetScroll()
        listState.scrollToItem(0)
    }

    fun resetScroll() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            performScrollReset()
        }
    }

    suspend fun resetScrollAndAwait() {
        resetJob?.cancelAndJoin()
        performScrollReset()
    }

    suspend fun scrollToItemAndAwait(index: Int) {
        resetJob?.cancelAndJoin()
        scrollBehavior.resetScroll()
        listState.scrollToItem(index)
    }

    @Composable
    fun ResetOnChange(vararg keys: Any?, enabled: Boolean = true) {
        val currentKeys = rememberUpdatedState(keys.toList())
        val currentEnabled = rememberUpdatedState(enabled)
        LaunchedEffect(this) {
            snapshotFlow { currentKeys.value }
                .drop(1)
                .collect { if (currentEnabled.value) resetScroll() }
        }
    }

    @Composable
    fun <T> ResetOnListChange(
        list: List<T>,
        key: (T) -> Any,
        leadingItemKey: Any? = null,
        enabled: Boolean = true,
    ) {
        GkResetOnItemKeysChange(list, key, leadingItemKey) {
            if (enabled) requestScrollReset()
        }
    }
}

@Stable
class ColumnScrollState(
    val scrollBehavior: ScrollBehavior,
    val scrollState: ScrollState,
    private val coroutineScope: CoroutineScope,
) {
    private var resetJob: Job? = null

    private suspend fun performScrollReset() {
        scrollBehavior.resetScroll()
        scrollState.scrollTo(0)
    }

    fun resetScroll() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            performScrollReset()
        }
    }

    suspend fun resetScrollAndAwait() {
        resetJob?.cancelAndJoin()
        performScrollReset()
    }
}

@Composable
fun rememberListScrollState(
    canScroll: () -> Boolean = { true },
): ListScrollState {
    val coroutineScope = rememberCoroutineScope()
    val currentCanScroll = rememberUpdatedState(canScroll)
    val stableCanScroll = remember { { currentCanScroll.value() } }
    val scrollBehavior = MiuixScrollBehavior(
        state = rememberSaveable(saver = TopAppBarState.Saver) {
            TopAppBarState(-Float.MAX_VALUE, 0f, 0f)
        },
        canScroll = stableCanScroll,
    )
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }
    return remember(scrollBehavior, listState, coroutineScope) {
        ListScrollState(scrollBehavior, listState, coroutineScope)
    }
}

@Composable
fun rememberPinnedListScrollState(): ListScrollState {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior(
        state = rememberSaveable(saver = TopAppBarState.Saver) {
            TopAppBarState(-Float.MAX_VALUE, 0f, 0f)
        },
        canScroll = { false },
    )
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }
    return remember(scrollBehavior, listState, coroutineScope) {
        ListScrollState(scrollBehavior, listState, coroutineScope)
    }
}

@Composable
fun rememberColumnScrollState(): ColumnScrollState {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior(
        state = rememberSaveable(saver = TopAppBarState.Saver) {
            TopAppBarState(-Float.MAX_VALUE, 0f, 0f)
        },
    )
    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(initial = 0) }
    return remember(scrollBehavior, scrollState, coroutineScope) {
        ColumnScrollState(scrollBehavior, scrollState, coroutineScope)
    }
}

val ScrollBehavior.isFullVisible: Boolean
    @Composable
    @ReadOnlyComposable
    get() = state.collapsedFraction == 0f

@Composable
@ReadOnlyComposable
fun Modifier.textSize(
    style: TextStyle = TextStyle.Default,
    density: Density = LocalDensity.current,
): Modifier {
    // MIUIX 部分 TextStyle 的 fontSize/lineHeight 不是 Sp，直接 toDp() 会抛
    // IllegalStateException: Only Sp can convert to Px
    val fontSizeDp = style.fontSize.toSpDpOr(density, 14.dp)
    val lineHeightDp = style.lineHeightDp(density)
    return height(lineHeightDp).width(fontSizeDp)
}

/** 兼容旧版：把可能为 State 的值解包。 */
@Composable
private fun getCompatStateValue(v: Any?): Any? = when (v) {
    is State<*> -> v.value
    else -> v
}

@Composable
fun useListScrollState(
    v1: Any?,
    v2: Any? = null,
    v3: Any? = null,
): LazyListState {
    val x1 = getCompatStateValue(v1)
    val x2 = getCompatStateValue(v2)
    val x3 = getCompatStateValue(v3)
    return rememberSaveable(x1, x2, x3, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
}

@Composable
fun usePinnedScrollBehaviorState(v1: Any?): LazyListState {
    val x1 = getCompatStateValue(v1)
    return rememberSaveable(x1, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
}

@Composable
fun useScrollBehaviorState(v1: Any?): ScrollState {
    val x1 = getCompatStateValue(v1)
    return rememberSaveable(x1, saver = ScrollState.Saver) { ScrollState(initial = 0) }
}
