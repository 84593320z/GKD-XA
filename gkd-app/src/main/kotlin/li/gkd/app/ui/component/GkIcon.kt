package li.gkd.app.ui.component

import li.gkd.app.text.UiStrings
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import li.songe.morph.compose.AnimatedMorphIcon
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.VerifiedUser
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Replace
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.basic.Close
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun GkIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = getIconDefaultDesc(imageVector),
    animateMorph: Boolean = false,
) {
    if (animateMorph) {
        AnimatedMorphIcon(
            imageVector = imageVector,
            modifier = modifier,
            contentDescription = contentDescription,
            tint = tint,
            animationSpec = tween(300),
        )
    } else {
        Icon(
            imageVector = imageVector,
            modifier = modifier,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

/** Material-free colors for [GkIconButton], including explicit disabled-state colors. */
@Immutable
data class GkIconButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object GkIconButtonDefaults {
    // Miuix defaults to 40.dp; keep the existing toolbar/copy buttons' 48.dp touch targets.
    val MinTouchTargetSize = 48.dp

    /** Named color arguments intentionally match the former Material icon-button factory. */
    @Composable
    fun iconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = defaultIconTint(),
        disabledContainerColor: Color = containerColor,
        disabledContentColor: Color = contentColor.copy(alpha = contentColor.alpha * 0.38f),
    ): GkIconButtonColors = GkIconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )
}

@Composable
fun GkIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: GkIconButtonColors = GkIconButtonDefaults.iconButtonColors(),
    contentDescription: String? = getIconDefaultDesc(imageVector),
    onClickLabel: String? = null,
    animateMorph: Boolean = false,
) = GkTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    IconButton(
        modifier = modifier.semantics(mergeDescendants = true) {
            // Miuix 0.9.4 omits clickable semantics entirely when disabled.
            role = Role.Button
            if (!enabled) disabled()
            if (onClickLabel != null) {
                this.onClick(label = onClickLabel, action = null)
            }
        },
        enabled = enabled,
        onClick = onClick,
        backgroundColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
        minWidth = GkIconButtonDefaults.MinTouchTargetSize,
        minHeight = GkIconButtonDefaults.MinTouchTargetSize,
    ) {
        GkIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) colors.contentColor else colors.disabledContentColor,
            animateMorph = animateMorph,
        )
    }
}

fun getIconDefaultDesc(imageVector: ImageVector): String? = when (imageVector) {
    GkIcons.Add -> UiStrings.action_add
    GkIcons.Edit -> UiStrings.action_edit
    GkIcons.Delete -> UiStrings.action_delete
    GkIcons.Share -> UiStrings.action_share
    GkIcons.Settings -> UiStrings.settings_title
    GkIcons.Close -> UiStrings.action_close
    GkIcons.ArrowBack -> UiStrings.action_back
    GkIcons.HelpOutline -> UiStrings.help_title
    GkIcons.ToggleOff -> UiStrings.action_close
    GkIcons.ToggleOn -> UiStrings.action_turn_on
    GkIcons.History -> UiStrings.history_title
    GkIcons.Sort -> UiStrings.sort_filter
    GkIcons.OpenInNew -> UiStrings.open_new_page
    GkIcons.ContentCopy -> UiStrings.text_copy
    GkIcons.MoreVert -> UiStrings.more_actions
    else -> null
}

