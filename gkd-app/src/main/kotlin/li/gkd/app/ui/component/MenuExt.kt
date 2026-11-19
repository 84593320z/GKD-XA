package li.gkd.app.ui.component

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun MenuGroupCard(inTop: Boolean = false, title: String, content: @Composable () -> Unit) =
    GkMenuGroupCard(inTop, title, content)

@Composable
fun MenuItemCheckbox(text: String, checked: Boolean, onClick: () -> Unit, enabled: Boolean = true) =
    GkMenuItemCheckbox(text, checked, onClick, enabled)

@Composable
fun MenuItemCheckbox(text: String, stateFlow: MutableStateFlow<Boolean>, enabled: Boolean = true) =
    GkMenuItemCheckbox(text, stateFlow, enabled)

@Composable
fun MenuItemRadioButton(text: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) =
    GkMenuItemRadioButton(text, selected, onClick, enabled)
