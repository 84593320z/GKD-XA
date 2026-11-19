// SPDX-License-Identifier: Apache-2.0
package priv.kit.ui.miuix

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

// Upstream uses content slots for buttons. Miuix's string-only TextButton cannot retain
// the tutorial icon or caller typography, so these adapters use its native slot Button.
internal val LocalPrivilegePrimaryAction = staticCompositionLocalOf { false }

@Composable
internal fun PrivilegePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixTheme(textStyles = MiuixTheme.textStyles.copy(main = MiuixTheme.textStyles.button)) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 48.dp,
        colors = ButtonDefaults.buttonColorsPrimary(),
        insideMargin = ButtonDefaults.InsideMargin,
        content = content,
    )
    }
}

@Composable
internal fun PrivilegeSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixTheme(textStyles = MiuixTheme.textStyles.copy(main = MiuixTheme.textStyles.button)) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 48.dp,
        colors = ButtonDefaults.buttonColors(),
        insideMargin = ButtonDefaults.InsideMargin,
        content = content,
    )
    }
}

@Composable
internal fun PrivilegeTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixTheme(textStyles = MiuixTheme.textStyles.copy(main = MiuixTheme.textStyles.button)) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minHeight = 48.dp,
        colors = colors ?: if (LocalPrivilegePrimaryAction.current) {
            ButtonDefaults.buttonColorsPrimary()
        } else {
            ButtonDefaults.buttonColors()
        },
        insideMargin = ButtonDefaults.InsideMargin,
        content = content,
    )
    }
}

internal object PrivilegeButtonDefaults {
    @Composable
    fun textButtonColors(contentColor: Color): ButtonColors = ButtonDefaults.buttonColors(
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = 0.38f),
    )
}

@Composable
internal fun PrivilegeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides if (enabled) LocalContentColor.current else MiuixTheme.colorScheme.disabledOnSurface,
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.semantics {
                role = Role.Button
                if (!enabled) disabled()
            },
            enabled = enabled,
            minWidth = 48.dp,
            minHeight = 48.dp,
            content = content,
        )
    }
}

internal data class PrivilegeIconColors(val containerColor: Color, val contentColor: Color)

internal object PrivilegeIconButtonDefaults {
    fun filledTonalIconButtonColors(containerColor: Color, contentColor: Color): PrivilegeIconColors =
        PrivilegeIconColors(containerColor, contentColor)
}

@Composable
internal fun PrivilegeStatusIconButton(
    onClick: () -> Unit,
    colors: PrivilegeIconColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        IconButton(
            onClick = onClick,
            modifier = modifier.alpha(if (enabled) 1f else 0.38f).semantics {
                role = Role.Button
                if (!enabled) disabled()
            },
            enabled = enabled,
            backgroundColor = colors.containerColor,
            cornerRadius = 16.dp,
            minWidth = 48.dp,
            minHeight = 48.dp,
            content = content,
        )
    }
}
