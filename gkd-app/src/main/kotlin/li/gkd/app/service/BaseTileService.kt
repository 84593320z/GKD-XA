package li.gkd.app.service

import android.os.SystemClock
import android.service.quicksettings.Tile
import li.gkd.app.appRuntimeReady
import li.gkd.app.priv.CrashRecovery
import android.service.quicksettings.TileService
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseTileService : TileService() {
    protected abstract val activeFlow: Flow<Boolean>
    protected abstract fun onTileClick()

    private val serviceScope = MainScope()
    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            updateTile()
        }
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            appRuntimeReady.await()
            if (CrashRecovery.isRecoveryBlocked) return@launch
            val timestamp = SystemClock.elapsedRealtime()
            if (lastA11yFixTime?.let { timestamp - it > 3_000L } != false) {
                lastA11yFixTime = timestamp
                fixRestartAutomatorService()
            }
            activeFlow.collect { active ->
                qsTile?.apply {
                    state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    updateTile()
                }
            }
        }
    }

    override fun onClick() {
        super.onClick()
        if (!appRuntimeReady.isCompleted || CrashRecovery.isRecoveryBlocked) return
        StatusService.autoStart()
        onTileClick()
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

private var lastA11yFixTime: Long? = null
