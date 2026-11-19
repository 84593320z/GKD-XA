package li.gkd.app.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import li.gkd.app.text.UiStrings
import li.gkd.app.MainActivity
import li.gkd.app.util.FolderUtils
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.ToastUtils.toast

class ShareLogState(
    private val scope: CoroutineScope,
    private val githubUpload: GithubUploadState,
) {
    private val visibleFlow = MutableStateFlow(false)
    private val snapshotDaysFlow = MutableStateFlow(0)

    fun show() {
        visibleFlow.value = true
    }

    private fun dismiss() {
        visibleFlow.value = false
    }

    private fun share(context: MainActivity) {
        val snapshotDays = snapshotDaysFlow.value
        dismiss()
        scope.launchUi {
            val logZipFile = withContext(Dispatchers.IO) { FolderUtils.buildLogFile(snapshotDays) }
            context.shareFile(logZipFile, UiStrings.logs_share_file)
        }
    }

    private fun save(context: MainActivity) {
        val snapshotDays = snapshotDaysFlow.value
        dismiss()
        scope.launchUi {
            FolderUtils.withTemporaryZip(
                create = { withContext(Dispatchers.IO) { FolderUtils.buildLogFile(snapshotDays) } },
                delete = FolderUtils::deleteSharedFile,
            ) { logZipFile ->
                context.saveFileToDownloads(logZipFile)
            }
        }
    }

    private fun upload() {
        val snapshotDays = snapshotDaysFlow.value
        dismiss()
        val item = GithubUploadItem(
            label = UiStrings.logs_title,
            getFile = { FolderUtils.buildLogFile(snapshotDays) },
            showHref = { "https://i.gkd.li/log/${it.id}" },
            releaseFile = FolderUtils::deleteSharedFile,
        )
        if (!githubUpload.startTask(item)) toast(UiStrings.upload_busy)
    }

    @Composable
    fun Render() {
        val visible by visibleFlow.collectAsStateWithLifecycle()
        if (visible) {
            val context = LocalActivity.current as MainActivity
            GkSettingsDialog(title = UiStrings.logs_title, onDismissRequest = ::dismiss) {
                val snapshotDays by snapshotDaysFlow.collectAsStateWithLifecycle()
                Text(UiStrings.logs_snapshot_range, style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Column(Modifier.selectableGroup()) {
                    listOf(0, 1, 3, 7).forEach { days ->
                        BasicComponent(
                            modifier = Modifier.semantics { selected = days == snapshotDays },
                            title = if (days == 0) UiStrings.logs_snapshot_none else UiStrings.logs_snapshot_days(days),
                            role = Role.RadioButton,
                            onClick = { snapshotDaysFlow.value = days },
                            endActions = {
                                RadioButton(selected = days == snapshotDays, onClick = null,
                                    modifier = Modifier.clearAndSetSemantics {})
                            },
                        )
                    }
                }
                GkSettingItem(UiStrings.action_share, imageVector = GkIcons.Share,
                    onClick = { share(context) })
                GkSettingItem(UiStrings.action_save_to_downloads, imageVector = GkIcons.ArrowDownward,
                    onClick = { save(context) })
                GkSettingItem(UiStrings.upload_generate_link, imageVector = GkIcons.Link,
                    onClick = ::upload)
            }
        }
    }
}
