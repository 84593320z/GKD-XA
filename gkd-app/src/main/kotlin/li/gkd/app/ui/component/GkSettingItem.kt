package li.gkd.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.style.itemPadding
import li.gkd.app.util.TimeUtils.throttle

@Composable
fun GkSettingItem(
    title: String,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    imageVector: ImageVector? = GkIcons.KeyboardArrowRight,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    subtitleOverflow: TextOverflow = TextOverflow.Clip,
) {
    Row(
        modifier = Modifier
            .let {
                if (onClick != null) {
                    it.clickable(
                        onClick = throttle(fn = onClick),
                        onClickLabel = onClickLabel ?: UiStrings.page_enter_description(title)
                    )
                } else {
                    it
                }
            }
            .fillMaxWidth()
            .itemPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = if (imageVector != null) Modifier.weight(1f) else Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
            )
            if (subtitle != null) {
                if (suffix != null) {
                    Row {
                        Text(
                            text = subtitle,
                            maxLines = subtitleMaxLines,
                            overflow = subtitleOverflow,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = suffix,
                            style = MiuixTheme.textStyles.body2.run {
                                if (suffixUnderline) {
                                    copy(textDecoration = TextDecoration.Underline)
                                } else {
                                    this
                                }
                            },
                            color = MiuixTheme.colorScheme.primary,
                            modifier = if (onSuffixClick != null) Modifier.clickable(
                                onClick = throttle(fn = onSuffixClick),
                            ) else Modifier
                        )
                    }
                } else {
                    Text(
                        text = subtitle,
                        maxLines = subtitleMaxLines,
                        overflow = subtitleOverflow,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        if (imageVector != null) {
            GkIcon(
                imageVector = imageVector,
                contentDescription = null,
            )
        }
    }
}
