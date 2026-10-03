package li.gkd.app.ui.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

val itemHorizontalPadding = 16.dp
val itemVerticalPadding = 12.dp
val EmptyHeight = 80.dp
val cardHorizontalPadding = 12.dp

/** MIUIX 部分 TextStyle 的 lineHeight/fontSize 不是 Sp，直接 toDp() 会崩溃 */
fun TextUnit.toSpDpOr(density: Density, fallback: Dp): Dp = with(density) {
    if (isSp) toDp() else fallback
}

fun TextStyle.lineHeightDp(density: Density, fallback: Dp = 20.dp): Dp =
    lineHeight.toSpDpOr(density, fontSize.toSpDpOr(density, fallback))

/**
 * MIUIX 的多数 [TextStyle]（body1 / body2 / footnote2 …）只声明了 fontSize，**lineHeight 是
 * [TextUnit.Unspecified]**；而 `TextStyle.Default`（Material 的 LocalTextStyle 默认值）两个都是
 * Unspecified。Compose 的 `Placeholder` 构造器会直接 require 失败：
 * `IllegalArgumentException: height cannot be TextUnit.Unspecified`。
 *
 * 内联图标（appendInlineContent）必须给出合法的 Placeholder 尺寸，这里统一按
 * 「有 lineHeight 用 lineHeight，否则退回 fontSize，再否则用 fallback」兜底，
 * 与旧版 `GroupNameText` 的处理保持一致。
 */
fun TextStyle.placeholderWidth(fallback: TextUnit = 16.sp): TextUnit =
    fontSize.takeUnless { it.isUnspecified } ?: fallback

fun TextStyle.placeholderHeight(fallback: TextUnit = 16.sp): TextUnit =
    lineHeight.takeUnless { it.isUnspecified }
        ?: fontSize.takeUnless { it.isUnspecified }
        ?: fallback

fun Modifier.itemPadding() = this.padding(itemHorizontalPadding, itemVerticalPadding)

fun Modifier.titleItemPadding(showTop: Boolean = true) = this.padding(
    itemHorizontalPadding,
    if (showTop) itemVerticalPadding + itemVerticalPadding / 2 else 0.dp,
    itemHorizontalPadding,
    itemVerticalPadding - itemVerticalPadding / 2
)

fun Modifier.appItemPadding() = this.padding(itemHorizontalPadding, itemVerticalPadding)

fun Modifier.scaffoldPadding(values: PaddingValues): Modifier {
    return padding(
        top = values.calculateTopPadding(),
        // 被 LazyXXX 使用时, 移除 bottom padding, 否则 底部导航栏 无法实现透明背景
    )
}

@Composable
fun Modifier.iconTextSize(
    textStyle: TextStyle = LocalTextStyle.current,
    square: Boolean = true,
): Modifier {
    val density = LocalDensity.current
    val lineHeightDp = textStyle.lineHeightDp(density)
    val fontSizeDp = textStyle.fontSize.toSpDpOr(density, 14.dp)
    return if (square) {
        padding((lineHeightDp - fontSizeDp) / 2).size(fontSizeDp)
    } else {
        size(height = lineHeightDp, width = fontSizeDp)
    }
}
