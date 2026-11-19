package li.gkd.app.ui.miuix

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import priv.kit.ui.PrivilegePreviewScaffold
import priv.kit.ui.R as PrivilegeR

/**
 * Only the local UI module's in-memory preview is permitted here. No real privilege host,
 * runtime, view model, service, permission request, shell command or start button is used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = CompactLightQualifiers)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class PrivilegePreviewRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun localSimulationRendersWithoutRequestingAuthorization() {
        var permissionCallbacks = 0
        var navigationCallbacks = 0
        compose.setMiuixContent {
            PrivilegePreviewScaffold(
                onPermissionsChanged = { _, _ -> permissionCallbacks++ },
                onViewPermissionSolutions = { navigationCallbacks++ },
                onViewStartupTutorial = { navigationCallbacks++ },
            )
        }

        val resources = RuntimeEnvironment.getApplication().resources
        compose.onNodeWithText(resources.getString(PrivilegeR.string.priv_ui_title)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(PrivilegeR.string.priv_ui_service_not_started)).assertIsDisplayed()
        compose.onNodeWithTag("miuix-test-root").assertIsDisplayed().assertWidthAtMost(320.dp)
        compose.runOnIdle { assertEquals(0, permissionCallbacks); assertEquals(0, navigationCallbacks) }
    }

    @Test
    @Config(qualifiers = CompactDarkQualifiers)
    fun localSimulationRendersAtLargeFontOnANarrowDarkScreenWithoutStartingAnything() {
        var permissionCallbacks = 0
        compose.setMiuixContent(dark = true, fontScale = 1.6f) {
            PrivilegePreviewScaffold(onPermissionsChanged = { _, _ -> permissionCallbacks++ })
        }

        val resources = RuntimeEnvironment.getApplication().resources
        compose.onNodeWithText(resources.getString(PrivilegeR.string.priv_ui_title)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(PrivilegeR.string.priv_ui_service_not_started)).assertIsDisplayed()
        compose.onNodeWithTag("miuix-test-root").assertIsDisplayed().assertWidthAtMost(320.dp)
        compose.runOnIdle { assertEquals(0, permissionCallbacks) }
    }
}
