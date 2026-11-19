package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.component.GkCheckbox
import li.gkd.app.ui.component.GkSwitch
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.TextSwitch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = CompactLightQualifiers)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MiuixSwitchRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaultTextSwitchRowRendersAndTogglesExactlyOnce() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(false) }
            GkTextSwitch(
                title = "Enable matching",
                subtitle = "Only the in-memory test value changes",
                checked = checked,
                onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onNodeWithText("Enable matching").assertIsDisplayed().assertIsOff()
        compose.onNodeWithText("Only the in-memory test value changes").assertIsDisplayed()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Enable matching").performClick().assertIsOn()
        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun legacyTextSwitchUsesTheSameDefaultRowAction() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(true) }
            TextSwitch(
                title = "Legacy switch",
                checked = checked,
                onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onNodeWithText("Legacy switch").assertIsDisplayed().assertIsOn()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Legacy switch").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test
    fun disabledTextSwitchAndAliasDoNotDispatchTouchCallbacks() {
        var callbacks = 0
        compose.setMiuixContent {
            Column {
                GkTextSwitch(
                    title = "Disabled switch", checked = false, enabled = false,
                    onCheckedChange = { callbacks++ },
                )
                TextSwitch(
                    modifier = Modifier.testTag("disabled-legacy-row"),
                    title = "Disabled legacy switch", checked = true, enabled = false,
                    onCheckedChange = { callbacks++ }, onClick = { callbacks++ },
                )
            }
        }

        compose.onNodeWithText("Disabled switch")
            .assertIsDisplayed().assertIsNotEnabled().assertIsOff()
            .performTouchInput { click() }
        // The custom-action alias keeps a separate switch; the disabled row remains inert.
        compose.onNodeWithTag("disabled-legacy-row").assertIsNotEnabled()
            .performTouchInput { click(Offset(2f, 2f)) }
        compose.onNodeWithContentDescription("Disabled legacy switch")
            .assertIsNotEnabled().assertIsOn().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, callbacks) }
    }

    @Test
    fun customTextSwitchClickDoesNotReplaceTheSeparateSwitchCallback() {
        var rowClicks = 0
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(false) }
            GkTextSwitch(
                title = "Open matching details", checked = checked,
                onClick = { rowClicks++ },
                onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onNodeWithText("Open matching details").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, rowClicks); assertEquals(emptyList<Boolean>(), changes) }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithContentDescription("Open matching details")
            .assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(1, rowClicks); assertEquals(listOf(true), changes) }
    }

    @Test
    fun legacyTextSwitchPreservesAnExplicitCustomRowClick() {
        var rowClicks = 0
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            TextSwitch(
                title = "Legacy details", checked = false,
                onClick = { rowClicks++ }, onCheckedChange = { changes += it },
            )
        }

        compose.onNodeWithText("Legacy details").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, rowClicks); assertEquals(emptyList<Boolean>(), changes) }
        compose.onNodeWithContentDescription("Legacy details").assertIsOff().performClick()
        compose.runOnIdle { assertEquals(1, rowClicks); assertEquals(listOf(true), changes) }
    }

    @Test
    fun explicitNullRowClickLeavesOnlyTheSwitchInteractive() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            GkTextSwitch(
                title = "Switch only", checked = false, onClick = null,
                onCheckedChange = { changes += it },
            )
        }

        compose.onNodeWithText("Switch only").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(emptyList<Boolean>(), changes) }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithContentDescription("Switch only").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun standaloneSwitchHasOneActionAndIts48dpPaddingIsClickable() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(false) }
            GkSwitch(
                modifier = Modifier.testTag("switch"), checked = checked,
                onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onAllNodes(hasRole(Role.Switch) and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("switch").assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performTouchInput { click(Offset(2f, 2f)) }.assertIsOn()
        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun tappingNativeSwitchCenterDispatchesOnlyOnce() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            GkSwitch(
                modifier = Modifier.testTag("switch"), checked = true,
                onCheckedChange = { changes += it },
            )
        }

        compose.onNodeWithTag("switch").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test
    fun checkboxHasOneActionAndIts48dpPaddingIsClickable() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            var checked by remember { mutableStateOf(false) }
            GkCheckbox(
                modifier = Modifier.testTag("checkbox"), checked = checked,
                onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onAllNodes(hasRole(Role.Checkbox) and hasClickAction(), useUnmergedTree = true)
            .assertCountEquals(1)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("checkbox").assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            .performTouchInput { click(Offset(2f, 2f)) }.assertIsOn()
        compose.runOnIdle { assertEquals(listOf(true), changes) }
    }

    @Test
    fun tappingNativeCheckboxCenterDispatchesOnlyOnce() {
        val changes = mutableListOf<Boolean>()
        compose.setMiuixContent {
            GkCheckbox(
                modifier = Modifier.testTag("checkbox"), checked = true,
                onCheckedChange = { changes += it },
            )
        }

        compose.onNodeWithTag("checkbox").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test
    fun disabledStandaloneControlsRemainVisibleButDoNotMutateState() {
        var callbacks = 0
        compose.setMiuixContent {
            Column {
                GkSwitch(
                    modifier = Modifier.testTag("switch"), checked = true, enabled = false,
                    onCheckedChange = { callbacks++ },
                )
                GkCheckbox(
                    modifier = Modifier.testTag("checkbox"), checked = false, enabled = false,
                    onCheckedChange = { callbacks++ },
                )
            }
        }

        compose.onNodeWithTag("switch").assertIsDisplayed().assertIsNotEnabled().assertIsOn()
            .performTouchInput { click() }
        compose.onNodeWithTag("checkbox").assertIsDisplayed().assertIsNotEnabled().assertIsOff()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, callbacks) }
    }

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun largeFontSwitchRowWrapsAndStillTogglesOnANarrowDarkScreen() {
        val changes = mutableListOf<Boolean>()
        val title = "A descriptive switch title that wraps"
        compose.setMiuixContent(dark = true, fontScale = 1.8f) {
            var checked by remember { mutableStateOf(true) }
            GkTextSwitch(
                title = title,
                subtitle = "A longer explanation remains part of the same accessible setting.",
                checked = checked, onCheckedChange = { changes += it; checked = it },
            )
        }

        compose.onNodeWithText(title).assertIsDisplayed().assertIsOn().performClick().assertIsOff()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { assertEquals(listOf(false), changes) }
    }

    @Test
    fun displayOnlyControlsDoNotInventClickActions() {
        compose.setMiuixContent {
            Column {
                GkSwitch(checked = true, onCheckedChange = null, modifier = Modifier.testTag("switch"))
                GkCheckbox(checked = false, modifier = Modifier.testTag("checkbox"))
            }
        }

        compose.onNodeWithTag("switch").assertIsDisplayed().assertIsOn()
        compose.onNodeWithTag("checkbox").assertIsDisplayed().assertIsOff()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(0)
    }
}
