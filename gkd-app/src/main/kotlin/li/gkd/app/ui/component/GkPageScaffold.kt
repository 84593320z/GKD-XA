package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.ui.share.LocalMiuixBlurActive
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

/**
 * 二级页顶栏作用域：共享 [scrollBehavior] 与毛玻璃透明色。
 *
 * 与旧版 `li.songe.gkd.ui.component.AppPageBarScope` 等价，供自定义顶栏的二级页使用，
 * 避免每个页面各自手写毛玻璃逻辑。
 */
class GkPageBarScope(
    val scrollBehavior: ScrollBehavior,
    val barColor: Color,
)

/**
 * 二级页壳（移植旧版 `AppPageScaffold`）：MIUIX TopAppBar + MiuixScrollBehavior + 毛玻璃顶栏。
 *
 * 旧版二级页统一长这样：顶栏内容滚动到下方时透出毛玻璃，滚动到位后变纯色 surface。
 * 底座此前直接用 `Scaffold + GkTopAppBar`，顶栏恒为实色，与首页的毛玻璃顶栏风格割裂，
 * 这里补齐旧版观感。
 *
 * @param enableContentBlur 为 false 时关闭内容采样毛玻璃（WebView 等会持续重绘导致顶栏闪烁）。
 */
@Composable
fun GkPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    enableContentBlur: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    GkPageScaffold(
        modifier = modifier,
        enableContentBlur = enableContentBlur,
        floatingActionButton = { floatingActionButton() },
        topBar = {
            MiuixTopAppBar(
                title = title,
                color = barColor,
                navigationIcon = navigationIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = true,
            )
        },
        content = content,
    )
}

/**
 * 自定义顶栏的二级页壳。在 [topBar] / [floatingActionButton] 中通过 [GkPageBarScope]
 * 使用统一的 [GkPageBarScope.scrollBehavior] 与 [GkPageBarScope.barColor]。
 */
@Composable
fun GkPageScaffold(
    modifier: Modifier = Modifier,
    enableContentBlur: Boolean = true,
    floatingActionButton: @Composable GkPageBarScope.() -> Unit = {},
    topBar: @Composable GkPageBarScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val store by storeFlow.collectAsStateWithLifecycle()
    val blurActive = enableContentBlur && store.enableMiuixBlur && isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val barColor = if (blurActive) Color.Transparent else surfaceColor
    val scrollBehavior = MiuixScrollBehavior()
    val barScope = remember(scrollBehavior, barColor) {
        GkPageBarScope(scrollBehavior = scrollBehavior, barColor = barColor)
    }
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
        MiuixScaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CompositionLocalProvider(LocalMiuixBlurActive provides blurActive) {
                    Box(
                        modifier = if (blurActive) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                blurRadius = 25f,
                                colors = BlurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(color = surfaceColor.copy(alpha = 0.87f)),
                                    ),
                                ),
                            )
                        } else {
                            Modifier
                        },
                    ) {
                        barScope.topBar()
                    }
                }
            },
            floatingActionButton = { barScope.floatingActionButton() },
            content = { padding ->
                if (blurActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {
                        content(padding)
                    }
                } else {
                    content(padding)
                }
            },
        )
    }
}
