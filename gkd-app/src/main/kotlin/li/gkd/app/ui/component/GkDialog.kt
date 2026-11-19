package li.gkd.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog as PlatformDialog
import li.gkd.app.ui.style.GkProvideTextStyle
import li.gkd.app.ui.style.GkShapes
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private data class DialogRegistration(
    val order: Long,
    val registrationOrder: Long,
)

private object DialogRegistry {
    private var nextOrder = 0L
    private var nextRegistrationOrder = 0L
    private val registrations = mutableStateMapOf<Any, DialogRegistration>()

    val topToken: Any?
        get() = registrations.maxWithOrNull(
            compareBy<Map.Entry<Any, DialogRegistration>> { it.value.order }
                .thenBy { it.value.registrationOrder }
        )?.key

    fun createOrder(): Long = ++nextOrder

    fun register(token: Any, order: Long) {
        nextOrder = maxOf(nextOrder, order)
        registrations[token] = DialogRegistration(
            order = order,
            registrationOrder = ++nextRegistrationOrder,
        )
    }

    fun unregister(token: Any) {
        registrations.remove(token)
    }
}

@Composable
private fun DialogLayer(content: @Composable () -> Unit) {
    val order = rememberSaveable { DialogRegistry.createOrder() }
    val token = remember { Any() }

    DisposableEffect(Unit) {
        DialogRegistry.register(token, order)
        onDispose {
            DialogRegistry.unregister(token)
        }
    }

    if (DialogRegistry.topToken === token) {
        content()
    }
}

@Composable
fun GkDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    DialogLayer {
        // Keep the platform Dialog here intentionally: callers rely on the exact
        // DialogProperties contract (including outside/back dismissal and fullscreen windows).
        PlatformDialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    }
}

@Composable
fun GkModalBottomSheet(
    onDismissRequest: () -> Unit,
    show: Boolean = true,
    modifier: Modifier = Modifier,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    onDismissFinished: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    WindowBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        modifier = modifier,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun GkProvideDialogTextStyle(
    style: TextStyle,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        GkProvideTextStyle(style, content)
    }
}

@Composable
internal fun GkDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    shadowElevation: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalWindowInfo.current.containerDpSize.height * 0.9f)
        .coerceAtLeast(240.dp)
    Surface(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .imePadding()
            .then(modifier),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
fun GkAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = GkShapes.dialog,
    containerColor: Color = Color.Unspecified,
    iconContentColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    shadowElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    val resolvedContainerColor = if (containerColor == Color.Unspecified) {
        MiuixTheme.colorScheme.background
    } else containerColor
    val resolvedIconContentColor = if (iconContentColor == Color.Unspecified) {
        MiuixTheme.colorScheme.onSurface
    } else iconContentColor
    val resolvedTitleContentColor = if (titleContentColor == Color.Unspecified) {
        MiuixTheme.colorScheme.onSurface
    } else titleContentColor
    val resolvedTextContentColor = if (textContentColor == Color.Unspecified) {
        MiuixTheme.colorScheme.onSurfaceSecondary
    } else textContentColor

    GkDialog(onDismissRequest = onDismissRequest, properties = properties) {
        GkDialogSurface(
            modifier = modifier,
            shape = shape,
            containerColor = resolvedContainerColor,
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = shadowElevation,
        ) {
            icon?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides resolvedIconContentColor,
                        content = it,
                    )
                }
            }
            title?.let {
                GkProvideDialogTextStyle(
                    style = MiuixTheme.textStyles.title3,
                    contentColor = resolvedTitleContentColor,
                    content = it,
                )
            }
            text?.let {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GkProvideDialogTextStyle(
                        style = MiuixTheme.textStyles.body1,
                        contentColor = resolvedTextContentColor,
                        content = it,
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
