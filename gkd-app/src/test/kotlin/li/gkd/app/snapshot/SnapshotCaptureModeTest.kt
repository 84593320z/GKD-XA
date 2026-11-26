package li.gkd.app.snapshot

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SnapshotCaptureModeTest {
    @Test fun snapshotOnlyNeverStartsAi() = runBlocking {
        SnapshotCaptureMode.SNAPSHOT_ONLY.run(capture = { 42L }, aiEnabled = true) { _, _ -> error("Snapshot-only must not request AI") }
    }

    @Test fun explicitGenerationRunsAfterCaptureEvenWhenAutoToggleIsOff() = runBlocking {
        val events = mutableListOf<String>()
        SnapshotCaptureMode.SNAPSHOT_AND_GENERATE.run(
            capture = { events.add("saved"); 42L }, aiEnabled = false,
            generate = { id, bypass -> events.add("generate:$id:$bypass") },
        )
        assertEquals(listOf("saved", "generate:42:true"), events)
    }

    @Test fun automaticGenerationFollowsToggleAndNeverOverridesIt() = runBlocking {
        val bypass = mutableListOf<Boolean>()
        SnapshotCaptureMode.AUTOMATIC.run(capture = { 7L }, aiEnabled = false) { _, flag -> bypass.add(flag) }
        SnapshotCaptureMode.AUTOMATIC.run(capture = { 8L }, aiEnabled = true) { _, flag -> bypass.add(flag) }
        assertEquals(listOf(false), bypass)
    }

    @Test fun failedCaptureDoesNotStartGeneration() = runBlocking {
        val failure = IllegalStateException("Cannot save")
        val result = runCatching {
            SnapshotCaptureMode.SNAPSHOT_AND_GENERATE.run(
                capture = { throw failure },
                generate = { _, _ -> error("Must not generate from a failed capture") },
            )
        }
        assertSame(failure, result.exceptionOrNull())
    }
}
