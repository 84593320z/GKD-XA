package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiModel
import li.gkd.app.feature.settings.ai.AiProviderSelection
import li.gkd.app.feature.settings.ai.ModelRow
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.TextMenu
import li.gkd.app.util.SnapshotDisplayModeOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Text

/** Component-only layout, semantics and input regressions; no application or provider hosts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = CompactLightQualifiers)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MiuixPolishRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaultSingleLineTextSwitchMatchesNativeTextMenuHeightAndTitleAlignment() {
        compose.setMiuixContent(fontScale = 1f) {
            Column {
                GkTextSwitch(
                    modifier = Modifier.testTag("switch-row"),
                    title = "Switch title",
                    checked = false,
                    onCheckedChange = {},
                )
                TextMenu(
                    modifier = Modifier.testTag("menu-row"),
                    title = "Menu title",
                    option = SnapshotDisplayModeOption.ByTime,
                    onOptionChange = {},
                )
            }
        }

        val switchRow = compose.onNodeWithTag("switch-row")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val menuRow = compose.onNodeWithTag("menu-row")
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val switchBounds = switchRow.getUnclippedBoundsInRoot()
        val menuBounds = menuRow.getUnclippedBoundsInRoot()
        // Read the actual text nodes, not their merged row-sized semantics bounds.
        val switchTitle = compose.onNodeWithText("Switch title", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val menuTitle = compose.onNodeWithText("Menu title", useUnmergedTree = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()

        assertEquals(
            "A 48dp trailing switch must not add another pair of native row insets",
            (menuBounds.bottom - menuBounds.top).value,
            (switchBounds.bottom - switchBounds.top).value,
            1f,
        )
        assertEquals(
            "Single-line titles retain the same leading inset",
            (menuTitle.left - menuBounds.left).value,
            (switchTitle.left - switchBounds.left).value,
            1f,
        )
        assertEquals(
            "Single-line titles retain the same vertical alignment within their rows",
            (menuTitle.top.value + menuTitle.bottom.value - menuBounds.top.value - menuBounds.bottom.value) / 2f,
            (switchTitle.top.value + switchTitle.bottom.value - switchBounds.top.value - switchBounds.bottom.value) / 2f,
            1f,
        )
    }

    @Test
    fun disabledDefaultTextSwitchRetainsSwitchStateWithoutAClickAction() {
        var changes = 0
        compose.setMiuixContent {
            Column {
                GkTextSwitch(
                    title = "Disabled off", checked = false, enabled = false,
                    onCheckedChange = { changes++ },
                )
                GkTextSwitch(
                    title = "Disabled on", checked = true, enabled = false,
                    onCheckedChange = { changes++ },
                )
            }
        }

        compose.onNodeWithText("Disabled off").assertIsDisplayed()
            .assert(hasRole(Role.Switch)).assertIsNotEnabled().assertIsOff()
            .assertHasNoClickAction().performTouchInput { click() }
        compose.onNodeWithText("Disabled on").assertIsDisplayed()
            .assert(hasRole(Role.Switch)).assertIsNotEnabled().assertIsOn()
            .assertHasNoClickAction().performTouchInput { click() }
        compose.onAllNodes(hasRole(Role.Switch), useUnmergedTree = true).assertCountEquals(2)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { assertEquals(0, changes) }
    }

    @Test
    fun disabledProviderSelectionRetainsRadioStateAndCannotActivate() {
        var activations = 0
        compose.setMiuixContent {
            Column {
                AiProviderSelection(
                    selected = false, enabled = false, label = "Unavailable provider",
                    onClick = { activations++ },
                )
                AiProviderSelection(
                    selected = true, enabled = false, label = "Unavailable current provider",
                    onClick = { activations++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Unavailable provider").assertIsDisplayed()
            .assert(hasRole(Role.RadioButton)).assertIsNotSelected().assertIsNotEnabled()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .assertHasNoClickAction().performTouchInput { click() }
        compose.onNodeWithContentDescription("Unavailable current provider").assertIsDisplayed()
            .assert(hasRole(Role.RadioButton)).assertIsSelected().assertIsNotEnabled()
            .assertHasNoClickAction().performTouchInput { click() }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { assertEquals(0, activations) }
    }

    @Test
    fun disabledProviderSelectionDoesNotDisableOuterDetailsSemantics() {
        var activations = 0
        var details = 0
        compose.setMiuixContent {
            ProviderDetailsRow(
                name = "Disabled provider", selected = false, enabled = false,
                onActivate = { activations++ }, onDetails = { details++ },
            )
        }

        // Default merged semantics must expose an enabled details row AND a disabled radio.
        compose.onNodeWithContentDescription("Select Disabled provider")
            .assert(hasRole(Role.RadioButton)).assertIsNotSelected().assertIsNotEnabled()
            .assertHasNoClickAction()
        compose.onNodeWithContentDescription("Select Disabled provider")
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, details); assertEquals(0, activations) }
        compose.onNodeWithTag("details-Disabled provider").assertIsDisplayed()
            .assertIsEnabled().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(1, details); assertEquals(0, activations) }
    }

    @Test
    fun enabledProviderSiblingsActivateExactlyOnceWithoutOpeningDetails() {
        val activations = mutableListOf<String>()
        var details = 0
        compose.setMiuixContent {
            var current by remember { mutableStateOf("First provider") }
            Column {
                ProviderDetailsRow(
                    name = "First provider", selected = current == "First provider", enabled = true,
                    onActivate = { activations += "first"; current = "First provider" },
                    onDetails = { details++ },
                )
                ProviderDetailsRow(
                    name = "Second provider", selected = current == "Second provider", enabled = true,
                    onActivate = { activations += "second"; current = "Second provider" },
                    onDetails = { details++ },
                )
            }
        }

        val first = compose.onNodeWithContentDescription("Select First provider")
        val second = compose.onNodeWithContentDescription("Select Second provider")
        first.assertIsDisplayed().assert(hasRole(Role.RadioButton)).assertIsSelected()
            .assertIsEnabled().assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        second.assertIsDisplayed().assert(hasRole(Role.RadioButton)).assertIsNotSelected()
            .assertIsEnabled().assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onAllNodes(hasRole(Role.RadioButton) and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(2)

        second.performTouchInput { click() }.assertIsSelected()
        first.assertIsNotSelected()
        compose.runOnIdle { assertEquals(listOf("second"), activations); assertEquals(0, details) }
        first.performTouchInput { click() }.assertIsSelected()
        second.assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(listOf("second", "first"), activations)
            assertEquals(0, details)
        }
    }

    @Test
    fun modelRowExposesOneRadioSelectionActionAndASeparateEditAction() {
        val model = AiModel(modelId = "local-model", displayName = "Local model")
        var selections = 0
        var edits = 0
        var batchChanges = 0
        compose.setMiuixContent {
            var currentModel by remember { mutableStateOf("another-model") }
            ModelRow(
                provider = AiConfig(id = "preview", model = currentModel), model = model,
                selectionMode = false, checked = false,
                onToggleChecked = { batchChanges++ },
                onEdit = { edits++ },
                onSetCurrent = { selections++; currentModel = model.modelId },
            )
        }

        val row = compose.onNode(hasRole(Role.RadioButton))
        row.assertIsDisplayed().assertIsNotSelected().assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ToggleableState))
        compose.onAllNodes(hasRole(Role.RadioButton), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodes(hasRole(Role.RadioButton) and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithContentDescription("编辑").assertIsDisplayed().assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performTouchInput { click() }
        row.assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(1, edits); assertEquals(0, selections); assertEquals(0, batchChanges)
        }

        // Tapping the title exercises the row, independently of its trailing edit target.
        compose.onNodeWithText("Local model", useUnmergedTree = true).performTouchInput { click() }
        row.assertIsSelected()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        compose.runOnIdle {
            assertEquals(1, selections); assertEquals(1, edits); assertEquals(0, batchChanges)
        }
    }

    @Test
    fun modelRowBatchModeUsesOnlyTheRowCheckboxAction() {
        val model = AiModel(modelId = "batch-model", displayName = "Batch model")
        var batchChanges = 0
        var selections = 0
        var edits = 0
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(false) }
            ModelRow(
                // Being the current model must not leak radio/selected semantics into batch mode.
                provider = AiConfig(id = "preview", model = model.modelId), model = model,
                selectionMode = true, checked = checked,
                onToggleChecked = { batchChanges++; checked = !checked },
                onEdit = { edits++ }, onSetCurrent = { selections++ },
            )
        }

        val row = compose.onNode(hasRole(Role.Checkbox))
        row.assertIsDisplayed().assertIsOff().assertHasClickAction().assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
        compose.onAllNodes(hasRole(Role.Checkbox), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState), useUnmergedTree = true,
        ).assertCountEquals(1)
        compose.onAllNodes(hasRole(Role.RadioButton), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)

        row.performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(1, batchChanges); assertEquals(0, selections); assertEquals(0, edits)
        }
        compose.onNodeWithText("Batch model", useUnmergedTree = true).performTouchInput { click() }
        row.assertIsOff()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle {
            assertEquals(2, batchChanges); assertEquals(0, selections); assertEquals(0, edits)
        }
    }

    /** Mock of separate details and selection targets; no navigation or stores. */
    @Composable
    private fun ProviderDetailsRow(
        name: String,
        selected: Boolean,
        enabled: Boolean,
        onActivate: () -> Unit,
        onDetails: () -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).testTag("details-$name")
                    .clickable(onClickLabel = "View provider details", onClick = onDetails),
            ) {
                Text(text = name)
            }
            AiProviderSelection(
                selected = selected, enabled = enabled, label = "Select $name", onClick = onActivate,
            )
        }
    }
}
