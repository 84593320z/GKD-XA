package li.gkd.app.util

import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.TimeUtils.throttle
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import li.gkd.app.META
import li.gkd.app.MainActivity
import li.gkd.app.app
import li.gkd.app.data.backup.BackupManager
import li.gkd.app.platform.hasPrivilegedInstallCapability
import li.gkd.app.platform.installPackagePrivileged
import li.gkd.app.platform.updateInstallSessions
import li.gkd.app.platform.verifyUpdatePackage
import li.gkd.app.platform.PrivilegedInstallResult
import li.gkd.app.store.FileStateStore
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.component.GkAlertDialog
import li.songe.codeorigin.CallSite
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.days
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text


private val UPDATE_URL: String
    get() = UpdateChannelOption.objects.findOption(storeFlow.value.updateChannel).url

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val digest: String? = null,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
data class NewVersion(
    val versionCode: Int? = null,
    val versionName: String,
    val downloadUrl: String,
    val fileSize: Long,
    val versionLogs: List<VersionLog> = emptyList(),
    val releaseNotes: String? = null,
    val sha256: String? = null,
) {
    val absoluteDownloadUrl: String
        get() = downloadUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: URI(UPDATE_URL).resolve(downloadUrl).toString()
}

@Serializable
data class VersionLog(
    val name: String,
    val code: Int,
    val desc: String,
)

private fun String.parseGitHubTime(): Long? {
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(this)?.time
    }.getOrNull()
}

private fun GitHubRelease.isNewerThanCurrent(): Boolean {
    if (tagName == META.tagName) return false
    return (publishedAt?.parseGitHubTime() ?: 0L) > META.commitTime
}

internal fun GitHubRelease.toNewVersion(): NewVersion {
    val apkAsset = assets.firstOrNull { asset ->
        asset.name.endsWith(".apk", ignoreCase = true)
    } ?: error("未找到 APK 下载文件")
    return NewVersion(
        // GitHub tags do not encode Android versionCode. Read that from the verified APK.
        versionCode = null,
        versionName = tagName.removePrefix("v"),
        downloadUrl = apkAsset.browserDownloadUrl,
        fileSize = apkAsset.size,
        releaseNotes = body,
        sha256 = apkAsset.digest?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
    )
}

private var lastCheckTime = 0L

class UpdateStatus(val scope: CoroutineScope) {
    private val checkUpdatingMutex = MutexState()
    val checkUpdatingFlow
        get() = checkUpdatingMutex.state
    private val newVersionFlow = MutableStateFlow<NewVersion?>(null)
    private val downloadStatusFlow = MutableStateFlow<LoadStatus<File>?>(null)
    private var downloadJob: Job? = null
    private var installJob: Job? = null
    private val ignoredReleaseNames by lazy {
        FileStateStore.createJsonFlow("ignore_release_names", default = { emptySet<String>() }, scope = scope)
    }

    private val ignoreVersionListFlow by lazy {
        FileStateStore.createJsonFlow(
            key = "ignore_version_list",
            default = { emptySet<Int>() },
            scope = scope,
        )
    }
    private var lastManual = false

    val canRecheck get() = System.currentTimeMillis() - lastCheckTime > 1.days.inWholeMilliseconds

