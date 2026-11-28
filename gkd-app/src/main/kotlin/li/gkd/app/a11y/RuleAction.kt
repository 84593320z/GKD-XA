package li.gkd.app.a11y

import li.gkd.app.data.ActionResult
import li.gkd.app.data.ResolvedRule
import li.gkd.app.data.RuleStatus

/** A settings snapshot catches pause/resume even when an asynchronous observer conflates it. */
internal class MatchGeneration(private val settings: Any, private val service: Any) {
    fun sameAs(other: MatchGeneration?): Boolean =
        other != null && settings === other.settings && service === other.service

    fun canSubmit(settings: Any, enabled: Boolean, service: Any?): Boolean =
        enabled && this.settings === settings && this.service === service
}

/** Capture the reset cycle before validation; only that cycle may consume this result. */
internal suspend fun performRuleAction(
    rule: ResolvedRule,
    isCurrent: () -> Boolean,
    now: () -> Long = ::ruleNow,
    canSubmit: () -> Boolean = { true },
    perform: suspend () -> ActionResult,
): ActionResult? {
    val attempt = rule.beginAction()
    if (rule.statusAt(now()) != RuleStatus.StatusOk || !isCurrent()) return null
    // Do not suspend or dispatch between this live check and entering the performer.
    if (!canSubmit()) return null
    val result = perform()
    if (result.result) rule.commitAction(attempt, now())
    return result
}
