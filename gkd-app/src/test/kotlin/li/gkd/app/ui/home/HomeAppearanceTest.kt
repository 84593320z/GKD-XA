package li.gkd.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAppearanceTest {
    @Test
    fun glassRequiresFloatingBarBlurAndShaderSupport() {
        assertEquals(HomeAppearance(true, true, true, true), homeAppearance(true, true, true, true))
        assertEquals(HomeAppearance(false, true, false, false), homeAppearance(false, true, true, true))
        assertEquals(HomeAppearance(true, false, false, false), homeAppearance(true, false, true, true))
        assertEquals(HomeAppearance(true, false, false, false), homeAppearance(true, true, true, false))
    }

    @Test
    fun glassCanBeTurnedOffWithoutDisablingBlur() {
        assertEquals(HomeAppearance(true, true, true, false), homeAppearance(true, true, false, true))
    }
}
