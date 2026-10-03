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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.rememberContentReady
import li.gkd.app.ui.component.rememberNavTransitionRunning
import li.gkd.app.ui.liquid.IosLiquidGlassNavigationBar
import li.gkd.app.ui.share.LocalLayerBackdrop
import li.gkd.app.ui.share.LocalMiuixBlurActive
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
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

@Composable
fun HomePage() {
    val mainVm = MainViewModel.requireCurrent()
    viewModel<SubsManageVm>()
    val tab by mainVm.tabFlow.collectAsStateWithLifecycle()
    // KernelSU：转场落定前只组当前 Tab，邻页延后
    val contentReady = rememberContentReady()
    // 进/退二级页时一级页仍在播退场动画；此时毛玻璃每帧采样会和转场抢 GPU
    val navTransitionRunning = rememberNavTransitionRunning()
    val navItems = BottomNavItem.allSubObjects
    val initialIndex = navItems.indexOfFirst { it.key == tab }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { navItems.size })
    val homePager = rememberHomePagerState(pagerState)
    val settled = pagerState.settledPage
    // 转场中只画当前 Tab，邻页先不参与布局/绘制（不卸载已创建的 page 状态，避免来回重组风暴）
    // 返回转场落定后先让毛玻璃在「轻树」上恢复采样，隔两帧再放开邻页重组：
    // 否则邻页重组风暴与模糊恢复抢同一帧，底栏模糊会视觉断档 1-2s
    var pagerRecovery by remember { mutableStateOf(true) }
    LaunchedEffect(navTransitionRunning) {
        if (navTransitionRunning || !contentReady) {
            pagerRecovery = true
        } else {
            withFrameNanos { }
            withFrameNanos { }
            pagerRecovery = false
        }
    }
    val lightPager = !contentReady || navTransitionRunning || pagerRecovery

    val dashboardPage = if (contentReady || settled == 0) useControlPage() else null
    val subsPage = if (contentReady || settled == 1) useSubsManagePage() else null
    val appListPage = if (contentReady || settled == 2) useAppListPage() else null
    val settingsPage = if (contentReady || settled == 3) useSettingsPage() else null
    val pages = arrayOf(dashboardPage, subsPage, appListPage, settingsPage)

    LaunchedEffect(tab) {
        val index = navItems.indexOfFirst { it.key == tab }.coerceAtLeast(0)
        homePager.animateToPage(index)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            homePager.syncPage()
            val key = navItems.getOrNull(settledPage)?.key ?: return@collect
            mainVm.setTab(key)
        }
    }

    val store by storeFlow.collectAsStateWithLifecycle()
    val useFloating = store.useFloatingNavBar
    val blurWanted = store.enableMiuixBlur && isRuntimeShaderSupported()
    // 返回转场（栈顶已回到首页）期间保持毛玻璃采样：否则底栏液态玻璃会在返回动画里
    // 先退化成普通模糊、落定后才恢复，肉眼可见断层。进二级页（首页被覆盖）仍降级，
    // 避免新页首帧的重负载与模糊采样抢 GPU。
    val returningHome = mainVm.topRoute is HomeRoute
    // 仅进页转场窗口内关毛玻璃；被盖住与返回时都保持（避免再出现“二级页底栏变实色”）
    val blurActive = blurWanted && (!navTransitionRunning || returningHome)
    // 液态玻璃组件树保持不变，只关采样；否则转场开头会整棵底栏换树，反而更卡
    val liquidGlass = useFloating && store.enableLiquidGlass && blurWanted

    if (useFloating) {
        MiuixFloatingNavScaffold(
            pages = pages,
            navItems = navItems,
            homePager = homePager,
            blurActive = blurActive,
            liquidGlass = liquidGlass,
            lightPager = lightPager,
            offscreenLayer = navTransitionRunning,
        )
    } else {
        MiuixDockedNavScaffold(
            pages = pages,
            navItems = navItems,
            homePager = homePager,
            blurActive = blurActive,
            lightPager = lightPager,
            offscreenLayer = navTransitionRunning,
        )
    }
}

/**
 * miuix Scaffold 会把 content 全屏铺在顶栏下方（place 0,0），顶栏叠在上面。
 * 顶栏必须走 Scaffold.topBar + textureBlur；内容区只挂**一个** layerBackdrop。
 * 切勿 Column 上下拆开顶栏，否则 backdrop 采不到顶栏区域，看起来像实色遮罩；
 * 也勿给每个 Pager 页各挂 layerBackdrop，多节点抢同一 LayerBackdrop 会让底栏偶发变黑。
 */
