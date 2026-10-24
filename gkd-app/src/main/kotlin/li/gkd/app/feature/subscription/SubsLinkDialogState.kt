package li.gkd.app.feature.subscription

import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.data.subscription.SubscriptionAddStage
import li.gkd.app.data.subscription.SubscriptionRepository
import li.gkd.app.data.subscription.SubscriptionResult
import li.gkd.app.ui.share.message
import li.gkd.app.util.NetworkUtils
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.ToastUtils.toast
import kotlin.coroutines.resume
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.autoFocus

private data class SubsLinkDialogRequest(
    val initialValue: String,
    val existingUrls: Set<String>,
    val value: String,
    val stage: SubscriptionAddStage? = null,
    val error: String? = null,
)

private fun stageText(stage: SubscriptionAddStage): String = when (stage) {
    SubscriptionAddStage.Downloading -> UiStrings.subscription_adding_download
    SubscriptionAddStage.Parsing -> UiStrings.subscription_adding_parse
    SubscriptionAddStage.Checking -> UiStrings.subscription_adding_check
    SubscriptionAddStage.Saving -> UiStrings.subscription_adding_save
}

class SubsLinkDialogState(
    private val onOpenHelp: () -> Unit,
    private val requestLocalNetworkPermission: suspend () -> Boolean,
) {
    private val requestFlow = MutableStateFlow<SubsLinkDialogRequest?>(null)
    private val requestMutex = Mutex()
    private var requestScope: CoroutineScope? = null
    private var addingJob: Job? = null
    private var currentContinuation: CancellableContinuation<String?>? = null

    private fun complete(value: String?) {
        val continuation = currentContinuation ?: return
        if (continuation.isActive) {
            requestFlow.value = null
            continuation.resume(value)
        }
    }

    private fun updateValue(value: String) {
        val request = requestFlow.value ?: return
        requestFlow.value = request.copy(value = value.trim(), error = null)
    }

    private fun submit(request: SubsLinkDialogRequest) {
        val value = request.value
        if (!URLUtil.isNetworkUrl(value)) {
            toast(UiStrings.link_invalid)
            return
        }
        if (request.initialValue.isNotEmpty() && request.initialValue == value) {
            toast(UiStrings.unchanged)
            complete(null)
            return
        }
        if (value in request.existingUrls) {
            toast(UiStrings.subscription_duplicate_link)
            return
        }
        // 修改链接由调用方处理；新增直接在对话框里完成，才能显示下载/解析阶段
        if (request.initialValue.isNotEmpty()) {
            complete(value)
            return
        }
        if (addingJob?.isActive == true) return
        requestFlow.value = request.copy(stage = SubscriptionAddStage.Checking, error = null)
        addingJob = requestScope?.launch {
            if (NetworkUtils.isLocalNetworkUrl(value) && !requestLocalNetworkPermission()) {
                requestFlow.value = request.copy(stage = null)
                return@launch
            }
            val result = SubscriptionRepository.addOrModifyRemote(value) { stage ->
                withContext(Dispatchers.Main.immediate) {
                    requestFlow.value = requestFlow.value?.copy(stage = stage)
                }
            }
            if (result is SubscriptionResult.Success) {
                result.message?.let { toast(it) }
                complete(value)
            } else {
                requestFlow.value = request.copy(stage = null, error = result.message)
            }
        }
    }

    private fun cancel() {
        // 进入落盘阶段后不允许取消，否则会出现只写了一半的订阅
        if (requestFlow.value?.stage == SubscriptionAddStage.Saving) return
        addingJob?.cancel()
        complete(null)
    }

    private fun openHelp() {
        cancel()
        onOpenHelp()
    }

    suspend fun request(initialValue: String = ""): String? {
        val existingUrls = withContext(Dispatchers.IO) {
            SubscriptionRepository.existingUpdateUrls()
        }
        val value = withContext(Dispatchers.Main.immediate) {
            requestMutex.withLock {
                try {
                    requestFlow.value = SubsLinkDialogRequest(
                        initialValue = initialValue,
                        existingUrls = existingUrls,
                        value = initialValue,
                    )
                    coroutineScope {
                        requestScope = this
                        suspendCancellableCoroutine { continuation ->
                            currentContinuation = continuation
                        }
                    }
                } finally {
                    addingJob?.cancel()
                    addingJob = null
                    requestScope = null
                    currentContinuation = null
                    requestFlow.value = null
                }
            }
        }
        // 新增链接已在对话框里申请过局域网权限；这里只兜底「修改链接」这条路径
        if (initialValue.isNotEmpty() && value != null &&
            NetworkUtils.isLocalNetworkUrl(value) && !requestLocalNetworkPermission()
        ) {
            return null
        }
        return value
    }

    @Composable
    fun Render() {
        val request by requestFlow.collectAsStateWithLifecycle()
        val currentRequest = request
        if (currentRequest != null) {
            val busy = currentRequest.stage != null
            val saving = currentRequest.stage == SubscriptionAddStage.Saving
            GkAlertDialog(
                properties = DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = !saving,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (currentRequest.initialValue.isNotEmpty()) {
                                UiStrings.subscription_edit
                            } else {
                                UiStrings.subscription_add
                            },
                        )
                        GkIconButton(
                            imageVector = GkIcons.HelpOutline,
                            contentDescription = UiStrings.subscription_help,
                            onClick = throttle(::openHelp),
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentRequest.value,
                            onValueChange = ::updateValue,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .autoFocus(),
                            enabled = !busy,
                            placeholder = {
                                Text(text = UiStrings.subscription_link_input_hint)
                            },
                            isError = currentRequest.value.isNotEmpty() &&
                                    !URLUtil.isNetworkUrl(currentRequest.value),
                        )
                        currentRequest.stage?.let { stage ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(text = stageText(stage))
                            }
                        }
                        currentRequest.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                onDismissRequest = ::cancel,
                confirmButton = {
                    TextButton(
                        enabled = !busy && currentRequest.value.isNotEmpty(),
                        onClick = throttle {
                            submit(currentRequest)
                        },
                    ) {
                        Text(text = UiStrings.action_ok)
                    }
                },
                dismissButton = {
                    TextButton(enabled = !saving, onClick = ::cancel) {
                        Text(text = UiStrings.action_cancel)
                    }
                },
            )
        }
    }
}
