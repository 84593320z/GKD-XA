package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings

object GkEmptyStateDefaults {
    val TopPadding = 80.dp
    val HorizontalPadding = 24.dp
    val ActionSpacing = 16.dp
}

/** Place inside the content area, after Scaffold insets and any page controls. */
@Composable
fun GkEmptyState(
    text: String = UiStrings.data_empty,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = GkEmptyStateDefaults.HorizontalPadding,
                top = GkEmptyStateDefaults.TopPadding,
                end = GkEmptyStateDefaults.HorizontalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .squircleClip(cornerRadius = 16.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                GkIcon(
                    imageVector = icon,
                    modifier = Modifier.size(26.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    contentDescription = null,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = if (icon != null) MiuixTheme.textStyles.body1 else MiuixTheme.textStyles.body2,
            textAlign = TextAlign.Center,
            color = if (icon != null) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.footnote1,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(GkEmptyStateDefaults.ActionSpacing))
            action()
        }
    }
}
