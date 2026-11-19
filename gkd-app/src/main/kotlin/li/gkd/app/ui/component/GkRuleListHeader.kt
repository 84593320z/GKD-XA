package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GkRuleListHeader(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    title: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = GkUi.PageHorizontal)
            .clip(GkShapes.control)
            .clickable(enabled = enabled, onClickLabel = onClickLabel, onClick = onClick)
            .heightIn(min = GkUi.MinTouchTarget)
            .padding(horizontal = GkUi.ContentPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        title()
        GkIcon(
            imageVector = GkIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(GkUi.IconSize),
        )
    }
}
