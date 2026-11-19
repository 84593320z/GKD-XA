package li.gkd.app.feature.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import li.gkd.app.MainViewModel
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkEditorScaffold
import li.gkd.app.ui.component.GkMultiTextField
import li.gkd.app.ui.component.GkSubscriptionPageContent
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.style.getJson5Transformation
import li.gkd.app.ui.style.scaffoldPadding
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
data class UpsertRuleGroupRoute(
    val subsId: Long,
    val groupKey: Int? = null,
    val appId: String? = null,
    val forward: Boolean = false,
) : NavKey

@Composable
fun UpsertRuleGroupPage(route: UpsertRuleGroupRoute) {
    val subsId = route.subsId
    val appId = route.appId
    val forward = route.forward

    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { UpsertRuleGroupVm(route) }
    GkSubscriptionPageContent(vm.uiState) { state ->
        val editedText by vm.textFlow.collectAsStateWithLifecycle()
        val text = editedText ?: state.initialText

        var addedAppId by remember { mutableStateOf<String?>(null) }
        GkEditorScaffold(
            title = if (vm.isEdit) UiStrings.rule_edit else UiStrings.rule_add,
            hasChanges = vm::hasTextChanged,
            saveEnabled = text.isNotBlank(),
            onSave = { addedAppId = vm.saveRule() },
            onSaved = {
                if (forward) {
                    mainVm.navigatePage(
                        if (appId == null) SubsGlobalGroupListRoute(subsItemId = subsId)
                        else SubsAppGroupListRoute(subsItemId = subsId, appId = addedAppId ?: appId),
                        replaced = true,
                    )
                } else {
                    mainVm.popPage()
                }
            },
        ) { paddingValues ->
            GkMultiTextField(
                modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize(),
                text = text,
                onTextChange = vm::setText,
                immediateFocus = true,
                visualTransformation = getJson5Transformation(LocalDarkTheme.current),
                placeholderText = if (vm.isApp) UiStrings.app_rule_input_hint else UiStrings.global_rule_input_hint,
            )
        }
    }
}
