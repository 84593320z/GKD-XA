package li.gkd.app.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AutomatorServiceSwitchingTest {
    @Test
    fun unvalidatedCandidateLeavesTheOldLeaseInPlace() {
        val handoff = AutomatorServiceHandoff<Any>()
        val old = Any()
        val candidate = Any()
        val oldLease = handoff.accept(old, null) { true }

        assertNull(handoff.accept(candidate, old) { false })
        assertNull(handoff.disconnect(candidate) { true })
        assertSame(oldLease, handoff.current)
    }

    @Test
    fun candidateDisconnectRollsBackAndInvalidatesItsScheduledRetirement() {
        val handoff = AutomatorServiceHandoff<Any>()
        val old = Any()
        val candidate = Any()
        val oldLease = handoff.accept(old, null) { true }
        val candidateLease = requireNotNull(handoff.accept(candidate, old) { true })

        val rollback = requireNotNull(handoff.disconnect(candidate) { it === old })
        assertSame(old, rollback.service)
        assertNotSame(oldLease, rollback)
        assertSame(rollback, handoff.current)
        assertNull(handoff.retire(candidateLease, { old }) { true })
    }

    @Test
    fun candidateFailureDoesNotRestoreWhenRollbackWasDisallowed() {
        val handoff = AutomatorServiceHandoff<Any>()
        val old = Any()
        val candidate = Any()
        handoff.accept(old, null) { true }
        handoff.accept(candidate, old) { true }

        assertNull(handoff.disconnect(candidate) { false })
        assertNull(handoff.current)
    }

    @Test
    fun retirementRequiresBothCapturedGenerationAndExactPreviousInstance() {
        val handoff = AutomatorServiceHandoff<Any>()
        val old = Any()
        val replacement = Any()
        val candidate = Any()
        handoff.accept(old, null) { true }
        val first = requireNotNull(handoff.accept(candidate, old) { true })
        handoff.accept(old, candidate) { true }
        val latest = requireNotNull(handoff.accept(candidate, old) { true })

        assertNull(handoff.retire(first, { old }) { true })
        assertNull(handoff.retire(latest, { replacement }) { true })
        assertNull(handoff.retire(latest, { old }) { false })
        assertSame(old, handoff.retire(latest, { old }) { true })
        assertNull(handoff.accept(old, candidate) { true })
        assertSame(latest, handoff.current)
    }

    @Test
    fun restartUsesTheLatestListAndOnlyTouchesOurComponent() = runBlocking {
        val ours = "ours/.Service"
        var raw = "removed/.Service:$ours:malformed:keep/.Service"
        val writes = mutableListOf<String>()
        val settings = AccessibilityServicesSetting(
            read = { raw },
            write = { raw = it; writes.add(it); true },
            isOurs = { it == ours || it == "ours/ours.Service" },
            component = { ours },
        )
        val waitStarted = CompletableDeferred<Unit>()
        val releaseWait = CompletableDeferred<Unit>()
        val restart = async(start = CoroutineStart.UNDISPATCHED) {
            settings.restart(shouldStart = { true }) {
                waitStarted.complete(Unit)
                releaseWait.await()
            }
        }
        waitStarted.await()
        assertEquals(listOf("removed/.Service:malformed:keep/.Service"), writes)
        raw = "added/.Service::malformed:keep/.Service"
        releaseWait.complete(Unit)
        assertTrue(restart.await())
        assertEquals("added/.Service::malformed:keep/.Service:$ours", raw)

        // A close re-reads as well, including unrecognized entries and our long-form alias.
        raw += ":ours/ours.Service:another/.Service"
        assertTrue(settings.setEnabled(false))
        assertEquals("added/.Service::malformed:keep/.Service:another/.Service", raw)
    }

    @Test
    fun closeDuringRestartWaitPreventsTheWriteAndAnyLateConnection() = runBlocking {
        val startup = AccessibilityStartIntent<Any>()
        val open = startup.toggle()
        val starting = Any()
        assertSame(open, startup.onCreated(starting))
        var raw = "other/.Service:ours/.Service"
        val settings = AccessibilityServicesSetting(
            read = { raw },
            write = { raw = it; true },
            isOurs = { it == "ours/.Service" },
            component = { "ours/.Service" },
        )

        val restarted = settings.restart(shouldStart = { startup.permits(open) }) {
            assertEquals("other/.Service", raw)
            val close = startup.toggle()
            assertFalse(close.enabled)
            assertSame(starting, startup.created)
            assertFalse(startup.canConnect(starting, open))
        }
        assertFalse(restarted)
        assertEquals("other/.Service", raw)
        assertTrue(startup.onDestroyed(starting))
        val late = Any()
        assertNull(startup.onCreated(late, systemEnabled = false))
        assertFalse(startup.canConnect(late, open))
    }

    @Test
    fun explicitCloseBeforeDestroyDoesNotRollbackToThePreviousService() {
        val startup = AccessibilityStartIntent<Any>()
        val open = startup.toggle()
        assertTrue(startup.allowRollback(open, stopping = false, tempShutdown = false))
        val close = startup.toggle()
        assertFalse(startup.allowRollback(open, stopping = false, tempShutdown = false))
        assertFalse(startup.allowRollback(close, stopping = true, tempShutdown = false))
        assertTrue(startup.allowRollback(close, stopping = true, tempShutdown = true))
    }

    @Test
    fun closeBeforeCreationRejectsLateBindButANewStartCanConnect() {
        val startup = AccessibilityStartIntent<Any>()
        val first = startup.toggle()
        assertTrue(first.enabled)
        assertFalse(startup.toggle().enabled)
        startup.finish(first)
        val late = Any()
        val lateToken = startup.onCreated(late, systemEnabled = false)
        assertFalse(startup.canConnect(late, lateToken))
        assertNull(startup.beginStart())

        val next = startup.toggle()
        assertTrue(next.enabled)
        val fresh = Any()
        assertSame(next, startup.onCreated(fresh))
        assertFalse(startup.onDestroyed(late))
        assertFalse(startup.canConnect(late, first))
        assertTrue(startup.canConnect(fresh, next))
    }

    @Test
    fun falseToTrueSystemEnableRevivesAnExplicitlyClosedStartup() {
        val startup = AccessibilityStartIntent<Any>()
        val open = startup.toggle()
        val first = Any()
        assertSame(open, startup.onCreated(first, systemEnabled = true))
        assertTrue(startup.canConnect(first, open))

        val close = startup.toggle()
        assertFalse(close.enabled)
        startup.recordSettingWrite(false)
        assertTrue(startup.onDestroyed(first))

        val rebound = Any()
        val revived = startup.onCreated(rebound, systemEnabled = true)
        assertNotNull(revived)
        assertTrue(revived!!.enabled)
        assertTrue(startup.canConnect(rebound, revived))
        assertFalse(startup.canConnect(first, open))
    }

    @Test
    fun confirmedDisableSelfAllowsManualReenableWithoutPrivilegedWrite() {
        val startup = AccessibilityStartIntent<Any>()
        val first = Any()
        val open = startup.onCreated(first, systemEnabled = true)
        assertTrue(startup.canConnect(first, open))
        assertFalse(startup.toggle().enabled)
        startup.observeSystemDisable(false)
        assertTrue(startup.onDestroyed(first))

        val rebound = Any()
        val revived = startup.onCreated(rebound, systemEnabled = true)
        assertTrue(startup.canConnect(rebound, revived))
        assertFalse(startup.canConnect(first, open))
    }

    @Test
    fun unknownOrStillEnabledSettingAfterDisableSelfCannotReviveAStaleBind() {
        for (systemEnabled in listOf(true, null)) {
            val startup = AccessibilityStartIntent<Any>()
            val first = Any()
            val open = startup.onCreated(first, systemEnabled = true)
            assertTrue(startup.canConnect(first, open))
            assertFalse(startup.toggle().enabled)
            startup.observeSystemDisable(systemEnabled)
            assertTrue(startup.onDestroyed(first))

            val late = Any()
            assertNull(startup.onCreated(late, systemEnabled = true))
            assertFalse(startup.canConnect(late, open))
        }
    }

    @Test
    fun confirmedSystemEnableReplacesAnOldInstanceBeforeItsDestroyCallback() {
        val startup = AccessibilityStartIntent<Any>()
        val first = Any()
        val original = startup.onCreated(first, systemEnabled = true)
        assertTrue(startup.canConnect(first, original))
        assertFalse(startup.toggle().enabled)
        startup.recordSettingWrite(false)

        val rebound = Any()
        val revived = startup.onCreated(rebound, systemEnabled = true)

        assertNotNull(revived)
        assertTrue(startup.canConnect(rebound, revived))
        assertFalse(startup.onDestroyed(first))
        assertFalse(startup.canConnect(first, original))
    }

    @Test
    fun aLiveGenerationCannotBeOverwrittenByASecondBind() {
        val startup = AccessibilityStartIntent<Any>()
        val request = startup.toggle()
        val active = Any()
        assertSame(request, startup.onCreated(active, systemEnabled = true))

        val duplicate = Any()
        assertNull(startup.onCreated(duplicate, systemEnabled = true))
        assertSame(active, startup.created)
        assertTrue(startup.canConnect(active, request))
        assertFalse(startup.canConnect(duplicate, request))
    }

    @Test
    fun settingCallbacksTrackNoOpWritesAndPreserveOtherServices() {
        val ours = "ours/.Service"
        var raw = "other/.Service:$ours:malformed"
        val updates = mutableListOf<Boolean>()
        val settings = AccessibilityServicesSetting(
            read = { raw },
            write = { raw = it; true },
            isOurs = { it == ours },
            component = { ours },
            onUpdate = { updates += it },
        )

        assertTrue(settings.setEnabled(true))
        assertEquals("other/.Service:$ours:malformed", raw)
        assertEquals(listOf(true), updates)
        assertTrue(settings.setEnabled(false))
        assertEquals("other/.Service:malformed", raw)
        assertEquals(listOf(true, false), updates)
    }
}
