// SPDX-License-Identifier: Apache-2.0
package priv.kit.ui.miuix

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.layout.DialogDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalPrivilegeCompactDialog = staticCompositionLocalOf { false }

/**
 * Native Miuix card, typography and action controls in a Compose window host.
 *
 * Miuix 0.9.4 WindowDialog exposes one dismissal callback for outside taps and back.
 * Authorization has stricter per-dialog policies, so a plain Compose Dialog hosts the
 * Miuix render layer here. This keeps every upstream DialogProperties dismissal flag;
 * no Material dialog, invisible scaffold or private/reflected API is involved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PrivilegeDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    scrollableBody: Boolean = true,
    primaryConfirmActions: Boolean = true,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .imePadding().safeDrawingPadding()
                .pointerInput(properties.dismissOnClickOutside) {
                    detectTapGestures {
                        if (properties.dismissOnClickOutside) dismiss()
                    }
                },
        ) {
            val wide = maxWidth >= 840.dp && maxHeight >= 480.dp
            val compact = maxHeight < (320f * LocalDensity.current.fontScale).dp
            val cardMaxHeight = (maxHeight - DialogDefaults.outsideMargin.height * 2f).coerceAtLeast(0.dp)
            Card(
                modifier = Modifier
                    .align(if (wide) Alignment.Center else Alignment.BottomCenter)
                    .padding(horizontal = DialogDefaults.outsideMargin.width, vertical = DialogDefaults.outsideMargin.height)
                    .widthIn(max = DialogDefaults.MaxWidth)
                    .fillMaxWidth()
                    .heightIn(max = cardMaxHeight)
                    // A tap on non-interactive card text must not dismiss the dialog.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .then(modifier),
                cornerRadius = DialogDefaults.CornerRadius,
                insideMargin = PaddingValues(DialogDefaults.insideMargin.width, DialogDefaults.insideMargin.height),
                colors = CardDefaults.defaultColors(
                    color = DialogDefaults.backgroundColor(),
                    contentColor = MiuixTheme.colorScheme.onSurface,
                ),
            ) {
                CompositionLocalProvider(LocalPrivilegeCompactDialog provides compact) {
                Column(
                    modifier = Modifier.fillMaxWidth().then(
                        if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (title != null) {
                        MiuixTheme(
                            textStyles = MiuixTheme.textStyles.copy(
                                main = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Medium),
                            ),
                        ) {
                            Box(Modifier.semantics { heading() }) { title() }
                        }
                    }
                    if (text != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().then(
                                if (!compact) Modifier.weight(1f, fill = false) else Modifier).then(
                                if (scrollableBody && !compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                        ) { text() }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompositionLocalProvider(LocalPrivilegePrimaryAction provides false) {
                            dismissButton?.invoke()
                        }
                        CompositionLocalProvider(LocalPrivilegePrimaryAction provides primaryConfirmActions) {
                            confirmButton()
                        }
                    }
                }
                }
            }
        }
    }
}
