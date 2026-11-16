package li.gkd.app.util

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AiAdmissionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun invalidInputIsReportedBeforeQueueAdmission() = runBlocking {
        val missing = File(temporary.root, "missing.json")
        val invalid = temporary.newFile("invalid.json").apply { writeText("not json") }
        val oversized = temporary.newFile("large.json").apply {
            outputStream().use { it.write(ByteArray(AiSnapshotInput.MAX_BYTES + 1)) }
        }
        for (file in listOf(missing, invalid, oversized)) {
            var failures = 0
            val input = admitAiInput(onFailure = { failures++ }) {
                AiSnapshotInput.read(file, emptySet(), false)
            }
            assertNull(input)
            assertEquals(1, failures)
        }
    }

    @Test fun validInputPassesWithoutFailure() = runBlocking {
        assertEquals("ready", admitAiInput(onFailure = { fail("unexpected failure") }) { "ready" })
    }

    @Test fun cancellationRemainsOwnedByCaller() = runBlocking {
        val cancellation = CancellationException("cancelled")
        try {
            admitAiInput(onFailure = { fail("cancellation must not be reported as input failure") }) { throw cancellation }
            fail("expected cancellation")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }
}
