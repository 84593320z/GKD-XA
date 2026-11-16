@file:Suppress("CAST_NEVER_SUCCEEDS")

package li.gkd.app.platform

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerSessionHidden
import android.content.pm.PackageInstallerSessionParamsHidden
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.os.IBinder
import android.content.pm.PackageManagerHidden
import android.os.Process
import androidx.core.content.IntentCompat
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import li.gkd.aidl.IInstallResultListener
import li.gkd.app.app
import li.gkd.app.priv.InstallCompletion
import li.gkd.app.priv.InstallEvent
import li.gkd.app.priv.InstallSessionCallback
import li.gkd.app.priv.InstallSessionHandle
import li.gkd.app.priv.InstallSessionRegistry
import li.gkd.app.priv.InstallSessionTerminal
import li.gkd.app.priv.PrivilegeOwnerLifecycle
import li.gkd.app.util.UpdateApkIdentity
import li.gkd.app.util.validateUpdateApkIdentity
import java.security.MessageDigest
import li.gkd.app.priv.currentUserId
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.util.AndroidTarget
import priv.kit.core.Privilege
import priv.kit.core.binder.PrivilegeBinderWrapper

/**
 * 特权静默安装更新（移植上游 8225f46a 的 PackageInstall.android）：
 * 借特权服务的 INSTALL_PACKAGES 权限走 IPackageInstaller 写会话安装，
 * 安装成功后由服务端里的 InstallRestartReceiver 拉回 MainActivity。
 * 调用前用 [hasPrivilegedInstallCapability] 判断，不满足时走系统安装器兜底。
 */
fun hasPrivilegedInstallCapability(): Boolean =
    try {
        privilegeContextFlow.value != null &&
            Privilege.checkServerPermission(Manifest.permission.INSTALL_PACKAGES) ==
            PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

private fun systemAppMarketPackage(): String? {
    val manager = app.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET)
    val candidates =
        manager
            .queryIntentActivities(intent, 0)
            .mapNotNull { result ->
                val activity = result.activityInfo
                val info = activity.applicationInfo
                activity.packageName.takeIf {
                    activity.enabled &&
                        activity.exported &&
                        info.enabled &&
                        info.flags and
                            (ApplicationInfo.FLAG_SYSTEM or
                                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                }
            }
            .toSet()
    if (candidates.size <= 1) return candidates.singleOrNull()
    // Resolve the user's default only if it is one of the preinstalled market candidates.
    return manager
        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
        ?.takeIf { it in candidates }
}

private const val INSTALL_RESULT_TIMEOUT_MILLIS = 2 * 60 * 1000L
private val installMutex = Mutex()
internal val updateInstallSessions = InstallSessionRegistry(PrivilegeOwnerLifecycle::setUpdateInstallationActive)

internal data class VerifiedUpdateApk(val identity: UpdateApkIdentity)
internal sealed interface PrivilegedInstallResult {
    data object Installed : PrivilegedInstallResult
    data class Failed(val canUseSystemInstaller: Boolean, val message: String) : PrivilegedInstallResult
}

@Suppress("DEPRECATION")
private fun PackageInfo.updateIdentity(): UpdateApkIdentity {
    // Requiring the current signer set is deliberately conservative: an unrecognised rotation
    // needs a separately reviewed trust migration, never trust a tag/download URL instead.
    val signers = if (AndroidTarget.P) signingInfo?.apkContentsSigners else signatures
    return UpdateApkIdentity(
        packageName = packageName,
        versionCode = PackageInfoCompat.getLongVersionCode(this),
        versionName = versionName,
        signerSha256 = signers.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }.toSet(),
    )
}

