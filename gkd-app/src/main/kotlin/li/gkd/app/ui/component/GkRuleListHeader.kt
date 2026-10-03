package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.iconTextSize
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
fun GkRuleListHeader(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    legacyStyle: Boolean = false,
    title: @Composable RowScope.() -> Unit,
) {
    val rowModifier = if (legacyStyle) {
        modifier
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 8.dp)
            .squircleClip(cornerRadius = 4.dp)
            .clickable(enabled = enabled, onClickLabel = onClickLabel, onClick = onClick)
            .fillMaxWidth()
            .padding(4.dp)
    } else {
        modifier
            .background(MiuixTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(enabled = enabled, onClickLabel = onClickLabel, onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        title()
        GkIcon(
            imageVector = GkIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.iconTextSize(),
        )
    }
}
