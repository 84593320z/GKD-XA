package li.gkd.app.feature.snapshot

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.style.itemHorizontalPadding
import li.gkd.app.ui.style.scaffoldPadding
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.json
import top.yukonga.miuix.kmp.nav.core.NavKey

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
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

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
    val highlightColor = MiuixTheme.colorScheme.primary

    GkPageScaffold(topBar = {
        GkTopAppBar(
            navigationIcon = {
                GkIconButton(imageVector = GkIcons.ArrowBack, onClick = { mainVm.popPage() })
            },
            titleText = UiStrings.snapshot_nodes_title,
        )
    }) { contentPadding ->
        Column(
            modifier = Modifier
                .scaffoldPadding(contentPadding)
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
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                return@Column
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(route.screenWidth.toFloat() / route.screenHeight)
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = SnapshotRepository.screenshotFile(route.snapshotId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
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
            }
            Text(
                text = UiStrings.snapshot_nodes_hint,
                modifier = Modifier.padding(horizontal = itemHorizontalPadding, vertical = 8.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = itemHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已选 ${selectedIds.size} 个组件",
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { selectedIds = emptySet() },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text(text = UiStrings.snapshot_nodes_clear)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val ids = selectedIds
                        scope.launch {
                            AiRuleGenerator.generateRule(route.snapshotId, ids)
                        }
                        mainVm.popPage()
                    },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text(text = UiStrings.snapshot_nodes_generate)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = state.nodes.filter {
                        it.attr.text?.isNotBlank() == true ||
                            it.attr.desc?.isNotBlank() == true ||
                            it.attr.vid?.isNotBlank() == true ||
                            it.attr.id?.isNotBlank() == true ||
                            it.attr.name?.isNotBlank() == true
                    },
                    key = { it.id },
                ) { node ->
                    val selected = node.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (selected) {
                                    selectedIds - node.id
                                } else {
                                    selectedIds + node.id
                                }
                            }
                            .padding(
                                start = itemHorizontalPadding + (node.attr.depth * 10).dp,
                                end = itemHorizontalPadding,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selected) {
                            GkIcon(
                                imageVector = GkIcons.CheckCircle,
                                tint = MiuixTheme.colorScheme.primary,
                                contentDescription = null,
                            )
                        }
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
                            )
                        }
                    }
                }
            }
        }
    }
}
