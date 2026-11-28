package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.util.TimeUtils.throttle
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 可搜索的列表选择弹窗：选中项用主色文字标记，不显示对勾。
 * [searchPlaceholder] 为空时退化为普通列表。
 */
@Composable
fun TextSearchListDialog(
    onDismiss: () -> Unit,
    textList: List<Pair<String, () -> Unit>>,
    title: String? = null,
    selectedText: String? = null,
    searchPlaceholder: String? = null,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(textList, query) {
        if (query.isEmpty()) textList
        else textList.filter { it.first.contains(query, ignoreCase = true) }
    }
    GkDialog(onDismissRequest = onDismiss, nativePlacement = true) {
        GkDialogSurface(
            shape = GkShapes.dialog,
            containerColor = DialogDefaults.backgroundColor(),
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                title?.let {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = it,
                            style = MiuixTheme.textStyles.title4,
                            color = DialogDefaults.titleColor(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (searchPlaceholder != null) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = searchPlaceholder,
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                }
                if (filtered.isEmpty()) {
                    Text(
                        text = "无匹配结果",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .selectableGroup()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        filtered.forEach { (text, onClickItem) ->
                            val isSelected = text == selectedText
                            BasicComponent(
                                modifier = Modifier.semantics { selected = isSelected },
                                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                role = if (selectedText != null) Role.RadioButton else Role.Button,
                                onClickLabel = if (selectedText != null) "选择 $text" else null,
                                onClick = throttle {
                                    onDismiss()
                                    onClickItem()
                                },
                                endActions = if (selectedText != null) {
                                    {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            modifier = Modifier.clearAndSetSemantics {},
                                        )
                                    }
                                } else null,
                            ) {
                                Text(text, style = MiuixTheme.textStyles.headline1,
                                    color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
