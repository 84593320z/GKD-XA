package li.gkd.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.data.CrashData
import li.gkd.app.data.deleteCrashDataList
import li.gkd.app.data.loadCrashDataList
import li.gkd.app.ui.share.BaseViewModel
import li.gkd.app.core.state.Loadable

class CrashReportVm(
    initialCrashDataList: List<CrashData>,
) : BaseViewModel() {
    // 详情改走全屏弹窗（对齐上游 344efde8）：长堆栈不再把列表条目撑爆
    var selectedCrash by mutableStateOf<CrashData?>(null)
        private set

    val crashDataState: StateFlow<Loadable<List<CrashData>>>
        field = MutableStateFlow(
            if (initialCrashDataList.isEmpty()) {
                Loadable.Loading
            } else {
                Loadable.Ready(initialCrashDataList)
            },
        )

    private val initialLoadJob = scope.launch(Dispatchers.IO) {
        crashDataState.value = try {
            Loadable.Ready(loadCrashDataList())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (initialCrashDataList.isEmpty()) {
                Loadable.Failure(e)
            } else {
                Loadable.Ready(initialCrashDataList)
            }
        }
    }

    suspend fun deleteCrash(crashData: CrashData) {
        initialLoadJob.join()
        val deleted = withContext(Dispatchers.IO) {
            crashData.delete()
        }
        if (deleted) {
            // 先关弹窗再改列表，避免弹窗子树还引用着已删除条目
            if (selectedCrash?.id == crashData.id) {
                selectedCrash = null
            }
            crashDataState.value = Loadable.Ready(
                crashDataState.value.value.orEmpty().filterNot { it.id == crashData.id },
            )
        } else {
            reloadAfterDeleteFailure()
            error(UiStrings.crash_report_delete_failed)
        }
    }

    suspend fun deleteAllCrashes() {
        initialLoadJob.join()
        val deleted = withContext(Dispatchers.IO) {
            deleteCrashDataList()
        }
        crashDataState.value = Loadable.Ready(
            withContext(Dispatchers.IO) { loadCrashDataList() },
        )
        selectedCrash = null
        if (!deleted) {
            error(UiStrings.crash_reports_delete_partially_failed)
        }
    }

    private suspend fun reloadAfterDeleteFailure() {
        crashDataState.value = Loadable.Ready(
            withContext(Dispatchers.IO) { loadCrashDataList() },
        )
    }

    fun showCrash(crashData: CrashData) {
        selectedCrash = crashData
    }

    fun dismissCrash() {
        selectedCrash = null
    }
}
