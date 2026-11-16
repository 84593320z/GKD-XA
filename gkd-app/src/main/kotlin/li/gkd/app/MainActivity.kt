package li.gkd.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import li.gkd.app.text.UiStrings
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

    private var runtimeInitialized = false

    private fun initializeRuntime() {
        // Register only after storage and application runtime are ready. LifecycleRegistry
        // replays the current state, so foreground tracking remains balanced after a slow migration.
        useMainActivityLifecycle()
        useLogLifecycle()
        lifecycleScope.launch {
            storeFlow.mapState(lifecycleScope) { s -> s.excludeFromRecents }.collect {
                app.activityManager.appTasks.forEach { task ->
                    task.setExcludeFromRecents(it)
                }
            }
        }
        applyPredictiveBackEnabled(storeFlow.value.enablePredictiveBack)
        activityResultHost.bind(mainVm.activityResults)
        permissionRequestHost.bind(mainVm.permissionRequests)
        mainVm.registerCurrent()
        runtimeInitialized = true
        StatusService.autoStart()
        if (storeFlow.value.enableBlockA11yAppList) {
            updateTopTaskAppId(META.appId)
        }
        setContent {
            AppRoot()
            LaunchedEffect(Unit) {
                intent?.let {
                    mainVm.handleIntent(it)
                    intent = null
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
        super.onCreate(savedInstanceState)
        addOnNewIntentListener { nextIntent ->
            intent = nextIntent
            if (runtimeInitialized) {
                mainVm.handleIntent(nextIntent)
                intent = null
            }
        }
        setContent {
            val failure by appStartupFailure.collectAsState()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (failure == null) {
                            CircularProgressIndicator()
                            Text("正在准备应用数据，请稍候…")
                        } else {
                            Text("应用数据准备失败")
                            Text(failure?.message ?: "请稍后重试")
                            Text("原有数据已保留。请检查存储空间后重试。")
                            Button(onClick = ::retryAppStartup) { Text("重试") }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            appRuntimeReady.await()
            initializeRuntime()
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
