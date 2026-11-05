package li.gkd.app.ui.component

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    @Test
    fun deletionAfterReorderIsVisibleBeforeCompositionReadsRelatedData() {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(UnitApplier(), recomposer)
        val original = listOf(Item(1, "one"), Item(2, "two"), Item(3, "three"))
        var session: ReorderSession<Item, Int>? = null
        try {
            // 同一份组合要跨数据更新保留同一个拖拽会话
            for (source in listOf(original, listOf(original[2], original[0]))) {
                composition.setContent {
                    val current = rememberReorderSession(source) { it.id }
                    session?.let { assertSame(it, current) }
                    session = current
                    val details = source.associateBy { it.id }
                    assertEquals(source, current.items)
                    current.items.forEach { assertEquals(it, details.getValue(it.id)) }
                }
                if (source === original) {
                    val current = checkNotNull(session)
                    current.startDragging()
                    current.move(2, 0)
                    current.finishDragging()
                }
            }
        } finally {
            composition.dispose()
            runBlocking { recomposer.cancel() }
        }
    }

    @Test
    fun editsDuringDragKeepOrderButUseCurrentItemsAndDoNotRestoreDeletedItems() {
        val original = listOf(Item(1, "one"), Item(2, "two"), Item(3, "three"))
        val session = ReorderSession(original) { it.id }
        session.startDragging()
        session.move(2, 0)

        val edited = Item(3, "edited")
        val added = Item(4, "new")
        val latest = listOf(original[0], edited, added)
        session.sync(latest)

        val expected = listOf(edited, original[0], added)
        assertEquals(expected, session.items)
        assertEquals(expected, session.finishDragging().reorderedItems)

        session.reset()
        assertEquals(latest, session.items)
    }

    @Test
    fun syncOutsideDragReplacesOrderImmediately() {
        val session = ReorderSession(listOf(Item(1, "one"), Item(2, "two"))) { it.id }
        val latest = listOf(Item(2, "two-new"))
        session.sync(latest)
        assertEquals(latest, session.items)
        assertNull(session.finishDragging().reorderedItems)
    }

    private data class Item(val id: Int, val name: String)

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
