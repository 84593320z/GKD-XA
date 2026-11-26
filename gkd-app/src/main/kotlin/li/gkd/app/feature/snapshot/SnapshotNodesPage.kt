package li.gkd.app.feature.snapshot

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import li.gkd.app.MainViewModel
import li.gkd.app.data.ComplexSnapshot
import li.gkd.app.data.NodeInfo
import li.gkd.app.data.snapshot.SnapshotRepository
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkCheckbox
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.style.itemHorizontalPadding
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.json
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data class SnapshotNodesRoute(
    val snapshotId: Long,
    val screenWidth: Int,
    val screenHeight: Int,
) : NavKey

private data class NodesUiState(
    val nodes: List<NodeInfo> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val loaded: Boolean = false,
    val error: String? = null,
)

private fun NodeInfo.displayLabel(): String =
    attr.text?.takeIf { it.isNotBlank() }
        ?: attr.desc?.takeIf { it.isNotBlank() }
        ?: attr.vid?.takeIf { it.isNotBlank() }
        ?: attr.id?.takeIf { it.isNotBlank() }
        ?: attr.name?.takeIf { it.isNotBlank() }
        ?: "节点 $id"

/**
 * 快照节点查看/圈选页（上游 kit 61e69d5 的节点查看 + plus 8ae43e3 的组件圈选在 XA 的合并落地）：
 * 截图上点选或在列表里勾选组件，生成规则时把选中组件单独附给模型，省 token 也减少无关节点干扰。
 * 坐标对齐：截图若按「隐藏状态栏」设置裁过顶栏，节点坐标（全屏系）统一减去裁剪高度再映射。
 */
