package li.gkd.app.util

import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiEndpointMode
import org.junit.Assert.*
import org.junit.Test

class AiCapabilitiesTest {
    private val config = AiConfig(
        id = "provider-a",
        apiUrl = "https://ai.example/v1",
        apiKey = "test-key",
        model = "model-a",
        maxTokens = 16384,
    )

    @Test fun learnedFailuresAreIsolatedByProviderEndpointProtocolAndModel() {
        val cache = AiCapabilities()
        cache.rejectBudget(config, 8192)
        cache.acceptBudget(config, 4096)
        cache.rejectThinking(config)
        val learned = cache.get(config)
        assertEquals(AiCapabilities.Value(8192, 4096, true), learned)
        val alternatives = listOf(
            config.copy(id = "provider-b"),
            config.copy(apiUrl = "https://other.example/v1"),
            config.copy(apiUrl = "https://ai.example/other/v1"),
            config.copy(endpointMode = AiEndpointMode.RESPONSES),
            config.copy(protocol = "anthropic"),
            config.copy(model = "model-b"),
        )
        alternatives.forEach { alternative ->
            assertEquals("learning leaked to " + alternative, AiCapabilities.Value(), cache.get(alternative))
            assertEquals(16384, cache.initialBudget(alternative))
        }
        assertEquals(learned, cache.get(config.copy(name = "renamed", apiKey = "rotated")))
        assertEquals(learned, cache.get(config.copy(apiUrl = "https://ai.example/v1/chat/completions")))
    }

    @Test fun rejectionConvergesThenReusesAcceptedBudgetWithoutRaisingUserBudget() {
        val cache = AiCapabilities()
        assertEquals(16384, cache.initialBudget(config))
        cache.rejectBudget(config, 16384)
        assertEquals(8192, cache.initialBudget(config))
        cache.rejectBudget(config, 8192)
        assertEquals(4096, cache.initialBudget(config))
        cache.acceptBudget(config, 4096)
        cache.rejectBudget(config, 32768)
        assertEquals(4096, cache.initialBudget(config))
        assertEquals(8192, cache.get(config).rejectedBudget)
        assertEquals(2048, cache.initialBudget(config.copy(maxTokens = 2048)))

        cache.rejectBudget(config, 4096)
        assertEquals("previous acceptance cannot override a tighter rejection", 2048, cache.initialBudget(config))
    }

    @Test fun legacyProviderWideFlagsDoNotPoisonNewEndpointModelLearning() {
        val cache = AiCapabilities()
        val legacy = config.copy(maxTokens = 4096, maxTokenCeiling = 1024, thinkingRejected = true)
        assertEquals(4096, cache.initialBudget(legacy))
        assertFalse(cache.get(legacy).thinkingRejected)
    }

    @Test fun leastRecentlyUsedLearningIsEvictedAtCapacity() {
        val cache = AiCapabilities(capacity = 2)
        val second = config.copy(model = "model-b")
        val third = config.copy(model = "model-c")
        cache.rejectThinking(config)
        cache.rejectBudget(second, 8192)
        assertTrue(cache.get(config).thinkingRejected)
        cache.acceptBudget(third, 4096)
        assertEquals(AiCapabilities.Value(), cache.get(second))
        assertTrue(cache.get(config).thinkingRejected)
        assertEquals(4096, cache.get(third).acceptedBudget)
    }
}
