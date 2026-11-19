package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkAppBarTextField
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.SettingItem
import li.gkd.app.ui.icon.GkSearchCloseIconButton
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
class MiuixSettingsSearchRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settingItemAndAliasRenderTheirContentAndDispatchOneRowClickEach() {
        var nativeClicks = 0
        var aliasClicks = 0
        compose.setMiuixContent {
            Column {
                GkSettingItem(
                    title = "Native settings", subtitle = "Shared native row",
                    suffix = "Available", onClick = { nativeClicks++ },
                )
                SettingItem(
                    title = "Legacy settings", subtitle = "Same component through its alias",
                    onClick = { aliasClicks++ },
                )
            }
        }

        compose.onNodeWithText("Shared native row").assertIsDisplayed()
        compose.onNodeWithText("Available").assertIsDisplayed()
        compose.onNodeWithText("Same component through its alias").assertIsDisplayed()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithText("Native settings").assertIsDisplayed().performClick()
        compose.onNodeWithText("Legacy settings").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, nativeClicks); assertEquals(1, aliasClicks) }
    }

    @Test
    fun interactiveSuffixDoesNotAccidentallyInvokeTheParentRow() {
        var rowClicks = 0
        var suffixClicks = 0
        compose.setMiuixContent {
            GkSettingItem(
                title = "Rule source", subtitle = "Source documentation", suffix = "Read help",
                suffixUnderline = true, onSuffixClick = { suffixClicks++ },
                onClick = { rowClicks++ },
            )
        }

        compose.onNodeWithText("Read help").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, rowClicks); assertEquals(1, suffixClicks) }
        compose.onNodeWithText("Rule source").performClick()
        compose.runOnIdle { assertEquals(1, rowClicks); assertEquals(1, suffixClicks) }
    }

    @Test
    fun nonNavigatingSettingRowsDoNotInventActions() {
        compose.setMiuixContent {
            Column {
                GkSettingItem(title = "Build version", subtitle = "Local preview")
                SettingItem(title = "Legacy version", subtitle = "Also informational", imageVector = null)
            }
        }

        compose.onNodeWithText("Build version").assertIsDisplayed()
        compose.onNodeWithText("Legacy version").assertIsDisplayed()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun settingsAndAliasStayReachableAtLargeFontInANarrowDarkWindow() {
        var clicks = 0
        val summary = "A long setting summary with enough words to wrap rather than require a wide screen."
        compose.setMiuixContent(dark = true, fontScale = 1.8f) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GkSettingItem(title = "A long setting title for a narrow screen", subtitle = summary)
                SettingItem(
                    title = "Legacy row at large font", subtitle = summary,
                    suffix = "Details", onClick = { clicks++ },
                )
            }
        }

        compose.onNodeWithTag("miuix-test-root").assertIsDisplayed().assertWidthAtMost(320.dp)
        compose.onNodeWithText("A long setting title for a narrow screen").assertIsDisplayed()
        compose.onNodeWithText("Legacy row at large font").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun appBarSearchSanitizesNewlinesAndKeepsItsImeSearchAction() {
        val changes = mutableListOf<String>()
        var searches = 0
        compose.setMiuixContent {
            var value by remember { mutableStateOf("") }
            GkAppBarTextField(
                value = value, onValueChange = { changes += it; value = it },
                hint = "Search rules",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searches++ }),
            )
        }

        val editor = hasContentDescription("Search rules") and hasSetTextAction()
        compose.onNodeWithText("Search rules").assertIsDisplayed()
        val input = compose.onNode(editor).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        input.performTextReplacement("work\nrules")
        input.assertTextContains("workrules")
        input.performImeAction()
        compose.runOnIdle { assertEquals(listOf("workrules"), changes); assertEquals(1, searches) }
    }

    @Test
    fun appBarSearchReflectsExternalReplacementWithoutLosingItsEditableNode() {
        val value = mutableStateOf("Original query")
        compose.setMiuixContent {
            GkAppBarTextField(value = value.value, onValueChange = { value.value = it }, hint = "Search items")
        }

        val input = compose.onNodeWithContentDescription("Search items")
        input.assertIsDisplayed().assertTextContains("Original query")
        compose.runOnIdle { value.value = "New" }
        input.assertTextContains("New")
        input.performTextReplacement("Edited")
        input.assertTextContains("Edited")
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.runOnIdle { assertEquals("Edited", value.value) }
    }

    @Test
    fun searchCloseIconChangesItsAccessibleNameAndDispatchesOnce() {
        var clicks = 0
        compose.setMiuixContent {
            var open by remember { mutableStateOf(false) }
            GkSearchCloseIconButton(
                isSearchOpen = open,
                onClick = { clicks++; open = !open },
            )
        }

        compose.onNodeWithContentDescription(UiStrings.search_open).assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription(UiStrings.search_close).assertIsDisplayed()
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun namedAndDecorativeIconsDoNotDuplicateActionsAndDisabledIconIsInert() {
        var clicks = 0
        compose.setMiuixContent {
            Row {
                GkIconButton(imageVector = GkIcons.Add, onClick = { clicks++ })
                GkIconButton(
                    modifier = Modifier.testTag("disabled-delete"), imageVector = GkIcons.Delete,
                    contentDescription = "Delete preview", onClick = { clicks++ }, enabled = false,
                )
                GkIcon(imageVector = GkIcons.KeyboardArrowRight, contentDescription = null)
            }
        }

        compose.onAllNodes(hasContentDescription(UiStrings.action_add)).assertCountEquals(1)
        compose.onNodeWithContentDescription(UiStrings.action_add).assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performTouchInput { click() }
        compose.onNodeWithTag("disabled-delete").assertIsDisplayed().assertIsNotEnabled()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
