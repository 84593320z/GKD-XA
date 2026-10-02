package li.gkd.app.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
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
    private var lastClickTime = 0L

    override fun onClickView() {
        if (isOverlayContentHidden) return
        lifecycleScope.launchUi {
            val now = System.currentTimeMillis()
            val gap = now - lastClickTime
            lastClickTime = now
            if (gap < 400) {
                // 双击 → 加强模式
                lastClickTime = 0L
                withAllOverlaysHidden {
                    AiRuleGenerator.enhancedGenerate()
                }
            } else {
                // 等待判断是否双击
                delay(400)
                if (lastClickTime == now) {
                    // 未被后续点击覆盖，执行单击
                    withAllOverlaysHidden {
                        SnapshotCapture.capture()
                    }
                }
            }
        }
    }

    override fun onLongClickView() = stopSelf()

    @Composable
    override fun ComposeContent() {
        val alpha = 0.75f
        GkIcon(
            imageVector = GkIcons.CenterFocusWeak,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
                .size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
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
