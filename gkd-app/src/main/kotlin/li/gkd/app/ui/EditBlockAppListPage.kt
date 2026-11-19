package li.gkd.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkEditorScaffold
import li.gkd.app.ui.component.GkMultiTextField
import li.gkd.app.ui.style.scaffoldPadding
import li.gkd.app.util.ToastUtils.toast
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
data object EditBlockAppListRoute : NavKey

@Composable
fun EditBlockAppListPage() {
    val vm = viewModel<EditBlockAppListVm>()
    val text by vm.textFlow.collectAsStateWithLifecycle()
    GkEditorScaffold(
        title = UiStrings.app_whitelist,
        hasChanges = { vm.getChangedSet() != null },
        onSave = {
            toast(if (vm.saveChanges()) UiStrings.update_success else UiStrings.unchanged)
        },
    ) { padding ->
        GkMultiTextField(
            modifier = Modifier.scaffoldPadding(padding),
            text = text,
            onTextChange = vm::setText,
            immediateFocus = true,
            placeholderText = UiStrings.app_ids_input_hint,
            indicatorSize = vm.indicatorSizeFlow.collectAsStateWithLifecycle().value,
        )
    }
}
