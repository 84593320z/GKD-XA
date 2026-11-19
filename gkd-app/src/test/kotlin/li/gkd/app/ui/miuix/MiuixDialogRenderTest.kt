package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.GkSettingsDialog
import li.gkd.app.ui.component.GkTextListDialog
import li.gkd.app.ui.component.PerfAlertDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Text

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = CompactLightQualifiers)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MiuixDialogRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun longAlertScrollsAtLargeFontWithoutHidingItsConfirmation() {
        val visible = mutableStateOf(true)
        var confirmations = 0
        var cancellations = 0
        compose.setMiuixContent(dark = true, fontScale = 1.6f) {
            if (visible.value) {
                GkAlertDialog(
                    onDismissRequest = { cancellations++; visible.value = false },
                    title = { Text("Review changes") },
                    text = {
                        Column {
                            repeat(24) { index ->
                                Text("Explanation $index: a deliberately long description that wraps on a narrow screen.")
                            }
                        }
                    },
                    confirmButton = {
                        GkActionButton(onClick = { confirmations++; visible.value = false }) { Text("Apply") }
                    },
                    dismissButton = {
                        GkActionButton(onClick = { cancellations++; visible.value = false }) { Text("Cancel") }
                    },
                )
            }
        }

        compose.onNodeWithText("Review changes").assertIsDisplayed()
        compose.onNodeWithText("Explanation 23: a deliberately long description that wraps on a narrow screen.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Review changes").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, confirmations); assertEquals(0, cancellations) }
    }

    @Test
    fun disabledConfirmationCannotFireAndCanBecomeEnabled() {
        val enabled = mutableStateOf(false)
        var confirmations = 0
        compose.setMiuixContent {
            GkAlertDialog(
                onDismissRequest = {}, title = { Text("Validate first") },
                text = { Text("Only local state controls this confirmation.") },
                confirmButton = {
                    GkActionButton(onClick = { confirmations++ }, enabled = enabled.value) { Text("Confirm") }
                },
            )
        }

        compose.onNodeWithText("Confirm").assertIsDisplayed().assertIsNotEnabled()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, confirmations); enabled.value = true }
        compose.onNodeWithText("Confirm").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test
    fun cancellingAnAlertDoesNotInvokeItsConfirmation() {
        val visible = mutableStateOf(true)
        var confirmations = 0
        var cancellations = 0
        compose.setMiuixContent {
            if (visible.value) {
                GkAlertDialog(
                    onDismissRequest = { visible.value = false },
                    title = { Text("Discard this preview?") },
                    confirmButton = { GkActionButton(onClick = { confirmations++ }) { Text("Discard") } },
                    dismissButton = {
                        GkActionButton(onClick = { cancellations++; visible.value = false }) { Text("Keep editing") }
                    },
                )
            }
        }

        compose.onNodeWithText("Keep editing").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Discard this preview?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, confirmations); assertEquals(1, cancellations) }
    }

    @Test
    fun legacyAlertPreservesRowScopeButtonsAndScrollableLongContent() {
        var confirmations = 0
        var cancellations = 0
        compose.setMiuixContent {
            PerfAlertDialog(
                onDismissRequest = { cancellations++ }, title = { Text("Legacy alert") },
                text = { Column { repeat(30) { Text("Legacy paragraph $it") } } },
                confirmButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { confirmations++ }) { Text("Continue") }
                },
                dismissButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { cancellations++ }) { Text("Back") }
                },
            )
        }

        compose.onNodeWithText("Legacy paragraph 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, confirmations); assertEquals(0, cancellations) }
    }

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun settingsDialogScrollsLongRowsAndKeepsItsCloseActionVisible() {
        val visible = mutableStateOf(true)
        var dismissals = 0
        var selections = 0
        val title = "A long settings title that can wrap"
        compose.setMiuixContent(dark = true, fontScale = 1.6f) {
            if (visible.value) {
                GkSettingsDialog(title = title, onDismissRequest = { dismissals++; visible.value = false }) {
                    repeat(25) { index ->
                        GkSettingItem(
                            title = "Setting $index",
                            subtitle = "A setting summary long enough to wrap at large font sizes.",
                            onClick = { selections++ },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Setting 24").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(UiStrings.dialog_close_description(title))
            .assertIsDisplayed().performClick()
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, selections); assertEquals(1, dismissals) }
    }

    @Test
    fun textListDialogScrollsAndCallsDismissThenOnlyTheSelectedAction() {
        val visible = mutableStateOf(true)
        val events = mutableListOf<String>()
        compose.setMiuixContent {
            if (visible.value) {
                GkTextListDialog(
                    onDismiss = { events += "dismiss"; visible.value = false },
                    textList = (0 until 30).map { index ->
                        "Choice $index" to { events.add("choice-$index"); Unit }
                    },
                )
            }
        }

        compose.onNodeWithText("Choice 29").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Choice 29").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("dismiss", "choice-29"), events) }
    }
}
