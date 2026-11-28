package li.gkd.app.ui.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/** Use the same continuous-corner path as native Miuix cards, including platform Dialog hosts. */
@Immutable
private data class MiuixShape(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply {
            addSquircleRect(size.width, size.height, with(density) { radius.toPx() })
        })
}

internal object GkShapes {
    val compact: Shape = MiuixShape(6.dp)
    val control: Shape = MiuixShape(10.dp)
    val panel: Shape = MiuixShape(14.dp)
    val card: Shape = MiuixShape(GkUi.CardRadius)
    val dialog: Shape = MiuixShape(GkUi.DialogRadius)
    val floatingBar: Shape = MiuixShape(FloatingToolbarDefaults.CornerRadius)
}
