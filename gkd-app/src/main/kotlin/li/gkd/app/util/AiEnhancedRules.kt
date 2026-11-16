package li.gkd.app.util

import li.gkd.app.data.AppInfo
import li.gkd.app.data.AppRule
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.ResolvedAppGroup
import li.gkd.db.SubsItem

internal object AiEnhancedRules {
    /** Preserve the formal engine's selectors, scopes, prerequisites, actions and timing. */
    fun resolve(subscription: RawSubscription, appInfo: AppInfo?, now: Long): List<AppRule> {
        val app = subscription.apps.single()
        val groupRules = app.groups.associateWith { group ->
            val resolved = ResolvedAppGroup(group, subscription, SubsItem(id = subscription.id, order = 0), null, app, group.enable != false)
            group.rules.map { AppRule(it, resolved, appInfo) }
        }
        val rules = groupRules.values.flatten()
        rules.forEach { it.resetState(now); it.bindGroupRules(groupRules) }
        return rules.filter { it.group.enable != false }.sortedBy { it.order }
    }

    fun eligible(rule: AppRule, appId: String, activityId: String?): Boolean {
        val raw = rule.rule as RawSubscription.RawAppRule
        // The runtime permits unknown activities; automatic AI verification must fail closed.
        if (activityId == null && listOf(
                raw.activityIds ?: rule.group.activityIds,
                raw.excludeActivityIds ?: rule.group.excludeActivityIds,
            ).any { !it.isNullOrEmpty() }) return false
        return rule.status.ok && rule.matchActivity(appId, activityId)
    }
}
