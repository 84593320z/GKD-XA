package li.gkd.app.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
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

class StatusServiceJobService : JobService() {                                  // line 16

    override fun onStartJob(params: JobParameters?): Boolean {                   // line 18
        try {
            StatusService.autoStart()                                             // line 19
        } catch (e: Throwable) {
            LogUtils.d("开机自启常驻通知失败", e)                                  // line 20-21
        }
        jobFinished(params, false)                                                // line 23
        scheduleStatusServiceBootWake(applicationContext)                         // line 25
        return false                                                              // line 27
    }

    override fun onStopJob(params: JobParameters?): Boolean = true                // line 30
}

/** 常驻通知自启兜底：BOOT_COMPLETED 在部分 ROM 上收不到，用 persisted Job 再拉一次。 */
fun scheduleStatusServiceBootWake(context: Context) {
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
