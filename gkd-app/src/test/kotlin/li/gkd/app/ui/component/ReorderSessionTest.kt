package li.gkd.app.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderSessionTest {
    @Test
    fun longPressWithoutMovingFinishesWithoutPersistingAnOrder() {
        val state = ReorderSession(listOf(1, 2)) { it }
        state.startDragging()
        assertTrue(state.dragging)
        val result = state.finishDragging()
        assertFalse(state.dragging)
        assertFalse(result.moved)
        assertNull(result.reorderedItems)
    }

    @Test
    fun refreshDuringDragMergesLatestItemsAndDoesNotResurrectDeletedRows() {
        val state = ReorderSession(listOf(1, 2, 3)) { it }
        state.startDragging()
        state.move(0, 2)
        state.sync(listOf(1, 3, 4))
        assertTrue(state.dragging)
        val result = state.finishDragging()
        assertFalse(state.dragging)
        assertEquals(listOf(3, 1, 4), result.reorderedItems)
    }
}
