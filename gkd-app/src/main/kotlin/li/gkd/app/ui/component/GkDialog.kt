package li.gkd.app.ui.component

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog as PlatformDialog
import li.gkd.app.ui.style.GkProvideTextStyle
import li.gkd.app.ui.style.GkShapes
import li.gkd.app.ui.style.GkUi
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.layout.DialogDefaults
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
    nativePlacement: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    DialogLayer {
        // Keep the platform Dialog here intentionally: callers rely on the exact
        // DialogProperties contract (including outside/back dismissal and fullscreen windows).
        PlatformDialog(
            onDismissRequest = onDismissRequest,
            properties = if (nativePlacement) DialogProperties(
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = properties.dismissOnClickOutside,
                securePolicy = properties.securePolicy,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ) else properties,
        ) {
            if (nativePlacement) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().imePadding().safeDrawingPadding()
                        .pointerInput(properties.dismissOnClickOutside) {
                            detectTapGestures { if (properties.dismissOnClickOutside) dismiss() }
                        },
                ) {
                    val large = maxWidth >= 840.dp && maxHeight >= 480.dp
                    Box(
                        modifier = Modifier.align(if (large) Alignment.Center else Alignment.BottomCenter)
                            .padding(vertical = DialogDefaults.outsideMargin.height)
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) { content() }
                }
            } else content()
        }
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

/**
 * One shrinking viewport for icon/title/body, leaving the following action row unweighted.
 * The viewport scrolls the header too, not just the text that a long title can otherwise starve.
 */
@Composable
internal fun ColumnScope.GkDialogContent(
    content: @Composable ColumnScope.(viewportHeight: Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = false)) {
        val viewportHeight = constraints.maxHeight
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content(viewportHeight)
        }
    }
}

/**
 * Give ordinary text its natural height in the shared scroll viewport, but never pass an
 * infinite height to a self-scrolling/subcomposed body (e.g. a caller's LazyColumn).
 * SubcomposeLayout intentionally has no intrinsic measurements. In that case the caller's
 * scrolling content owns overflow inside a finite viewport, without eagerly composing a list.
 */
@Composable
internal fun GkDialogBody(
    viewportHeight: Int,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            val intrinsicHeight = try {
                measurable.maxIntrinsicHeight(constraints.maxWidth)
            } catch (error: IllegalStateException) {
                if (error.message?.startsWith("Asking for intrinsic measurements of SubcomposeLayout") != true) {
                    throw error
                }
                0
            }
            val maxHeight = maxOf(
                viewportHeight,
                intrinsicHeight.takeUnless { it == Constraints.Infinity } ?: 0,
            )
            measurable.measure(
                Constraints.fitPrioritizingWidth(
                    minWidth = constraints.minWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = maxHeight,
                ),
            )
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0),
        ) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}

/** Shared Miuix dialog frame; content slots stay within the actual available window height. */
@Composable
internal fun GkDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    shadowElevation: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowSize = LocalWindowInfo.current.containerDpSize
    val isLargeScreen = windowSize.height >= 480.dp && windowSize.width >= 840.dp
    val maxHeight = (
        if (isLargeScreen) windowSize.height * (2f / 3f)
        else windowSize.height - DialogDefaults.outsideMargin.height * 2f
    ).coerceAtLeast(1.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DialogDefaults.outsideMargin.width),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = DialogDefaults.MaxWidth)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .then(modifier),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = shadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DialogDefaults.insideMargin.width, DialogDefaults.insideMargin.height),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
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
        DialogDefaults.backgroundColor()
    } else containerColor
    val resolvedIconContentColor = if (iconContentColor == Color.Unspecified) {
        MiuixTheme.colorScheme.onSurface
    } else iconContentColor
    val resolvedTitleContentColor = if (titleContentColor == Color.Unspecified) {
        DialogDefaults.titleColor()
    } else titleContentColor
    val resolvedTextContentColor = if (textContentColor == Color.Unspecified) {
        DialogDefaults.summaryColor()
    } else textContentColor

    GkDialog(onDismissRequest = onDismissRequest, properties = properties, nativePlacement = true) {
        GkDialogSurface(
            modifier = modifier,
            shape = shape,
            containerColor = resolvedContainerColor,
            contentColor = MiuixTheme.colorScheme.onSurface,
            shadowElevation = shadowElevation,
        ) {
            if (icon != null || title != null || text != null) {
                GkDialogContent { viewportHeight ->
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
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            GkProvideDialogTextStyle(
                                style = MiuixTheme.textStyles.title4.copy(textAlign = TextAlign.Center),
                                contentColor = resolvedTitleContentColor,
                                content = it,
                            )
                        }
                    }
                    text?.let {
                        GkDialogBody(viewportHeight) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                GkProvideDialogTextStyle(
                                    style = MiuixTheme.textStyles.body1.copy(textAlign = TextAlign.Center),
                                    contentColor = resolvedTextContentColor,
                                    content = it,
                                )
                            }
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GkUi.MinTouchTarget),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