    fun checkUpdate(
        manual: Boolean = false,
        @CallSite loc: String = "",
    ) {
        scope.launchLogged(Dispatchers.IO, loc = loc) {
            try {
                lastManual = manual
                checkUpdatingMutex.tryWithStateLock {
                    lastCheckTime = System.currentTimeMillis()
                    if (!NetworkUtils.isAvailable()) {
                        error(UiStrings.network_unavailable)
                    }
                    // 通道既可能是 GitHub Releases API（返回 release 对象），也可能是 version/*.json
                    // （直接就是 NewVersion）。按 URL 形态分流，而不是认某个具体仓库名 ——
                    // 之前这里写死了上游 fork 的仓库地址，换源后这条分支就永远进不去。
                    val updateUrl = UPDATE_URL
                    val metadata = client.downloadTextBounded(updateUrl, maxBytes = 1024 * 1024, timeoutMillis = 30_000L)
                    val newVersion = if (updateUrl.contains("api.github.com/repos/")) {
                        val release = json.decodeFromString<GitHubRelease>(metadata)
                        if (!release.isNewerThanCurrent()) {
                            if (manual) toast(UiStrings.updates_none, loc = loc)
                            return@tryWithStateLock
                        }
                        release.toNewVersion()
                    } else {
                        json.decodeFromString<NewVersion>(metadata).also { version ->
                            if (requireNotNull(version.versionCode) { "更新源缺少 versionCode" } <= META.versionCode) {
                                if (manual) toast(UiStrings.updates_none, loc = loc)
                                return@tryWithStateLock
                            }
                        }
                    }
                    if (
                        !manual &&
                        (newVersion.versionCode?.let { it in ignoreVersionListFlow.value }
                            ?: (newVersion.versionName in ignoredReleaseNames.value))
                    ) return@tryWithStateLock
                    newVersionFlow.value = newVersion
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (manual) {
                    toast(e.message ?: e.stackTraceToString(), loc = "")
                }
                throw e
            }
        }
    }

    private var downloadedVersion: NewVersion? = null

    private fun startDownload(newVersion: NewVersion) {
        if (downloadJob?.isActive == true || installJob?.isActive == true) return
        if (updateInstallSessions.hasPendingInstallations) {
            toast("上次安装结果尚未确认，请等待系统完成后检查应用版本")
            return
        }
        downloadedVersion = newVersion
        downloadStatusFlow.value = LoadStatus.Loading(0f)
        downloadJob = scope.launch(Dispatchers.IO) {
            var apkFile: File? = null
            try {
                apkFile = downloadUpdateApk(client, newVersion.absoluteDownloadUrl, FolderUtils.sharedDir, newVersion.fileSize, expectedSha256 = newVersion.sha256) {
                    downloadStatusFlow.value = LoadStatus.Loading(it)
                }
                verifyUpdatePackage(apkFile, newVersion.versionCode, newVersion.versionName)
                downloadStatusFlow.value = LoadStatus.Success(apkFile)
                apkFile = null // The success dialog now owns this file.
            } catch (error: TimeoutCancellationException) {
                downloadStatusFlow.value = LoadStatus.Failure(java.io.IOException("更新包下载超时，请重试", error))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                downloadStatusFlow.value = LoadStatus.Failure(error)
            } finally {
                apkFile?.delete()
                downloadJob = null
            }
        }
    }

    @Composable
    fun UpgradeDialog() {
        newVersionFlow.collectAsStateWithLifecycle().value?.let { newVersionVal ->
            val context = LocalContext.current
            val backupScope = rememberCoroutineScope()
            val text = remember {
                val logs = newVersionVal.versionLogs.takeWhile { v ->
                    v.code > META.versionCode
                }
                "v${META.versionName} -> v${newVersionVal.versionName}\n\n${
                    if (logs.size > 1) {
                        logs.joinToString("\n\n") { v -> "v${v.name}\n${v.desc}" }
                    } else if (logs.isNotEmpty()) {
                        logs.first().desc
                    } else {
                        newVersionVal.releaseNotes.orEmpty()
                    }
                }".trimEnd()
            }
            GkAlertDialog(
                title = {
                    Text(text = UiStrings.update_new_version)
                },
                text = {
                    Text(
                        text = text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                onDismissRequest = { newVersionFlow.value = null },
                confirmButton = {
                    GkActionButton(emphasized = true, onClick = {
                        newVersionFlow.value = null
                        startDownload(newVersionVal)
                    }) {
                        Text(text = UiStrings.update_download)
                    }
                },
                dismissButton = {
                    // 升级前先备份（对齐上游 98139c4b）：导出后拉起分享，弹窗保留，用户备份完再点下载
                    GkActionButton(onClick = throttle {
                        backupScope.launch {
                            try {
                                val activity = context as? MainActivity ?: return@launch
                                if (!activity.mainVm.dialogRequests.confirm(
                                        title = "确认分享完整备份",
                                        text = "完整备份包含 AI 服务商凭据和 API Key；仅分享给你信任的接收方。",
                                        confirmText = "继续分享",
                                    )
                                ) return@launch
                                val file = BackupManager.exportData()
                                activity.shareFile(file, "分享备份文件")
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                LogUtils.d("export backup before update failed", e)
                                toast("备份失败：${e.message ?: "未知错误"}")
                            }
                        }
                    }) {
                        Text(text = "先备份")
                    }
                    GkActionButton(onClick = { newVersionFlow.value = null }) {
                        Text(text = UiStrings.action_cancel)
                    }
                    if (!lastManual) {
                        GkActionButton(onClick = {
                            newVersionFlow.value = null
                            val code = newVersionVal.versionCode
                            if (code != null) ignoreVersionListFlow.update { it + code }
                            else ignoredReleaseNames.update { it + newVersionVal.versionName }
                            toast(UiStrings.update_version_ignored)
                        }) {
                            Text(text = UiStrings.action_ignore)
                        }
                    }
                },
            )
        }

        downloadStatusFlow.collectAsStateWithLifecycle().value?.let { downloadStatusVal ->
            when (downloadStatusVal) {
                is LoadStatus.Loading -> {
                    GkAlertDialog(
                        properties = androidx.compose.ui.window.DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                        ),
                        title = { Text(text = UiStrings.image_downloading) },
                        text = {
                            LinearProgressIndicator(
                                progress = downloadStatusVal.progress,
                            )
                        },
                        onDismissRequest = {},
                        confirmButton = {
                            GkActionButton(onClick = {
                                downloadJob?.cancel()
                                downloadStatusFlow.value = LoadStatus.Failure(
                                    Exception(UiStrings.download_abort)
                                )
                            }) {
                                Text(text = UiStrings.download_abort)
                            }
                        },
                    )
                }

                is LoadStatus.Failure -> {
                    GkAlertDialog(
                        title = { Text(text = UiStrings.download_failed) },
                        text = {
                            Text(text = downloadStatusVal.exception.let {
                                it.message ?: it.toString()
                            })
                        },
                        onDismissRequest = { downloadStatusFlow.value = null },
                        confirmButton = {
                            GkActionButton(onClick = {
                                downloadStatusFlow.value = null
                            }) {
                                Text(text = UiStrings.action_close)
                            }
                        },
                    )
                }

                is LoadStatus.Success -> {
                    GkAlertDialog(
                        title = { Text(text = UiStrings.download_complete) },
                        text = {
                            Text(text = UiStrings.update_ready_to_install)
                        },
                        onDismissRequest = { downloadStatusFlow.value = null },
                        dismissButton = {
                            GkActionButton(onClick = {
                                downloadStatusFlow.value = null
                            }) {
                                Text(text = UiStrings.action_close)
                            }
                        },
                        confirmButton = {
                            GkActionButton(onClick = throttle {
                                installDownloadedApk(downloadStatusVal.result)
                            }) {
                                Text(text = UiStrings.action_install)
                            }
                        })
                }
            }
        }
    }
    /** Every route is validated and backed up before handing the APK to an installer. */
    private fun installDownloadedApk(file: File) {
        if (installJob?.isActive == true) return
        if (updateInstallSessions.hasPendingInstallations) {
            toast("上次安装结果尚未确认，请等待系统完成后检查应用版本")
            return
        }
        val version = downloadedVersion ?: return
        installJob = scope.launch(Dispatchers.IO) {
            try {
                val verified = verifyUpdatePackage(file, version.versionCode, version.versionName)
                try {
                    BackupManager.createPreUpgradeBackup()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LogUtils.d("pre-upgrade backup failed", error)
                    toast("自动备份失败，已停止安装：" + (error.message ?: "未知错误"))
                    return@launch
                }
                // Backup may suspend while another UpdateStatus starts an installation.
                if (updateInstallSessions.hasPendingInstallations) {
                    toast("上次安装结果尚未确认，请等待系统完成后检查应用版本")
                    return@launch
                }
                if (!hasPrivilegedInstallCapability()) {
                    verifyUpdatePackage(file, version.versionCode, version.versionName)
                    withContext(Dispatchers.Main) { installApk(file) }
                    downloadStatusFlow.value = null
                    return@launch
                }
                when (val result = installPackagePrivileged(file, verified)) {
                    PrivilegedInstallResult.Installed -> downloadStatusFlow.value = null
                    is PrivilegedInstallResult.Failed -> {
                        if (result.canUseSystemInstaller && !updateInstallSessions.hasPendingInstallations) {
                            verifyUpdatePackage(file, version.versionCode, version.versionName)
                            toast("特权安装未完成，已改用系统安装")
                            withContext(Dispatchers.Main) { installApk(file) }
                            downloadStatusFlow.value = null
                        } else {
                            LogUtils.d("privileged install result unresolved", result.message)
                            toast(result.message)
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogUtils.d("update installation stopped", error)
                toast("安装已停止：" + (error.message ?: "未知错误"))
            } finally {
                installJob = null
            }
        }
    }

}


private fun installApk(file: File) {
    val uri = FileProvider.getUriForFile(
        app,
        "${app.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(uri, "application/vnd.android.package-archive")
    }
    app.tryStartActivity(intent)
}
