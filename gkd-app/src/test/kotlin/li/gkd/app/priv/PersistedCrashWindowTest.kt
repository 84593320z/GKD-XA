package li.gkd.app.priv

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistedCrashWindowTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun thirdCrashWithinWindowPersistsFuseAcrossProcessRecreation() {
        val file = temporary.root.resolve("state")
        assertFalse(PersistedCrashWindow(file).recordCrash(1_000L))
        assertFalse(PersistedCrashWindow(file).recordCrash(2_000L))
        assertTrue(PersistedCrashWindow(file).recordCrash(3_000L))
        assertTrue(PersistedCrashWindow(file).isBlocked)
        assertTrue(PersistedCrashWindow(file).recordCrash(99_000_000L))
    }

    @Test fun crashesOutsideWindowDoNotTripFuse() {
        val file = temporary.root.resolve("state")
        val window = PersistedCrashWindow(file, windowMillis = 100)
        assertFalse(window.recordCrash(1_000))
        assertFalse(window.recordCrash(1_200))
        assertFalse(window.recordCrash(1_400))
        assertFalse(PersistedCrashWindow(file).isBlocked)
    }

    @Test fun clockRollbackCannotResetCrashHistory() {
        val window = PersistedCrashWindow(temporary.root.resolve("state"), windowMillis = 100)
        assertFalse(window.recordCrash(1_000))
        assertFalse(window.recordCrash(900))
        assertTrue(window.recordCrash(800))
    }

    @Test fun corruptStateFailsClosedAndOnlyExplicitResetClearsIt() {
        val file = temporary.newFile("state").apply { writeText("partial write") }
        val window = PersistedCrashWindow(file)
        assertTrue(window.isBlocked)
        window.reset()
        assertFalse(PersistedCrashWindow(file).isBlocked)
        assertFalse(PersistedCrashWindow(file).recordCrash(1_000))
    }

    @Test fun unsuccessfulResetKeepsPreviouslyBlockedState() {
        val file = temporary.newFile("state").apply { writeText("corrupt") }
        val window = PersistedCrashWindow(file)
        temporary.newFolder("state.pending") // Force the actual atomic writer to fail.
        try { window.reset(); fail("Reset accepted without durable write") }
        catch (_: java.io.IOException) { }
        assertTrue(window.isBlocked)
        assertTrue(PersistedCrashWindow(file).isBlocked)
    }
}
