package priv.kit.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PrivilegeUiTutorialTest {
    @Test
    fun customGuidePreservesQueryAndReplacesFragmentForEveryTopic() {
        for (topic in PrivilegeUiTutorialTopic.entries) {
            var opened: String? = null
            openPrivilegeUiTutorial(topic, "https://example.com/help?lang=zh#old", null, null,
                openUri = { opened = it }, onOpenFailure = { error("Unexpected failure") })
            assertEquals("https://example.com/help?lang=zh#${topic.anchor}", opened)
        }
    }

    @Test
    fun legacyPermissionCallbackWinsOnlyForPermissionHelp() {
        val calls = mutableListOf<String>()
        for (topic in PrivilegeUiTutorialTopic.entries) {
            openPrivilegeUiTutorial(topic, "https://example.com/help",
                onViewPermissionSolutions = { calls += "legacy" },
                onViewStartupTutorial = { calls += it.anchor },
                openUri = { error("Callbacks must suppress browser navigation") },
                onOpenFailure = { error("Unexpected failure") })
        }
        assertEquals(PrivilegeUiTutorialTopic.entries.map {
            if (it == PrivilegeUiTutorialTopic.PERMISSION_RESTRICTIONS) "legacy" else it.anchor
        }, calls)
    }

    @Test
    fun unifiedCallbackAlsoHandlesPermissionHelp() {
        var received: PrivilegeUiTutorialTopic? = null
        openPrivilegeUiTutorial(PrivilegeUiTutorialTopic.PERMISSION_RESTRICTIONS, "unused", null,
            onViewStartupTutorial = { received = it },
            openUri = { error("Callback must suppress browser navigation") },
            onOpenFailure = { error("Unexpected failure") })
        assertEquals(PrivilegeUiTutorialTopic.PERMISSION_RESTRICTIONS, received)
    }

    @Test
    fun unavailableBrowserReportsFailure() {
        var failures = 0
        openPrivilegeUiTutorial(PrivilegeUiTutorialTopic.PAIRING, "https://example.com", null, null,
            openUri = { throw IllegalStateException("No browser") },
            onOpenFailure = { failures++ })
        assertEquals(1, failures)
    }
}
