package li.gkd.app.a11y

import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import li.gkd.app.appRuntimeReady
import li.gkd.app.domain.rule.OwnedEventQueue
import li.gkd.app.domain.rule.QueryRequest
import li.gkd.app.domain.rule.QueryWakeup
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.META
import li.gkd.app.data.ActionPerformer
import li.gkd.app.data.AppRule
import li.gkd.app.data.ResolvedRule
import li.gkd.app.data.RuleStatus
import li.gkd.app.platform.lifecycle.MainActivityVisibility
import li.gkd.app.service.EventService
import li.gkd.app.service.topAppIdFlow
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.store.AppStore.actualBlockA11yAppList
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.util.AndroidTarget
import li.gkd.app.util.launchLogged
import li.gkd.app.util.ToastUtils.showActionToast
import li.gkd.app.util.systemUiAppId
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds


private val eventDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val queryDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val actionDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class A11yRuleEngine(private val service: A11yCommonImpl) {
    private val a11yContext = A11yContext(getRoot = { safeActiveWindow })
    private val effective get() = appRuntimeReady.isCompleted &&
        !CrashRecovery.isRecoveryBlocked && A11yRuntime.isEffective(service)
    private val hasOthersService = A11yRuntime.hasOtherService(service)

    fun onA11yConnected() {
        if (!effective) return
        if (storeFlow.value.enableBlockA11yAppList && !actualBlockA11yAppList.contains(topAppIdFlow.value)) {
            startQueryJob(byForced = true)
        }
    }

    fun onScreenForcedActive() {
        if (!effective) return
        // 关闭屏幕 -> Activity::onStop -> 点亮屏幕 -> Activity::onStart -> Activity::onResume
        A11yState.onScreenForcedActive()
        startQueryJob()
    }

    @Volatile
    private var lastWindowReadFailureAt = 0L

    @Volatile
    private var lastWindowReadFailureClass: String? = null

    private data class WindowRead(val root: AccessibilityNodeInfo?, val failure: Throwable?)

    // Workers return data only: abandoned reads must never write the engine cache.
    private fun readActiveWindow(): WindowRead = try {
        WindowRead(service.windowNodeInfo?.setGeneratedTime(), null)
    } catch (e: Exception) {
        WindowRead(null, e)
    }

    private fun acceptActiveWindow(read: WindowRead?, expectedActivity: ActivityRule): AccessibilityNodeInfo? {
        if (!effective || activityRuleFlow.value !== expectedActivity) {
            read?.root?.recycleUnused()
            return null
        }
        read?.failure?.let { failure ->
            val failureClass = failure.javaClass.name
            val now = ruleNow()
            if (failureClass != lastWindowReadFailureClass || now - lastWindowReadFailureAt > 60_000L) {
                lastWindowReadFailureClass = failureClass
                lastWindowReadFailureAt = now
                Log.d("A11yRuleEngine", "safeActiveWindow failed: $failureClass: "+failure.message)
            }
        }
        return read?.root.also { a11yContext.rootCache.value = it }
    }

    val safeActiveWindow: AccessibilityNodeInfo?
        get() {
            if (!effective) return null
            val expectedActivity = activityRuleFlow.value
            return acceptActiveWindow(A11yBlockingCalls.root.call(
                timeoutMillis = 500,
                onDiscard = { it.root?.recycleUnused() },
                block = ::readActiveWindow,
            ), expectedActivity)
        }

    private val scope get() = service.scope

    @Volatile
    private var latestStateEvent: A11yEvent? = null
    private var lastContentEventTime = 0L
    private var lastEventTime = 0L
    private val eventDeque = OwnedEventQueue(256, A11yEvent::sameAs, A11yEvent::close)
    private val queryEvents = OwnedEventQueue(256, A11yEvent::sameAs, A11yEvent::close)
    private val eventWakeup = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion {
            eventWakeup.close()
            eventDeque.close()
            queryEvents.close()
        }
        scope.launchLogged(eventDispatcher) {
            for (ignored in eventWakeup) {
                while (true) {
                    val batch = eventDeque.takePrefix()
                    if (batch.isEmpty()) break
                    try {
                        consumeEvent(batch)
                    } catch (e: CancellationException) {
                        currentCoroutineContext().ensureActive()
                    } catch (e: Exception) {
                        Log.e("A11yRuleEngine", "consumeEvent failed", e)
                    }
                }
            }
        }
    }
    fun onA11yEvent(event: AccessibilityEvent?) {
        if (!effective) return
        if (!event.isUseful()) return
        // 拒绝副屏无障碍事件
        if (AndroidTarget.TIRAMISU && event.displayId != Display.DEFAULT_DISPLAY) return
        onA11yFeatEvent(event)
        if (event.eventType == CONTENT_CHANGED) {
            if (!isInteractive) return // 屏幕关闭后仍然有无障碍事件 type:2048, time:8094, app:com.miui.aod, cls:android.widget.TextView
            if (event.packageName == systemUiAppId && event.packageName != currentTopActivity.appId) return
        }
        // 过滤部分输入法事件
        if (event.packageName == imeAppId && currentTopActivity.appId != imeAppId) {
            if (event.recordCount == 0 && event.action == 0 && !event.isFullScreen) return
        }
        // 直接丢弃自身事件，自行更新 topActivity
        if (
            (event.eventType == CONTENT_CHANGED || !MainActivityVisibility.isVisible) &&
            event.packageName == META.appId
        ) return

        val a11yEvent = event.toA11yEvent() ?: return
        if (a11yEvent.type == CONTENT_CHANGED) {
            // 防止 content 类型事件过快
            if (a11yEvent.time - lastContentEventTime < 100 && a11yEvent.time - appChangeTime > 5000 && a11yEvent.time - lastTriggerTime > 3000) {
                a11yEvent.close()
                return
            }
            lastContentEventTime = a11yEvent.time
        }
        EventService.logEvent(event)
        if (META.debuggable) {
            Log.d(
                "onNewA11yEvent",
                "type:${event.eventType}, time:${event.eventTime - lastEventTime}, app:${event.packageName}, cls:${event.className}"
            )
        }
        if (event.eventTime < lastEventTime) {
            // 某些应用会发送负时间事件, 直接丢弃
            // type:32, time:-104, app:com.miui.home, cls:com.miui.home.launcher.Launcher
            a11yEvent.close()
            return
        }
        lastEventTime = event.eventTime
        if (event.eventType == STATE_CHANGED) {
            latestStateEvent = a11yEvent
        }
        eventDeque.add(a11yEvent)
        eventWakeup.trySend(Unit)
    }

    private suspend fun consumeEvent(consumedEvents: List<A11yEvent>) {
        var transferred = false
        try {
            if (!effective) return
            val latestEvent = consumedEvents.last()
            val evAppId = latestEvent.appId
            val evActivityId = latestEvent.name
            val oldAppId = currentTopActivity.appId
            val rightAppId = if (oldAppId == evAppId) evAppId else getTimeoutAppId() ?: return
            if (!effective) return
            if (rightAppId == evAppId && latestEvent.type == STATE_CHANGED) {
                A11yState.withTopActivityLock {
                    if (isActivity(evAppId, evActivityId)) updateTopActivity(evAppId, evActivityId)
                }
            }
            if (rightAppId != currentTopActivity.appId) {
                val topCpn = getTimeoutTopCpn()
                if (!effective) return
                A11yState.withTopActivityLock {
                    if (topCpn?.packageName == rightAppId) {
                        updateTopActivity(topCpn.packageName, topCpn.className)
                    } else {
                        updateTopActivity(rightAppId, null)
                    }
                }
            }
            val activityRule = activityRuleFlow.value
            if (evAppId != rightAppId || activityRule.skipConsumeEvent || !storeFlow.value.enableMatch) return
            consumedEvents.forEach(queryEvents::add)
            transferred = true
            a11yContext.interruptKey++
            startQueryJob(byEvent = true)
        } finally {
            if (!transferred) consumedEvents.forEach(A11yEvent::close)
        }
    }

    private var lastGetAppIdTime = 0L
    private var lastAppId: String? = null
    private suspend fun getTimeoutAppId(): String? {
        if (lastAppId != null && ruleNow() - lastGetAppIdTime <= 100) return lastAppId
        val read = A11yBlockingCalls.root.run(
            timeoutMillis = 100,
            onDiscard = { it.root?.recycleUnused() },
            block = ::readActiveWindow,
        )
        val appId = try { read?.root?.packageName?.toString() } finally { read?.root?.recycleUnused() }
        lastAppId = appId ?: getTimeoutTopCpn()?.packageName
        lastGetAppIdTime = ruleNow()
        return lastAppId
    }

    private suspend fun getTimeoutTopCpn() = A11yBlockingCalls.foreground.run(100) {
        privilegeContextFlow.value?.topCpn()
    }

    private suspend fun getTimeoutActiveWindow(): AccessibilityNodeInfo? {
        val expectedActivity = activityRuleFlow.value
        return acceptActiveWindow(A11yBlockingCalls.root.run(
            timeoutMillis = 500,
            onDiscard = { it.root?.recycleUnused() },
            block = ::readActiveWindow,
        ), expectedActivity)
    }

    private val queryWakeup = QueryWakeup<QueryRequest<ResolvedRule>> { a, b -> a.merge(b) }

    private fun startQueryJob(
        byEvent: Boolean = false,
        byForced: Boolean = false,
        byDelayRule: ResolvedRule? = null,
    ) {
        if (!effective || !storeFlow.value.enableMatch) return
        if (activityRuleFlow.value.currentRules.isEmpty()) return
        if (!byEvent && service.justStarted && !hasOthersService) return checkFutureStartJob()
        val request = QueryRequest(byEvent, byForced, byDelayRule == null, setOfNotNull(byDelayRule))
        if (!queryWakeup.request(request)) return
        scope.launchLogged(queryDispatcher) {
            while (true) {
                val next = queryWakeup.takeOrFinish() ?: break
                try {
                    if (effective) queryAction(next.byEvent, next.byForced,
                        if (next.normal) null else next.delayRules)
                } catch (e: CancellationException) {
                    // Selector interruption is recoverable; service cancellation is not.
                    currentCoroutineContext().ensureActive()
                } catch (e: Exception) {
                    Log.e("A11yRuleEngine", "queryAction failed", e)
                }
            }
            checkFutureStartJob()
        }
    }

    private var futureStartJob: Job? = null
    @Synchronized
    private fun checkFutureStartJob() {
        if (!effective || futureStartJob?.isActive == true) return
        val t = ruleNow()
        if (t - lastTriggerTime < 3000L || t - appChangeTime < 3000L) {
            futureStartJob = scope.launch(actionDispatcher) {
                delay(300.milliseconds)
                synchronized(this@A11yRuleEngine) { futureStartJob = null }
                startQueryJob()
            }
        } else if (activityRuleFlow.value.hasFeatureAction) {
            futureStartJob = scope.launch(actionDispatcher) {
                delay(300.milliseconds)
                synchronized(this@A11yRuleEngine) { futureStartJob = null }
                startQueryJob(byForced = true)
            }
        }
    }

    private suspend fun fixAppId(rightAppId: String) {
        if (currentTopActivity.appId == rightAppId) return
        val topCpn = getTimeoutTopCpn()
        if (!effective) return
        A11yState.withTopActivityLock {
            if (topCpn?.packageName == rightAppId) {
                updateTopActivity(topCpn.packageName, topCpn.className)
            } else {
                updateTopActivity(rightAppId, null)
            }
        }
        scope.launch(actionDispatcher) {
            delay(300.milliseconds)
            startQueryJob()
        }
    }

    private suspend fun queryAction(
        byEvent: Boolean = false,
        byForced: Boolean = false,
        delayRules: Set<ResolvedRule>? = null,
    ) {
        val tempStateEvent = latestStateEvent
        val drainedEvents = if (delayRules != null) emptyList() else queryEvents.drain()
        try {
            val newEvents = if (drainedEvents.isNotEmpty() &&
                drainedEvents.all { it.sameAs(drainedEvents.first()) }) drainedEvents.takeLast(2) else emptyList()
            val activityRule = A11yState.currentRule
            activityRule.currentRules.forEach { rule ->
                if (rule.status == RuleStatus.Status3 && rule.matchDelayJob.value == null) {
                    rule.matchDelayJob.value = scope.launch(actionDispatcher) {
                        delay(rule.matchDelay.milliseconds)
                        rule.matchDelayJob.value = null
                        startQueryJob(byDelayRule = rule)
                    }
                }
            }
            if (activityRule.skipMatch) {
                // 如果当前应用没有规则/暂停匹配, 则不去调用获取事件节点避免阻塞
                return
            }
            var lastNode = if (newEvents.size <= 1) {
                newEvents.firstOrNull()?.readSource()
            } else {
                // Only use a source when the final two consecutive events agree.
                val previous = newEvents[0].readSource()
                val latest = try { newEvents[1].readSource() } catch (e: CancellationException) {
                    previous?.recycleUnused()
                    throw e
                }
                val sameNode = latest != null && latest == previous
                if (previous !== latest) previous?.recycleUnused()
                if (sameNode) latest else { latest?.recycleUnused(); null }
            }
            var lastNodeUsed = false
            if (!a11yContext.clearOldAppNodeCache()) {
                if (byEvent) { // 此为多数情况
                    // 新事件到来时, 若缓存清理不及时会导致无法查询到节点
                    a11yContext.clearNodeCache(lastNode)
                }
            }
            for (rule in activityRule.priorityRules) { // 规则数量有可能过多导致耗时过长
                if (!effective) return
                if (checkOutDate(activityRule, tempStateEvent)) break
                if (delayRules != null && rule !in delayRules) continue
                if (rule.status != RuleStatus.StatusOk) continue
                if (byForced && !rule.checkForced()) continue
                lastNode?.let { n ->
                    val refreshOk = (!lastNodeUsed) || (try {
                        val e = n.refresh()
                        if (e) {
                            n.setGeneratedTime()
                        }
                        e
                    } catch (_: Throwable) {
                        false
                    })
                    lastNodeUsed = true
                    if (!refreshOk) {
                        lastNode = null
                    }
                }
                val nodeVal = (lastNode ?: getTimeoutActiveWindow()) ?: continue
                val rightAppId = nodeVal.packageName?.toString() ?: break
                val matchApp = rule.matchActivity(rightAppId)
                if (currentTopActivity.appId != rightAppId || (!matchApp && rule is AppRule)) {
                    scope.launch(eventDispatcher) { fixAppId(rightAppId) }
                    return
                }
                if (!matchApp) continue
                val target = a11yContext.queryRule(rule, nodeVal) ?: continue
                if (rule.checkDelay() && rule.actionDelayJob.value == null) {
                    rule.actionDelayJob.value = scope.launch(actionDispatcher) {
                        delay(rule.actionDelay.milliseconds)
                        rule.actionDelayJob.value = null
                        startQueryJob(byDelayRule = rule)
                    }
                    continue
                }
                val topActivity = activityRule.topActivity
                val actionResult = performRuleAction(
                    rule = rule,
                    isCurrent = { !checkOutDate(activityRule, tempStateEvent) },
                ) { rule.performAction(target) } ?: break
                if (actionResult.result) {
                    rule.recordAction()
                    scope.launch(actionDispatcher) {
                        delay(300.milliseconds)
                        startQueryJob()
                    }
                    if (actionResult.action != ActionPerformer.None.action) {
                        showActionToast(rule)
                    }
                    addActionLog(rule, topActivity, target, actionResult)
                }
            }
        } finally {
            drainedEvents.forEach(A11yEvent::close)
        }
    }

    private fun checkOutDate(
        activityRule: ActivityRule,
        stateEvent: A11yEvent?
    ): Boolean {
        if (stateEvent !== latestStateEvent) return true
        return activityRule !== A11yState.currentRule
    }

}
