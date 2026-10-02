package li.gkd.app.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏：统一使用 MIUIX [TopAppBar]。
 *
 * 与旧 M3 版本相比：
 * - [titleText] 为字符串标题；副标题 [subtitle]；[bottomContent] 放搜索等自定义内容。
 * - [scrollBehavior] 为 MIUIX [ScrollBehavior]，配合 `rememberListScrollState` 等使用。
 * - 不再需要 windowInsets 参数，MIUIX 通过 [defaultWindowInsetsPadding] 处理状态栏内边距。
 */
@Composable
fun GkTopAppBar(
    titleText: String = "",
    subtitle: String = "",
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    largeTitleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    scrollBehavior: ScrollBehavior? = null,
    canScroll: Boolean = true,
) {
    TopAppBar(
        title = titleText,
        subtitle = subtitle,
        bottomContent = bottomContent,
        modifier = modifier,
        color = color,
        titleColor = titleColor,
        largeTitleColor = largeTitleColor,
        subtitleColor = subtitleColor,
        titlePadding = titlePadding,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = if (canScroll) scrollBehavior else null,
        defaultWindowInsetsPadding = true,
    )
}