@Suppress("DEPRECATION")
internal fun verifyUpdatePackage(file: File, expectedVersionCode: Int?, expectedVersionName: String): VerifiedUpdateApk {
    require(file.isFile && file.length() > 0) { "更新包文件不存在或为空" }
    val flags = if (AndroidTarget.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val archive = requireNotNull(app.packageManager.getPackageArchiveInfo(file.absolutePath, flags)) {
        "无法读取更新包元数据或签名"
    }.updateIdentity()
    val installed = app.packageManager.getPackageInfo(app.packageName, flags).updateIdentity()
    return VerifiedUpdateApk(validateUpdateApkIdentity(archive, installed, expectedVersionCode, expectedVersionName))
}

internal suspend fun installPackagePrivileged(
    file: File,
    verified: VerifiedUpdateApk,
): PrivilegedInstallResult = withContext(Dispatchers.IO) {
    installMutex.withLock {
        if (updateInstallSessions.hasPendingInstallations) {
            return@withLock PrivilegedInstallResult.Failed(false, "上次安装结果尚未确认，请等待系统完成后检查应用版本")
        }
        // Recheck the actual APK immediately before sending bytes to the privileged process.
        require(verifyUpdatePackage(file, null, requireNotNull(verified.identity.versionName)) == verified)
        val context = privilegeContextFlow.value
            ?: return@withLock PrivilegedInstallResult.Failed(true, "特权服务未连接")
        if (!hasPrivilegedInstallCapability()) {
            return@withLock PrivilegedInstallResult.Failed(true, "特权服务没有 INSTALL_PACKAGES 权限")
        }
        var installer: IPackageInstaller? = null
        var sessionId: Int? = null
        var commitStarted = false
        var sessionHandle: InstallSessionHandle? = null
        val completion = InstallCompletion<Intent>()
        val linkedBinders = mutableListOf<IBinder>()
        val deathRecipient = IBinder.DeathRecipient { completion.disconnected() }

        fun abandonUncommitted(): Boolean {
            if (sessionId == null) return true
            return runCatching { requireNotNull(installer).abandonSession(requireNotNull(sessionId)) }.isSuccess
        }

        fun terminalResult(): PrivilegedInstallResult? = when (val terminal = sessionHandle?.terminal) {
            InstallSessionTerminal.Success -> PrivilegedInstallResult.Installed
            is InstallSessionTerminal.Failure -> PrivilegedInstallResult.Failed(true, terminal.message)
            null -> null
        }

        fun reconcile(message: String): PrivilegedInstallResult {
            terminalResult()?.let { return it }
            if (!commitStarted) {
                val abandoned = abandonUncommitted()
                if (abandoned) sessionHandle?.forget()
                return PrivilegedInstallResult.Failed(abandoned, message)
            }
            // A thrown commit RPC can still have reached system_server. Never resubmit based on
            // an exception alone. Check both the installed identity and the live session first.
            val installed = runCatching {
                @Suppress("DEPRECATION")
                val flags = if (AndroidTarget.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
                app.packageManager.getPackageInfo(app.packageName, flags).updateIdentity()
            }.getOrNull()
            val sessionProbe = runCatching { requireNotNull(installer).getSessionInfo(requireNotNull(sessionId)) }
            if (installed == verified.identity) {
                sessionHandle?.settle(InstallSessionTerminal.Success)
            }
            terminalResult()?.let { return it }
            // Session absence alone can race successful replacement. Only a terminal failure
            // callback authorises a post-commit fallback; all uncertain outcomes stay closed.
            // Keep process pending/suppression ownership for a late callback after await ends.
            val detail = if (sessionProbe.isSuccess && sessionProbe.getOrNull() != null) "系统仍保留安装会话" else "无法确认最终安装结果"
            return PrivilegedInstallResult.Failed(false, message + "；" + detail + "。请等待系统完成后检查应用版本，再重试。")
        }

        try {
            val uid = context.serverInfo.uid
            val isRootOrShell = uid == 0 || uid == Process.SHELL_UID
            val marketPackage = systemAppMarketPackage()
            val installerPackage = if (isRootOrShell) "com.android.shell" else {
                requireNotNull(app.packageManager.getPackagesForUid(uid)?.firstOrNull()) { "无法确定安装器包名" }
            }
            val packageManager = IPackageManager.Stub.asInterface(
                requireNotNull(PrivilegeBinderWrapper.fromSystemService("package")),
            )
            val packageInstaller = IPackageInstaller.Stub.asInterface(
                PrivilegeBinderWrapper.fromBinder(packageManager.packageInstaller.asBinder()),
            )
            installer = packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(verified.identity.packageName)
                setSize(file.length())
                val hidden = this as PackageInstallerSessionParamsHidden
                hidden.installFlags = hidden.installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING
                if (AndroidTarget.P) hidden.installerPackageName = marketPackage
                if (AndroidTarget.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val sessionInstallerPackage = if (!AndroidTarget.P && isRootOrShell) marketPackage ?: installerPackage else installerPackage
            val createdId = if (AndroidTarget.S) {
                packageInstaller.createSession(params, sessionInstallerPackage, null, currentUserId)
            } else {
                packageInstaller.createSession(params, sessionInstallerPackage, currentUserId)
            }
            sessionId = createdId
            val handle = updateInstallSessions.register(createdId, file.absolutePath)
            sessionHandle = handle
            val callback = InstallSessionCallback(handle, completion)
            listOf(context.serverLifecycleBinder, context.installCallbackBinder).distinct().forEach { binder ->
                binder.linkToDeath(deathRecipient, 0)
                linkedBinders += binder
            }
            val sessionBinder = IPackageInstallerSession.Stub.asInterface(
                PrivilegeBinderWrapper.fromBinder(packageInstaller.openSession(createdId).asBinder()),
            )
            (PackageInstallerSessionHidden(sessionBinder) as PackageInstaller.Session).use { session ->
                session.openWrite("base.apk", 0, file.length()).use { output ->
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            ensureActive()
                            output.write(buffer, 0, count)
                        }
                    }
                    ensureActive()
                    session.fsync(output)
                }
                ensureActive()
                val receiver = context.createInstallResultSender(installerPackage, object : IInstallResultListener.Stub() {
                    override fun onResult(result: Intent) {
                        when (val status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
                            PackageInstaller.STATUS_SUCCESS -> callback.accept(InstallEvent.Success)
                            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                                val intent = IntentCompat.getParcelableExtra(result, Intent.EXTRA_INTENT, Intent::class.java)
                                if (intent != null) callback.accept(InstallEvent.Pending(intent))
                                else completion.disconnected()
                            }
                            PackageInstaller.STATUS_FAILURE,
                            PackageInstaller.STATUS_FAILURE_BLOCKED,
                            PackageInstaller.STATUS_FAILURE_ABORTED,
                            PackageInstaller.STATUS_FAILURE_INVALID,
                            PackageInstaller.STATUS_FAILURE_CONFLICT,
                            PackageInstaller.STATUS_FAILURE_STORAGE,
                            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                            PackageInstaller.STATUS_FAILURE_TIMEOUT -> callback.accept(InstallEvent.Failure(
                                result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.takeIf { it.isNotBlank() }
                                    ?: "特权安装失败，状态码：" + status,
                            ))
                            else -> completion.disconnected() // Missing/unrecognised status is not a failure.
                        }
                    }
                })
                ensureActive()
                commitStarted = true
                session.commit(receiver)
            }
            when (val result = completion.await(INSTALL_RESULT_TIMEOUT_MILLIS) { intent ->
                withContext(Dispatchers.Main) { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) {
                InstallEvent.Success -> PrivilegedInstallResult.Installed
                is InstallEvent.Failure -> PrivilegedInstallResult.Failed(true, result.message)
                is InstallEvent.Pending -> error("Pending action did not reach a terminal result")
            }
        } catch (_: TimeoutCancellationException) {
            reconcile("等待安装回调超时")
        } catch (error: CancellationException) {
            reconcile("安装等待已取消")
            throw error
        } catch (error: Exception) {
            reconcile(error.message ?: "特权安装异常")
        } finally {
            completion.close()
            linkedBinders.forEach { binder -> runCatching { binder.unlinkToDeath(deathRecipient, 0) } }
        }
    }
}
