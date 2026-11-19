package li.gkd.app.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import li.gkd.app.META
import li.gkd.app.MainActivity
import li.gkd.app.MainViewModel
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkTextListDialog
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.util.IntentUtils
import li.gkd.app.util.PLAY_STORE_URL
import li.gkd.app.util.ShortUrlSet
import li.gkd.app.util.format
import li.gkd.app.util.getShareApkFile
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutDialogs(
    showVersionInfo: Boolean,
    onDismissVersionInfo: () -> Unit,
    showShareApp: Boolean,
    onDismissShareApp: () -> Unit,
) {
    VersionInfoDialog(showVersionInfo, onDismissVersionInfo)
    ShareAppDialog(showShareApp, onDismissShareApp)
}

@Composable
private fun VersionInfoDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (visible) {
        GkAlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = UiStrings.version_info) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VersionInfoItem(label = UiStrings.build_channel, value = META.channel)
                    VersionInfoItem(label = UiStrings.version_code, value = META.versionCode.toString())
                    VersionInfoItem(label = UiStrings.version_name, value = META.versionName)
                    Column {
                        Text(
                            text = UiStrings.code_history,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable { IntentUtils.openUri(META.commitUrl) }
                                .padding(vertical = 8.dp),
                            text = META.tagName ?: META.commitId.substring(0, 16),
                            color = MiuixTheme.colorScheme.primary,
                            style = MiuixTheme.textStyles.body1.copy(textDecoration = TextDecoration.Underline),
                        )
                    }
                    VersionInfoItem(label = UiStrings.commit_time, value = META.commitTime.format("yyyy-MM-dd HH:mm:ss ZZ"))
                }
            },
            confirmButton = {
                GkActionButton(onClick = onDismissRequest) {
                    Text(text = UiStrings.action_close)
                }
            },
        )
    }
}

@Composable
private fun VersionInfoItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(text = value, style = MiuixTheme.textStyles.body1)
    }
}

@Composable
private fun ShareAppDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    val context = LocalActivity.current as MainActivity
    val mainVm = MainViewModel.requireCurrent()
    if (visible) {
        val exportPlayTipText = buildAnnotatedString {
            append(UiStrings.apk_google_services_required)
            withLink(
                LinkAnnotation.Url(
                    ShortUrlSet.URL13,
                    TextLinkStyles(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    )
                )
            ) {
                append(UiStrings.apk_download_official_link)
            }
            append(UiStrings.apk_continue_suffix)
        }
        GkTextListDialog(
            onDismiss = onDismissRequest,
            textList = listOf(
                UiStrings.action_share to mainVm.scope.launchUiAction(Dispatchers.IO) {
                    if (!META.isGkdChannel) {
                        if (!mainVm.dialogRequests.confirm(
                            title = UiStrings.share_notice,
                            text = exportPlayTipText,
                            confirmText = UiStrings.action_continue,
                        )) return@launchUiAction
                    }
                    context.shareFile(getShareApkFile(), UiStrings.apk_share)
                },
                UiStrings.action_save_to_downloads to mainVm.scope.launchUiAction(Dispatchers.IO) {
                    if (!META.isGkdChannel) {
                        if (!mainVm.dialogRequests.confirm(
                            title = UiStrings.save_notice,
                            text = exportPlayTipText,
                            confirmText = UiStrings.action_continue,
                        )) return@launchUiAction
                    }
                    context.saveFileToDownloads(getShareApkFile())
                },
                UiStrings.google_play_label to {
                    mainVm.openUrl(PLAY_STORE_URL)
                },
            )
        )
    }
}
