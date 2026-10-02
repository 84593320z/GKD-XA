package li.gkd.app.ui.home

import li.gkd.app.platform.service.ServiceController
import li.gkd.app.ui.share.BaseViewModel

class DashboardVm : BaseViewModel() {
    fun stopStatusService() {
        ServiceController.setStatusEnabled(false)
    }
}
