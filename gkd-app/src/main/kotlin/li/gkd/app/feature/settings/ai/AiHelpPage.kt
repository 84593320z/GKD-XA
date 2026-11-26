package li.gkd.app.feature.settings.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkPageScaffold
import li.gkd.app.ui.component.PreferenceGroup
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data object AiHelpPageRoute : NavKey

private val HelpParagraphs = listOf(
    "开启流程" to listOf(
        "1. 在「高级设置 - 快照」打开自动 AI 规则开关；要仅保留本地快照可关闭此项。手动选择“快照并生成规则”仍会请求 AI。若无可用服务商，会跳到「AI 服务商」页让你新建。",
        "2. 在服务商详情里填好名称、Base URL、API Key，切到「模型」页签拉取或添加模型，并把其中一个设为当前模型。",
        "3. 配好多个服务商时，用列表页的勾选框切换当前生效的那个；自动捕获仅在启用 AI 规则时生成，手动选择“快照并生成规则”可单次发起生成。",
        "4. 广告弹窗出现时按快照快捷键保存快照；启用 AI 规则后，生成结果可在「快照记录」里查看，规则会写入本地订阅的对应应用分组。",
    ),
    "免费额度" to listOf(
        "AI 规则需要一个大模型的 API Key。下面这些平台注册即可领免费额度，日常生成规则基本够用（额度以各平台当前政策为准）：",
        "智谱 GLM（https://www.bigmodel.cn/）：注册送 2000 万 Token。",
        "硅基流动 SiliconFlow（https://cloud.siliconflow.cn/）：注册送 16 元额度。",
        "七牛云（https://www.qiniu.com/）：注册送 300 万 Token，部分免费模型另有每日免费额度。",
        "阿里云百炼（https://bailian.console.aliyun.com/cn-beijing/costing-balance/free-quota）：每款模型 100 万 Token 免费额度，90 天重置。",
        "Kimi（https://platform.kimi.com/console/account）：注册送 15 元额度，约 100 万 Token。",
        "拿到 Key 后回到「AI 服务商」新建服务商填进去，在详情页点「测试连接」确认能通即可。",
    ),
    "快照与规则生成" to listOf(
        "点击悬浮快照按钮可选“仅快照”或“快照并生成规则”；仅快照不会调用 AI，关闭全局开关仅停止自动生成。",
        "生成规则只向配置的 AI 接口发送快照节点 JSON，不包含屏幕截图。",
    ),
    "模型选择" to listOf(
        "选择适合文本输入的模型；请求包含快照节点文本和控件属性，不包含屏幕截图。",
        "Temperature 与 Top P 建议保持默认低值，可减少选择器漂移；Max Tokens 建议 4096 以上。",
    ),
    "隐私提示" to listOf(
        "快照节点数据会发送到已配置的 AI 接口；请使用可信服务。公网 HTTP 地址会被拒绝。",
        "日志与分享链接中的 API Key 会被自动脱敏，但仍建议不要公开完整日志。",
    ),
)

@Composable
fun AiHelpPage() {
    val mainVm = MainViewModel.requireCurrent()

    GkPageScaffold(
        title = "使用说明",
        navigationIcon = {
            GkIconButton(
                imageVector = GkIcons.ArrowBack,
                onClick = { mainVm.popPage() },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            HelpParagraphs.forEach { (section, lines) ->
                PreferenceGroup(title = section) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        lines.forEach { line ->
                            Text(
                                text = line,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
            }
            GkPageBottomSpace()
        }
    }
}
