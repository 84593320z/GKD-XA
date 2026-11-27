package li.gkd.app.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import li.gkd.app.META
import li.gkd.app.MainViewModel
import li.gkd.app.R
import li.gkd.app.store.AppStore
import li.gkd.app.store.AppStore.storeFlow
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.GkRotatingLoadingIcon
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.TextMenu
import li.gkd.app.ui.component.PerfIcon
import li.gkd.app.ui.component.PerfIconButton
import li.gkd.app.ui.component.PreferenceGroup
import li.gkd.app.util.ISSUES_URL
import li.gkd.app.util.REPOSITORY_URL
import li.gkd.app.util.ShortUrlSet
import li.gkd.app.util.TimeUtils.throttle
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.UpdateChannelOption
import li.gkd.app.util.findOption
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data object AboutRoute : NavKey

@Composable
fun AboutPage() {
    val mainVm = MainViewModel.requireCurrent()
    val store by storeFlow.collectAsStateWithLifecycle()
    val primaryColor = MiuixTheme.colorScheme.primary
    var showInfoDlg by rememberSaveable { mutableStateOf(false) }
    var showShareAppDlg by rememberSaveable { mutableStateOf(false) }

    AboutDialogs(
        showVersionInfo = showInfoDlg,
        onDismissVersionInfo = { showInfoDlg = false },
        showShareApp = showShareAppDlg,
        onDismissShareApp = { showShareAppDlg = false },
    )

    GkPageScaffold(
        title = "关于",
        navigationIcon = {
            PerfIconButton(imageVector = PerfIcon.ArrowBack, onClick = mainVm::popPage)
        },
        actions = {
            PerfIconButton(
                imageVector = PerfIcon.Share,
                contentDescription = "分享应用",
                onClick = { showShareAppDlg = true },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
        ) {
            item(key = "identity") {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp).clipToBounds()
                                .clickable(onClick = throttle { toast("你干嘛~ 哎呦~") }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = META.appName,
                                modifier = Modifier.requiredSize(220.dp),
                                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable(onClickLabel = "版本信息") { showInfoDlg = true }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = META.appName,
                                style = MiuixTheme.textStyles.title1,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = META.versionName,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            item(key = "project") {
                PreferenceGroup(title = "项目") {
                    GkSettingItem(
                        imageVector = null,
                        title = "开源代码",
                        onClick = { mainVm.openUrl(REPOSITORY_URL) },
                    )
                    if (META.isGkdChannel) {
                        GkSettingItem(
                            imageVector = null,
                            title = "捐赠支持",
                            onClick = { mainVm.navigateWebPage(ShortUrlSet.URL10) },
                        )
                    }
                    GkSettingItem(
                        imageVector = null,
                        title = "使用协议",
                        onClick = { mainVm.navigateWebPage(ShortUrlSet.URL12) },
                    )
                    GkSettingItem(
                        imageVector = null,
                        title = "隐私政策",
                        onClick = { mainVm.navigateWebPage(ShortUrlSet.URL11) },
                    )
                }
            }
            item(key = "feedback") {
                PreferenceGroup(title = "反馈") {
                    GkSettingItem(
                        imageVector = null,
                        title = "问题反馈",
                        onClick = throttle {
                            mainVm.viewModelScope.launch {
                                if (mainVm.dialogRequests.confirm(
                                        title = "反馈须知",
                                        dismissOnRequest = true,
                                        text = buildAnnotatedString {
                                            val highlightStyle = SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = primaryColor,
                                            )
                                            append("感谢您愿意花时间反馈，")
                                            withStyle(style = highlightStyle) {
                                                append("GKD 默认不携带任何规则，只接受应用本体功能相关的反馈")
                                            }
                                            append("\n\n")
                                            append("请先判断是不是第三方规则订阅的问题，如果是，您应该向规则提供者反馈，而不是在此处反馈。")
                                            withStyle(style = highlightStyle) {
                                                append("如果您已经确信是 GKD 应用本体的问题")
                                            }
                                            append("，可点击下方继续反馈")
                                        },
                                        confirmText = "继续",
                                    )
                                ) {
                                    mainVm.openUrl(ISSUES_URL)
                                }
                            }
                        },
                    )
                    GkSettingItem(
                        title = "导出日志",
                        imageVector = PerfIcon.Share,
                        onClick = {
                            mainVm.shareLog.show()
                        },
                    )
                }
            }
            if (mainVm.updateStatus != null) {
                item(key = "updates") {
                    PreferenceGroup(title = "更新") {
                        TextMenu(
                            title = "更新渠道",
                            option = UpdateChannelOption.objects.findOption(store.updateChannel),
                        ) {
                            if (mainVm.updateStatus.checkUpdatingFlow.value) return@TextMenu
                            val selected = it
                            if (it.value == UpdateChannelOption.Beta.value) {
                                mainVm.viewModelScope.launch {
                                    if (mainVm.dialogRequests.confirm(
                                            title = "版本渠道",
                                            text = "测试版本渠道更新快\n但不稳定可能存在较多BUG\n请谨慎使用",
                                        )
                                    ) {
                                        AppStore.updateSettings { s -> s.copy(updateChannel = selected.value) }
                                    }
                                }
                            } else {
                                AppStore.updateSettings { s -> s.copy(updateChannel = selected.value) }
                            }
                        }
                        BasicComponent(
                            title = "检查更新",
                            onClick = throttle {
                                mainVm.updateStatus.checkUpdate(true)
                            },
                            endActions = {
                                GkRotatingLoadingIcon(
                                    loading = mainVm.updateStatus.checkUpdatingFlow
                                        .collectAsStateWithLifecycle().value,
                                )
                            },
                        )
                    }
                }
            }
            item(key = "bottom") {
                GkPageBottomSpace(height = 40.dp)
            }
        }
    }
}
