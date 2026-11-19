package li.gkd.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GkPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        pullToRefreshState = state,
        contentPadding = contentPadding,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        refreshTexts = listOf(
            UiStrings.pull_refresh_hint,
            UiStrings.pull_refresh_release,
            UiStrings.pull_refresh_running,
            // The callback may be a no-op or the refresh may fail; do not report success here.
            UiStrings.pull_refresh_finished,
        ),
        refreshTextStyle = MiuixTheme.textStyles.footnote1,
        content = content,
    )
}
