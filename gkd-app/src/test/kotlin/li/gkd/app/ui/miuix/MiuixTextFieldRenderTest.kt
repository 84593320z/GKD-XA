package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.component.GkOutlinedTextField
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
class MiuixTextFieldRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun nativeFormInputOwnsItsLabelAndAcceptsText() {
        val changes = mutableListOf<String>()
        compose.setMiuixContent {
            var value by remember { mutableStateOf("") }
            GkFormTextField(
                value = value,
                onValueChange = { changes += it; value = it },
                label = "Subscription URL", placeholder = "https://example.test/rules",
                singleLine = true,
                supportingText = { Text("Stored only in this composition") },
            )
        }

        val editor = hasContentDescription("Subscription URL") and hasSetTextAction()
        compose.onAllNodes(editor).assertCountEquals(1)
        compose.onNodeWithText("https://example.test/rules").assertIsDisplayed()
        compose.onNodeWithText("Stored only in this composition").assertIsDisplayed()
        val input = compose.onNode(editor).assertIsDisplayed()
        input.performTextInput("https://example.test/sub.json")
        input.assertTextContains("https://example.test/sub.json")
        compose.runOnIdle { assertEquals(listOf("https://example.test/sub.json"), changes) }
    }

    @Test
    fun nativeValidationErrorPersistsWithoutFocusAndCanClearAfterEditing() {
        compose.setMiuixContent {
            var value by remember { mutableStateOf("") }
            GkFormTextField(
                value = value, onValueChange = { value = it },
                label = "Required name", isError = value.isEmpty(), errorMessage = "Name is required",
                supportingText = { Text(if (value.isEmpty()) "Enter a name" else "Name accepted") },
            )
        }

        val editor = hasContentDescription("Required name") and hasSetTextAction()
        val input = compose.onNode(editor).assertIsDisplayed()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Name is required"))
        compose.onNodeWithText("Enter a name").assertIsDisplayed()
        input.performTextInput("Work rules")
        input.assertTextContains("Work rules")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose.onNodeWithText("Name accepted").assertIsDisplayed()
    }

    @Test
    fun disabledNativeFormInputStillRendersItsValueAndSupportingText() {
        var callbacks = 0
        compose.setMiuixContent {
            GkFormTextField(
                value = "Read-only preview", onValueChange = { callbacks++ },
                label = "Disabled name", enabled = false,
                supportingText = { Text("Unavailable in this preview") },
            )
        }

        compose.onNodeWithContentDescription("Disabled name")
            .assertIsDisplayed().assertIsNotEnabled().assertTextContains("Read-only preview")
        compose.onNodeWithText("Unavailable in this preview").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, callbacks) }
    }

    @Test
    fun outlinedCompatibilityFieldKeepsSlotsAndValidationWithFoundationEditing() {
        compose.setMiuixContent {
            var value by remember { mutableStateOf("") }
            GkOutlinedTextField(
                modifier = Modifier.fillMaxWidth().testTag("outlined-input"),
                value = value, onValueChange = { value = it }, isError = true,
                label = { Text("Compatibility label") },
                placeholder = { Text("Compatibility placeholder") },
                prefix = { Text("#") }, suffix = { Text("items") },
                supportingText = { Text("Correct this value") },
            )
        }

        compose.onNodeWithText("Compatibility placeholder").assertIsDisplayed()
        val input = compose.onNodeWithTag("outlined-input").assertIsDisplayed()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, UiStrings.error_label))
        input.performTextInput("12")
        input.assertTextContains("12")
        compose.onNodeWithText("Correct this value").assertIsDisplayed()
        compose.onNodeWithText("#").assertIsDisplayed()
        compose.onNodeWithText("items").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun largeFontDarkFormFitsA320dpWindowAndRemainsEditable() {
        compose.setMiuixContent(dark = true, fontScale = 1.8f) {
            Column {
                var value by remember { mutableStateOf("") }
                GkFormTextField(
                    value = value, onValueChange = { value = it },
                    label = "A long descriptive field label that may wrap",
                    placeholder = "Name", isError = true, errorMessage = "Please enter a value",
                    supportingText = { Text("This explanation may wrap onto several lines.") },
                )
            }
        }

        compose.onNodeWithTag("miuix-test-root").assertIsDisplayed()
        val input = compose.onNode(hasSetTextAction()).assertIsDisplayed()
        input.performTextReplacement("Local value")
        input.assertTextContains("Local value")
        compose.onNodeWithText("This explanation may wrap onto several lines.").assertIsDisplayed()
    }
}
