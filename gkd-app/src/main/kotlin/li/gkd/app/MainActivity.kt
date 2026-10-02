package li.gkd.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import li.gkd.app.text.UiStrings
import li.gkd.app.platform.lifecycle.onCreated
import li.gkd.app.platform.lifecycle.useMainActivityLifecycle
import li.gkd.app.platform.lifecycle.useLogLifecycle
import li.gkd.app.permission.PermissionRequests
import li.gkd.app.permission.PermissionStates
import li.gkd.app.service.StatusService
import li.gkd.app.service.updateTopTaskAppId
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.share.ActivityImeController
import li.gkd.app.ui.share.ActivityResultRequests
import li.gkd.app.ui.app.AppRoot
import li.gkd.app.util.AndroidTarget
import li.gkd.app.util.SystemDownloads
import li.gkd.app.util.applyPredictiveBackEnabled
import li.gkd.app.util.mapState
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.tryStartActivity
import java.io.File

class MainActivity : ComponentActivity() {
    val mainVm by viewModels<MainViewModel>()
    val imeController = ActivityImeController(this)
    private val activityResultHost = ActivityResultRequests.Host(this)
    private val permissionRequestHost = PermissionRequests.Host(this)

    init {
        useMainActivityLifecycle()
        useLogLifecycle()
        onCreated {
            lifecycleScope.launch {
                storeFlow.mapState(lifecycleScope) { s -> s.excludeFromRecents }.collect {
                    app.activityManager.appTasks.forEach { task ->
                        task.setExcludeFromRecents(it)
                    }
                }
            }
        }
    }

    fun shareFile(file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.provider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        tryStartActivity(Intent.createChooser(intent, title))
    }

    suspend fun saveFileToDownloads(file: File) {
        if (!mainVm.permissionRequests.ensurePermissions(PermissionStates.writeExternalStorage)) {
            return
        }
        val savedName = SystemDownloads.save(file) ?: return
        toast(UiStrings.file_saved_to_downloads(savedName))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        fixTransparentNavigationBar()
        applyPredictiveBackEnabled(storeFlow.value.enablePredictiveBack)
        super.onCreate(savedInstanceState)
        activityResultHost.bind(mainVm.activityResults)
        permissionRequestHost.bind(mainVm.permissionRequests)
        mainVm.registerCurrent()
        addOnNewIntentListener {
            mainVm.handleIntent(it)
            intent = null
        }
        StatusService.autoStart()
        if (storeFlow.value.enableBlockA11yAppList) {
            updateTopTaskAppId(META.appId)
        }
        setContent {
            AppRoot()
            LaunchedEffect(null) {
                intent?.let {
                    mainVm.handleIntent(it)
                    intent = null
                }
            }
        }
    }

    private fun fixTransparentNavigationBar() {
        // 修复在浅色主题下导航栏背景不透明的问题
        if (AndroidTarget.Q) {
            window.isNavigationBarContrastEnforced = false
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
    }
}
