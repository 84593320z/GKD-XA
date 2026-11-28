package li.gkd.app.service

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flowOf
import li.gkd.app.a11y.A11yRuntime
import li.gkd.app.appScope
import li.gkd.app.snapshot.SnapshotCapture
import li.gkd.app.snapshot.SnapshotCaptureMode
import li.gkd.app.store.AppStore
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.AiRuleGenerator
import li.gkd.app.util.AndroidGlobalActions
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.systemUiAppId

class SnapshotTileService : BaseTileService() {
    override val activeFlow = flowOf(false)
    override fun onTileClick() = execSnapshot()
}

private fun execSnapshot() {
    LogUtils.d("SnapshotTileService::onClick")
    val service = A11yRuntime.service
    if (service == null) {
        toast(UiStrings.service_not_connected, forced = true)
        return
    }
    appScope.launchUi(Dispatchers.IO) {
        val target = awaitSnapshotTileTarget(
            readActivePackage = { A11yRuntime.getRoot(service)?.packageName?.toString() },
            readDismissActionAvailability = { AndroidGlobalActions.queryNotificationShadeDismissAvailability(service) },
            dismissShade = { AndroidGlobalActions.dismissNotificationShade(service) },
            showManualDismissHint = { toast("请手动收起通知栏以继续捕获快照", forced = true) },
        )
        val error = when (target) {
            SnapshotTileTarget.APP -> null
            SnapshotTileTarget.ROOT_UNAVAILABLE -> UiStrings.snapshot_app_a11y_missing
            SnapshotTileTarget.NO_APP_TRANSITION -> UiStrings.snapshot_activity_switch_missing
        }
        if (error != null) {
            toast(error, forced = true)
            return@launchUi
        }
        if (A11yRuntime.service !== service) {
            toast(UiStrings.service_not_connected, forced = true)
            return@launchUi
        }
        SnapshotCaptureMode.AUTOMATIC.run(
            capture = { SnapshotCapture.capture(forcedCropStatusBar = true).id },
            aiEnabled = AppStore.storeFlow.value.aiEnable,
            generate = { id, bypass -> AiRuleGenerator.generateRule(id, allowWhenAiDisabled = bypass) },
        )
    }
}

internal enum class SnapshotTileTarget { APP, ROOT_UNAVAILABLE, NO_APP_TRANSITION }

/** Only an advertised shade action or observed SystemUI root may start dismissal detection. */
internal suspend fun awaitSnapshotTileTarget(
    readActivePackage: () -> String?,
    readDismissActionAvailability: () -> Boolean?,
    dismissShade: () -> Boolean,
    showManualDismissHint: () -> Unit,
    elapsedMillis: () -> Long = SystemClock::elapsedRealtime,
    pause: suspend (Long) -> Unit = { delay(it) },
): SnapshotTileTarget {
    val deadline = elapsedMillis() + 3_000L
    var sawSystemUi = false
    var sawShadeAction = false
    var attemptedDismiss = false
    var dismissRequestAccepted = false
    var manualHintShown = false
    var appId: String? = null
    while (elapsedMillis() < deadline) {
        currentCoroutineContext().ensureActive()
        appId = readActivePackage()
        currentCoroutineContext().ensureActive()
        if (elapsedMillis() >= deadline) break
        val availability = readDismissActionAvailability()
        currentCoroutineContext().ensureActive()
        if (elapsedMillis() >= deadline) break
        val hasAppRoot = appId != null && appId != systemUiAppId
        // Focus can return to the app during the animation while the shade is still expanded.
        if (hasAppRoot && sawSystemUi && availability != true) return SnapshotTileTarget.APP
        if (appId == systemUiAppId) sawSystemUi = true
        if (availability == true) {
            sawShadeAction = true
            if (!attemptedDismiss) {
                attemptedDismiss = true
                dismissRequestAccepted = dismissShade()
                if (!dismissRequestAccepted && !manualHintShown) {
                    manualHintShown = true
                    showManualDismissHint()
                }
            }
        } else {
            // A failed or unsupported query needs independent evidence of a root transition.
            if (hasAppRoot && sawShadeAction && dismissRequestAccepted && availability == false) {
                return SnapshotTileTarget.APP
            }
            val needsManualDismissal = (sawSystemUi && !dismissRequestAccepted) ||
                (sawShadeAction && availability == null)
            if (!manualHintShown && needsManualDismissal) {
                manualHintShown = true
                showManualDismissHint()
            }
        }
        val remaining = deadline - elapsedMillis()
        if (remaining > 0) pause(minOf(remaining, 250L))
    }
    return if (appId == null) SnapshotTileTarget.ROOT_UNAVAILABLE else SnapshotTileTarget.NO_APP_TRANSITION
}