/** Regular-weight native glyphs; specialized device/rule symbols retain their domain shapes. */
object GkIcons {
    val Search get() = MiuixIcons.Basic.Search
    val SelectAll get() = MiuixIcons.Regular.SelectAll
    val InvertSelection get() = MiuixIcons.Regular.Replace
    val Filter get() = MiuixIcons.Regular.Filter
    val PlayArrow get() = MiuixIcons.Regular.Play
    val Link get() = MiuixIcons.Regular.Link
    val CheckCircle get() = MiuixIcons.Regular.Ok
    val RemoveCircleOutline get() = MiuixIcons.Regular.Remove
    val StackedDocuments get() = li.gkd.app.ui.icon.StackedDocuments
    val PageInfo get() = li.gkd.app.ui.icon.PageInfo
    val FlashOn get() = li.gkd.app.ui.icon.FlashOn
    val FlashOff get() = li.gkd.app.ui.icon.FlashOff
    val Block get() = MiuixIcons.Regular.Blocklist
    val History get() = MiuixIcons.Regular.Recent
    val Schedule get() = MiuixIcons.Regular.Timer
    val CollapseContent get() = li.gkd.app.ui.icon.CollapseContent
    val ExpandContent get() = li.gkd.app.ui.icon.ExpandContent
    val Sort get() = MiuixIcons.Regular.Sort
    val Add get() = MiuixIcons.Regular.Add
    val KeyboardArrowRight get() = MiuixIcons.Regular.ChevronForward
    val ContentCopy get() = MiuixIcons.Regular.Copy
    val MoreVert get() = MiuixIcons.Regular.More
    val ArrowBack get() = MiuixIcons.Regular.Back
    val Android get() = li.gkd.app.ui.icon.AndroidHead
    val Edit get() = MiuixIcons.Regular.Edit
    val Share get() = MiuixIcons.Regular.Share
    val Delete get() = MiuixIcons.Regular.Delete
    val Close get() = MiuixIcons.Basic.Close
    val OpenInNew get() = Icons.AutoMirrored.Outlined.OpenInNew
    val Settings get() = MiuixIcons.Regular.Settings
    val Home get() = MiuixIcons.Regular.Home
    val FormatListBulleted get() = MiuixIcons.Regular.ListView
    val CalendarMonth get() = MiuixIcons.Regular.Months
    val Apps get() = MiuixIcons.Regular.GridView
    val Info get() = MiuixIcons.Regular.Info
    val Flowchart get() = li.gkd.app.ui.icon.Flowchart
    val Visibility get() = MiuixIcons.Regular.Show
    val VisibilityOff get() = MiuixIcons.Regular.Hide
    val ToggleOff get() = Icons.Outlined.ToggleOff
    val ToggleOn get() = Icons.Outlined.ToggleOn
    val HelpOutline get() = MiuixIcons.Regular.Help
    val ArrowForward get() = MiuixIcons.Regular.Forward
    val Image get() = MiuixIcons.Regular.Image
    val AddPhotoAlternate get() = MiuixIcons.Regular.Photos
    val WarningAmber get() = MiuixIcons.Regular.Report
    val RocketLaunch get() = MiuixIcons.Regular.Promotions
    val CenterFocusWeak get() = MiuixIcons.Regular.Scan
    val AutoMode get() = Icons.Outlined.AutoMode
    val BrightnessAuto get() = Icons.Outlined.BrightnessAuto
    val LightMode get() = Icons.Outlined.LightMode
    val DarkMode get() = Icons.Outlined.DarkMode
    val VerifiedUser get() = Icons.Outlined.VerifiedUser
    val Autorenew get() = MiuixIcons.Regular.Refresh
    val UnfoldMore get() = MiuixIcons.Basic.ArrowUpDown
    val ExpandLess get() = MiuixIcons.Regular.ExpandLess
    val ExpandMore get() = MiuixIcons.Regular.ExpandMore
    val Memory get() = Icons.Default.Memory
    val Notifications get() = Icons.Outlined.Notifications
    val Layers get() = MiuixIcons.Regular.Layers
    val Lock get() = MiuixIcons.Regular.Lock
    val Title get() = MiuixIcons.Regular.Rename
    val TextFields get() = MiuixIcons.Regular.Translate
    val ArrowDownward get() = MiuixIcons.Regular.Download
    val Check get() = MiuixIcons.Basic.Check
}

/** 跟随明暗主题的默认图标色：优先 LocalContentColor，否则 MIUIX onSurface */
@Composable
fun defaultIconTint(): Color {
    val local = LocalContentColor.current
    return if (local == Color.Unspecified) {
        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface
    } else {
        local
    }
}
