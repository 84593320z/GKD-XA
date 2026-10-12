package li.gkd.app.data

import li.gkd.app.a11y.launcherAppId
import li.gkd.app.data.appinfo.AppInfoRepository
import li.gkd.app.domain.rule.RuleScopePolicy

class GlobalRule(
    private val rawRule: RawSubscription.RawGlobalRule,
    g: ResolvedGlobalGroup,
    private val appInfoCache: Map<String, AppInfo>,
) : ResolvedRule(
    rule = rawRule,
    g = g,
) {
    val groupExcludeAppIds = g.groupExcludeAppIds
    val group = g.group
    // 对齐上游 v1.13.0-beta-1：应用启用范围只看 group 级字段，子规则 apps 只覆盖页面/版本限制。
    private val matchAnyApp = group.matchAnyApp ?: true
    private val matchLauncher = group.matchLauncher ?: false
    private val matchSystemApp = group.matchSystemApp ?: false
    private val apps = (group.apps.orEmpty().map { it.id } + rawRule.apps.orEmpty().map { it.id })
        .distinct()
        // Only installed apps can supply runtime activity events (issue #619).
        .filter { appInfoCache.isEmpty() || it in appInfoCache }
        .mapNotNull { id ->
            RuleScopePolicy.globalApp(group, rawRule, id)
                ?.let { id to RuleScopePolicy.globalScope(it, appInfoCache[id]) }
        }.toMap()

    override val type = "global"

    override fun matchActivity(appId: String, activityId: String?): Boolean = RuleScopePolicy.matchGlobalActivity(
        apps[appId], matchAnyApp && (matchLauncher || appId != launcherAppId) &&
            (matchSystemApp || appId !in AppInfoRepository.systemAppsFlow.value),
        appId, activityId, appId in groupExcludeAppIds, excludeData,
    )
}
