package li.gkd.app.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import li.gkd.app.data.RawSubscription
import li.gkd.app.domain.rule.CategoryPolicy
import li.gkd.app.text.UiStrings
import li.gkd.app.ui.component.GkActionButton
import li.gkd.app.ui.component.GkAppNameText
import li.gkd.app.ui.component.GkEditorScaffold
import li.gkd.app.ui.component.GkFormTextField
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkSubscriptionPageContent
import li.gkd.app.ui.style.GkUi
import li.gkd.app.util.ToastUtils.toast
import li.gkd.app.util.json
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
data class CategoryEditorRoute(val subsId: Long, val categoryKey: Int? = null) : NavKey

@Composable
fun CategoryEditorPage(route: CategoryEditorRoute) {
    val vm = viewModel { CategoryEditorVm(route) }
    GkSubscriptionPageContent(vm.uiState) { subscription ->
        CategoryEditorContent(subscription, route.categoryKey) { name, description ->
            val changed = vm.save(subscription, name, description)
            toast(if (!changed) UiStrings.unchanged else if (route.categoryKey == null) UiStrings.add_success else UiStrings.update_success)
        }
    }
}

@Composable
private fun CategoryEditorContent(
    subscription: RawSubscription,
    categoryKey: Int?,
    onSave: suspend (name: String, description: String) -> Unit,
) {
    val category = subscription.categories.find { it.key == categoryKey }
    val categorySnapshot = remember(category) { category?.let { json.encodeToString(it) } }
    var originalCategory by rememberSaveable { mutableStateOf(categorySnapshot) }
    var name by rememberSaveable { mutableStateOf(category?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(category?.desc.orEmpty()) }
    var originalName by rememberSaveable { mutableStateOf(name) }
    var originalDescription by rememberSaveable { mutableStateOf(description) }
    var previewName by remember(subscription, categoryKey) { mutableStateOf(name) }
    LaunchedEffect(name) {
        delay(300)
        previewName = name
    }
    val conflict = categoryKey != null && categorySnapshot != originalCategory
    val validation = remember(subscription, categoryKey, name) {
        runCatching { CategoryPolicy.validateEdit(subscription, categoryKey, name) }
    }
    val preview = remember(subscription, categoryKey, previewName) {
        runCatching { CategoryPolicy.previewEdit(subscription, categoryKey, previewName, "") }
    }
    val error = if (conflict) UiStrings.category_edit_conflict
    else validation.exceptionOrNull()?.message
    val previewCategoryKey = categoryKey ?: preview.getOrNull()?.categories?.maxOfOrNull { it.key }
    val apps = preview.getOrNull()?.let { edited ->
        previewCategoryKey?.let(edited::getCategoryApps)
    }.orEmpty()
    val nameError = error?.takeIf { !conflict && name.isNotBlank() }
    val nameFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    GkEditorScaffold(
        title = if (categoryKey == null) UiStrings.category_add else UiStrings.category_edit,
        hasChanges = { name.trim() != originalName.trim() || description.trim() != originalDescription.trim() },
        saveEnabled = error == null,
        onSave = { onSave(name.trim(), description.trim()) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            // One bounded, weighted workspace lets the form scroll with its preview under
            // the IME or large fonts, without measuring a nested LazyColumn at infinity.
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = GkUi.PageHorizontal,
                    top = GkUi.SectionGap,
                    end = GkUi.PageHorizontal,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "form", contentType = "form") {
                    // Keep both fields composed together so IME Next always has a target.
                    Column(verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap)) {
                        GkFormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = nameError ?: UiStrings.category_name_prefix,
                            labelMaxLines = 1,
                            labelOverflow = TextOverflow.Ellipsis,
                            isError = nameError != null,
                            errorMessage = nameError ?: UiStrings.error_label,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { descriptionFocusRequester.requestFocus() },
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
                        )
                        GkFormTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = UiStrings.category_description,
                            minLines = 1,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().focusRequester(descriptionFocusRequester),
                        )
                    }
                    LaunchedEffect(Unit) {
                        // Do not steal focus when this lazy item is scrolled back into view.
                        if (!initialFocusRequested) {
                            nameFocusRequester.requestFocus()
                            initialFocusRequested = true
                        }
                    }
                }
                if (conflict) {
                    item(key = "conflict", contentType = "conflict") {
                        Card(colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.errorContainer)) {
                            Column(
                                Modifier.fillMaxWidth().padding(GkUi.ContentPadding),
                                verticalArrangement = Arrangement.spacedBy(GkUi.SectionGap),
                            ) {
                                Text(
                                    UiStrings.category_edit_conflict,
                                    color = MiuixTheme.colorScheme.onErrorContainer,
                                )
                                if (category != null) {
                                    GkActionButton(onClick = {
                                        originalCategory = categorySnapshot
                                        name = category.name
                                        description = category.desc.orEmpty()
                                        originalName = name
                                        originalDescription = description
                                    }) { Text(UiStrings.action_reload_latest) }
                                }
                            }
                        }
                    }
                }
                if (!conflict && name.isNotBlank()) {
                    if (apps.isEmpty()) {
                        item(key = "preview_empty", contentType = "summary") {
                            Text(
                                text = UiStrings.category_no_matching_groups,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    } else {
                        item(key = "preview_summary", contentType = "summary") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(apps.size.toString(), style = MiuixTheme.textStyles.body2)
                                GkIcon(
                                    imageVector = GkIcons.Android,
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = UiStrings.app_count(apps.size),
                                )
                                val groupCount = apps.sumOf { it.groups.size }
                                Text(groupCount.toString(), style = MiuixTheme.textStyles.body2)
                                GkIcon(
                                    imageVector = GkIcons.FlashOn,
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = UiStrings.category_group_count(groupCount),
                                )
                            }
                        }
                        apps.forEachIndexed { index, app ->
                            item(key = "app:${app.id}", contentType = "app") {
                                GkAppNameText(
                                    appId = app.id,
                                    fallbackName = app.name,
                                    modifier = Modifier.fillMaxWidth().padding(
                                        top = if (index == 0) 0.dp else 12.dp,
                                        bottom = 4.dp,
                                    ),
                                    style = MiuixTheme.textStyles.subtitle,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                            }
                            items(app.groups, key = { "group:${app.id}:${it.key}" }, contentType = { "group" }) { group ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(GkUi.ContentPadding),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            group.name, style = MiuixTheme.textStyles.body1,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        )
                                        group.desc?.takeIf { it.isNotBlank() }?.let {
                                            Text(
                                                it,
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
