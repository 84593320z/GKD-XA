// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import priv.kit.ui.PrivilegeUiPromptState

@Composable
internal fun PrivilegeSystemPromptOverlay(
    prompt: PrivilegeUiPromptState?,
    modifier: Modifier = Modifier,
) {
    var displayedPrompt by remember { mutableStateOf(prompt) }
    val visibility = remember {
        MutableTransitionState(prompt != null)
    }
    LaunchedEffect(prompt) {
        if (prompt != null) {
            displayedPrompt = prompt
        }
        visibility.targetState = prompt != null
    }
    LaunchedEffect(
        prompt,
        visibility.currentState,
        visibility.isIdle,
    ) {
        if (prompt == null && visibility.isIdle && !visibility.currentState) {
            displayedPrompt = null
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            displayedPrompt?.let { currentPrompt ->
                PrivilegeSystemPromptCard(currentPrompt)
            }
        }
    }
}

@Composable
private fun PrivilegeSystemPromptCard(prompt: PrivilegeUiPromptState) {
    Surface(
        modifier = Modifier
            .widthIn(max = PROMPT_MAX_WIDTH)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        shape = PrivilegeMiuixTheme.shapes.medium,
        color = PrivilegeMiuixTheme.colorScheme.inverseSurface,
        contentColor = PrivilegeMiuixTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = PrivilegeUiSpacing.large,
                vertical = PrivilegeUiSpacing.medium,
            ),
            verticalArrangement = Arrangement.spacedBy(PrivilegeUiSpacing.extraSmall),
        ) {
            Text(
                text = prompt.title,
                style = PrivilegeMiuixTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = prompt.message,
                style = PrivilegeMiuixTheme.typography.bodyMedium,
            )
        }
    }
}

private val PROMPT_MAX_WIDTH = 560.dp
