package li.gkd.app.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import li.gkd.app.appRuntimeReady
import li.gkd.app.appStartupFailure
import li.gkd.app.priv.CrashRecovery
import li.gkd.app.util.LogUtils

/**
 * 恢复自本地构建 APK（仓库 main 没有这个文件，且它从未进过 git —— source-paths.txt 里查不到）。
 * 本地包 manifest 额外声明了 li.gkd.app.receiver.BootReceiver 与
 * li.gkd.app.service.StatusServiceJobService，并出现 RECEIVE_BOOT_COMPLETED /
 * android.permission.BIND_JOB_SERVICE 两个字符串（CI 包里都没有）。
 *
 * 行号来自 debug 包 .line：JOB_ID/DELAY 常量、JobService 16-30、scheduleStatusServiceBootWake 33-49。
 */
private const val JOB_ID = 0x474b44 // 'G''K''D'

private const val BOOT_WAKE_DELAY_MILLIS = 10_000L

class StatusServiceJobService : JobService() {
    private val serviceScope = MainScope()
    private var wakeJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        params ?: return false
        if (CrashRecovery.isRecoveryBlocked || appStartupFailure.value != null) return false
        wakeJob?.cancel()
        wakeJob = serviceScope.launch {
            // A failed migration ends this attempt. Only an explicit retry in the app can
            // open the runtime gate again; the persisted job must not form a retry loop.
            val ready = coroutineScope {
                val failed = async { appStartupFailure.filterNotNull().first() }
                try {
                    select {
                        appRuntimeReady.onAwait { true }
                        failed.onAwait { false }
                    }
                } finally {
                    failed.cancel()
                }
            }
            if (ready && !CrashRecovery.isRecoveryBlocked) {
                try {
                    StatusService.autoStart()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LogUtils.d("开机自启常驻通知失败", error)
                }
            }
            jobFinished(params, false)
            if (ready) scheduleStatusServiceBootWake(applicationContext)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        wakeJob?.cancel()
        wakeJob = null
        return appRuntimeReady.isCompleted && !CrashRecovery.isRecoveryBlocked
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/** 常驻通知自启兜底：BOOT_COMPLETED 在部分 ROM 上收不到，用 persisted Job 再拉一次。 */
fun scheduleStatusServiceBootWake(context: Context) {
    if (!appRuntimeReady.isCompleted || CrashRecovery.isRecoveryBlocked) return
    val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
    if (scheduler.allPendingJobs.none { it.id == JOB_ID }) {
        val jobInfo = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, StatusServiceJobService::class.java),
        ).setPersisted(true)
            .setMinimumLatency(BOOT_WAKE_DELAY_MILLIS)
            .build()
        val result = scheduler.schedule(jobInfo)
        if (result != JobScheduler.RESULT_SUCCESS) {                              // line 43
            LogUtils.d("注册常驻通知自启兜底失败: $result")
        }
    }
}
