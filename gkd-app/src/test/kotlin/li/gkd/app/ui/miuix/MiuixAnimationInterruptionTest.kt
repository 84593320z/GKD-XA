package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import li.gkd.app.ui.component.rememberGkVisibilityProgress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = CompactLightQualifiers)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiuixAnimationInterruptionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun interruptedFabExitRestoresTheVisibleSurface() {
        compose.mainClock.autoAdvance = false
        compose.setMiuixContent {
            var visible by remember { mutableStateOf(true) }
            val animation = rememberGkVisibilityProgress(visible, durationMillis = 300)
            if (animation.mounted) {
                Column(Modifier.size(72.dp)) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(48.dp).alpha(animation.fraction).background(Color.Red).testTag("animated-fab"))
                    }
                    Row {
                        Box(Modifier.size(24.dp).clickable { visible = false }.testTag("hide"))
                        Box(Modifier.size(24.dp).clickable { visible = true }.testTag("show"))
                    }
                }
            }
        }
        fun centerRed(): Float {
            val bitmap = compose.onNodeWithTag("animated-fab").captureToImage().toPixelMap()
            return bitmap[bitmap.width / 2, bitmap.height / 2].red
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        val fullyVisible = compose.runOnIdle { centerRed() }
        compose.onNodeWithTag("hide").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val duringExit = compose.runOnIdle { centerRed() }
        assertTrue("The hide action must start an exit before reversal ($duringExit)", duringExit in 0.01f..0.99f)
        compose.onNodeWithTag("show").performClick()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        val restored = compose.runOnIdle { centerRed() }
        assertTrue("Reversing the exit must return to the fully visible native FAB surface ($fullyVisible vs $restored)",
            restored > 0.99f && kotlin.math.abs(fullyVisible - restored) < 0.01f)
    }
}
