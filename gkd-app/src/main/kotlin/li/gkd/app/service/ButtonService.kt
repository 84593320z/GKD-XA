package li.gkd.app.service

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import li.gkd.app.R
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.permission.PermissionStates
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.snapshot.SnapshotCaptureMode
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.IntentUtils
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle

// Keep the snapshot overlay light independently of the overlay host theme.
private val SnapshotIconTint = Color(0xFF0D84FF)
private val SnapshotDividerColor = Color(0xFFE5E5E5)

class ButtonService : OverlayWindowService(
    positionKey = "button"
) {
    private val menuOpenFlow = MutableStateFlow(false)

    override fun onClickView() {
        if (isOverlayContentHidden) return
        // 展开态下动作由菜单项自己处理；这里不能再动，否则点「取消」会连带执行一次快照。
        if (menuOpenFlow.value) return
        menuOpenFlow.value = true
    }

    override fun onLongClickView() = stopSelf()

    override fun shouldDispatchTouchToCompose(): Boolean = menuOpenFlow.value

    @Composable
    override fun ComposeContent() {
        val menuOpen by menuOpenFlow.collectAsStateWithLifecycle()
        if (!menuOpen) {
            // 只渲染 Miuix 外观并暴露无障碍动作；物理触摸留给 overlay host 处理拖动、点击和长按。
            Card(
                modifier = Modifier.size(48.dp).semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        if (isOverlayContentHidden) false else {
                            onClickView()
                            true
                        }
                    }
                    onLongClick {
                        if (isOverlayContentHidden) false else {
                            onLongClickView()
                            true
                        }
                    }
                },
                colors = CardDefaults.defaultColors(color = Color.White),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GkIcon(
                        imageVector = GkIcons.CenterFocusWeak,
                        contentDescription = stringResource(R.string.snapshot_button),
                        modifier = Modifier.size(24.dp),
                        tint = SnapshotIconTint,
                    )
                }
            }
            return
        }
        // 非聚焦悬浮窗收不到外部点击，始终保留一个明确的收起入口。
        Card(
            modifier = Modifier.width(224.dp),
            colors = CardDefaults.defaultColors(color = Color.White),
        ) {
            SmallTitle(
                text = UiStrings.snapshot_title,
                modifier = Modifier.semantics { heading() },
                textColor = Color.Black,
                insideMargin = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
            )
            MenuItem(UiStrings.button_menu_snapshot, GkIcons.CenterFocusWeak) {
                runCapture(SnapshotCaptureMode.SNAPSHOT_ONLY)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SnapshotDividerColor)
            MenuItem(UiStrings.button_menu_snapshot_generate, GkIcons.Edit) {
                runCapture(SnapshotCaptureMode.SNAPSHOT_AND_GENERATE)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SnapshotDividerColor)
            MenuItem(UiStrings.action_cancel, GkIcons.Close) {
                menuOpenFlow.value = false
            }
        }
    }

    private fun runCapture(mode: SnapshotCaptureMode) {
        menuOpenFlow.value = false
        lifecycleScope.launchUi {
            mode.run(
                capture = { withAllOverlaysHidden { SnapshotCapture.capture().id } },
                aiEnabled = li.gkd.app.store.AppStore.storeFlow.value.aiEnable,
                generate = { id, bypass -> AiRuleGenerator.generateRule(id, allowWhenAiDisabled = bypass) },
            )
        }
    }

    @Composable
    private fun MenuItem(
        label: String,
        icon: ImageVector,
        onClick: () -> Unit,
    ) {
        BasicComponent(
            title = label,
            titleColor = BasicComponentDefaults.titleColor(color = Color.Black),
            modifier = Modifier.heightIn(min = 48.dp),
            startAction = {
                GkIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SnapshotIconTint,
                )
            },
            onClick = onClick,
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
