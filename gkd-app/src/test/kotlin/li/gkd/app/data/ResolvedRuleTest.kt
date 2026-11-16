package li.gkd.app.data

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import li.gkd.app.a11y.resolvedTestRules
import org.junit.Assert.*
import org.junit.Test

class ResolvedRuleTest {
    private val hour = 3_600_000L
    private val day = 86_400_000L

    @Test
    fun neverTriggeredAndResetRulesAreNotCoolingDownWithALongCooldown() {
        val rule = resolvedTestRules("[{matches:'*',actionCd:86400000}]").single()
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        rule.resetState(hour)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        assertTrue(rule.commitAction(rule.beginAction(), hour))
        assertEquals(RuleStatus.Status5, rule.statusAt(hour + day - 1))
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour + day))
        rule.resetState(hour + day + 1)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour + day + 1))
    }

    @Test
    fun longCooldownDoesNotStealTheShortMatchWindowAndZeroIsAValidTriggerTime() {
        val short = resolvedTestRules("[{matches:'*',actionCd:86400000,matchTime:10000}]").single()
        short.resetState(hour)
        assertEquals(RuleStatus.StatusOk, short.statusAt(hour))
        assertEquals(RuleStatus.StatusOk, short.statusAt(hour + 10_000))
        assertEquals(RuleStatus.Status4, short.statusAt(hour + 10_001))
        val zero = resolvedTestRules("[{matches:'*',actionCd:86400000}]").single()
        assertTrue(zero.commitAction(zero.beginAction(), 0))
        assertEquals(RuleStatus.Status5, zero.statusAt(0))
        assertEquals(RuleStatus.Status5, zero.statusAt(day - 1))
        assertEquals(RuleStatus.StatusOk, zero.statusAt(day))
        zero.resetState(0)
        assertEquals(RuleStatus.StatusOk, zero.statusAt(0))
    }

    @Test
    fun cooldownKeysShareTriggersAndResetsInBothDirections() {
        val (first, second) = resolvedTestRules("""[
          {key:1,matches:'*',actionCd:86400000},
          {key:2,matches:'*',actionCdKey:1}
        ]""")
        assertEquals(RuleStatus.StatusOk, second.statusAt(hour))
        assertTrue(first.commitAction(first.beginAction(), hour))
        assertEquals(RuleStatus.Status5, second.statusAt(hour))
        second.resetState(hour + 1)
        assertEquals(RuleStatus.StatusOk, first.statusAt(hour + 1))
        assertTrue(second.commitAction(second.beginAction(), hour + 2))
        assertEquals(RuleStatus.Status5, first.statusAt(hour + 2))
        first.resetState(hour + 3)
        assertEquals(RuleStatus.StatusOk, second.statusAt(hour + 3))
    }

    @Test
    fun resetInvalidatesOldAttemptsWithoutClearingTheNewActionDelay() {
        val rule = resolvedTestRules("[{matches:'*',actionCd:86400000,actionMaximum:1,actionDelay:100}]").single()
        rule.resetState(hour)
        assertTrue(rule.checkDelayAt(hour))
        val attempt = rule.beginAction()
        rule.resetState(hour + 200)
        assertTrue(rule.checkDelayAt(hour + 200))
        assertFalse(rule.commitAction(attempt, hour + 250))
        assertEquals(RuleStatus.Status6, rule.statusAt(hour + 250))
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour + 300))
    }

    @Test
    fun oneAttemptCountsOnlyOnceAndDoesNotRepopulateResetSharedCells() {
        val (first, second) = resolvedTestRules("""[
          {key:1,matches:'*',actionMaximum:2,actionCd:0},
          {key:2,matches:'*',actionMaximumKey:1,actionCdKey:1}
        ]""")
        val once = second.beginAction()
        assertTrue(second.commitAction(once, hour))
        assertFalse(second.commitAction(once, hour))
        assertEquals(RuleStatus.StatusOk, first.statusAt(hour))
        val beforeReset = second.beginAction()
        first.resetState(hour + 1)
        assertTrue(second.commitAction(beforeReset, hour + 2))
        assertEquals(RuleStatus.StatusOk, second.statusAt(hour + 2))
        assertTrue(second.commitAction(second.beginAction(), hour + 3))
        assertEquals(RuleStatus.StatusOk, first.statusAt(hour + 3))
        assertTrue(first.commitAction(first.beginAction(), hour + 4))
        assertEquals(RuleStatus.Status1, second.statusAt(hour + 4))
    }

    @Test
    fun activityResetPolicyPreservesAppAndContinuingMatchCycles() {
        val (activity, app, match) = resolvedTestRules("""[
          {matches:'*',actionMaximum:1},
          {matches:'*',actionMaximum:1,resetMatch:'app'},
          {matches:'*',actionMaximum:1,resetMatch:'match'}
        ]""")
        listOf(activity, app, match).forEach { it.resetState(hour) }
        val attempts = listOf(activity, app, match).map { it.beginAction() }
        listOf(activity, app, match).forEach {
            it.resetOnActivityChange(hour + 1, hour, wasMatching = true)
        }
        assertFalse(activity.commitAction(attempts[0], hour + 2))
        assertTrue(app.commitAction(attempts[1], hour + 2))
        assertTrue(match.commitAction(attempts[2], hour + 2))
        assertEquals(RuleStatus.StatusOk, activity.statusAt(hour + 2))
        assertEquals(RuleStatus.Status1, app.statusAt(hour + 2))
        assertEquals(RuleStatus.Status1, match.statusAt(hour + 2))
        app.resetOnActivityChange(hour + 3, hour + 3, wasMatching = true)
        match.resetOnActivityChange(hour + 3, hour, wasMatching = false)
        assertEquals(RuleStatus.StatusOk, app.statusAt(hour + 3))
        assertEquals(RuleStatus.StatusOk, match.statusAt(hour + 3))
    }

    @Test
    fun resetAndCommitRaceCannotLeaveAnyOldQuotaOrCooldown() {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1,actionCd:86400000}]").single()
        val workers = Executors.newFixedThreadPool(2)
        try {
            repeat(200) {
                rule.resetState(hour)
                val attempt = rule.beginAction()
                val start = CountDownLatch(1)
                val reset = workers.submit { start.await(); rule.resetState(hour + 1) }
                val commit = workers.submit { start.await(); rule.commitAction(attempt, hour + 2) }
                start.countDown()
                reset.get(2, TimeUnit.SECONDS)
                commit.get(2, TimeUnit.SECONDS)
                assertEquals(RuleStatus.StatusOk, rule.statusAt(hour + 3))
            }
        } finally {
            workers.shutdownNow()
        }
    }
}
