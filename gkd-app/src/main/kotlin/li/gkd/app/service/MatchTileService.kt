package li.gkd.app.service

import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.store.AppStore.toggleEnableMatch
import kotlinx.coroutines.flow.map

class MatchTileService : BaseTileService() {
    override val activeFlow = storeFlow.map { it.enableMatch }

    override fun onTileClick() = toggleEnableMatch()
}
