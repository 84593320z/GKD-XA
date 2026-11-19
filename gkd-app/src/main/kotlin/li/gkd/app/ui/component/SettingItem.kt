package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** Kept as a source-compatible alias; no second visual design for settings. */
@Composable
fun SettingItem(
    title: String,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    imageVector: ImageVector? = PerfIcon.KeyboardArrowRight,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) = GkSettingItem(title, subtitle, suffix, suffixUnderline, onSuffixClick,
    imageVector, onClick, onClickLabel)
