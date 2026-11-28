package li.gkd.app.service

import kotlinx.coroutines.runBlocking
import li.gkd.app.util.systemUiAppId
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotTileServiceTest {
    @Test
    fun requestsDismissalOnceFromSystemUiAndWaitsForAppRoot() = runBlocking {
        val scenario = Scenario(
            listOf(null, systemUiAppId, systemUiAppId, null, "test.app"),
            actionAvailability = listOf(false, true, true, false, false),
        )
        assertEquals(SnapshotTileTarget.APP, scenario.awaitTarget())
        assertEquals(listOf(systemUiAppId), scenario.dismissals)
        assertEquals(0, scenario.manualHints)
        assertEquals(1_000L, scenario.elapsedMillis)
    }

    @Test
    fun appRootWaitsUntilShadeActionDisappearsAfterTransition() = runBlocking {
        val scenario = Scenario(
            listOf(systemUiAppId, "test.app"),
            actionAvailability = listOf(true, true, false),
        )
        assertEquals(SnapshotTileTarget.APP, scenario.awaitTarget())
        assertEquals(listOf(systemUiAppId), scenario.dismissals)
        assertEquals(500L, scenario.elapsedMillis)
    }

    @Test
    fun availableShadeActionWorksWhenActiveRootIsUnderlyingApp() = runBlocking {
        val scenario = Scenario(listOf("test.app"), actionAvailability = listOf(true, false))
        assertEquals(SnapshotTileTarget.APP, scenario.awaitTarget())
        assertEquals(listOf("test.app"), scenario.dismissals)
        assertEquals(0, scenario.manualHints)
        assertEquals(250L, scenario.elapsedMillis)
    }

    @Test
    fun unavailableActionWaitsForManualDismissalWithoutBack() = runBlocking {
        val scenario = Scenario(listOf(systemUiAppId, systemUiAppId, "test.app"))
        assertEquals(SnapshotTileTarget.APP, scenario.awaitTarget())
        assertEquals(emptyList<String?>(), scenario.dismissals)
        assertEquals(1, scenario.manualHints)
    }

    @Test
    fun persistentSystemUiTimesOutWithoutRetryingDismissal() = runBlocking {
        for (accepted in listOf(true, false)) {
            val scenario = Scenario(
                listOf(systemUiAppId),
                actionAvailability = listOf(true),
                dismissAccepted = accepted,
            )
            assertEquals(SnapshotTileTarget.NO_APP_TRANSITION, scenario.awaitTarget())
            assertEquals(listOf(systemUiAppId), scenario.dismissals)
            assertEquals(if (accepted) 0 else 1, scenario.manualHints)
            assertEquals(3_000L, scenario.elapsedMillis)
        }
    }

    @Test
    fun rejectedDismissalWithUnderlyingAppRootDoesNotConfirmTransition() = runBlocking {
        val scenario = Scenario(listOf("test.app"), actionAvailability = listOf(true, false), dismissAccepted = false)
        assertEquals(SnapshotTileTarget.NO_APP_TRANSITION, scenario.awaitTarget())
        assertEquals(listOf("test.app"), scenario.dismissals)
        assertEquals(1, scenario.manualHints)
    }

    @Test
    fun appRootWithoutObservedShadeDoesNotDispatchOrCapture() = runBlocking {
        val scenario = Scenario(listOf("test.app", "test.other"))
        assertEquals(SnapshotTileTarget.NO_APP_TRANSITION, scenario.awaitTarget())
        assertEquals(emptyList<String?>(), scenario.dismissals)
        assertEquals(0, scenario.manualHints)
    }

    @Test
    fun noActiveRootDoesNotDispatchOrCapture() = runBlocking {
        val scenario = Scenario(listOf(null))
        assertEquals(SnapshotTileTarget.ROOT_UNAVAILABLE, scenario.awaitTarget())
        assertEquals(emptyList<String?>(), scenario.dismissals)
        assertEquals(3_000L, scenario.elapsedMillis)
    }

    @Test
    fun disappearanceOfShadeActionWithoutAppRootDoesNotPermitCapture() = runBlocking {
        val scenario = Scenario(
            listOf(systemUiAppId, null),
            actionAvailability = listOf(true, false),
        )
        assertEquals(SnapshotTileTarget.ROOT_UNAVAILABLE, scenario.awaitTarget())
        assertEquals(listOf(systemUiAppId), scenario.dismissals)
    }

    @Test
    fun unreadableActionAfterAnUnderlyingAppRootDoesNotConfirmDismissal() = runBlocking {
        val scenario = Scenario(
            listOf("test.app"),
            actionAvailability = listOf(true, true, null),
        )
        assertEquals(SnapshotTileTarget.NO_APP_TRANSITION, scenario.awaitTarget())
        assertEquals(listOf("test.app"), scenario.dismissals)
        assertEquals(1, scenario.manualHints)
    }

    @Test
    fun unreadableActionStillAllowsAnObservedSystemUiToAppTransition() = runBlocking {
        val scenario = Scenario(
            listOf(systemUiAppId, null, "test.app"),
            actionAvailability = listOf(true, null, null),
        )
        assertEquals(SnapshotTileTarget.APP, scenario.awaitTarget())
        assertEquals(listOf(systemUiAppId), scenario.dismissals)
        assertEquals(500L, scenario.elapsedMillis)
    }

    private class Scenario(
        private val packages: List<String?>,
        private val actionAvailability: List<Boolean?> = emptyList(),
        private val dismissAccepted: Boolean = true,
    ) {
        var elapsedMillis = 0L
            private set
        var manualHints = 0
            private set
        val dismissals = mutableListOf<String?>()
        private var reads = 0
        private var actionReads = 0
        private var activePackage: String? = null

        suspend fun awaitTarget() = awaitSnapshotTileTarget(
            readActivePackage = {
                packages[reads.coerceAtMost(packages.lastIndex)].also {
                    reads++
                    activePackage = it
                }
            },
            readDismissActionAvailability = {
                if (actionAvailability.isEmpty()) null
                else actionAvailability[actionReads.coerceAtMost(actionAvailability.lastIndex)].also { actionReads++ }
            },
            dismissShade = {
                dismissals += activePackage
                dismissAccepted
            },
            showManualDismissHint = { manualHints++ },
            elapsedMillis = { elapsedMillis },
            pause = { elapsedMillis += it },
        )
    }
}
