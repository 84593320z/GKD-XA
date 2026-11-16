package li.gkd.app.store

import java.nio.file.Files
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test

class FileStateStoreTest {
    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ConcurrentLinkedQueue<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.add(block) }
        fun drain() { while (true) (tasks.poll() ?: return).run() }
    }

    @Test
    fun updateBeforeDispatcherStartsIsPersisted() = runBlocking {
        val folder = Files.createTempDirectory("file-state-race").toFile()
        val dispatcher = QueuedDispatcher()
        val owner = SupervisorJob()
        val scope = CoroutineScope(owner + dispatcher)
        try {
            val file = folder.resolve("state.txt")
            val flow = FileStateStore.createTextFlow(file, { it ?: "initial" }, { it }, scope)
            flow.value = "updated-before-dispatch"
            dispatcher.drain()
            val persisted = withTimeout(5_000) {
                while (true) {
                    val actual = try { file.takeIf { it.isFile }?.readText() } catch (_: IOException) { null }
                    if (actual == "updated-before-dispatch") return@withTimeout actual
                    delay(10)
                }
            }
            assertEquals("updated-before-dispatch", persisted)
        } finally {
            scope.cancel()
            withTimeout(5_000) {
                while (!owner.isCompleted) {
                    dispatcher.drain()
                    delay(10)
                }
            }
            folder.deleteRecursively()
        }
    }
}
