package li.gkd.app.data

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.Job
import li.gkd.app.text.UiStrings
import li.gkd.app.a11y.appChangeTime
import li.gkd.app.a11y.lastTriggerRule
import li.gkd.app.a11y.lastTriggerTime
import li.gkd.app.a11y.ruleNow
import li.gkd.app.store.AppStore
import li.gkd.selector.MatchOptions
import li.gkd.selector.Selector

sealed class ResolvedRule(
    val rule: RawSubscription.RawRuleProps,
    val g: ResolvedGroup,
) {
    companion object {
        // Cooldown/count cells can be shared by different rules (including scopeKeys).
        // One lock makes their resets and conditional commits a single transaction.
        private val stateLock = Any()
    }

    private class ResettableValue<T>(var value: T) {
        var epoch: Any = Any()
        fun reset(value: T) {
            this.value = value
            epoch = Any()
        }
    }

    internal class ActionAttempt internal constructor(
        internal val rule: ResolvedRule,
        internal val epoch: Any,
        internal val cooldownEpoch: Any,
        internal val countEpoch: Any,
    ) {
        internal var completed = false
    }

    private var resetEpoch: Any = Any()
    private val group = g.group
    val subsItem = g.subsItem
    val rawSubs = g.subscription
    val key = rule.key
    val index = group.rules.indexOfFirst { r -> r === rule }
    val excludeData = g.excludeData
    private val preKeys = (rule.preKeys ?: emptyList()).toSet()
    val matches =
        (rule.matches ?: emptyList()).map { s -> group.cacheMap[s] ?: Selector.compile(s).value }
    val anyMatches =
        (rule.anyMatches ?: emptyList()).map { s -> group.cacheMap[s] ?: Selector.compile(s).value }
    val excludeMatches =
        (rule.excludeMatches ?: emptyList()).map { s ->
            group.cacheMap[s] ?: Selector.compile(s).value
        }
    val excludeAllMatches =
        (rule.excludeAllMatches ?: emptyList()).map { s ->
            group.cacheMap[s] ?: Selector.compile(s).value
        }

    private val resetMatch = rule.resetMatch ?: group.resetMatch
    val matchDelay = rule.matchDelay ?: group.matchDelay ?: 0L
    val actionDelay = rule.actionDelay ?: group.actionDelay ?: 0L
    private val matchTime = rule.matchTime ?: group.matchTime
    private val forcedTime = rule.forcedTime ?: group.forcedTime ?: 0L
    val matchOptions = MatchOptions(
        fastQuery = rule.fastQuery ?: group.fastQuery ?: false
    )
    val matchRoot = rule.matchRoot ?: group.matchRoot ?: false
    val order = rule.order ?: group.order ?: 0

    private val actionCdKey = rule.actionCdKey ?: group.actionCdKey
    private val actionCd = rule.actionCd ?: if (actionCdKey != null) {
        group.rules.find { r -> r.key == actionCdKey }?.actionCd
    } else {
        null
    } ?: group.actionCd ?: 1000L

    private val actionMaximumKey = rule.actionMaximumKey ?: group.actionMaximumKey
    private val actionMaximum = rule.actionMaximum ?: if (actionMaximumKey != null) {
        group.rules.find { r -> r.key == actionMaximumKey }?.actionMaximum
    } else {
        null
    } ?: group.actionMaximum

    val priorityTime = rule.priorityTime ?: group.priorityTime ?: 0
    val priorityActionMaximum = rule.priorityActionMaximum ?: group.priorityActionMaximum ?: 1
    val priorityEnabled: Boolean
        get() = priorityTime > 0

    fun isPriority(): Boolean = synchronized(stateLock) {
        if (!priorityEnabled) return false
        if (priorityActionMaximum <= actionCount.value) return false
        if (!status.ok) return false
        val t = ruleNow()
        t - matchChangedTime.value < priorityTime + matchDelay
    }

    fun bindGroupRules(
        groupToRules: Map<out RawSubscription.RawGroupProps, List<ResolvedRule>>,
    ): Unit = synchronized(stateLock) {
        val selfGroupRules = groupToRules[group] ?: emptyList()
        val othersGroupRules =
            (group.scopeKeys ?: emptyList()).distinct().filter { k -> k != group.key }
                .flatMap { k ->
                    groupToRules.entries.find { e -> e.key.key == k }?.value ?: emptyList()
                }
        val groupRules = selfGroupRules + othersGroupRules

        // 共享次数
        if (actionMaximumKey != null) {
            val otherRule = groupRules.find { r -> r.key == actionMaximumKey }
            if (otherRule != null) {
                actionCount = otherRule.actionCount
            }
        }
        // 共享 cd
        if (actionCdKey != null) {
            val otherRule = groupRules.find { r -> r.key == actionCdKey }
            if (otherRule != null) {
                actionTriggerTime = otherRule.actionTriggerTime
            }
        }
        preRules = groupRules.filter { otherRule ->
            (otherRule.key != null) && preKeys.contains(
                otherRule.key
            )
        }.toSet()
    }

    private var preRules = emptySet<ResolvedRule>()

    private val actionDelayTriggerTime = atomic(-1L)
    val actionDelayJob = atomic<Job?>(null)
    fun checkDelay(): Boolean = checkDelayAt(ruleNow())

    internal fun checkDelayAt(t: Long): Boolean = synchronized(stateLock) {
        if (actionDelay > 0 && actionDelayTriggerTime.value < 0L) {
            actionDelayTriggerTime.value = t
            true
        } else {
            false
        }
    }

    fun checkForced(): Boolean {
        if (forcedTime <= 0) return false
        return ruleNow() < matchChangedTime.value + matchDelay + forcedTime
    }

    private var actionTriggerTime = ResettableValue<Long?>(null)
    private var actionCount = ResettableValue(0)

    internal fun beginAction(): ActionAttempt = synchronized(stateLock) {
        ActionAttempt(this, resetEpoch, actionTriggerTime.epoch, actionCount.epoch)
    }

    /** Reset and every conditional write share one lock, including shared count/cooldown cells. */
    internal fun commitAction(attempt: ActionAttempt, t: Long): Boolean = synchronized(stateLock) {
        if (attempt.rule !== this || attempt.completed) return false
        attempt.completed = true
        if (attempt.epoch !== resetEpoch) return false
        // A peer may reset a shared cell while this App/Match rule keeps its own cycle.
        // Preserve the unaffected cells without writing the peer's newly reset state.
        if (attempt.cooldownEpoch === actionTriggerTime.epoch) actionTriggerTime.value = t
        if (attempt.countEpoch === actionCount.epoch) actionCount.value++
        actionDelayTriggerTime.value = -1L
        true
    }

    // A completed physical action still belongs in the original page's log/total,
    // even when a reset prevented its result from consuming the new cycle's quota.
    internal fun recordAction() {
        lastTriggerTime = ruleNow()
        lastTriggerRule = this
        AppStore.incrementActionCount()
    }

    private val matchChangedTime = atomic(0L)
    val isFirstMatchApp: Boolean
        get() = matchChangedTime.value < appChangeTime

    private val matchLimitTime = (matchTime ?: 0) + matchDelay

    val resetMatchType = ResetMatchType.allSubObject.find {
        it.value == resetMatch
    } ?: ResetMatchType.Activity

    fun resetState(t: Long): Unit = synchronized(stateLock) {
        resetEpoch = Any()
        actionCount.reset(0)
        actionDelayTriggerTime.value = -1L
        actionTriggerTime.reset(null)
        actionDelayJob.update { it?.cancel(); null }
        matchDelayJob.update { it?.cancel(); null }
        matchChangedTime.value = t
    }

    internal fun resetOnActivityChange(t: Long, appChangedAt: Long, wasMatching: Boolean): Unit =
        synchronized(stateLock) {
            when (resetMatchType) {
                ResetMatchType.App -> if (matchChangedTime.value < appChangedAt) resetState(t)
                ResetMatchType.Activity -> resetState(t)
                ResetMatchType.Match -> if (!wasMatching) resetState(t)
            }
            Unit
        }

    private val performer = ActionPerformer.getAction(rule.action ?: rule.position?.let {
        ActionPerformer.ClickCenter.action
    } ?: rule.swipeArg?.let {
        ActionPerformer.Swipe.action
    })

    suspend fun performAction(node: AccessibilityNodeInfo) = performer.perform(node, rule)

    val matchDelayJob = atomic<Job?>(null)

    val status: RuleStatus
        get() = statusAt(ruleNow())

    internal fun statusAt(t: Long): RuleStatus = synchronized(stateLock) {
        if (actionMaximum != null && actionCount.value >= actionMaximum) {
            return RuleStatus.Status1 // 达到最大执行次数
        }
        if (preRules.isNotEmpty() && !preRules.any { it === lastTriggerRule }) {
            return RuleStatus.Status2 // 需要提前触发某个规则
        }
        val c = matchChangedTime.value
        if (matchDelay > 0 && t - c < matchDelay) {
            return RuleStatus.Status3 // 处于匹配延迟中
        }
        if (matchTime != null && t - c > matchLimitTime) {
            return RuleStatus.Status4 // 超出匹配时间
        }
        val lastAction = actionTriggerTime.value
        if (lastAction != null && t - lastAction < actionCd) {
            return RuleStatus.Status5 // 处于冷却时间
        }
        val d = actionDelayTriggerTime.value
        if (d >= 0 && t - d < actionDelay) {
            return RuleStatus.Status6 // 处于触发延迟中
        }
        RuleStatus.StatusOk
    }

    fun statusText(): String {
        return "id:${subsItem.id}, v:${rawSubs.version}, type:${type}, gKey=${group.key}, gName:${group.name}, index:${index}, key:${key}, status:${status.name}"
    }

    abstract val type: String

    // 范围越精确, 优先级越高
    abstract fun matchActivity(appId: String, activityId: String? = null): Boolean
}

sealed class ResetMatchType(val value: String) {
    data object Activity : ResetMatchType("activity")
    data object Match : ResetMatchType("match")
    data object App : ResetMatchType("app")

    companion object {
        val allSubObject by lazy { listOf(Activity, Match, App) }
    }
}

sealed class RuleStatus(val name: String) {
    data object StatusOk : RuleStatus("ok")
    data object Status1 : RuleStatus(UiStrings.rule_status_maximum_actions)
    data object Status2 : RuleStatus(UiStrings.rule_status_prerequisite)
    data object Status3 : RuleStatus(UiStrings.rule_status_match_delay)
    data object Status4 : RuleStatus(UiStrings.rule_status_match_timeout)
    data object Status5 : RuleStatus(UiStrings.rule_status_cooldown)
    data object Status6 : RuleStatus(UiStrings.rule_status_action_delay)

    val ok: Boolean
        get() = this === StatusOk

    val alive: Boolean
        get() = this !== Status1 && this !== Status2 && this !== Status4
}
