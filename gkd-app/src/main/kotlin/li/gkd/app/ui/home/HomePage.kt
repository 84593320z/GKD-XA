package li.gkd.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.ui.share.LocalMiuixBlurActive
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold

sealed class BottomNavItem(
    val key: Int,
    val label: String,
    val icon: ImageVector,
) {
    object Dashboard : BottomNavItem(
        key = 0,
        label = UiStrings.home_title,
        icon = PerfIcon.Home,
    )

    object SubsManage : BottomNavItem(
        key = 1,
        label = UiStrings.subscription_title,
        icon = PerfIcon.FormatListBulleted,
    )

    object AppList : BottomNavItem(
        key = 2,
        label = UiStrings.apps_title,
        icon = PerfIcon.Apps,
    )

    object Settings : BottomNavItem(
        key = 3,
        label = UiStrings.settings_title,
        icon = PerfIcon.Settings,
    )

    companion object {
        val allSubObjects by lazy { arrayOf(Dashboard, SubsManage, AppList, Settings) }
    }
}

@Serializable
data object HomeRoute : NavKey

@Composable
fun ResetPageScrollOnRequest(
    navItem: BottomNavItem,
    resetScroll: suspend () -> Unit,
) {
    val mainVm = MainViewModel.requireCurrent()
    val request by mainVm.pageScrollResetRequestFlow.collectAsStateWithLifecycle()
    val currentRequest = request
    LaunchedEffect(currentRequest) {
        if (currentRequest?.navItem == navItem) {
            resetScroll()
            mainVm.consumePageScrollResetRequest(currentRequest)
        }
    }
}

/**
 * 首页外壳：完全使用原版 miuix 组件（Scaffold / TopAppBar / NavigationBar / FloatingNavigationBar
 * + miuix-blur 的 textureBlur）。
 *
 * 已弃用的 gkd-miuix 方案（自研液态玻璃底栏、分页采样互斥 hack、离屏图层栅格化）全部移除：
 * 那些逻辑不但代码量大，还会互相抢 LayerBackdrop 导致底栏偶发变黑、转场后毛玻璃断档。
 * 现在只剩「一个 Scaffold + 一个 LayerBackdrop」，模糊由 miuix 官方 textureBlur 统一处理。
 */
@Composable
fun HomePage() {
    val mainVm = MainViewModel.requireCurrent()
    viewModel<SubsManageVm>()
    val tab by mainVm.tabFlow.collectAsStateWithLifecycle()
    val navItems = BottomNavItem.allSubObjects
    val initialIndex = navItems.indexOfFirst { it.key == tab }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { navItems.size })
    // 四个 Tab 的壳在这里构建；内容只由当前页真正组合（HorizontalPager 负责）
    val pages = arrayOf(useControlPage(), useSubsManagePage(), useAppListPage(), useSettingsPage())

    LaunchedEffect(tab) {
        val index = navItems.indexOfFirst { it.key == tab }.coerceAtLeast(0)
        if (index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            val key = navItems.getOrNull(settledPage)?.key ?: return@collect
            mainVm.setTab(key)
        }
    }

    val store by storeFlow.collectAsStateWithLifecycle()
    val blurActive = store.enableMiuixBlur && isRuntimeShaderSupported()
    if (store.useFloatingNavBar) {
        FloatingNavShell(
            pages = pages,
            navItems = navItems,
            pagerState = pagerState,
            blurActive = blurActive,
        )
    } else {
        DockedNavShell(
            pages = pages,
            navItems = navItems,
            pagerState = pagerState,
            blurActive = blurActive,
        )
    }
}

/**
 * miuix Scaffold 会把 content 全屏铺在顶栏下方（place 0,0），顶栏叠在上面。
 * 顶栏走 Scaffold.topBar + textureBlur；内容区只挂**一个** layerBackdrop，
 * 否则多节点抢同一 LayerBackdrop 会让底栏偶发变黑。
 */
@Composable
private fun HomePagerContent(
    pages: Array<ScaffoldExt>,
    pagerState: PagerState,
    contentPadding: PaddingValues,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
    ) { index ->
        val page = pages[index]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(page.modifier),
        ) {
            page.content(contentPadding)
        }
    }
}

