package li.gkd.app.a11y

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import li.gkd.app.data.ActionResult
import li.gkd.app.data.RuleStatus
import li.gkd.app.domain.rule.awaitSubmittedAction
import org.junit.Assert.*
import org.junit.Test

class RuleActionTest {
    private val hour = 3_600_000L

    @Test
    fun completionAfterResetCannotConsumeTheNewPagesQuotaCooldownOrDelay() = runBlocking {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1,actionCd:86400000,actionDelay:100}]").single()
        rule.resetState(hour)
        var now = hour + 100
        lateinit var complete: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            performRuleAction(rule, isCurrent = { true }, now = { now }) {
                ActionResult("clickCenter", awaitSubmittedAction(2_000) { complete = it; true })
            }
        }
        assertFalse(result.isCompleted)
        rule.resetState(++now)
        assertTrue(rule.checkDelayAt(now))
        complete(true)
        assertTrue(requireNotNull(result.await()).result) // The physical action remains loggable.
        assertEquals(RuleStatus.Status6, rule.statusAt(now))
        assertEquals(RuleStatus.StatusOk, rule.statusAt(now + 100))
    }

    @Test
    fun duplicateSuccessfulCallbacksCountOnlyOnce() = runBlocking {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:2,actionCd:0}]").single()
        lateinit var complete: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            performRuleAction(rule, isCurrent = { true }, now = { hour }) {
                ActionResult("clickCenter", awaitSubmittedAction(2_000) { complete = it; true })
            }
        }
        complete(true)
        complete(true)
        assertTrue(requireNotNull(result.await()).result)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        assertTrue(rule.commitAction(rule.beginAction(), hour))
        assertEquals(RuleStatus.Status1, rule.statusAt(hour))
    }

    @Test
    fun appAndContinuingMatchRulesKeepTheirCycleAcrossPageChangesWhileActionWaits() = runBlocking {
        for (policy in listOf("app", "match")) {
            val rule = resolvedTestRules("[{matches:'*',actionMaximum:1,resetMatch:'$policy'}]").single()
            rule.resetState(hour)
            var pageIsCurrent = true
            lateinit var complete: (Boolean) -> Unit
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                performRuleAction(rule, isCurrent = { pageIsCurrent }, now = { hour + 2 }) {
                    ActionResult("clickCenter", awaitSubmittedAction(2_000) { complete = it; true })
                }
            }
            pageIsCurrent = false
            rule.resetOnActivityChange(hour + 1, hour, wasMatching = true)
            complete(true)
            assertTrue(requireNotNull(result.await()).result)
            assertEquals(policy, RuleStatus.Status1, rule.statusAt(hour + 2))
        }
    }

    @Test
    fun sharedCooldownResetDoesNotEraseAnAppRulesUnchangedPrivateCount() = runBlocking {
        val (first, second) = resolvedTestRules("""[
          {key:1,matches:'*',actionCd:86400000},
          {key:2,matches:'*',actionMaximum:1,actionCdKey:1,resetMatch:'app'}
        ]""")
        lateinit var complete: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            performRuleAction(second, isCurrent = { true }, now = { hour }) {
                ActionResult("clickCenter", awaitSubmittedAction(2_000) { complete = it; true })
            }
        }
        first.resetState(hour)
        complete(true)
        assertTrue(requireNotNull(result.await()).result)
        assertEquals(RuleStatus.StatusOk, first.statusAt(hour)) // The reset shared cooldown stays clear.
        assertEquals(RuleStatus.Status1, second.statusAt(hour)) // The unchanged App quota counts the action.
    }

    @Test
    fun rejectedCancelledAndTimedOutGesturesDoNotChangeTheRealRule() = runBlocking {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1,actionCd:86400000}]").single()
        suspend fun action(submit: ((Boolean) -> Unit) -> Boolean): ActionResult? =
            performRuleAction(rule, isCurrent = { true }, now = { hour }) {
                ActionResult("clickCenter", awaitSubmittedAction(20, submit))
            }
        assertFalse(requireNotNull(action { it(true); false }).result)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        assertFalse(requireNotNull(action { it(false); true }).result)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        lateinit var lateCompletion: (Boolean) -> Unit
        assertFalse(requireNotNull(action { lateCompletion = it; true }).result)
        lateCompletion(true)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
    }

    @Test
    fun callerCancellationDoesNotCommitALateCompletion() = runBlocking {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1,actionCd:86400000}]").single()
        lateinit var complete: (Boolean) -> Unit
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            performRuleAction(rule, isCurrent = { true }, now = { hour }) {
                ActionResult("clickCenter", awaitSubmittedAction(2_000) { complete = it; true })
            }
        }
        result.cancelAndJoin()
        complete(true)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
    }

    @Test
    fun actionRechecksMatchingAndTheEffectiveServiceAfterPageValidation() = runBlocking {
        for (pause in listOf(true, false)) {
            val rule = resolvedTestRules("[{matches:'*',actionMaximum:1}]").single()
            val settings = Any()
            val lease = Any()
            val generation = MatchGeneration(settings, lease)
            var enabled = true
            var effectiveLease: Any? = lease
            var submitted = false
            val result = performRuleAction(
                rule,
                isCurrent = {
                    if (pause) enabled = false else effectiveLease = null
                    true
                },
                now = { hour },
                canSubmit = { generation.canSubmit(settings, enabled, effectiveLease) },
            ) {
                submitted = true
                ActionResult("clickCenter", true)
            }
            assertNull(result)
            assertFalse(submitted)
            assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        }
    }

    @Test
    fun queuedAndDelayedWorkCannotSubmitAfterPauseAndResume() = runBlocking {
        data class Settings(val enableMatch: Boolean)
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1}]").single()
        val lease = Any()
        val original = Settings(true)
        var settings = original
        val queued = MatchGeneration(settings, lease)
        val delayed = MatchGeneration(settings, lease)
        val releaseDelay = CompletableDeferred<Unit>()
        var submissions = 0
        suspend fun submit(generation: MatchGeneration) = performRuleAction(
            rule,
            isCurrent = { true },
            now = { hour },
            canSubmit = { generation.canSubmit(settings, settings.enableMatch, lease) },
        ) {
            submissions++
            ActionResult("clickCenter", true)
        }
        val delayedResult = async(start = CoroutineStart.UNDISPATCHED) {
            releaseDelay.await()
            submit(delayed)
        }

        settings = Settings(false)
        assertNull(submit(queued))
        settings = settings.copy(enableMatch = true)
        assertEquals(original, settings)
        assertNull(submit(queued))
        releaseDelay.complete(Unit)
        assertNull(delayedResult.await())
        assertEquals(0, submissions)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
        assertTrue(requireNotNull(submit(MatchGeneration(settings, lease))).result)
        assertEquals(1, submissions)
    }

    @Test
    fun obsoletePageIsRejectedBeforeSubmission() = runBlocking {
        val rule = resolvedTestRules("[{matches:'*',actionMaximum:1}]").single()
        var calls = 0
        val result = performRuleAction(rule, isCurrent = { false }, now = { hour }) {
            calls++
            ActionResult("clickCenter", true)
        }
        assertNull(result)
        assertEquals(0, calls)
        assertEquals(RuleStatus.StatusOk, rule.statusAt(hour))
    }
}
