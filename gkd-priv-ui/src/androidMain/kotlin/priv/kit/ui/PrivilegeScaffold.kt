// GKD-XA modification: native Miuix presentation / Android R resources; see gkd-priv-ui/README.md.
package priv.kit.ui

import android.Manifest
import android.view.ViewTreeObserver
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import top.yukonga.miuix.kmp.basic.FabPosition
import priv.kit.ui.miuix.PrivilegeMiuixTheme
import priv.kit.ui.miuix.PrivilegeScaffoldDefaults
import priv.kit.ui.miuix.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import priv.kit.ui.miuix.privilegeContentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import priv.kit.ui.component.PrivilegeTopBar
import java.util.UUID

/**
 * Displays the authorization page. [onViewPermissionSolutions] replaces the default action
 * that opens the localized permission troubleshooting page in the system URI handler.
 * [startupTutorialUrl] replaces the localized guide URL for every help topic.
 * [onViewStartupTutorial] overrides guide navigation; [onViewPermissionSolutions] takes
 * precedence for permission help. Topic anchors are stable across guide languages.
 * [onShowToast] handles all transient page messages on the UI thread, including action
 * feedback and controller errors. When null, messages use the built-in snackbar host.
 */
@Composable
public fun PrivilegeScaffold(
    modifier: Modifier = Modifier,
    viewModel: PrivilegeUiViewModel = viewModel(),
    topBar: @Composable () -> Unit = {
        PrivilegeAndroidTopBar(viewModel)
    },
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = {
        SnackbarHost(it)
    },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = PrivilegeMiuixTheme.colorScheme.background,
    contentColor: Color = privilegeContentColorFor(containerColor),
    contentWindowInsets: WindowInsets = PrivilegeScaffoldDefaults.contentWindowInsets,
    onViewPermissionSolutions: (() -> Unit)? = null,
    startupTutorialUrl: String? = null,
    onViewStartupTutorial: ((PrivilegeUiTutorialTopic) -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null,
) {
    val activity = LocalActivity.current!!
    // A localized LocalContext may no longer retain the host Activity.
    val registryOwner = checkNotNull(
        LocalActivityResultRegistryOwner.current ?: (activity as? ActivityResultRegistryOwner),
    ) {
        "PrivilegeScaffold requires an ActivityResultRegistryOwner. " +
            "Provide it via LocalActivityResultRegistryOwner."
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val view = LocalView.current
        val permissionHostId = rememberSaveable { UUID.randomUUID().toString() }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val startGateState by viewModel.startGateState.collectAsStateWithLifecycle()
        val visibleSystemPrompt by remember(viewModel.visibleSystemPrompt) {
            viewModel.visibleSystemPrompt.debouncedForDisplay()
        }.collectAsStateWithLifecycle(initialValue = null)
        val interactionEnabled = viewModel.canInteract(startGateState)
        val notificationPermission = if (isPrivilegeUiNotificationPermissionSupported()) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
        val requestPermissionContract = remember { ActivityResultContracts.RequestPermission() }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = requestPermissionContract,
            onResult = { granted ->
                val permissionState = notificationPermission?.let { permission ->
                    privilegeUiPermissionState(activity, permission)
                } ?: if (granted) {
                    PrivilegeUiPermissionState.Granted
                } else {
                    PrivilegeUiPermissionState.NotGranted.Denied
                }
                viewModel.completeNotificationPermissionRequest(permissionHostId, permissionState)
            },
        )
        val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
            contract = requestPermissionContract,
            onResult = { _ ->
                viewModel.completeLocalNetworkPermissionRequest(
                    hostId = permissionHostId,
                    permissionState = privilegeUiPermissionState(
                        activity, "android.permission.ACCESS_LOCAL_NETWORK",
                    ),
                )
            },
        )
        DisposableEffect(viewModel, permissionHostId, lifecycleOwner, view) {
            viewModel.registerPermissionHost(
                hostId = permissionHostId,
                resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                hasWindowFocus = view.hasWindowFocus(),
            )
            onDispose {
                viewModel.unregisterPermissionHost(
                    hostId = permissionHostId,
                    changingConfigurations = activity.isChangingConfigurations,
                )
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarScope = rememberCoroutineScope()
        val currentOnShowToast by rememberUpdatedState(onShowToast)
        fun showFeedback(message: String) {
            currentOnShowToast?.let { showToast ->
                showToast(message)
                return
            }
            snackbarScope.launch {
                snackbarHostState.newestSnackbarData()?.dismiss()
                snackbarHostState.showSnackbar(message = message)
            }
        }
        val batteryOptimizationPromptVisible by viewModel.batteryOptimizationPromptVisible.collectAsStateWithLifecycle()
        val staticTcpSwitchConfirmation by viewModel.staticTcpSwitchConfirmation.collectAsStateWithLifecycle()
        // Reading LocalConfiguration makes retained text follow application locale changes.
        androidx.compose.ui.platform.LocalConfiguration.current
        val screenState = state.toScreenState { it.asString(context) }.copy(
            wirelessAdbSupported = priv.kit.ui.state.isPrivilegeUiWirelessAdbSupported(),
            adbTcpPolicy = viewModel.config.adbTcpPolicy,
            tcpPort = viewModel.config.tcpPort,
            batteryOptimizationPromptVisible = batteryOptimizationPromptVisible,
            staticTcpSwitchConfirmation = staticTcpSwitchConfirmation,
        )
        val actions = remember(viewModel, context) { viewModel.screenActions(context) }
        LaunchedEffect(Unit) {
            viewModel.permissionRequests.collect { request ->
                when (request) {
                    is PrivilegeUiPermissionRequest.Notification -> {
                        if (request.wasLaunched) {
                            request.awaitCompletion()
                        } else {
                            if (notificationPermission == null) {
                                viewModel.cancelPermissionRequest(permissionHostId, request)
                            } else {
                                val permissionState = privilegeUiPermissionState(activity, notificationPermission)
                                if (permissionState.shouldLaunchPermissionRequest()) {
                                    if (request.tryMarkLaunched(permissionHostId)) {
                                        runCatching {
                                            notificationPermissionLauncher.launch(notificationPermission)
                                        }.onSuccess {
                                            markPrivilegeUiPermissionRequested(notificationPermission)
                                        }.onFailure {
                                            viewModel.cancelPermissionRequest(permissionHostId, request)
                                        }
                                    }
                                    request.awaitCompletion()
                                } else {
                                    viewModel.completeUnlaunchedNotificationPermissionRequest(
                                        permissionHostId,
                                        request,
                                        permissionState,
                                    )
                                }
                            }
                        }
                    }
                    is PrivilegeUiPermissionRequest.LocalNetwork -> {
                        if (request.tryMarkLaunched(permissionHostId)) {
                            runCatching {
                                localNetworkPermissionLauncher.launch(request.permission)
                            }.onSuccess {
                                markPrivilegeUiPermissionRequested(request.permission)
                            }.onFailure {
                                viewModel.cancelPermissionRequest(permissionHostId, request)
                            }
                        }
                        request.awaitCompletion()
                    }
                }
            }
        }
        LaunchedEffect(viewModel, context, onShowToast != null) {
            if (currentOnShowToast != null) {
                snackbarHostState.newestSnackbarData()?.dismiss()
            }
            viewModel.snackbarTexts.collect { text ->
                val message = text.asString(context)
                val showToast = currentOnShowToast
                if (showToast != null) {
                    showToast(message)
                } else {
                    snackbarHostState.newestSnackbarData()?.dismiss()
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
        LifecycleEventEffect(
            event = Lifecycle.Event.ON_RESUME,
            lifecycleOwner = lifecycleOwner,
            onEvent = {
                viewModel.dispatchHostResume(
                    hostId = permissionHostId,
                    hasWindowFocus = view.hasWindowFocus(),
                )
            },
        )
        LifecycleEventEffect(
            event = Lifecycle.Event.ON_PAUSE,
            lifecycleOwner = lifecycleOwner,
            onEvent = {
                viewModel.dispatchHostPause(permissionHostId)
            },
        )
        DisposableEffect(viewModel, permissionHostId, lifecycleOwner, view) {
            val lifecycle = lifecycleOwner.lifecycle
            val refreshObserver = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                viewModel.dispatchHostWindowFocus(
                    hostId = permissionHostId,
                    hasWindowFocus = hasFocus,
                    resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                )
            }
            val viewTreeObserver = view.viewTreeObserver
            viewTreeObserver.addOnWindowFocusChangeListener(refreshObserver)
            onDispose {
                val removalObserver = if (viewTreeObserver.isAlive) {
                    viewTreeObserver
                } else {
                    view.viewTreeObserver
                }
                if (removalObserver.isAlive) {
                    removalObserver.removeOnWindowFocusChangeListener(refreshObserver)
                }
            }
        }

        PrivilegeScreen(
            state = screenState,
            actions = actions,
            interactionEnabled = interactionEnabled,
            showFeedback = ::showFeedback,
            onViewPermissionSolutions = onViewPermissionSolutions,
            startupTutorialUrl = startupTutorialUrl,
            onViewStartupTutorial = onViewStartupTutorial,
            systemPrompt = visibleSystemPrompt?.takeIf { it.ownerHostId == permissionHostId }?.prompt?.let {
                PrivilegeUiPromptState(it.title.asString(context), it.message.asString(context))
            },
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            snackbarHostState = snackbarHostState,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
        )
    }
}

@Composable
private fun PrivilegeAndroidTopBar(viewModel: PrivilegeUiViewModel) {
    val dispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    PrivilegeTopBar(onBack = {
        if (!viewModel.dispatchBackClick()) dispatcher?.onBackPressed()
    })
}
