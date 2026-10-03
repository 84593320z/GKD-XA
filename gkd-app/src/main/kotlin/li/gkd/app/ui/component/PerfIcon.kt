package li.gkd.app.ui.component

import androidx.annotation.DrawableRes
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import li.gkd.app.ui.icon.Rocket
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

/** 跟随明暗主题的默认图标色：优先 LocalContentColor，否则 MIUIX onSurface */
@Composable
fun perfDefaultIconTint(): Color = defaultIconTint()

@Composable
fun PerfIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = perfDefaultIconTint(),
    contentDescription: String? = getIconDefaultDesc(imageVector),
) {
    MiuixIcon(
        imageVector = imageVector,
        modifier = modifier,
        contentDescription = contentDescription,
        tint = tint,
    )
}

@Composable
fun PerfIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = perfDefaultIconTint(),
    contentDescription: String? = getIconDefaultDesc(imageVector),
    onClickLabel: String? = null,
) = GkTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    val buttonModifier = modifier.semantics {
        if (onClickLabel != null) {
            this.onClick(label = onClickLabel, action = null)
        }
    }
    MiuixIconButton(
        modifier = buttonModifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        PerfIcon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun PerfIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = perfDefaultIconTint(),
    contentDescription: String? = null,
) = MiuixIcon(
    painter = painterResource(id),
    modifier = modifier,
    contentDescription = contentDescription,
    tint = tint,
)

@Composable
fun PerfIconButton(
    @DrawableRes id: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = perfDefaultIconTint(),
    contentDescription: String? = null,
    onClickLabel: String? = null,
) = GkTooltipIconButtonBox(
    contentDescription = contentDescription,
) {
    MiuixIconButton(
        modifier = modifier.semantics {
            if (onClickLabel != null) {
                this.onClick(label = onClickLabel, action = null)
            }
        },
        enabled = enabled,
        onClick = onClick,
    ) {
        PerfIcon(
            id = id,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

/** 全局图标入口：沿用旧版命名，底层使用底座 GkIcons */
object PerfIcon {
    val Block get() = GkIcons.Block
    val History get() = GkIcons.History
    val Sort get() = GkIcons.Sort
    val Add get() = GkIcons.Add
    val KeyboardArrowRight get() = GkIcons.KeyboardArrowRight
    val ContentCopy get() = GkIcons.ContentCopy
    val MoreVert get() = GkIcons.MoreVert
    val ArrowBack get() = GkIcons.ArrowBack
    val Android get() = GkIcons.Android
    val Edit get() = GkIcons.Edit
    val Save get() = GkIcons.Check
    val Share get() = GkIcons.Share
    val Delete get() = GkIcons.Delete
    val Eco get() = GkIcons.Schedule
    val Close get() = GkIcons.Close
    val OpenInNew get() = GkIcons.OpenInNew
    val Settings get() = GkIcons.Settings
    val Home get() = GkIcons.Home
    val FormatListBulleted get() = GkIcons.FormatListBulleted
    val Apps get() = GkIcons.Apps
    val Info get() = GkIcons.Info
    val ToggleOff get() = GkIcons.ToggleOff
    val ToggleOn get() = GkIcons.ToggleOn
    val HelpOutline get() = GkIcons.HelpOutline
    val ArrowForward get() = GkIcons.ArrowForward
    val Image get() = GkIcons.Image
    val WarningAmber get() = GkIcons.WarningAmber
    val RocketLaunch get() = Rocket
    val WhiteList get() = GkIcons.Lock
    val CenterFocusWeak get() = GkIcons.CenterFocusWeak
    val AutoMode get() = GkIcons.AutoMode
    val LightMode get() = GkIcons.LightMode
    val DarkMode get() = GkIcons.DarkMode
    val VerifiedUser get() = GkIcons.VerifiedUser
    val Api get() = GkIcons.Link
    val Autorenew get() = GkIcons.Autorenew
    val UnfoldMore get() = GkIcons.UnfoldMore
    val Memory get() = GkIcons.Memory
    val Notifications get() = GkIcons.Notifications
    val Layers get() = GkIcons.Layers
    val Equalizer get() = GkIcons.Layers
    val Lock get() = GkIcons.Lock
    val Title get() = GkIcons.Title
    val TextFields get() = GkIcons.TextFields
    val ArrowDownward get() = GkIcons.ArrowDownward
    val Check get() = GkIcons.Check
    val Update get() = GkIcons.Autorenew
    val PageInfo get() = GkIcons.PageInfo
}
