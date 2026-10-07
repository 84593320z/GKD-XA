package li.gkd.app.platform.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import li.gkd.app.a11y.updateSystemDefaultAppId
import li.gkd.app.appScope
import li.gkd.app.permission.PermissionStates
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.service.armA11yService
import li.gkd.app.service.fixRestartAutomatorService
import li.gkd.app.util.LogUtils
import li.songe.codeorigin.CallSite

object RuntimeStateSynchronizer {
    private val requests = Channel<String>(Channel.CONFLATED)

    init {
        appScope.launch(Dispatchers.IO) {
            for (initialLoc in requests) {
                delay(COALESCE_DELAY_MILLIS)
                var loc = initialLoc
                while (true) {
                    loc = requests.tryReceive().getOrNull() ?: break
                }
                // 逐步隔离：任何一步抛异常都不能连累后面的步骤 —— 之前 updateSystemDefaultAppId
                // 在没有可解析 HOME 的设备上抛 NPE，会把 grantSelf / refreshAll / 自动开无障碍一起带走。
                runStep(loc, "updateSystemDefaultAppId") { updateSystemDefaultAppId() }
                runStep(loc, "grantSelf") { privilegeContextFlow.value?.grantSelf() }
                runStep(loc, "refreshAll") { PermissionStates.refreshAll() }
                runStep(loc, "fixRestartAutomatorService") { fixRestartAutomatorService() }
                runStep(loc, "armA11yService") { armA11yService(loc = "runtime sync") }
            }
        }
    }

    fun requestSync(@CallSite loc: String = "") {
        check(requests.trySend(loc).isSuccess) { "运行时状态同步队列已关闭" }
    }

    private inline fun runStep(
        loc: String,
        name: String,
        body: () -> Unit,
    ) {
        try {
            body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtils.d(e, loc = "$loc -> $name")
        }
    }

    private const val COALESCE_DELAY_MILLIS = 50L
}
