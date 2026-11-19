package li.gkd.app.ui.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.layout.DialogDefaults

/** Layout rhythm shared by every app page; native component geometry stays owned by Miuix. */
internal object GkUi {
    val PageHorizontal = 16.dp
    val SectionGap = 12.dp
    val ContentPadding = 16.dp
    val RowVertical = 12.dp
    val MinTouchTarget = 48.dp
    val IconSize = 24.dp
    val RowPadding = PaddingValues(horizontal = ContentPadding, vertical = RowVertical)
    val CardRadius = CardDefaults.CornerRadius
    val FieldRadius = TextFieldDefaults.CornerRadius
    val DialogRadius = DialogDefaults.CornerRadius
}
