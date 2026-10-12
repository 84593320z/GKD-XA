package li.gkd.app.domain.rule

import li.gkd.app.data.AppInfo
import li.gkd.app.data.ExcludeData
import li.gkd.app.data.RawSubscription

data class GlobalAppScope(val enabled: Boolean, val included: List<String>, val excluded: List<String>)

/** Pure applicability rules shared by execution and the settings UI. */
object RuleScopePolicy {
    fun fixActivities(appId: String, values: List<String>?): List<String> =
        values.orEmpty().map { if (it.startsWith('.')) appId + it else it }

    fun versionMatches(props: RawSubscription.RawAppRuleProps, info: AppInfo?): Boolean =
        info == null || (props.versionCode?.match(info.versionCode) != false &&
            props.versionName?.match(info.versionName) != false)

    fun globalAppEnabled(app: RawSubscription.RawGlobalApp, info: AppInfo?): Boolean =
        app.enable ?: versionMatches(app, info)

    fun globalScope(app: RawSubscription.RawGlobalApp, info: AppInfo?): GlobalAppScope = GlobalAppScope(
        globalAppEnabled(app, info), fixActivities(app.id, app.activityIds), fixActivities(app.id, app.excludeActivityIds),
    )

    fun appVersionMatches(group: RawSubscription.RawAppGroup, rule: RawSubscription.RawAppRule, info: AppInfo?): Boolean =
        info == null || ((rule.versionCode ?: group.versionCode)?.match(info.versionCode) != false &&
            (rule.versionName ?: group.versionName)?.match(info.versionName) != false)

    fun globalApp(
        group: RawSubscription.RawGlobalGroup,
        rule: RawSubscription.RawGlobalRule?,
        appId: String,
    ): RawSubscription.RawGlobalApp? {
        // 对齐上游 v1.13.0-beta-1：子规则的 apps 不再整条替换父项，而是按字段合并 ——
        // enable（应用启用范围）只认 group 级，activityIds / 版本限制才允许子规则覆盖。
        // 旧的 `rule?.apps ?: group.apps` 会让子规则里写的 enable 直接决定某个应用开不开，
        // 同一份社区订阅在本应用和上游 GKD 上会匹配到不同的应用范围。
        val parent = group.apps.orEmpty().lastOrNull { it.id == appId }
        val child = rule?.apps.orEmpty().lastOrNull { it.id == appId }
        if (parent == null && child == null) return null
        return RawSubscription.RawGlobalApp(
            id = appId,
            enable = parent?.enable,
            activityIds = child?.activityIds ?: parent?.activityIds,
            excludeActivityIds = child?.excludeActivityIds ?: parent?.excludeActivityIds,
            versionCode = child?.versionCode ?: parent?.versionCode,
            versionName = child?.versionName ?: parent?.versionName,
        )
    }

    fun globalDefault(
        group: RawSubscription.RawGlobalGroup,
        rule: RawSubscription.RawGlobalRule?,
        appId: String,
        launcherAppId: String,
        systemAppIds: Set<String>,
        matchAnyApp: Boolean? = null,
    ): Boolean {
        // 同上：是否在应用范围内只看 group 级条目与 group 级三个字段。
        if (group.apps.orEmpty().any { it.id == appId }) return true
        if (appId == launcherAppId && group.matchLauncher != true) return false
        if (appId in systemAppIds && group.matchSystemApp != true) return false
        if (matchAnyApp == false) return false
        return !(matchAnyApp == null && group.matchAnyApp == false)
    }

    fun globalRuleAllowed(
        group: RawSubscription.RawGlobalGroup,
        rule: RawSubscription.RawGlobalRule?,
        appId: String,
        info: AppInfo?,
        groupExcluded: Boolean,
    ): Boolean = !groupExcluded && globalApp(group, rule, appId)?.let { globalAppEnabled(it, info) } != false

    fun matchGlobalActivity(
        app: GlobalAppScope?,
        defaultEnabled: Boolean,
        appId: String,
        activityId: String?,
        groupExcluded: Boolean,
        exclude: ExcludeData,
    ): Boolean {
        if (groupExcluded || app?.enabled == false) return false
        if (exclude.appIds[appId] == true) return false
        // Personal global page entries have always been exact activity IDs.
        if (activityId != null && appId to activityId in exclude.activityIds) return false
        if (activityId != null && app?.excluded.orEmpty().any(activityId::startsWith)) return false
        if (exclude.appIds[appId] == false) return true
        if (app != null) {
            return activityId == null || app.included.isEmpty() || app.included.any(activityId::startsWith)
        }
        return defaultEnabled
    }
}
