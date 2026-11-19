package li.gkd.app.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PerfCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    key: Any? = null,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") interactionSource: MutableInteractionSource? = null,
) = GkCheckbox(checked, modifier, onCheckedChange, key, enabled)
