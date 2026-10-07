package li.gkd.app.ui.app

import li.gkd.app.MainViewModel

import li.gkd.app.feature.subscription.CategoryEditorPage
import li.gkd.app.feature.subscription.CategoryEditorRoute
import li.gkd.app.feature.subscription.RuleExcludeEditorPage
import li.gkd.app.feature.subscription.RuleExcludeEditorRoute
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavEntryBuilder
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import li.gkd.app.ui.A11YScopeAppListRoute
import li.gkd.app.feature.log.A11yEventLogPage
import li.gkd.app.feature.log.A11yEventLogRoute
import li.gkd.app.ui.A11yScopeAppListPage
import li.gkd.app.feature.settings.AboutPage
import li.gkd.app.feature.settings.AboutRoute
import li.gkd.app.feature.log.ActionLogPage
import li.gkd.app.feature.log.ActionLogRoute
import li.gkd.app.feature.log.ActivityLogPage
import li.gkd.app.feature.log.ActivityLogRoute
import li.gkd.app.feature.settings.AdvancedPage
import li.gkd.app.feature.settings.AdvancedPageRoute
import li.gkd.app.feature.settings.ai.AiHelpPage
import li.gkd.app.feature.settings.ai.AiHelpPageRoute
import li.gkd.app.feature.settings.ai.AiProviderDetailPage
import li.gkd.app.feature.settings.ai.AiProviderDetailRoute
import li.gkd.app.feature.settings.ai.AiProvidersPage
import li.gkd.app.feature.settings.ai.AiProvidersPageRoute
import li.gkd.app.ui.AppConfigPage
import li.gkd.app.ui.AppConfigRoute
import li.gkd.app.ui.BlockA11yAppListPage
import li.gkd.app.ui.BlockA11yAppListRoute
import li.gkd.app.ui.CrashReportPage
import li.gkd.app.ui.CrashReportRoute
import li.gkd.app.ui.EditBlockAppListPage
import li.gkd.app.ui.EditBlockAppListRoute
import li.gkd.app.ui.ImagePreviewPage
import li.gkd.app.ui.ImagePreviewRoute
import li.gkd.app.ui.PrivilegeServicePage
import li.gkd.app.ui.PrivilegeServiceRoute
import li.gkd.app.feature.snapshot.SnapshotPage
import li.gkd.app.feature.snapshot.SnapshotPageRoute
import li.gkd.app.feature.snapshot.SnapshotPreviewPage
import li.gkd.app.feature.snapshot.SnapshotPreviewRoute
import li.gkd.app.feature.snapshot.SnapshotSettingsPage
import li.gkd.app.feature.snapshot.SnapshotSettingsRoute
import li.gkd.app.feature.subscription.SubsAppGroupListPage
import li.gkd.app.feature.subscription.SubsAppGroupListRoute
import li.gkd.app.feature.subscription.SubsAppListPage
import li.gkd.app.feature.subscription.SubsAppListRoute
import li.gkd.app.feature.subscription.SubsCategoryGroupPage
import li.gkd.app.feature.subscription.SubsCategoryGroupRoute
import li.gkd.app.feature.subscription.SubsCategoryPage
import li.gkd.app.feature.subscription.SubsCategoryRoute
import li.gkd.app.feature.subscription.SubsGlobalGroupExcludePage
import li.gkd.app.feature.subscription.SubsGlobalGroupExcludeRoute
import li.gkd.app.feature.subscription.SubsGlobalGroupListPage
import li.gkd.app.feature.subscription.SubsGlobalGroupListRoute
import li.gkd.app.feature.subscription.UpsertRuleGroupPage
import li.gkd.app.feature.subscription.UpsertRuleGroupRoute
import li.gkd.app.ui.WebViewPage
import li.gkd.app.ui.WebViewRoute
import li.gkd.app.feature.settings.WorkModePage
import li.gkd.app.feature.settings.WorkModeRoute
import li.gkd.app.ui.home.HomePage
import li.gkd.app.ui.home.BlockA11ySetupPage
import li.gkd.app.ui.home.ActionToastPage
import li.gkd.app.ui.home.ActionToastRoute
import li.gkd.app.ui.home.NotificationTextPage
import li.gkd.app.ui.home.NotificationTextRoute
import li.gkd.app.ui.home.BlockA11ySetupRoute
import li.gkd.app.ui.home.HomeRoute


private fun NavEntryBuilder.mainRoutes() {
    entry<HomeRoute> { HomePage() }
    entry<WorkModeRoute> { WorkModePage() }
    entry<AboutRoute> { AboutPage() }
    entry<ActionToastRoute>(transition = NavTransitions.Modal) { ActionToastPage() }
    entry<NotificationTextRoute>(transition = NavTransitions.Modal) { NotificationTextPage() }
    entry<BlockA11ySetupRoute>(transition = NavTransitions.Modal) { BlockA11ySetupPage() }
    entry<BlockA11yAppListRoute> { BlockA11yAppListPage() }
    entry<AdvancedPageRoute> { AdvancedPage() }
    entry<AiProvidersPageRoute> { AiProvidersPage() }
    entry<AiProviderDetailRoute> { AiProviderDetailPage(it) }
    entry<AiHelpPageRoute> { AiHelpPage() }
    entry<PrivilegeServiceRoute> { PrivilegeServicePage() }
    entry<SnapshotPageRoute> { SnapshotPage() }
    entry<SnapshotPreviewRoute> { SnapshotPreviewPage(it) }
    entry<SnapshotSettingsRoute> { SnapshotSettingsPage() }
    entry<A11YScopeAppListRoute> { A11yScopeAppListPage() }
    entry<ActivityLogRoute> { ActivityLogPage() }
    entry<A11yEventLogRoute> { A11yEventLogPage() }
    entry<EditBlockAppListRoute>(transition = NavTransitions.Modal) { EditBlockAppListPage() }
    entry<SubsAppListRoute> { SubsAppListPage(it) }
    entry<WebViewRoute> { WebViewPage(it) }
    entry<SubsCategoryRoute> { SubsCategoryPage(it) }
    entry<SubsGlobalGroupListRoute> { SubsGlobalGroupListPage(it) }
    entry<SubsGlobalGroupExcludeRoute> { SubsGlobalGroupExcludePage(it) }
    entry<ActionLogRoute> { ActionLogPage(it) }
    entry<ImagePreviewRoute> { ImagePreviewPage(it) }
    entry<UpsertRuleGroupRoute>(transition = NavTransitions.Modal) { UpsertRuleGroupPage(it) }
    entry<CategoryEditorRoute>(transition = NavTransitions.Modal) { CategoryEditorPage(it) }
    entry<RuleExcludeEditorRoute>(transition = NavTransitions.Modal) { RuleExcludeEditorPage(it) }
    entry<SubsAppGroupListRoute> { SubsAppGroupListPage(it) }
    entry<AppConfigRoute> { AppConfigPage(it) }
    entry<CrashReportRoute> { CrashReportPage() }
    entry<SubsCategoryGroupRoute> { SubsCategoryGroupPage(it) }
}

@Composable
fun MainNavigation() {
    val mainVm = MainViewModel.requireCurrent()
    NavDisplay(
        backStack = mainVm.backStack,
        onBack = mainVm::popPage,
        transition = NavTransitions.MiuixDefault,
    ) {
        mainRoutes()
    }
}
