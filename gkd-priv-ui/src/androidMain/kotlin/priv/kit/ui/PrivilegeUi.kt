package priv.kit.ui

import android.os.Process
import androidx.annotation.MainThread
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import priv.kit.core.Privilege
import priv.kit.core.PrivilegeServerInfo
import priv.kit.core.internal.runtime.PrivilegeContext
import priv.kit.core.internal.runtime.PrivilegeRuntimeStartCoordinator
import priv.kit.ui.runtime.PrivilegeUiDesiredEnabledStore
import priv.kit.ui.runtime.PrivilegeUiSilentStartRunner
import priv.kit.ui.runtime.PrivilegeUiStartGate
import priv.kit.ui.runtime.PrivilegeUiStartMethodStore
import priv.kit.ui.runtime.preparePrivilegeUiExternalShellScript
import priv.kit.ui.runtime.requireValidManualShellScriptPath
import kotlin.time.Duration.Companion.milliseconds

public object PrivilegeUi {
    internal var localeOverride: Locale? = null
        private set

    /**
     * Sets the language used by the pairing service, including its notification channel,
     * actions and accessibility text. Refreshes active notifications without restarting pairing
     * or clearing the entered code. Compose content continues to use the host's language context.
     *
     * Call on the main thread at application initialization and when the host language changes.
     * This setting is process-local and is not persisted. The default, `null`, follows Android's
     * application language configuration. Already-materialized external text is not translated.
     */
    @MainThread
    public fun setLocale(locale: Locale?) {
        if (localeOverride == locale) return
        localeOverride = locale
        PrivilegeAdbPairingService.refreshLocale()
    }

    private val desiredEnabledLock = Any()
    private val desiredEnabledStore by lazy {
        PrivilegeUiDesiredEnabledStore(PrivilegeContext.require())
    }
    private val mutableDesiredEnabled by lazy {
        MutableStateFlow(desiredEnabledStore.read())
    }

    /**
     * Whether the user still wants automatic recovery of the Privileged Server.
     *
     * A matching successful foreground start enables this value. A confirmed stop or the
     * built-in disable action clears it. Disconnection, server death, and failed silent recovery
     * leave it unchanged. This state does not indicate whether a server is currently connected;
     * observe [Privilege.serverState] for the process-wide connection state. Its initial value is
     * read synchronously from app-private storage on first access.
     */
    public val desiredEnabled: StateFlow<Boolean> by lazy {
        mutableDesiredEnabled.asStateFlow()
    }

    internal fun setDesiredEnabled(enabled: Boolean) {
        synchronized(desiredEnabledLock) {
            desiredEnabledStore.write(enabled)
            mutableDesiredEnabled.value = enabled
        }
    }

    /**
     * Creates or updates the external manual startup script and returns its actual absolute file.
     * Uses [scriptPath] relative to the app's external `Android/data/<package>` directory;
     * the returned path is not rewritten to the UI's `/sdcard` alias.
     * Pass `priv.sh` or [PrivilegeUiConfig.manualShellScriptPath].
     * Invalid relative paths throw [IllegalArgumentException].
     *
     * Returns `null` for a `null` path, a nonzero Android user, unavailable external storage, or a
     * file read/write failure. The first two cases skip native command resolution and storage access.
     * Failures resolving the native starter propagate to the caller.
     *
     * Runs synchronously on the calling thread, including native starter resolution (which may
     * inspect APKs on first access), content comparison, and any needed directory creation and
     * atomic write with file sync. The caller chooses the execution context. Matching content is
     * reused; missing or changed scripts are written. Concurrent preparation in this process is
     * serialized, so application preloading and UI initialization do not repeat the same write.
     * This function does not create UI or start a server, and is independent of automatic recovery
     * and the configured startup tabs.
     */
    public fun prepareExternalShellScript(scriptPath: String?): File? {
        requireValidManualShellScriptPath(scriptPath)
        return preparePrivilegeUiExternalShellScript(
            nativeStarterCommand = { Privilege.nativeStarterCommand },
            ownerUserId = Process.myUserHandle().hashCode(),
            scriptPath = scriptPath,
            externalFilesDirectory = { PrivilegeContext.require().getExternalFilesDir(null) },
        )
    }

    /**
     * Reads the method saved by the last successful foreground start in this UI. Returns `null`
     * when there is no readable, recognized record. Reads synchronously on the calling thread,
     * independently of the automatic recovery setting and current startup configuration.
     */
    public fun getLastStartMethod(): PrivilegeUiStartMethod? {
        val context = PrivilegeContext.require()
        return try {
            PrivilegeUiStartMethodStore(context).read()
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Replays the last successful foreground startup method with the supplied current config.
     *
     * By default, replay runs only when a previous accepted server launch has enabled automatic
     * recovery. A missing or invalid setting is treated as disabled. Pass
     * [ignoreAutomaticRecoverySetting] only when the application intentionally needs to replay
     * regardless of that user setting.
     *
     * This function does not create UI, fall back to another method, invoke Android permission
     * launchers, pair Wireless ADB, submit an ADB public key for authorization, or call an external
     * provider's authorization request. When no server is already connected, a missing, disabled,
     * unavailable, or unauthorized method returns `null`. A root manager remains responsible for
     * any UI caused by an expired Root grant. Foreground and silent startup are mutually exclusive;
     * this function returns `null` when foreground startup owns the process-local gate. Existing
     * built-in UI remains disabled until a silent attempt releases the gate and runtime state has
     * been refreshed. A multi-process app must call this function from only its designated Priv Kit
     * process.
    */
    public suspend fun startSilently(
        config: PrivilegeUiConfig,
        ignoreAutomaticRecoverySetting: Boolean = false,
    ): PrivilegeServerInfo? = withContext(Dispatchers.IO) {
        val applicationContext = PrivilegeContext.require()
        if (!ignoreAutomaticRecoverySetting) {
            val automaticRecoveryEnabled = runCatching {
                PrivilegeUiDesiredEnabledStore(applicationContext).read()
            }.getOrDefault(false)
            if (!automaticRecoveryEnabled) return@withContext null
        }
        val permit = PrivilegeUiStartGate.tryAcquireSilent() ?: return@withContext null
        permit.use {
            connectedOrReadyServerOrNull()?.let { return@withContext it }
            val preflight = PrivilegeRuntimeStartCoordinator.beginPreflight()
            if (preflight.remainingReconnectGraceMillis > 0L) {
                delay(preflight.remainingReconnectGraceMillis.milliseconds)
            }
            connectedOrReadyServerOrNull()?.let { return@withContext it }
            val method = try {
                PrivilegeUiStartMethodStore(applicationContext).read()
            } catch (_: Exception) {
                null
            } ?: return@withContext null
            val runtimeStartLease =
                PrivilegeRuntimeStartCoordinator.tryCommitClientStart(preflight)
                    ?: return@withContext connectedOrReadyServerOrNull()
            try {
                runtimeStartLease.use {
                    val launch = PrivilegeRuntimeStartCoordinator.beginClientLaunch(runtimeStartLease)
                        ?: return@withContext connectedOrReadyServerOrNull()
                    PrivilegeUiSilentStartRunner(
                        context = applicationContext,
                        config = config,
                    ).start(method, launch)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun connectedOrReadyServerOrNull(): PrivilegeServerInfo? =
        try {
            when {
                Privilege.pingServer() -> Privilege.getServerInfo()
                else -> Privilege.connectReadyServer()
            }
        } catch (_: Exception) {
            null
        }
}