/** 停靠式底栏：官方 [MiuixNavigationBar] + textureBlur。 */
@Composable
private fun DockedNavShell(
    pages: Array<ScaffoldExt>,
    navItems: Array<BottomNavItem>,
    pagerState: PagerState,
    blurActive: Boolean,
) {
    val mainVm = MainViewModel.requireCurrent()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val settledPage = pagerState.settledPage

    CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
        MiuixScaffold(
            topBar = {
                BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                    pages.getOrNull(settledPage)?.topBar?.invoke()
                }
            },
            floatingActionButton = {
                pages.getOrNull(settledPage)?.floatingActionButton?.invoke()
            },
            bottomBar = {
                val barColor = if (blurActive) Color.Transparent else surfaceColor
                Box(
                    modifier = if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 25f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(BlendColorEntry(color = surfaceColor.copy(alpha = 0.8f))),
                            ),
                        )
                    } else {
                        Modifier
                    },
                ) {
                    MiuixNavigationBar(color = barColor) {
                        navItems.forEachIndexed { index, item ->
                            MiuixNavigationBarItem(
                                selected = index == pagerState.currentPage,
                                onClick = { mainVm.handleClickTab(item) },
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier),
                ) {
                    HomePagerContent(
                        pages = pages,
                        pagerState = pagerState,
                        contentPadding = padding,
                    )
                }
            },
        )
    }
}

/** 悬浮式底栏：官方 [FloatingNavigationBar] + textureBlur。 */
@Composable
private fun FloatingNavShell(
    pages: Array<ScaffoldExt>,
    navItems: Array<BottomNavItem>,
    pagerState: PagerState,
    blurActive: Boolean,
) {
    val mainVm = MainViewModel.requireCurrent()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val surfaceContainer = MiuixTheme.colorScheme.surfaceContainer
    val floatingBarShape = remember { RoundedCornerShape(FloatingToolbarDefaults.CornerRadius) }
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = surfaceContainer.copy(alpha = 0.6f)),
        ),
    )
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomSpace = 112.dp + navInset
    val settled = pagerState.settledPage
    val hasFab = navItems.getOrNull(settled)?.key == BottomNavItem.AppList.key
    val listBottomSpace = if (hasFab) contentBottomSpace + 72.dp else contentBottomSpace
    val floatingBarColor = if (blurActive) Color.Transparent else surfaceContainer

    CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
        MiuixScaffold(
            topBar = {
                BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                    pages.getOrNull(settled)?.topBar?.invoke()
                }
            },
            floatingActionButton = {
                Box(modifier = Modifier.padding(bottom = 112.dp)) {
                    pages.getOrNull(settled)?.floatingActionButton?.invoke()
                }
            },
            bottomBar = {},
            content = { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier),
                    ) {
                        HomePagerContent(
                            pages = pages,
                            pagerState = pagerState,
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding(),
                                bottom = listBottomSpace,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    ) {
                        FloatingNavigationBar(
                            modifier = if (blurActive) {
                                Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = floatingBarShape,
                                    blurRadius = 25f,
                                    colors = blurColors,
                                    highlight = null,
                                )
                            } else {
                                Modifier
                            },
                            color = floatingBarColor,
                            defaultWindowInsetsPadding = true,
                        ) {
                            navItems.forEachIndexed { index, item ->
                                FloatingNavigationBarItem(
                                    selected = index == pagerState.currentPage,
                                    onClick = { mainVm.handleClickTab(item) },
                                    icon = item.icon,
                                    label = item.label,
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

/**
 * 顶栏毛玻璃容器：miuix 的 textureBlur + 透明 TopAppBar（经 [LocalMiuixBlurActive] 通知页面）。
 */
@Composable
private fun BlurredBar(
    backdrop: LayerBackdrop,
    blurActive: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMiuixBlurActive provides blurActive) {
        Box(
            modifier = if (blurActive) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = 25f,
                    colors = BlurColors(
                        blendColors = listOf(
                            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.87f)),
                        ),
                    ),
                )
            } else {
                Modifier
            },
        ) {
            content()
        }
    }
}
