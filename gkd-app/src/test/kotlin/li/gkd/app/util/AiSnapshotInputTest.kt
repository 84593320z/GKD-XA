package li.gkd.app.util

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AiSnapshotInputTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun admittedInputSurvivesSourceDeletionAndSelectionChanges() {
        val file = temporary.newFile("snapshot.json")
        file.writeText("""{"appId":"app.test","nodes":[{"id":0,"pid":-1},{"id":1,"pid":0,"text":"selected"},{"id":2,"pid":0,"text":"unrelated"}]}""")
        val selection = mutableSetOf(1)
        val input = AiSnapshotInput.read(file, selection, false)
        selection.clear()
        file.writeText("""{"appId":"other.app","nodes":[]}""")
        assertTrue(file.delete())
        assertEquals("app.test", input.appId)
        assertTrue(input.content.contains("selected"))
        assertFalse(input.content.contains("unrelated"))
        assertFalse(input.content.contains("other.app"))
        assertTrue(input.content.contains("ID：1"))
    }

    @Test fun missingInvalidAndOversizedSnapshotsFailBeforeAdmission() {
        assertThrows(Exception::class.java) { AiSnapshotInput.read(File(temporary.root, "missing.json"), emptySet(), false) }
        val file = temporary.newFile("invalid.json")
        for (text in listOf("not json", """{"nodes":[]}""", """{"appId":" ","nodes":[]}""")) {
            file.writeText(text)
            assertThrows(Exception::class.java) { AiSnapshotInput.read(file, emptySet(), false) }
        }
        file.outputStream().use { it.write(ByteArray(AiSnapshotInput.MAX_BYTES + 1)) }
        val error = assertThrows(IllegalArgumentException::class.java) { AiSnapshotInput.read(file, emptySet(), false) }
        assertEquals("Snapshot exceeds AI size limit", error.message)
    }
}
