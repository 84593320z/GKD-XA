package li.gkd.app.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import li.gkd.app.data.RawSubscription
import li.gkd.app.text.UiStrings
import li.gkd.app.util.json
import li.gkd.db.LOCAL_SUBS_ID
import li.gkd.db.SubsItem
import li.gkd.db.SubscriptionConfigSnapshot

internal data class SubscriptionRestorePlan(
    val database: SubscriptionConfigSnapshot,
    val subscriptions: List<RawSubscription>,
    val conflictingIds: Set<Long>,
    val skippedOrphanConfigs: Int,
)

/**
 * Import metadata, body and overrides together. Preserve everything already owned by the user,
 * except for the empty local subscription automatically created on a fresh installation.
 * Call while holding the subscription writer lock and reading the current config in its Room transaction.
 */
internal fun planSubscriptionRestore(
    database: SubscriptionConfigSnapshot,
    subscriptions: List<RawSubscription>,
    existing: SubscriptionConfigSnapshot,
    hasExistingBody: (Long) -> Boolean,
    readLocalPlaceholder: () -> String? = { null },
    localPlaceholderName: String = UiStrings.subscription_local,
): SubscriptionRestorePlan {
    val bodyIds = subscriptions.mapTo(mutableSetOf()) { it.id }
    val itemIds = database.subsItems.mapTo(mutableSetOf()) { it.id }
    require(bodyIds.size == subscriptions.size && itemIds.size == database.subsItems.size) {
        UiStrings.backup_duplicate_subscription_id
    }
    val existingIds = existing.subsItems.mapTo(mutableSetOf()) { it.id }
    val overriddenIds = buildSet {
        existing.appConfigs.forEach { add(it.subsId) }
        existing.categoryConfigs.forEach { add(it.subsId) }
        existing.appGroupConfigs.forEach { add(it.subsId) }
        existing.globalGroupConfigs.forEach { add(it.subsId) }
    }
    val canReplaceLocal = LOCAL_SUBS_ID in bodyIds && LOCAL_SUBS_ID in itemIds &&
        LOCAL_SUBS_ID !in overriddenIds &&
        existing.subsItems.any {
            it.id == LOCAL_SUBS_ID && !it.enable && it.enableUpdate && it.updateUrl == null
        } && (!hasExistingBody(LOCAL_SUBS_ID) ||
            readLocalPlaceholder()?.let { isPristineLocalPlaceholder(it, localPlaceholderName) } == true)

    val conflictingIds = (bodyIds + itemIds).filterTo(mutableSetOf()) {
        !(it == LOCAL_SUBS_ID && canReplaceLocal) &&
            (it in existingIds || it in overriddenIds || hasExistingBody(it))
    }
    val importedIds = bodyIds.intersect(itemIds) - conflictingIds
    val availableIds = existingIds + importedIds
    return SubscriptionRestorePlan(
        database = SubscriptionConfigSnapshot(
            subsItems = database.subsItems.filter { it.id in importedIds },
            appConfigs = database.appConfigs.filter { it.subsId in importedIds },
            categoryConfigs = database.categoryConfigs.filter { it.subsId in importedIds },
            appGroupConfigs = database.appGroupConfigs.filter { it.subsId in importedIds },
            globalGroupConfigs = database.globalGroupConfigs.filter { it.subsId in importedIds },
        ),
        subscriptions = subscriptions.filter { it.id in importedIds },
        conflictingIds = conflictingIds,
        skippedOrphanConfigs = database.appConfigs.count { it.subsId !in availableIds } +
            database.categoryConfigs.count { it.subsId !in availableIds } +
            database.appGroupConfigs.count { it.subsId !in availableIds } +
            database.globalGroupConfigs.count { it.subsId !in availableIds },
    )
}

private fun isPristineLocalPlaceholder(text: String, localName: String): Boolean {
    // Strict decoding rejects unknown user fields; RawSubscription equality is identity-based.
    val body = try {
        Json.decodeFromString<RawSubscription>(text)
    } catch (_: SerializationException) {
        return false
    }
    val empty = RawSubscription(id = LOCAL_SUBS_ID, name = localName, version = 0)
    return json.encodeToString(body) == json.encodeToString(empty)
}

/** Keep bodies from legacy or partial backups, using disabled defaults for missing rows. */
internal fun completeSubscriptionMetadata(
    database: SubscriptionConfigSnapshot?,
    subscriptions: List<RawSubscription>,
): SubscriptionConfigSnapshot {
    val snapshot = database ?: SubscriptionConfigSnapshot()
    val existingIds = snapshot.subsItems.mapTo(mutableSetOf()) { it.id }
    val missingRows = subscriptions.filterNot { it.id in existingIds }.mapIndexed { index, subscription ->
        SubsItem(id = subscription.id, order = snapshot.subsItems.size + index)
    }
    return snapshot.copy(subsItems = snapshot.subsItems + missingRows)
}
