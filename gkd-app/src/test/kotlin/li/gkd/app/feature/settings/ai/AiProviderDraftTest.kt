package li.gkd.app.feature.settings.ai

import li.gkd.app.data.settings.AiConfig
import li.gkd.app.data.settings.AiHeader
import li.gkd.app.data.settings.AiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderDraftTest {
    @Test
    fun savingStaleDraftRetainsLatestModelAndDeletesOrRenamesHeaders() {
        val original = AiConfig(id = "provider", name = "Provider", model = "old", headers = listOf(
            AiHeader("X-Delete", "secret-a"), AiHeader("X-Old", "secret-b"),
        ))
        val draft = AiProviderDraft.of(original).copy(headers = listOf(
            AiHeaderDraft(name = "X-New", value = "replacement"),
        ))
        val models = listOf(AiModel(modelId = "new", displayName = "New"))
        val saved = draft.toProvider(original.copy(model = "new", models = models))
        assertEquals("new", saved.model)
        assertEquals(models, saved.models)
        assertEquals(listOf(AiHeader("X-New", "replacement")), saved.headers)
    }

    @Test
    fun emptyHeaderDraftClearsAllExistingHeaders() {
        val original = AiConfig(headers = listOf(AiHeader("Authorization", "secret")))
        assertEquals(emptyList<AiHeader>(), AiProviderDraft.of(original).copy(headers = emptyList()).toProvider(original).headers)
    }
}
