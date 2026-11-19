package priv.kit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import priv.kit.ui.component.privilegeUiAuthorizationModeItems

class PrivilegeUiAuthorizationModeTest {
    private val modes = listOf(
        PrivilegeUiStartupMode.ROOT,
        PrivilegeUiStartupMode.ADB,
        PrivilegeUiStartupMode.MANUAL_SHELL,
    )

    @Test
    fun selectedModeResolvesToItsItem() {
        val items = privilegeUiAuthorizationModeItems(
            modes = modes,
            selectedMode = PrivilegeUiStartupMode.ADB,
            busy = false,
            interactionEnabled = true,
        )

        assertEquals(1, items.indexOfFirst { it.selected })
        assertTrue(items.all { it.enabled })
    }

    @Test
    fun unavailableSelectionFallsBackToFirstMode() {
        val items = privilegeUiAuthorizationModeItems(
            modes = modes,
            selectedMode = PrivilegeUiStartupMode.EXTERNAL,
            busy = false,
            interactionEnabled = true,
        )

        assertEquals(0, items.indexOfFirst { it.selected })
        assertEquals(PrivilegeUiStartupMode.ROOT, items.first().mode)
    }

    @Test
    fun busyStateOnlyKeepsResolvedSelectionEnabled() {
        val items = privilegeUiAuthorizationModeItems(
            modes = modes,
            selectedMode = PrivilegeUiStartupMode.ADB,
            busy = true,
            interactionEnabled = true,
        )

        assertTrue(items.single { it.mode == PrivilegeUiStartupMode.ADB }.enabled)
        assertFalse(items.single { it.mode == PrivilegeUiStartupMode.ROOT }.enabled)
        assertFalse(items.single { it.mode == PrivilegeUiStartupMode.MANUAL_SHELL }.enabled)
    }

    @Test
    fun silentInteractionBlockDisablesEveryModeIncludingSelection() {
        val items = privilegeUiAuthorizationModeItems(
            modes = modes,
            selectedMode = PrivilegeUiStartupMode.ADB,
            busy = false,
            interactionEnabled = false,
        )

        assertTrue(items.none { it.enabled })
    }
}
