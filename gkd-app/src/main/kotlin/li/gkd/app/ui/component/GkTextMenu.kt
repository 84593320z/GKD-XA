package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import li.gkd.app.ui.style.itemPadding
import li.gkd.app.util.Option
import li.gkd.app.util.OptionIcon

@Composable
fun <T> GkTextMenu(
    modifier: Modifier = Modifier,
    title: String,
    option: Option<T>,
    onOptionChange: ((Option<T>) -> Unit),
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable {
                expanded = true
            }
            .fillMaxWidth().let {
                if (modifier == Modifier) {
                    it.itemPadding()
                } else {
                    it.then(modifier)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.label,
                style = MiuixTheme.textStyles.body2,
            )
            GkIcon(
                imageVector = GkIcons.UnfoldMore,
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                option.options.forEach { otherOption ->
                    val selected = otherOption.value == option.value
                    DropdownMenuItem(
                        modifier = if (selected) Modifier.background(MiuixTheme.colorScheme.secondaryContainer) else Modifier,
                        leadingIcon = if (otherOption is OptionIcon) ({
                            GkIcon(
                                imageVector = otherOption.icon,
                            )
                        }) else null,
                        text = {
                            Text(text = otherOption.label)
                        },
                        onClick = {
                            expanded = false
                            if (otherOption != option) {
                                onOptionChange(otherOption)
                            }
                        },
                    )
                }
            }
        }
    }
}
