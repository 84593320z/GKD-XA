package li.gkd.app.feature.snapshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import li.gkd.app.data.screenshotFile
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkAppNameText
import li.gkd.app.ui.component.GkFixedTimeText
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkModalBottomSheet
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.util.IMPORT_SHORT_URL
import li.gkd.app.util.ToastUtils.copyText
import li.gkd.app.util.format
import li.gkd.app.util.getShowActivityId
import li.gkd.db.Snapshot
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkSnapshotActionsSheet(
    snapshot: Snapshot,
    appName: String,
    onDismissRequest: () -> Unit,
    show: Boolean = true,
    onDismissFinished: () -> Unit = {},
    onShare: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onUpload: () -> Unit,
    onSaveToAlbum: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onViewNodes: () -> Unit,
) {
    fun perform(action: () -> Unit) {
        onDismissRequest()
        action()
    }
    GkModalBottomSheet(
        onDismissRequest = onDismissRequest,
        show = show,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SnapshotActionsHeader(snapshot, appName, modifier = Modifier.padding(bottom = 8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                SnapshotActionRow(
                    icon = if (snapshot.githubAssetId == null) GkIcons.Link else GkIcons.ContentCopy,
                    title = if (snapshot.githubAssetId == null) {
                        UiStrings.snapshot_generate_link
                    } else {
                        UiStrings.link_copy
                    },
                    trailingText = snapshot.githubAssetId?.let { "#$it" },
                    onClick = {
                        val assetId = snapshot.githubAssetId
                        if (assetId == null) perform(onUpload) else copyText(IMPORT_SHORT_URL + assetId)
                    },
                )
                SnapshotActionRow(
                    icon = GkIcons.CenterFocusWeak,
                    title = UiStrings.snapshot_view_nodes,
                    onClick = { perform(onViewNodes) },
                )
                SnapshotActionRow(
                    icon = GkIcons.Share,
                    title = UiStrings.action_share,
                    onClick = { perform(onShare) },
                )
                SnapshotActionRow(
                    icon = GkIcons.ArrowDownward,
                    title = UiStrings.action_save_to_downloads,
                    onClick = { perform(onSaveToDownloads) },
                )
                SnapshotActionRow(
                    icon = GkIcons.AddPhotoAlternate,
                    title = UiStrings.action_save_to_album,
                    onClick = { perform(onSaveToAlbum) },
                )
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                SnapshotActionRow(
                    icon = GkIcons.Edit,
                    title = UiStrings.snapshot_replace_screenshot,
                    subtitle = UiStrings.snapshot_replace_screenshot_description,
                    onClick = { perform(onReplace) },
                )
                SnapshotActionRow(
                    icon = GkIcons.Delete,
                    title = UiStrings.action_delete,
                    destructive = true,
                    onClick = { perform(onDelete) },
                )
            }
        }
    }
}

@Composable
private fun SnapshotActionsHeader(
    snapshot: Snapshot,
    appName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val screenshotFile = snapshot.screenshotFile
    val screenshotModifiedAt = screenshotFile.lastModified()
    val request = remember(context, screenshotFile, screenshotModifiedAt) {
        ImageRequest.Builder(context)
            .data(screenshotFile)
            .memoryCacheKey("snapshot-sheet-${snapshot.id}-$screenshotModifiedAt")
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(GkShapes.control)
                .background(MiuixTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GkAppNameText(
                appId = snapshot.appId,
                fallbackName = appName,
                style = MiuixTheme.textStyles.title3,
            )
            getShowActivityId(snapshot.appId, snapshot.activityId)
                ?.takeIf(String::isNotBlank)?.let { activityId ->
                Text(
                    text = activityId,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                GkFixedTimeText(
                    text = snapshot.id.format("yyyy-MM-dd HH:mm:ss"),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = "${snapshot.screenWidth} × ${snapshot.screenHeight}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun SnapshotActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingText: String? = null,
    destructive: Boolean = false,
) {
    val contentColor = if (destructive) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface
    BasicComponent(
        title = title,
        titleColor = BasicComponentDefaults.titleColor(color = contentColor),
        summary = subtitle,
        startAction = {
            GkIcon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        },
        bottomAction = trailingText?.let { value ->
            {
                Text(
                    text = value,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        },
        onClick = onClick,
    )
}
