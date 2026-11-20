package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.component.PerfAlertDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Text

/** Tiny layout budgets without MainActivity, a real keyboard, application stores or a backend. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-rUS-w320dp-h280dp-notnight-mdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class MiuixDialogOverflowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun alertKeepsAnOversizedIconMultilineTitleBodyAndActionsReachable() {
        var confirmations = 0
        var cancellations = 0
        compose.setMiuixContent(fontScale = 2f) {
            GkAlertDialog(
                onDismissRequest = { cancellations++ },
                icon = {
                    Column {
                        Text("Icon start")
                        Spacer(Modifier.height(180.dp))
                        Text("Icon end")
                    }
                },
                title = { Column { repeat(12) { Text("Alert title line $it") } } },
                text = { Column { repeat(12) { Text("Alert body line $it") } } },
                confirmButton = {
                    GkActionButton(onClick = { confirmations++ }) { Text("Apply") }
                },
                dismissButton = {
                    GkActionButton(onClick = { cancellations++ }) { Text("Back") }
                },
            )
        }

        compose.onNodeWithText("Apply").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Icon end").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Alert title line 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Alert body line 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Icon start").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, confirmations); assertEquals(0, cancellations) }
    }

    @Test
    fun perfKeepsMultilineTitlePlainBodyAndWeightedActionsReachable() {
        var confirmations = 0
        var cancellations = 0
        compose.setMiuixContent(fontScale = 2f) {
            PerfAlertDialog(
                onDismissRequest = { cancellations++ },
                title = { Column { repeat(12) { Text("Perf title line $it") } } },
                text = { Column { repeat(12) { Text("Perf body line $it") } } },
                confirmButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { confirmations++ }) { Text("Apply") }
                },
                dismissButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { cancellations++ }) { Text("Back") }
                },
            )
        }

        compose.onNodeWithText("Apply").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Perf title line 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Perf body line 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Perf title line 0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, confirmations); assertEquals(0, cancellations) }
    }

    @Test
    fun perfDoesNotMeasureItsSelfScrollingBodyWithInfiniteHeight() {
        var confirmations = 0
        val composedItems = mutableSetOf<Int>()
        compose.setMiuixContent(fontScale = 1.6f) {
            PerfAlertDialog(
                onDismissRequest = {},
                title = { Column { repeat(12) { Text("Lazy title line $it") } } },
                text = {
                    LazyColumn(modifier = Modifier.testTag("dialog-lazy-body")) {
                        items(50) { index ->
                            SideEffect { composedItems += index }
                            Text("Lazy body item $index")
                        }
                    }
                },
                confirmButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { confirmations++ }) { Text("Apply") }
                },
            )
        }

        compose.onNodeWithText("Apply").assertIsDisplayed()
        compose.runOnIdle { assertTrue("The body must stay lazy", composedItems.size < 20) }
        compose.onNodeWithText("Lazy title line 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dialog-lazy-body").performScrollTo().performScrollToIndex(49)
        compose.onNodeWithText("Lazy body item 49").assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test
    @Config(qualifiers = "en-rUS-w320dp-h240dp-night-mdpi")
    fun alertWithoutBodyCanScrollItsTitleAndKeepBothActionsTouchable() {
        var cancellations = 0
        compose.setMiuixContent(dark = true, fontScale = 2f) {
            GkAlertDialog(
                onDismissRequest = {},
                title = { Column { repeat(16) { Text("Title only line $it") } } },
                confirmButton = { GkActionButton(onClick = {}) { Text("Apply") } },
                dismissButton = { GkActionButton(onClick = { cancellations++ }) { Text("Back") } },
            )
        }

        compose.onNodeWithText("Title only line 15").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Title only line 0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed()
        compose.onNodeWithText("Back").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, cancellations) }
    }

    @Test
    @Config(qualifiers = CompactLightQualifiers)
    fun perfFormRemainsReachableWhenAvailableHeightShrinksLikeAnImeViewport() {
        val height = mutableStateOf(480.dp)
        var confirmations = 0
        compose.setMiuixContent(fontScale = 1.6f) {
            var first by remember { mutableStateOf("") }
            var second by remember { mutableStateOf("") }
            PerfAlertDialog(
                modifier = Modifier.heightIn(max = height.value),
                onDismissRequest = {},
                title = { Text("A multiline form title\nwith another line\nand another line") },
                text = {
                    Column {
                        GkFormTextField(value = first, onValueChange = { first = it }, label = "First field")
                        GkFormTextField(value = second, onValueChange = { second = it }, label = "Second field")
                        Text("Form body end")
                    }
                },
                confirmButton = {
                    GkActionButton(modifier = Modifier.weight(1f), onClick = { confirmations++ }) { Text("Apply") }
                },
            )
        }

        compose.onNodeWithText("Form body end").performScrollTo().assertIsDisplayed()
        // Parent constraints, not LocalWindowInfo, are the source of truth after an IME/resize.
        // This simulates the reduced layout budget; it does not launch a software keyboard.
        compose.runOnIdle { height.value = 240.dp }
        compose.onNodeWithText("Form body end").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextReplacement("Local edit")
        compose.onNodeWithText("Form body end").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    @Test
    @Config(qualifiers = "en-rUS-w320dp-h320dp-notnight-mdpi")
    fun alertReservesBothRowsOfWrappingActionsBeforeItsScrollableContent() {
        var cancellations = 0
        compose.setMiuixContent(fontScale = 2f) {
            GkAlertDialog(
                onDismissRequest = {},
                title = { Column { repeat(12) { Text("Wrapped action title $it") } } },
                text = { Column { repeat(12) { Text("Wrapped action body $it") } } },
                confirmButton = {
                    GkActionButton(modifier = Modifier.fillMaxWidth().testTag("apply"), onClick = {}) { Text("Apply settings") }
                },
                dismissButton = {
                    GkActionButton(modifier = Modifier.fillMaxWidth().testTag("back"), onClick = { cancellations++ }) { Text("Keep editing") }
                },
            )
        }

        val back = compose.onNodeWithTag("back").assertIsDisplayed().getUnclippedBoundsInRoot()
        val apply = compose.onNodeWithTag("apply").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Long actions should wrap onto separate rows", apply.top >= back.bottom)
        compose.onNodeWithText("Wrapped action body 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("back").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, cancellations) }
    }

    @Test
    fun onlyTheTopDialogRendersAndTheUnderlyingDialogReturnsAfterDismissal() {
        val topVisible = mutableStateOf(false)
        var topDismissals = 0
        var bottomDismissals = 0
        compose.setMiuixContent {
            GkAlertDialog(
                onDismissRequest = { bottomDismissals++ },
                title = { Text("Underlying dialog") },
                confirmButton = { GkActionButton(onClick = {}) { Text("Underlying action") } },
            )
            if (topVisible.value) {
                PerfAlertDialog(
                    onDismissRequest = { topDismissals++; topVisible.value = false },
                    title = { Text("Top dialog") },
                    confirmButton = {
                        GkActionButton(onClick = { topDismissals++; topVisible.value = false }) { Text("Close top") }
                    },
                )
            }
        }

        compose.onNodeWithText("Underlying dialog").assertIsDisplayed()
        compose.runOnIdle { topVisible.value = true }
        compose.onNodeWithText("Underlying dialog").assertDoesNotExist()
        compose.onNodeWithText("Top dialog").assertIsDisplayed()
        compose.onNodeWithText("Close top").performTouchInput { click() }
        compose.onNodeWithText("Top dialog").assertDoesNotExist()
        compose.onNodeWithText("Underlying dialog").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, topDismissals); assertEquals(0, bottomDismissals) }
    }

    @Test
    @Config(qualifiers = CompactLightQualifiers)
    fun regularAlertKeepsItsExistingTwelveDpSlotSpacing() = assertRegularSpacing(perf = false)

    @Test
    @Config(qualifiers = CompactLightQualifiers)
    fun regularPerfAlertKeepsItsExistingTwelveDpSlotSpacing() = assertRegularSpacing(perf = true)

    private fun assertRegularSpacing(perf: Boolean) {
        compose.setMiuixContent {
            if (perf) {
                PerfAlertDialog(
                    onDismissRequest = {},
                    title = { Text("Title", modifier = Modifier.testTag("title")) },
                    text = { Text("Body", modifier = Modifier.testTag("body")) },
                    confirmButton = {
                        GkActionButton(modifier = Modifier.testTag("action"), onClick = {}) { Text("Apply") }
                    },
                )
            } else {
                GkAlertDialog(
                    onDismissRequest = {},
                    title = { Text("Title", modifier = Modifier.testTag("title")) },
                    text = { Text("Body", modifier = Modifier.testTag("body")) },
                    confirmButton = {
                        GkActionButton(modifier = Modifier.testTag("action"), onClick = {}) { Text("Apply") }
                    },
                )
            }
        }

        val title = compose.onNodeWithTag("title").getUnclippedBoundsInRoot()
        val body = compose.onNodeWithTag("body").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithTag("action").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertEquals(12.dp, body.top - title.bottom)
        assertEquals(12.dp, action.top - body.bottom)
    }
}
