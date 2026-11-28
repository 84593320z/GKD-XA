package li.gkd.app.store

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileStateRetryTest {
    @Test
    fun retryFileWriteUsesBoundedAttemptsAndInjectedBackoff() = runBlocking {
        var writes = 0
        val waits = mutableListOf<Int>()
        retryFileWrite(waitBeforeRetry = { waits += it }) {
            writes++
            if (writes < 3) throw IOException("transient")
        }
        assertEquals(3, writes)
        assertEquals(listOf(0, 1), waits)

        writes = 0
        waits.clear()
        val failure = runCatching {
            retryFileWrite(waitBeforeRetry = { waits += it }) {
                writes++
                throw IOException("persistent")
            }
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(4, writes)
        assertEquals(listOf(0, 1, 2), waits)
    }

    @Test
    fun stateWriterPersistsLatestValueAfterRetryingStaleEmission() = runBlocking {
        val directory = Files.createTempDirectory("file-state-retry").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val retryEntered = CompletableDeferred<Unit>()
        val resumeRetry = CompletableDeferred<Unit>()
        var failFirst = true
        try {
            val file = directory.resolve("state.txt")
            val flow = FileStateStore.createTextFlow(
                file = file,
                decode = { it ?: "initial" },
                encode = { it },
                scope = scope,
                writeText = { target: File, value: String ->
                    if (value == "first" && failFirst) {
                        failFirst = false
                        throw IOException("transient")
                    }
                    target.writeText(value)
                },
                waitBeforeRetry = {
                    retryEntered.complete(Unit)
                    resumeRetry.await()
                },
            )
            flow.value = "first"
            withTimeout(5_000) { retryEntered.await() }
            flow.value = "latest"
            resumeRetry.complete(Unit)
            withTimeout(5_000) {
                while (!file.isFile || file.readText() != "latest") delay(5)
            }
            assertEquals("latest", file.readText())
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }
}
