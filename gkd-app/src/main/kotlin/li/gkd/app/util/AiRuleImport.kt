package li.gkd.app.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import li.gkd.app.data.ActionPerformer
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.SubscriptionInputParser
import li.gkd.app.data.edit
import li.songe.json5.Json5

/** Bind untrusted generated rules to the snapshot before either executing or importing them. */
internal object AiRuleImport {
    fun parse(text: String, expectedAppId: String): RawSubscription {
        require(expectedAppId.isNotBlank()) { "Missing snapshot application" }
        val root = Json5.parseToJsonElement(extract(text)) as? JsonObject ?: error("Expected rule object")
        require(!root.containsKey("globalGroups") || (root["globalGroups"] as? JsonArray)?.isEmpty() == true) { "Global AI rules are not supported" }
        val appObject = if (root.containsKey("apps")) {
            val apps = root["apps"] as? JsonArray ?: error("Expected application array")
            require(apps.size == 1) { "Expected exactly one application" }
            apps.single() as? JsonObject ?: error("Expected application object")
        } else root
        val id = appObject["id"] as? JsonPrimitive
        require(id?.isString == true && id.content == expectedAppId) { "AI application does not match snapshot" }
        val rawGroups = appObject["groups"] as? JsonArray ?: error("Expected rule groups")
        rawGroups.forEach { raw ->
            val group = raw as? JsonObject ?: error("Expected rule group object")
            val rules = group["rules"] as? JsonArray
            val keys = rules.orEmpty().mapNotNull { rawRule ->
                val key = (rawRule as? JsonObject)?.get("key")
                if (key == null || key == kotlinx.serialization.json.JsonNull) null
                else (key as? JsonPrimitive)?.intOrNull ?: error("Invalid AI rule key")
            }
            require(keys.distinct().size == keys.size) { "Duplicate AI rule keys" }
        }
        val app = SubscriptionInputParser.parse(appObject.toString()).parseApp()
        require(app.groups.isNotEmpty() && app.groups.all { it.rules.isNotEmpty() }) { "AI rules are empty" }
        require(app.groups.size == (appObject["groups"] as? JsonArray)?.size) { "Duplicate AI group keys" }
        require(app.groups.map { it.key }.distinct().size == app.groups.size) { "Duplicate AI group keys" }
        require(app.groups.map { it.name }.distinct().size == app.groups.size) { "Duplicate AI group names" }
        val groupKeys = app.groups.map { it.key }.toSet()
        app.groups.forEach { group ->
            require(group.scopeKeys.orEmpty().all { it in groupKeys }) { "Unresolved AI group reference" }
            val scopedGroups = app.groups.filter { it.key == group.key || it.key in group.scopeKeys.orEmpty() }
            val ruleKeys = scopedGroups.flatMap { it.rules }.mapNotNull { it.key }.toSet()
            group.rules.forEach { rule ->
                require(!rule.matches.isNullOrEmpty() || !rule.anyMatches.isNullOrEmpty()) { "AI rule has no positive selector" }
                require(rule.action == null || ActionPerformer.getAction(rule.action).action == rule.action) { "Unknown AI action" }
                require(rule.preKeys.orEmpty().all { it in ruleKeys }) { "Unresolved AI prerequisite" }
                listOfNotNull(rule.actionCdKey ?: group.actionCdKey, rule.actionMaximumKey ?: group.actionMaximumKey)
                    .forEach { require(it in ruleKeys) { "Unresolved AI shared rule reference" } }
            }
        }
        return RawSubscription(id = -1L, name = "AI Generated", version = 1, apps = listOf(app))
    }

    /** AI output is a closed batch: its scope keys never refer to pre-existing local groups. */
    fun merge(current: RawSubscription, generated: RawSubscription): RawSubscription {
        val app = generated.apps.single()
        val usedKeys = current.apps.find { it.id == app.id }?.groups
            .orEmpty().mapTo(mutableSetOf()) { it.key }
        require(app.groups.map { it.key }.distinct().size == app.groups.size) { "Duplicate AI group keys" }
        val assignedKeys = app.groups.associate { group ->
            val key = if (usedKeys.add(group.key)) group.key else {
                var candidate = requireNotNull(usedKeys.maxOrNull()) + 1
                while (!usedKeys.add(candidate)) candidate++
                candidate
            }
            group.key to key
        }
        val isolated = app.copy(groups = app.groups.map { group ->
            group.copy(
                key = assignedKeys.getValue(group.key),
                scopeKeys = group.scopeKeys?.map { assignedKeys.getValue(it) },
            )
        })
        // No collisions remain. Keep the manual editor's naming/append policy without
        // changing its contract that a same-key scope can target an existing group.
        return current.edit { mergeApp(isolated) }
    }

    fun extract(text: String): String {
        val content = text.trim()
        val fence = Regex("""```(?:json5?)?\s*\n?([\s\S]*?)\n?```""").find(content)
        return fence?.groupValues?.get(1)?.trim() ?: content
    }
}

/** A failed refinement cannot erase the most recent fully validated candidate. */
internal class AiRuleCandidate(private val appId: String) {
    var latest: RawSubscription? = null
        private set
    fun accept(text: String): RawSubscription? {
        val parsed = try { AiRuleImport.parse(text, appId) } catch (_: Exception) { return null }
        latest = parsed
        return parsed
    }
}
