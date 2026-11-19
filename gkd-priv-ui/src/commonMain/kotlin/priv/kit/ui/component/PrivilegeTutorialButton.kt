// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui.component

import top.yukonga.miuix.kmp.basic.Text
import priv.kit.ui.miuix.PrivilegeTextButton
import priv.kit.ui.miuix.PrivilegeSecondaryButton
import top.yukonga.miuix.kmp.basic.Icon
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import priv.kit.ui.PrivilegeUiScreenScope
import priv.kit.ui.PrivilegeUiTutorialTopic
import priv.kit.ui.openPrivilegeUiTutorial
import priv.kit.ui.R

@Composable
internal fun PrivilegeUiScreenScope.tutorialAction(topic: PrivilegeUiTutorialTopic): () -> Unit {
    val uriHandler = LocalUriHandler.current
    val defaultUrl = stringResource(R.string.priv_ui_startup_tutorial_url)
    val failure = stringResource(R.string.priv_ui_tutorial_open_failed)
    return {
        openPrivilegeUiTutorial(
            topic = topic,
            baseUrl = startupTutorialUrl ?: defaultUrl,
            onViewPermissionSolutions = onViewPermissionSolutions,
            onViewStartupTutorial = onViewStartupTutorial,
            openUri = uriHandler::openUri,
            onOpenFailure = { showFeedback(failure) },
        )
    }
}

@Composable
internal fun PrivilegeUiScreenScope.TutorialButton(
    topic: PrivilegeUiTutorialTopic,
    label: Int = R.string.priv_ui_startup_tutorial,
    outlined: Boolean = false,
) {
    val content: @Composable RowScope.() -> Unit = {
        Text(stringResource(label))
        if (topic == PrivilegeUiTutorialTopic.ADB || topic == PrivilegeUiTutorialTopic.MANUAL) {
            Spacer(Modifier.width(PrivilegeUiSpacing.small))
            Icon(
                imageVector = PrivilegeUiIcons.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    if (outlined) {
        PrivilegeSecondaryButton(
            onClick = tutorialAction(topic),
            content = content,
        )
    } else {
        PrivilegeTextButton(onClick = tutorialAction(topic), content = content)
    }
}
