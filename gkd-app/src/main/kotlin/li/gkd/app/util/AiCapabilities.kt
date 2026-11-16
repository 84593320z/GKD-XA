package li.gkd.app.util

import li.gkd.app.data.settings.AiConfig

/** Learned failures belong to a concrete endpoint and model, never an entire provider. */
internal class AiCapabilities(private val capacity: Int = 64) {
    private data class Key(val provider: String, val endpoint: String, val protocol: String, val model: String)
    data class Value(val rejectedBudget: Int? = null, val acceptedBudget: Int? = null, val thinkingRejected: Boolean = false)
    private val entries = LinkedHashMap<Key, Value>(16, 0.75f, true)
    private fun key(config: AiConfig) = Key(config.id, AiApiUrls.buildApiEndpoint(config), config.protocol, config.model)

    @Synchronized fun get(config: AiConfig): Value = entries[key(config)] ?: Value()

    @Synchronized fun rejectBudget(config: AiConfig, budget: Int) {
        val old = get(config)
        put(config, old.copy(rejectedBudget = AiTokenBudget.narrowedCeiling(old.rejectedBudget, budget)))
    }

    @Synchronized fun acceptBudget(config: AiConfig, budget: Int) {
        put(config, get(config).copy(acceptedBudget = budget))
    }

    @Synchronized fun rejectThinking(config: AiConfig) {
        put(config, get(config).copy(thinkingRejected = true))
    }

    private fun put(config: AiConfig, value: Value) {
        entries[key(config)] = value
        while (entries.size > capacity) entries.remove(entries.keys.first())
    }

    fun initialBudget(config: AiConfig): Int {
        val learned = get(config)
        val requested = AiTokenBudget.budgetForRequest(config.maxTokens, null)
        return if (learned.rejectedBudget != null && requested >= learned.rejectedBudget) {
            learned.acceptedBudget?.takeIf { it < learned.rejectedBudget }
                ?: AiTokenBudget.budgetForRejection(learned.rejectedBudget)
        } else requested
    }
}
