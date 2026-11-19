package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.text.UiStrings

/** Historical API, now backed by the same row and native switch as GkTextSwitch. */
@Composable
fun TextSwitch(
    modifier: Modifier = Modifier,
    title: String,
    paddingDisabled: Boolean = false,
    subtitle: String? = null,
    suffix: String? = null,
    suffixUnderline: Boolean = false,
    onSuffixClick: (() -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    checked: Boolean = true,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = DefaultSwitchClick,
    onClickLabel: String? = UiStrings.switch_state_description(title),
) = GkTextSwitch(modifier, title, paddingDisabled, subtitle, suffix, suffixUnderline,
    onSuffixClick, suffixIcon, checked, enabled, onCheckedChange, onClick, onClickLabel)
