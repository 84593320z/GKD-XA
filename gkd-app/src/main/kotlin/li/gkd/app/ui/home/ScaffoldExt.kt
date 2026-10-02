package li.gkd.app.ui.home

import androidx.compose.foundation.layout.PaddingValues
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.ui.component.GkTopAppBar

data class ScaffoldExt(
    val navItem: BottomNavItem,
    val modifier: Modifier = Modifier,
    val topBar: @Composable () -> Unit = {
        GkTopAppBar(titleText = navItem.label)
    },
    val floatingActionButton: @Composable () -> Unit = {},
    val content: @Composable (PaddingValues) -> Unit
)

