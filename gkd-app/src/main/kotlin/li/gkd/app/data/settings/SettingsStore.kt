package li.gkd.app.data.settings

import kotlinx.serialization.Serializable
import li.gkd.app.text.UiStrings
import li.gkd.app.META
import li.gkd.app.util.AppGroupOption
import li.gkd.app.util.AppSortOption
import li.gkd.app.util.AutomatorModeOption
import li.gkd.app.util.RuleSortOption
import li.gkd.app.util.SnapshotDisplayModeOption
import li.gkd.app.util.UpdateChannelOption
import li.gkd.app.util.UpdateTimeOption

/** 服务商下的一个模型条目：modelId 是发给接口的名字，其余字段只用于界面与选模型参考。 */
@Serializable
data class AiModel(
    val modelId: String,
    val displayName: String = modelId,
    /** 上下文长度，0 表示接口未给出且用户未填 */
    val contextWindow: Int = 0,
    /** 是否支持思考/推理，null 表示未知 */
    val reasoning: Boolean? = null,
)

@Serializable
data class AiHeader(
    val name: String,
    val value: String,
)

/** OpenAI 兼容接口的两种端点：/chat/completions 与 /responses */
object AiEndpointMode {
    const val CHAT = "chat"
    const val RESPONSES = "responses"

    val labels = mapOf(
        CHAT to "Chat Completions API",
        RESPONSES to "Responses API",
    )
}

/** 一个服务商连接：接口协议 + 地址 + 密钥 + 模型池，[SettingsStore.aiProviders] 可存多份 */
@Serializable
data class AiConfig(
    val id: String = "",
    val name: String = "",
    val protocol: String = "openai",
    val endpointMode: String = AiEndpointMode.CHAT,
    val apiUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val models: List<AiModel> = emptyList(),
    val headers: List<AiHeader> = emptyList(),
    val anthropicVersion: String = DEFAULT_ANTHROPIC_VERSION,
    /** 留空则使用内置的 gkd-rule-generator-prompt.md */
    val systemPrompt: String = "",
    val enabled: Boolean = true,
    val temperature: Float = 0f,
    val topP: Float = 1f,
    val maxTokens: Int = 4096,
) {
    val isAnthropic get() = protocol == "anthropic"

    val usable: Boolean
        get() = apiUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    fun currentModel(): AiModel? = models.firstOrNull { it.modelId == model }

    companion object {
        const val DEFAULT_ANTHROPIC_VERSION = "2023-06-01"
    }
}

fun newAiProviderId(): String = java.util.UUID.randomUUID().toString()

@Serializable
data class SettingsStore(
    val enableAutomator: Boolean = false,
    val automatorMode: Int = AutomatorModeOption.A11yMode.value,
    val enableMatch: Boolean = true,
    val enableStatusService: Boolean = false,
    val excludeFromRecents: Boolean = false,
    val captureScreenshot: Boolean = false,
    val screenshotTargetAppId: String = "",
    val screenshotEventSelector: String = "",
    val httpServerPort: Int = 8888,
    val updateSubsInterval: Long = UpdateTimeOption.Everyday.value,
    val captureVolumeChange: Boolean = false,
    val toastWhenClick: Boolean = true,
    val actionToast: String = META.appName,
    val autoClearMemorySubs: Boolean = false,
    val hideSnapshotStatusBar: Boolean = false,
    val autoSaveSnapshotToDownloads: Boolean = false,
    val enableDarkTheme: Boolean? = null,
    val enableDynamicColor: Boolean = true,
    /** MIUIX：顶栏/底栏模糊（需 RuntimeShader） */
    val enableMiuixBlur: Boolean = true,
    /** MIUIX：悬浮底栏（类 Apple / FloatingNavigationBar） */
    val useFloatingNavBar: Boolean = true,
    /** 系统预测式返回手势（Android 13+，切换后需重建 Activity） */
    val enablePredictiveBack: Boolean = false,
    val useSystemToast: Boolean = false,
    val useCustomNotifText: Boolean = false,
    val customNotifTitle: String = META.appName,
    val customNotifText: String = UiStrings.notification_summary_template,
    val updateChannel: Int = if (META.isBeta) UpdateChannelOption.Beta.value else UpdateChannelOption.Stable.value,
    val appSort: Int = AppSortOption.ByUsedTime.value,
    val showBlockApp: Boolean = true,
    val appRuleSort: Int = RuleSortOption.ByDefault.value,
    val subsAppSort: Int = AppSortOption.ByUsedTime.value,
    val subsCategorySort: Int = AppSortOption.ByUsedTime.value,
    val subsAppShowUninstall: Boolean = false,
    val subsAppGroupType: Int = AppGroupOption.UserGroup.value or AppGroupOption.SystemGroup.value,
    val subsCategoryGroupType: Int = AppGroupOption.UserGroup.value or AppGroupOption.SystemGroup.value,
    val subsAppShowBlock: Boolean = false,
    val subsCategoryShowBlock: Boolean = false,
    val subsExcludeSort: Int = AppSortOption.ByUsedTime.value,
    val subsExcludeShowBlockApp: Boolean = true,
    val subsExcludeShowInnerDisabledApp: Boolean = true,
    val subsPowerWarn: Boolean = true,
    val enableBlockA11yAppList: Boolean = false,
    val blockA11yAppListFollowMatch: Boolean = true,
    val a11yAppSort: Int = AppSortOption.ByUsedTime.value,
    val a11yScopeAppSort: Int = AppSortOption.ByUsedTime.value,
    val appGroupType: Int = (1 shl AppGroupOption.normalObjects.size) - 1,
    val a11yAppGroupType: Int = appGroupType,
    val a11yScopeAppGroupType: Int = appGroupType,
    val subsExcludeAppGroupType: Int = appGroupType,
    val showDisabledRule: Boolean = true,
    val snapshotDisplayMode: Int = SnapshotDisplayModeOption.ByTime.value,
    val aiEnable: Boolean = false,
    /** @deprecated 旧版单份配置，首次加载时被 [migrateAiProviders] 展开进 [aiProviders] */
    val aiConfig: AiConfig? = null,
    val aiProviders: List<AiConfig> = emptyList(),
    /** 界面中选定的服务商；为空时回落到第一个已启用的 */
    val aiActiveProviderId: String = "",
) {
    val useA11y get() = automatorMode == AutomatorModeOption.A11yMode.value
    val useAutomation get() = automatorMode == AutomatorModeOption.AutomationMode.value

    /** 实际用于生成的服务商：先认界面上选中的，再回落到第一个启用的 */
    fun activeAiProvider(): AiConfig? =
        aiProviders.firstOrNull { it.id == aiActiveProviderId && it.enabled }
            ?: aiProviders.firstOrNull { it.enabled }
}

/** 旧版只有一份 aiConfig：首次加载时展开成一个服务商，之后不再读取旧字段。 */
fun SettingsStore.migrateAiProviders(): SettingsStore {
    if (aiProviders.isNotEmpty()) return this
    val legacy = aiConfig ?: return this
    if (legacy.apiUrl.isBlank() && legacy.apiKey.isBlank() && legacy.model.isBlank()) {
        return copy(aiConfig = null)
    }
    val provider = legacy.copy(
        id = newAiProviderId(),
        name = legacy.name.ifBlank { legacy.model.ifBlank { "默认服务商" } },
        model = legacy.model.trim(),
        models = legacy.model.trim().takeIf { it.isNotEmpty() }?.let { listOf(AiModel(it)) }.orEmpty(),
    )
    return copy(
        aiConfig = null,
        aiProviders = listOf(provider),
        aiActiveProviderId = provider.id,
    )
}
