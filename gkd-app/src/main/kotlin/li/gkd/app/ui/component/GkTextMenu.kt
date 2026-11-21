package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.util.Option

/** Compatibility entry point; all option rows use the same native window dropdown. */
@Composable
fun <T> GkTextMenu(
    modifier: Modifier = Modifier,
    title: String,
    option: Option<T>,
    onOptionChange: ((Option<T>) -> Unit),
) = TextMenu(modifier, title, option, onOptionChange)
