package li.gkd.app.service

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.text.UiStrings
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.permission.PermissionStates
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.IntentUtils
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons

class ButtonService : OverlayWindowService(
    positionKey = "button"
) {
    private val menuOpenFlow = MutableStateFlow(false)

    override fun onClickView() {
        if (isOverlayContentHidden) return
        // 展开态下动作由菜单项自己处理；这里不能再动，否则点「算了」会连带执行一次快照。
        if (menuOpenFlow.value) return
        menuOpenFlow.value = true
    }

    override fun onLongClickView() = stopSelf()

    @Composable
    override fun ComposeContent() {
        val menuOpen by menuOpenFlow.collectAsStateWithLifecycle()
        if (!menuOpen) {
            val alpha = 0.75f
            GkIcon(
                imageVector = GkIcons.CenterFocusWeak,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = alpha))
                    .size(40.dp),
                tint = MiuixTheme.colorScheme.primary.copy(alpha = alpha),
            )
            return
        }
        // 窗口是 WRAP_CONTENT，内容撑开它自己就变大；基类带 FLAG_NOT_FOCUSABLE，
        // 收不到「点外部」事件，所以收起只能靠显式的「算了」项。
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.94f))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MenuItem(UiStrings.button_menu_snapshot) { runMenuItem { withAllOverlaysHidden { SnapshotCapture.capture() } } }
            MenuItem(UiStrings.button_menu_enhanced) { runMenuItem { withAllOverlaysHidden { AiRuleGenerator.enhancedGenerate() } } }
            MenuItem(UiStrings.button_menu_cancel) { menuOpenFlow.value = false }
        }
    }

    private fun runMenuItem(block: suspend () -> Unit) {
        menuOpenFlow.value = false
        lifecycleScope.launchUi { block() }
    }

    @Composable
    private fun MenuItem(label: String, onClick: () -> Unit) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }

    init {
        useServicePresence(
            stateFlow = isRunning,
            name = UiStrings.snapshot_button_service,
        )
        onCreated {
            NotificationCatalog.button().startForeground()
        }
    }

    companion object {
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)
        fun start() {
            if (!PermissionStates.drawOverlays.checkOrToast()) return
            IntentUtils.startForegroundServiceByClass(ButtonService::class)
        }

        fun stop() = IntentUtils.stopServiceByClass(ButtonService::class)

    }
}
