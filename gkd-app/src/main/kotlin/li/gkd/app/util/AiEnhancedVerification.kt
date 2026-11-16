package li.gkd.app.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import li.gkd.app.a11y.ruleNow
import li.gkd.app.data.AppRule

/** One-shot verification. Never calls trigger(): temporary rules must not mutate runtime history. */
internal class AiEnhancedVerification<N>(
    private val getRoot: () -> N?,
    private val matchesContext: (N) -> Boolean,
    private val sameWindow: (N) -> Boolean,
    private val refresh: (N) -> Boolean,
    private val eligible: (AppRule) -> Boolean,
    private val query: (AppRule, N) -> N?,
    private val perform: suspend (AppRule, N) -> Boolean,
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val now: () -> Long = ::ruleNow,
) {
    enum class Result { Verified, Unmatched, ContextChanged }

    suspend fun run(rules: List<AppRule>, resolvedAt: Long): Result {
        for (rule in rules) {
            // Respect matchDelay before the first query. Excessive waits remain unverified;
            // the generated rule is still saved for manual use with its timing intact.
            val remainingDelay = rule.matchDelay - (now() - resolvedAt).coerceAtLeast(0)
            if (remainingDelay > 10_000) continue
            if (remainingDelay > 0) pause(remainingDelay)
            if (!eligible(rule)) continue
            var root = getRoot() ?: return Result.ContextChanged
            if (!matchesContext(root)) return Result.ContextChanged
            val candidate = query(rule, root) ?: continue
            if (rule.checkDelay()) {
                if (rule.actionDelay > 10_000) continue
                pause(rule.actionDelay)
            }
            // Refresh BEFORE re-running every positive/negative selector with a fresh query context.
            if (!refresh(candidate)) return Result.ContextChanged
            root = getRoot() ?: return Result.ContextChanged
            if (!refresh(root) || !matchesContext(root)) return Result.ContextChanged
            if (!eligible(rule)) continue
            val target = query(rule, root) ?: continue
            if (target != candidate) continue
            currentCoroutineContext().ensureActive()
            if (!sameWindow(target)) return Result.ContextChanged
            if (!perform(rule, target)) continue
            pause(1500)
            val after = getRoot() ?: return Result.ContextChanged
            if (!sameWindow(after)) return Result.ContextChanged
            return if (query(rule, after) == null) Result.Verified else Result.Unmatched
        }
        return Result.Unmatched
    }
}