@Composable
private fun HomePagerContent(
    pages: Array<ScaffoldExt?>,
    homePager: HomePagerState,
    contentPadding: PaddingValues,
    lightPager: Boolean,
    modifier: Modifier = Modifier,
) {
    val settledPage = homePager.pagerState.settledPage
    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        state = homePager.pagerState,
        beyondViewportPageCount = if (lightPager) 0 else 3,
        userScrollEnabled = !lightPager,
    ) { index ->
        val isCurrentPage = index == settledPage
        if (isCurrentPage || !lightPager) {
            val page = pages[index] ?: return@HorizontalPager
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(page.modifier),
            ) {
                page.content(contentPadding)
            }
        }
    }
}

@Composable
private fun MiuixDockedNavScaffold(
    pages: Array<ScaffoldExt?>,
    navItems: Array<BottomNavItem>,
    homePager: HomePagerState,
    blurActive: Boolean,
    lightPager: Boolean,
    offscreenLayer: Boolean,
) {
    val mainVm = MainViewModel.requireCurrent()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val barColor = if (blurActive) Color.Transparent else surfaceColor
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = surfaceColor.copy(alpha = 0.8f)),
        ),
    )
    val selectedIndex = homePager.selectedPage
    val settledPage = homePager.pagerState.settledPage

    CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
        MiuixScaffold(
            modifier = Modifier.graphicsLayer {
                // 转场时栅格成一层，让 NavDisplay 只做层变换，避免每帧重绘整棵首页树
                if (offscreenLayer) {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            },
            topBar = {
                MiuixBlurredTopBar(
                    backdrop = backdrop,
                    blurActive = blurActive,
                ) {
                    // 顶栏随点击立即切换（selectedPage 在点击当帧更新）：
                    // 任何淡入淡出/落定瞬切在掉帧时都会被感知为「顶栏延迟才切」
                    pages.getOrNull(homePager.selectedPage)?.topBar?.invoke()
                }
            },
            floatingActionButton = {
                pages.getOrNull(settledPage)?.floatingActionButton?.invoke()
            },
            bottomBar = {
                Box(
                    modifier = if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 25f,
                            colors = blurColors,
                        )
                    } else {
                        Modifier
                    },
                ) {
                    MiuixNavigationBar(color = barColor) {
                        navItems.forEachIndexed { index, item ->
                            MiuixNavigationBarItem(
                                selected = index == selectedIndex,
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
                        homePager = homePager,
                        contentPadding = padding,
                        lightPager = lightPager,
                    )
                }
            },
        )
    }
}

@Composable
private fun MiuixFloatingNavScaffold(
    pages: Array<ScaffoldExt?>,
    navItems: Array<BottomNavItem>,
    homePager: HomePagerState,
    blurActive: Boolean,
    liquidGlass: Boolean,
    lightPager: Boolean,
    offscreenLayer: Boolean,
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
    val glassActive = blurActive
    val floatingBarColor = if (glassActive) Color.Transparent else surfaceContainer
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarBody = if (liquidGlass) 72.dp + 40.dp else 52.dp + 40.dp
    val contentBottomSpace = 112.dp + navInset
    val settled = homePager.pagerState.settledPage
    val hasFab = navItems.getOrNull(settled)?.key == BottomNavItem.AppList.key
    val listBottomSpace = if (hasFab) contentBottomSpace + 72.dp else contentBottomSpace
    val selectedIndex = homePager.selectedPage

    CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
        MiuixScaffold(
            modifier = Modifier.graphicsLayer {
                if (offscreenLayer) {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            },
            topBar = {
                MiuixBlurredTopBar(
                    backdrop = backdrop,
                    blurActive = blurActive,
                ) {
                    // 同 docked：顶栏随点击立即切换
                    pages.getOrNull(homePager.selectedPage)?.topBar?.invoke()
                }
            },
            floatingActionButton = {
                Box(modifier = Modifier.padding(bottom = floatingBarBody)) {
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
                            homePager = homePager,
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding(),
                                bottom = listBottomSpace,
                            ),
                            lightPager = lightPager,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    ) {
                        val barItems = remember(navItems) {
                            navItems.map { NavigationItem(label = it.label, icon = it.icon) }
                        }
                        if (liquidGlass) {
                            IosLiquidGlassNavigationBar(
                                items = barItems,
                                selectedIndex = selectedIndex.coerceIn(0, navItems.lastIndex),
                                onItemClick = { index ->
                                    navItems.getOrNull(index)?.let { mainVm.handleClickTab(it) }
                                },
                                backdrop = backdrop,
                                isBlurActive = glassActive,
                            )
                        } else {
                            FloatingNavigationBar(
                                modifier = if (glassActive) {
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
                                        selected = index == selectedIndex,
                                        onClick = { mainVm.handleClickTab(item) },
                                        icon = item.icon,
                                        label = item.label,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

/** Mishka BlurredBar：textureBlur + 透明 TopAppBar（经 LocalMiuixBlurActive）。 */
@Composable
private fun MiuixBlurredTopBar(
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
