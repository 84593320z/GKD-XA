package li.gkd.app.ui.miuix

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import li.gkd.app.ui.component.GkAlertDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import java.io.File
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.PreferenceGroup
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import priv.kit.ui.PrivilegePreviewScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Real CPU-rendered Skia images; not screenshots of the physical, locked phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w400dp-h820dp-notnight-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiuixScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun save(name: String, dialog: Boolean = false) {
        val node = if (dialog) compose.onNode(isDialog()) else compose.onNodeWithTag("miuix-test-root")
        val bitmap = node.captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width >= 300 && bitmap.height >= 300)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue("The rendered hierarchy must contain visible foreground as well as its background",
            pixels.toSet().size > 12)
        val output = File("build/reports/miuix-ui", "$name.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun controls(dark: Boolean, fontScale: Float = 1f) {
        compose.setMiuixContent(dark = dark, fontScale = fontScale) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("界面与交互", style = MiuixTheme.textStyles.title1,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))
                PreferenceGroup(title = "显示设置", showTop = false) {
                    var enabled by remember { mutableStateOf(true) }
                    GkTextSwitch(title = "模糊效果", subtitle = "与所有页面共享同一套控件和配色",
                        checked = enabled, onCheckedChange = { enabled = it })
                    GkTextSwitch(title = "液态玻璃", subtitle = "当前设备不支持时保持清晰的禁用状态",
                        checked = false, enabled = false, onCheckedChange = {})
                    GkSettingItem(title = "工作模式", subtitle = "查看授权和服务状态", onClick = {})
                }
                PreferenceGroup(title = "编辑内容") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GkFormTextField(value = "", onValueChange = {}, label = "订阅地址",
                            placeholder = "https://example.com/rules.json", isError = true,
                            errorMessage = "请输入有效地址", supportingText = { Text("请输入有效地址") })
                        GkActionButton(onClick = {}, emphasized = true,
                            modifier = Modifier.fillMaxWidth()) { Text("保存设置") }
                    }
                }
            }
        }
    }

    @Test fun controlsLight() { controls(false); save("controls-light") }

    @Test
    @Config(qualifiers = "zh-rCN-w400dp-h820dp-night-mdpi")
    fun controlsDark() { controls(true); save("controls-dark") }

    @Test
    @Config(qualifiers = "zh-rCN-w320dp-h900dp-night-mdpi")
    fun controlsNarrowLargeFont() { controls(true, 1.6f); save("controls-large-font") }

    @Test fun privilegeLight() {
        compose.setMiuixContent { PrivilegePreviewScaffold() }
        save("privilege-light")
    }

    @Test
    @Config(qualifiers = "zh-rCN-w400dp-h820dp-night-mdpi")
    fun privilegeDark() {
        compose.setMiuixContent(dark = true) { PrivilegePreviewScaffold() }
        save("privilege-dark")
    }
    @Test fun nativeAlertLight() {
        compose.setMiuixContent {
            GkAlertDialog(onDismissRequest = {}, title = { Text("确认删除订阅") },
                text = { Text("删除后将移除此订阅及关联规则。此操作不会更改其他订阅。") },
                confirmButton = { GkActionButton(onClick = {}, emphasized = true) { Text("删除") } },
                dismissButton = { GkActionButton(onClick = {}) { Text("取消") } })
        }
        compose.onNodeWithText("删除").assertIsDisplayed()
        save("alert-light", dialog = true)
    }

    @Test
    @Config(qualifiers = "zh-rCN-w640dp-h320dp-land-night-mdpi")
    fun nativeAlertLandscapeLargeFont() {
        compose.setMiuixContent(dark = true, fontScale = 1.6f) {
            GkAlertDialog(onDismissRequest = {}, title = { Text("界面与交互确认") },
                text = { Text("在窄屏、横屏及大字号下，确认和取消操作均应保持可见。".repeat(12)) },
                confirmButton = { GkActionButton(onClick = {}, emphasized = true) { Text("确认") } },
                dismissButton = { GkActionButton(onClick = {}) { Text("取消") } })
        }
        compose.onNodeWithText("确认").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        save("alert-landscape-large-font", dialog = true)
    }

}
