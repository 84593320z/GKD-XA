package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import li.gkd.app.data.appinfo.AppInfoRepository
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.share.ListPlaceholder
import li.gkd.app.ui.style.GkUi
import li.gkd.app.ui.style.TABULAR_NUMBERS_FONT_FEATURE
import li.gkd.app.util.format
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun <T : Any> GkLogTimeline(
    items: LazyPagingItems<T>,
    listState: LazyListState,
    key: (T) -> Any,
    appId: (T) -> String,
    time: (T) -> Long,
    onOpenApp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showAppHeaders: Boolean = true,
    contextChanged: ((T, T) -> Boolean)? = null,
    contextHeader: (@Composable (T, T?) -> Unit)? = null,
    content: @Composable LazyItemScope.(T) -> Unit,
) {
    LazyColumn(modifier = modifier, state = listState) {
        val itemKey = items.itemKey(key)
        val contexts = mutableMapOf<Any, GkLogAppContext>()
        var previousDate: String? = null
        var previousApp: String? = null
        var previousItem: T? = null
        var currentContext: GkLogAppContext? = null
        var currentDateKey: String? = null
        for (index in 0 until items.itemCount) {
            val preview = items.peek(index)
            if (preview != null) {
                val date = time(preview).format("yyyy-MM-dd")
                val id = appId(preview)
                val dateChanged = date != previousDate
                if (dateChanged) {
                    val dateKey = "date:${key(preview)}"
                    currentDateKey = dateKey
                    stickyHeader(key = dateKey) {
                        GkLogDateHeader(date, dateKey, listState, contexts)
                    }
                }
                val appChanged = dateChanged || id != previousApp
                if (appChanged && showAppHeaders) {
                    val appKey = "app:${key(preview)}"
                    currentContext = GkLogAppContext(requireNotNull(currentDateKey), appKey, id)
                    contexts[appKey] = currentContext
                    item(key = appKey) {
                        GkLogAppHeader(id, dateChanged, onClick = onOpenApp?.let { open -> { open(id) } })
                    }
                }
                val precedingItem = previousItem.takeUnless { appChanged }
                if (contextHeader != null &&
                    (precedingItem == null || contextChanged?.invoke(precedingItem, preview) == true)
                ) {
                    val contextKey = "context:${key(preview)}"
                    currentContext?.let { contexts[contextKey] = it }
                    item(key = contextKey) { contextHeader(preview, precedingItem) }
                }
                currentContext?.let { contexts[itemKey(index)] = it }
                previousDate = date
                previousApp = id
                previousItem = preview
            }
            item(key = itemKey(index)) {
                items[index]?.let { content(it) }
            }
        }
        item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
            val refresh = items.loadState.refresh
            val append = items.loadState.append
            // One load-state branch owns the footer; retries happen only on a user tap.
            when {
                refresh is LoadState.Error -> GkLogLoadError(refresh.error, onRetry = items::retry)
                items.itemCount == 0 && refresh is LoadState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap),
                    ) {
                        CircularProgressIndicator()
                        Text(UiStrings.loading_progress, style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
                append is LoadState.Error -> GkLogLoadError(append.error, onRetry = items::retry)
                items.itemCount == 0 -> GkEmptyState(text = UiStrings.data_empty)
                append is LoadState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(GkUi.ContentPadding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(GkUi.IconSize))
                    }
                }
            }
            GkPageBottomSpace()
        }
    }
}

@Composable
private fun GkLogLoadError(cause: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(GkUi.ContentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap),
    ) {
        GkIcon(
            imageVector = GkIcons.WarningAmber,
            modifier = Modifier.size(GkUi.IconSize),
            tint = MiuixTheme.colorScheme.error,
            contentDescription = null,
        )
        Text(
            text = cause.message?.takeIf { it.isNotBlank() } ?: UiStrings.data_load_failed,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        GkActionButton(onClick = onRetry) {
            Text(UiStrings.action_reload)
        }
    }
}

@Composable
fun GkLogTimeText(time: Long) {
    val formatted = time.format("HH:mm:ss.SSS")
    val color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Text(
        text = buildAnnotatedString {
            append(formatted.dropLast(4))
            withStyle(SpanStyle(color = color.copy(alpha = 0.65f))) {
                append(formatted.takeLast(4))
            }
        },
        style = MiuixTheme.textStyles.footnote1.copy(
            fontFeatureSettings = TABULAR_NUMBERS_FONT_FEATURE,
        ),
        color = color,
        softWrap = false,
        maxLines = 1,
    )
}

fun Modifier.gkLogTimelineRail(color: Color): Modifier = drawBehind {
    val railX = 32.dp.toPx()
    drawLine(
        color = color,
        start = Offset(railX, 0f),
        end = Offset(railX, size.height),
        strokeWidth = 1.dp.toPx(),
    )
}

private data class GkLogAppContext(
    val dateKey: String,
    val headerKey: String,
    val appId: String,
)

@Composable
private fun GkLogDateHeader(
    date: String,
    dateKey: String,
    listState: LazyListState,
    appContexts: Map<Any, GkLogAppContext>,
) {
    val appId by remember(dateKey, listState, appContexts) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val header = layout.visibleItemsInfo.find { it.key == dateKey }
            if (header == null || header.offset > layout.viewportStartOffset) {
                null
            } else {
                val headerBottom = header.offset + header.size
                val first = layout.visibleItemsInfo
                    .filter {
                        it.key != dateKey && it.offset + it.size > headerBottom &&
                            it.offset < layout.viewportEndOffset
                    }
                    .minByOrNull { it.index }
                val context = first?.let { appContexts[it.key] }
                val appHeader = layout.visibleItemsInfo.find { it.key == context?.headerKey }
                if (context?.dateKey == dateKey &&
                    (appHeader == null || appHeader.offset + appHeader.size <= headerBottom)
                ) {
                    context.appId
                } else {
                    null
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .heightIn(min = GkUi.MinTouchTarget)
            .padding(start = 24.dp, end = GkUi.PageHorizontal, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GkIcon(
            imageVector = GkIcons.Schedule,
            modifier = Modifier.size(16.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            contentDescription = null,
        )
        Text(
            text = date,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.primary,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            appId?.let { id ->
                val apps by AppInfoRepository.appInfoMapFlow.collectAsStateWithLifecycle()
                Text(
                    text = apps[id]?.name ?: id,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GkLogAppHeader(
    appId: String,
    firstInDate: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .gkLogTimelineRail(MiuixTheme.colorScheme.outline)
            .padding(top = if (firstInDate) 0.dp else GkUi.SectionGap)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = GkUi.MinTouchTarget)
            .padding(
                start = GkUi.PageHorizontal,
                end = GkUi.PageHorizontal,
                top = 8.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            GkAppIcon(appId = appId, size = GkUi.IconSize)
        }
        GkAppNameText(
            appId = appId,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.weight(1f),
        )
    }
}
