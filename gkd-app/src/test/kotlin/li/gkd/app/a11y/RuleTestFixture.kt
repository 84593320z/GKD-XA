package li.gkd.app.a11y

import li.gkd.app.data.AppRule
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.ResolvedAppGroup
import li.gkd.db.SubsItem

internal fun resolvedTestRules(rules: String): List<AppRule> {
    val subscription = RawSubscription.parse("""{
      id:1,name:'Runtime regression',version:1,
      apps:[{id:'test.app',groups:[{key:1,name:'Runtime',rules:$rules}]}]
    }""")
    val app = subscription.apps.single()
    val rawGroup = app.groups.single()
    val group = ResolvedAppGroup(rawGroup, subscription,
        SubsItem(id = 1, enable = true, order = 0), null, app, true)
    val resolved = rawGroup.rules.map { AppRule(it, group, null) }
    val groups = mapOf(rawGroup to resolved)
    resolved.forEach { it.bindGroupRules(groups) }
    return resolved
}
