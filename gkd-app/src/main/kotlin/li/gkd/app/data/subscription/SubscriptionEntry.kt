package li.gkd.app.data.subscription

import li.gkd.app.data.RawSubscription
import li.gkd.db.SubsItem
import java.net.URI

private fun resolveCheckUpdateUrl(
    subsItem: SubsItem,
    subscription: RawSubscription?,
): String? {
    val checkUpdateUrl = subscription?.checkUpdateUrl ?: return null
    val updateUrl = subscription.updateUrl ?: subsItem.updateUrl ?: return checkUpdateUrl
    return runCatching { URI(updateUrl).resolve(checkUpdateUrl).toString() }
        .onFailure(Throwable::printStackTrace)
        .getOrNull()
}

sealed class SubscriptionEntry {
    abstract val subsItem: SubsItem
    abstract val subscription: RawSubscription?
    val checkUpdateUrl by lazy { resolveCheckUpdateUrl(subsItem, subscription) }
}

data class SubsEntry(
    override val subsItem: SubsItem,
    override val subscription: RawSubscription?,
) : SubscriptionEntry()

data class UsedSubsEntry(
    override val subsItem: SubsItem,
    override val subscription: RawSubscription,
) : SubscriptionEntry()
