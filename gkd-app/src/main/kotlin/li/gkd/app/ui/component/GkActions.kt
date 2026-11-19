package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.GkProvideTextStyle
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shared compact Miuix action; the label can also contain an icon or rich text. */
@Composable
internal fun GkActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    contentColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = ButtonDefaults.InsideMargin,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val colors = if (emphasized) {
        ButtonDefaults.buttonColorsPrimary(contentColor = contentColor.takeOrElse { scheme.onPrimary })
    } else {
        ButtonDefaults.buttonColors(contentColor = contentColor.takeOrElse { scheme.onSecondaryVariant })
    }
    GkProvideTextStyle(MiuixTheme.textStyles.button) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            minWidth = ButtonDefaults.MinWidth,
            minHeight = 48.dp,
            insideMargin = contentPadding,
            content = content,
        )
    }
}

@Composable
internal fun GkBottomActionBar(content: @Composable RowScope.() -> Unit) {
    Surface(color = MiuixTheme.colorScheme.surface) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
