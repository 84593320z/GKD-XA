package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 旧版命名兼容层：转发到底座 [GkAppIcon]。 */
@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    appId: String,
    size: Dp = 32.dp,
) = GkAppIcon(modifier = modifier, appId = appId, size = size)