@Composable
fun SnapshotNodesPage(route: SnapshotNodesRoute) {
    val mainVm = MainViewModel.requireCurrent()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(NodesUiState()) }
    var selectedIds by remember(route.snapshotId) { mutableStateOf<Set<Int>>(emptySet()) }
    var includeAllNodes by remember(route.snapshotId) { mutableStateOf(false) }

    LaunchedEffect(route.snapshotId) {
        state = withContext(Dispatchers.IO) {
            try {
                val snapshot = json.decodeFromString<ComplexSnapshot>(
                    SnapshotRepository.snapshotFile(route.snapshotId).readText(),
                )
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(
                    SnapshotRepository.screenshotFile(route.snapshotId).absolutePath,
                    options,
                )
                NodesUiState(
                    nodes = snapshot.nodes,
                    imageWidth = options.outWidth,
                    imageHeight = options.outHeight,
                    loaded = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NodesUiState(loaded = true, error = e.message ?: e.toString())
            }
        }
    }

    val topCrop = if (state.imageHeight in 1 until route.screenHeight) {
        route.screenHeight - state.imageHeight
    } else {
        0
    }
    val selectedNodes = state.nodes.filter { it.id in selectedIds }
    // 有可读标签的节点才进列表；remember 避免每次选中变化都全量重过滤（节点可达数千）
    val visibleNodes = remember(state.nodes) {
        state.nodes.filter {
            it.attr.text?.isNotBlank() == true ||
                it.attr.desc?.isNotBlank() == true ||
                it.attr.vid?.isNotBlank() == true ||
                it.attr.id?.isNotBlank() == true ||
                it.attr.name?.isNotBlank() == true
        }
    }
    val highlightColor = MiuixTheme.colorScheme.primary
    var imageReady by remember(route.snapshotId, state.imageWidth, state.imageHeight) { mutableStateOf(false) }
    var imageFailed by remember(route.snapshotId, state.imageWidth, state.imageHeight) { mutableStateOf(false) }
    val validImageBounds = state.imageWidth > 0 && state.imageHeight > 0 &&
        route.screenWidth > 0 && route.screenHeight > 0

    GkPageScaffold(
        title = UiStrings.snapshot_nodes_title,
        navigationIcon = {
            GkIconButton(imageVector = GkIcons.ArrowBack, onClick = mainVm::popPage)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
        ) {
            if (!state.loaded) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                return@Column
            }
            val error = state.error
            if (error != null || state.nodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = error ?: UiStrings.snapshot_nodes_empty,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        style = MiuixTheme.textStyles.body2,
                        textAlign = TextAlign.Center,
                        color = if (error != null) MiuixTheme.colorScheme.error
                            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                return@Column
            }
            val preview: @Composable (Modifier) -> Unit = { previewModifier ->
                if (!validImageBounds || imageFailed) {
                    Card(modifier = previewModifier.padding(horizontal = itemHorizontalPadding, vertical = 8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = UiStrings.snapshot_screenshot_unavailable,
                                style = MiuixTheme.textStyles.headline1,
                                color = MiuixTheme.colorScheme.error,
                            )
                            Text(
                                text = "仍可在列表中选择组件，或保留完整节点树生成规则。",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(
                        modifier = previewModifier.background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ratio = state.imageWidth.coerceAtLeast(1).toFloat() / state.imageHeight.coerceAtLeast(1)
                        val imageWidth = minOf(maxWidth, maxHeight * ratio)
                        Box(Modifier.size(imageWidth, imageWidth / ratio).clipToBounds()) {
                            AsyncImage(
                                model = SnapshotRepository.screenshotFile(route.snapshotId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                onLoading = { imageReady = false },
                                onSuccess = { imageReady = true },
                                onError = {
                                    imageReady = false
                                    imageFailed = true
                                },
                            )
                            // Bounds decoding alone cannot prove the actual screenshot is readable.
                            // Never install coordinate hit testing until Coil has decoded the image.
                            if (imageReady) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(state.nodes, topCrop) {
                                            detectTapGestures { offset ->
                                                val screenX = offset.x / size.width * route.screenWidth
                                                val screenY = offset.y / size.height * state.imageHeight + topCrop
                                                val hit = state.nodes
                                                    .filter {
                                                        screenX >= it.attr.left && screenX < it.attr.right &&
                                                            screenY >= it.attr.top && screenY < it.attr.bottom
                                                    }
                                                    .minByOrNull {
                                                        (it.attr.right - it.attr.left).toLong() *
                                                            (it.attr.bottom - it.attr.top)
                                                    }
                                                if (hit != null) {
                                                    selectedIds = if (hit.id in selectedIds) {
                                                        selectedIds - hit.id
                                                    } else {
                                                        selectedIds + hit.id
                                                    }
                                                }
                                            }
                                        },
                                ) {
                                    val scaleX = size.width / route.screenWidth
                                    val scaleY = size.height / state.imageHeight.coerceAtLeast(1)
                                    selectedNodes.forEach { node ->
                                        drawRect(
                                            color = highlightColor,
                                            topLeft = Offset(
                                                node.attr.left * scaleX,
                                                (node.attr.top - topCrop) * scaleY,
                                            ),
                                            size = Size(
                                                (node.attr.right - node.attr.left) * scaleX,
                                                (node.attr.bottom - node.attr.top) * scaleY,
                                            ),
                                            style = Stroke(width = 3f),
                                        )
                                    }
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
            val panel: LazyListScope.() -> Unit = {
                item(key = "controls") {
                    Column {
                        Text(
                            text = UiStrings.snapshot_nodes_hint,
                            modifier = Modifier.padding(horizontal = itemHorizontalPadding, vertical = 8.dp),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = "已选 ${selectedIds.size} 个组件",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.padding(horizontal = itemHorizontalPadding, vertical = 4.dp),
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = itemHorizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GkActionButton(
                                onClick = { selectedIds = emptySet() },
                                enabled = selectedIds.isNotEmpty(),
                            ) {
                                Text(text = UiStrings.snapshot_nodes_clear)
                            }
                            GkActionButton(
                                emphasized = true,
                                onClick = {
                                    val ids = selectedIds
                                    scope.launch {
                                        if (AiRuleGenerator.generateRule(route.snapshotId, ids, includeAllNodes, allowWhenAiDisabled = true)) {
                                            mainVm.popPage()
                                        }
                                    }
                                },
                                enabled = selectedIds.isNotEmpty() || includeAllNodes,
                            ) {
                                Text(
                                    text = if (includeAllNodes && selectedIds.isEmpty()) "用完整节点树生成规则"
                                        else UiStrings.snapshot_nodes_generate,
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .toggleable(value = includeAllNodes, role = Role.Checkbox, onValueChange = { includeAllNodes = it })
                                .heightIn(min = 48.dp)
                                .padding(horizontal = itemHorizontalPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GkCheckbox(checked = includeAllNodes)
                            Text("保留完整节点树", style = MiuixTheme.textStyles.footnote1)
                        }
                    }
                }
                items(
                    items = visibleNodes,
                    key = { it.id },
                ) { node ->
                    val selected = node.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) MiuixTheme.colorScheme.primaryContainer
                                else MiuixTheme.colorScheme.surface,
                            )
                            .toggleable(value = selected, role = Role.Checkbox, onValueChange = {
                                selectedIds = if (selected) selectedIds - node.id else selectedIds + node.id
                            })
                            .heightIn(min = 48.dp)
                            .padding(
                                // 层级缩进封顶，深树节点别把文字挤没
                                start = itemHorizontalPadding + (node.attr.depth * 10).coerceAtMost(60).dp,
                                end = itemHorizontalPadding,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GkCheckbox(checked = selected)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.displayLabel(),
                                style = MiuixTheme.textStyles.body2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "[${node.attr.left},${node.attr.top}][${node.attr.right},${node.attr.bottom}]" +
                                    if (node.attr.clickable) " · 可点击" else "",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Available height is after the scaffold insets. At large font scales, prefer
                // one scroll surface instead of dividing an already short window into slivers.
                val compact = maxHeight < 480.dp * LocalDensity.current.fontScale
                if (compact) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "preview") {
                            preview(Modifier.fillMaxWidth().height((maxHeight * 0.6f).coerceIn(120.dp, 280.dp)))
                        }
                        panel()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        preview(Modifier.fillMaxWidth().weight(1.2f))
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), content = panel)
                    }
                }
            }
        }
    }
}
