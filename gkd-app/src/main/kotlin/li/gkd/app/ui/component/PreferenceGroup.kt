package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

/** A Miuix settings card with an optional accessible section heading. */
@Composable
fun PreferenceGroup(
    title: String? = null,
    showTop: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = 16.dp, end = 16.dp,
            top = if (showTop) 8.dp else 0.dp,
            bottom = 6.dp,
        ),
    ) {
        if (!title.isNullOrBlank()) {
            SmallTitle(
                text = title,
                modifier = Modifier.semantics { heading() },
                insideMargin = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        Card(modifier = Modifier.fillMaxWidth(), content = content)
    }
}
