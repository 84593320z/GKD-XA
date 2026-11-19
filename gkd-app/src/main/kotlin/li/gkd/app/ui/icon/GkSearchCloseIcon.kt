package li.gkd.app.ui.icon

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButtonDefaults
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkTooltipIconButtonBox
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor

@Composable
fun GkSearchCloseIconButton(
    onClick: () -> Unit,
    isSearchOpen: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String = if (isSearchOpen) UiStrings.search_close else UiStrings.search_open,
) = GkTooltipIconButtonBox(contentDescription) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        minWidth = GkIconButtonDefaults.MinTouchTargetSize,
        minHeight = GkIconButtonDefaults.MinTouchTargetSize,
    ) {
        GkSearchCloseIcon(isSearchOpen = isSearchOpen, tint = tint,
            contentDescription = contentDescription)
    }
}

@Composable
fun GkSearchCloseIcon(
    isSearchOpen: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String = if (isSearchOpen) UiStrings.search_close else UiStrings.search_open,
) {
    // Crossfade native glyphs instead of a Material contour or JNI-dependent stroke inference.
    Crossfade(targetState = isSearchOpen, modifier = modifier, animationSpec = tween(180), label = "SearchClose") { open ->
        GkIcon(imageVector = if (open) GkIcons.Close else GkIcons.Search,
            tint = tint, contentDescription = contentDescription)
    }
}
