package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 设置分组：分组小标题 + 圆角 24dp 卡片。
 *
 * 全 App（首页 / 订阅 / 应用 / 设置 / 二级页）统一用它包一组设置项，
 * 圆角、外边距与组间距来自 [GkRowDefaults]，组内分割线用 [GkDivider]。
 */
@Composable
fun PreferenceGroup(
    title: String? = null,
    @Suppress("UNUSED_PARAMETER") showTop: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val counter = remember { GkRowIndexCounter() }
    counter.reset()
    Column(modifier = modifier) {
        if (!title.isNullOrBlank()) {
            GkGroupTitle(title)
        }
        GkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GkRowDefaults.SidePadding)
                .padding(bottom = GkRowDefaults.GroupSpacing),
        ) {
            CompositionLocalProvider(LocalGkRowIndexCounter provides counter) {
                content()
            }
        }
    }
}
