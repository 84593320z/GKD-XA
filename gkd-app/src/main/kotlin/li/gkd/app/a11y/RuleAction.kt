package li.gkd.app.a11y

import li.gkd.app.data.ActionResult
import li.gkd.app.data.ResolvedRule
import li.gkd.app.data.RuleStatus

/** Capture the reset cycle before validation; only that cycle may consume this result. */
internal suspend fun performRuleAction(
    rule: ResolvedRule,
    isCurrent: () -> Boolean,
    now: () -> Long = ::ruleNow,
    perform: suspend () -> ActionResult,
): ActionResult? {
    val attempt = rule.beginAction()
    if (rule.statusAt(now()) != RuleStatus.StatusOk || !isCurrent()) return null
    val result = perform()
    if (result.result) rule.commitAction(attempt, now())
    return result
}
