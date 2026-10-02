package li.gkd.app.data.subscription

import li.gkd.app.data.RawSubscription

data class SubscriptionSnapshot(
    val subscriptions: Map<Long, RawSubscription> = emptyMap(),
    val loadErrors: Map<Long, Exception> = emptyMap(),
    val updateErrors: Map<Long, Exception> = emptyMap(),
)
