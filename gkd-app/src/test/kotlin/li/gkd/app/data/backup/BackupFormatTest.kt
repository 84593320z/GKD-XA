package li.gkd.app.data.backup

import li.gkd.app.data.RawSubscription
import li.gkd.app.text.UiStrings
import li.gkd.app.data.subscription.UsedSubsEntry
import li.gkd.app.domain.rule.RuleGroupPolicy
import li.gkd.app.domain.rule.RuleSummaryBuilder
import li.gkd.db.SubsAppConfig
import li.gkd.db.SubsAppGroupConfig
import li.gkd.db.SubsCategoryConfig
import li.gkd.db.SubsGlobalGroupConfig
import li.gkd.db.SubsItem
import li.gkd.db.SubscriptionConfigSnapshot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    // Frozen V1 archive payload: identity, discriminator and omitted nulls are a compatibility contract.
    private val legacy = """
        {
          "subsItems": [{"id":7,"ctime":1,"mtime":2,"enable":true,"enableUpdate":true,"order":0}],
          "appConfigs": [
            {"id":20,"subsId":7,"appId":"app.one","enable":false},
            {"id":10,"subsId":7,"appId":"app.one","enable":true}
          ],
          "categoryConfigs": [
            {"id":80,"subsId":7,"categoryKey":3,"enable":false},
            {"id":70,"subsId":7,"categoryKey":3}
          ],
          "subsConfigs": [
            {"id":40,"type":2,"subsId":7,"appId":"app.one","groupKey":4,"enable":false,"exclude":"new"},
            {"id":30,"type":2,"subsId":7,"appId":"app.one","groupKey":4,"exclude":"old"},
            {"id":60,"type":3,"subsId":7,"groupKey":4,"enable":true,"exclude":"new-global"},
            {"id":50,"type":3,"subsId":7,"groupKey":4,"enable":false,"exclude":"old-global"}
          ]
        }
    """.trimIndent()

    @Test
    fun legacyBackupSplitsAndDeduplicatesWholeRowsUsingTheSmallestId() {
        val data = BackupFormat.decode(legacy).toSnapshot()
        assertEquals(listOf(SubsAppConfig(true, 7, "app.one")), data.appConfigs)
        assertEquals(listOf(SubsCategoryConfig(null, 7, 3)), data.categoryConfigs)
        assertEquals(listOf(SubsAppGroupConfig(7, "app.one", 4, null, "old")), data.appGroupConfigs)
        assertEquals(listOf(SubsGlobalGroupConfig(7, 4, false, "old-global")), data.globalGroupConfigs)
    }

    @Test
    fun importedConfigurationGivesUiAndRuntimeTheSameEffectiveSwitchState() {
        val data = BackupFormat.decode(legacy).toSnapshot()
        val subscription = RawSubscription.parse(
            """
            {
              id: 7, name: 'Sample', version: 1,
              categories: [{key: 3, name: 'Batch', enable: false}],
              apps: [{id: 'app.one', groups: [{key: 4, name: 'Batch', enable: true, rules: []}]}],
              globalGroups: [{key: 4, name: 'Global', enable: true, rules: []}],
            }
            """.trimIndent(),
        )
        val group = subscription.apps.single().groups.single()
        val category = subscription.categories.single()
        val uiEnabled = RuleGroupPolicy.getGroupEnabled(
            group,
            data.appGroupConfigs.find { it.groupKey == group.key },
            category,
            data.categoryConfigs.find { it.categoryKey == category.key },
        )
        val summary = RuleSummaryBuilder.build(
            subscriptions = listOf(UsedSubsEntry(data.subsItems.single(), subscription)),
            appInfoById = emptyMap(),
            appConfigs = data.appConfigs,
            groupConfigs = data.appGroupConfigs + data.globalGroupConfigs,
            categoryConfigs = data.categoryConfigs,
        )
        assertTrue(uiEnabled)
        assertEquals(uiEnabled, summary.appIdToAllGroups.getValue("app.one").single().enable)
        assertTrue(summary.globalGroups.isEmpty())
    }

    @Test
    fun reexportedBackupKeepsNullRecordsAndAllConfigurationValues() {
        val converted = BackupFormat.decode(legacy)
        val encoded = BackupFormat.encode(converted)
        assertTrue(encoded.contains("\"formatVersion\":2"))
        assertFalse(encoded.contains("\"subsConfigs\""))
        assertEquals(converted.toSnapshot(), BackupFormat.decode(encoded).toSnapshot())
        assertNull(BackupFormat.decode(encoded).categoryConfigs.single().enable)
    }

    @Test
    fun version2BackupBeforeTableRenamingKeepsItsFieldNamesAndValues() {
        // Frozen database-v15 export: Room names must not change the V2 archive contract.
        val exportedBeforeRename = """
            {
              "formatVersion":2,
              "subsItems":[{"id":7,"ctime":1,"mtime":2,"enable":true,"enableUpdate":true,"order":0}],
              "appConfigs":[{"subsId":7,"appId":"app.one","enable":false}],
              "categoryConfigs":[{"subsId":7,"categoryKey":3}],
              "appGroupConfigs":[{"subsId":7,"appId":"app.one","groupKey":4,"exclude":"app-exclude"}],
              "globalGroupConfigs":[{"subsId":7,"groupKey":4,"enable":true,"exclude":"global-exclude"}]
            }
        """.trimIndent()
        val expected = SubscriptionConfigSnapshot(
            subsItems = listOf(SubsItem(7, ctime = 1, mtime = 2, enable = true, order = 0)),
            appConfigs = listOf(SubsAppConfig(false, 7, "app.one")),
            categoryConfigs = listOf(SubsCategoryConfig(null, 7, 3)),
            appGroupConfigs = listOf(SubsAppGroupConfig(7, "app.one", 4, null, "app-exclude")),
            globalGroupConfigs = listOf(SubsGlobalGroupConfig(7, 4, true, "global-exclude")),
        )
        assertEquals(expected, BackupFormat.decode(exportedBeforeRename).toSnapshot())
        val reexported = BackupFormat.encode(BackupDatabaseData.fromSnapshot(expected))
        assertEquals(Json.parseToJsonElement(exportedBeforeRename), Json.parseToJsonElement(reexported))
    }

    @Test
    fun legacyMissingCollectionsAndOptionalGroupFieldsUseOriginalDefaults() {
        val data = BackupFormat.decode(
            """{"subsConfigs":[{"id":1,"type":3,"subsId":7,"groupKey":4}]}""",
        ).toSnapshot()
        assertTrue(data.categoryConfigs.isEmpty())
        assertEquals(listOf(SubsGlobalGroupConfig(7, 4)), data.globalGroupConfigs)
    }

    @Test
    fun unsupportedVersionsAndTypesFailBeforeImportCanStart() {
        assertThrows(IllegalStateException::class.java) {
            BackupFormat.decode("""{"formatVersion":99}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupFormat.decode("""{"subsConfigs":[{"id":1,"type":99,"subsId":7}]}""")
        }
    }

    @Test
    fun restorePlanKeepsExistingRowsAndBodiesAndImportsOnlyCompleteNewItems() {
        val existing = SubscriptionConfigSnapshot(
            subsItems = listOf(SubsItem(id = 7, order = 0)),
        )
        val archive = SubscriptionConfigSnapshot(
            subsItems = listOf(
                SubsItem(id = 7, order = 0),
                SubsItem(id = 8, order = 1),
            ),
            appConfigs = listOf(SubsAppConfig(true, 7, "existing")),
        )
        val archived = listOf(
            RawSubscription(id = 7, name = "old", version = 1),
            RawSubscription(id = 8, name = "new", version = 2),
            RawSubscription(id = 9, name = "body-only", version = 1),
        )

        val plan = planSubscriptionRestore(
            database = archive,
            subscriptions = archived,
            existing = existing,
            hasExistingBody = { it == 7L || it == 9L },
        )

        assertEquals(setOf(7L, 9L), plan.conflictingIds)
        assertEquals(listOf(8L), plan.subscriptions.map { it.id })
        assertEquals(listOf(8L), plan.database.subsItems.map { it.id })
        assertTrue(plan.database.appConfigs.isEmpty())
        assertEquals(0, plan.skippedOrphanConfigs)
    }

    @Test
    fun restorePlanReplacesOnlyPristineDefaultLocalPlaceholder() {
        val localBody = """{"id":-2,"name":"Local","version":0}"""
        val plan = planSubscriptionRestore(
            database = SubscriptionConfigSnapshot(
                subsItems = listOf(SubsItem(id = -2, ctime = 1, mtime = 2, order = 4)),
                appConfigs = listOf(SubsAppConfig(true, -2, "app.local")),
            ),
            subscriptions = listOf(RawSubscription(id = -2, name = "Restored", version = 1)),
            existing = SubscriptionConfigSnapshot(
                subsItems = listOf(SubsItem(id = -2, ctime = 10, mtime = 11, order = 0)),
            ),
            hasExistingBody = { it == -2L },
            readLocalPlaceholder = { localBody },
            localPlaceholderName = "Local",
        )
        assertTrue(plan.conflictingIds.isEmpty())
        assertEquals(listOf(-2L), plan.subscriptions.map { it.id })
        assertEquals(listOf(-2L), plan.database.subsItems.map { it.id })
        assertEquals(listOf(-2L), plan.database.appConfigs.map { it.subsId })
    }

    @Test
    fun restorePlanPreservesEditedLocalBodyOrAnyLocalOverride() {
        val item = SubsItem(id = -2, order = 0)
        fun plan(body: String, configs: List<SubsAppConfig> = emptyList()) = planSubscriptionRestore(
            database = SubscriptionConfigSnapshot(subsItems = listOf(item)),
            subscriptions = listOf(RawSubscription(id = -2, name = "Restored", version = 1)),
            existing = SubscriptionConfigSnapshot(subsItems = listOf(item), appConfigs = configs),
            hasExistingBody = { true },
            readLocalPlaceholder = { body },
            localPlaceholderName = "Local",
        )
        val editedBody = plan("""{"id":-2,"name":"Local","version":1}""")
        val overridden = plan("""{"id":-2,"name":"Local","version":0}""", listOf(SubsAppConfig(true, -2, "app")))
        assertTrue(editedBody.subscriptions.isEmpty())
        assertTrue(overridden.subscriptions.isEmpty())
        assertEquals(setOf(-2L), editedBody.conflictingIds)
        assertEquals(setOf(-2L), overridden.conflictingIds)
    }

    @Test
    fun localBootstrapWithoutBodyCanBeReplacedWhenNoOverridesExist() {
        val placeholder = SubsItem(id = -2, order = 0)
        val plan = planSubscriptionRestore(
            database = SubscriptionConfigSnapshot(subsItems = listOf(placeholder)),
            subscriptions = listOf(RawSubscription(id = -2, name = "Restored", version = 1)),
            existing = SubscriptionConfigSnapshot(subsItems = listOf(placeholder)),
            hasExistingBody = { false },
            localPlaceholderName = "Local",
        )
        assertEquals(listOf(-2L), plan.subscriptions.map { it.id })
    }

    @Test
    fun doesNotInspectLocalBodyWhenArchiveHasNoLocalSubscription() {
        val archived = SubsItem(id = 21, order = 0)
        val plan = planSubscriptionRestore(
            database = SubscriptionConfigSnapshot(subsItems = listOf(archived)),
            subscriptions = listOf(RawSubscription(id = 21, name = "new", version = 1)),
            existing = SubscriptionConfigSnapshot(subsItems = listOf(SubsItem(id = -2, order = 0))),
            hasExistingBody = { id ->
                check(id == 21L) { "Unrelated local storage must not be inspected" }
                false
            },
        )
        assertEquals(listOf(archived), plan.database.subsItems)
    }

    @Test
    fun incompleteArchivePreservesMetadataAndDefaultsOnlyMissingRows() {
        val archived = SubsItem(id = 21, order = 0, enable = true, enableUpdate = false)
        val appConfig = SubsAppConfig(true, 21, "app.local")
        val snapshot = completeSubscriptionMetadata(
            database = SubscriptionConfigSnapshot(subsItems = listOf(archived), appConfigs = listOf(appConfig)),
            subscriptions = listOf(
                RawSubscription(id = 21, name = "first", version = 1),
                RawSubscription(id = 22, name = "second", version = 1),
            ),
        )
        assertEquals(listOf(21L, 22L), snapshot.subsItems.map { it.id })
        assertEquals(archived, snapshot.subsItems.first())
        assertEquals(listOf(appConfig), snapshot.appConfigs)
        assertFalse(snapshot.subsItems.last().enable)
    }

    @Test
    fun legacyArchiveGetsSafeDisabledMetadata() {
        val snapshot = completeSubscriptionMetadata(
            database = null,
            subscriptions = listOf(
                RawSubscription(id = 21, name = "first", version = 1),
                RawSubscription(id = 22, name = "second", version = 1),
            ),
        )
        assertEquals(listOf(21L, 22L), snapshot.subsItems.map { it.id })
        assertEquals(listOf(0, 1), snapshot.subsItems.map { it.order })
        assertTrue(snapshot.subsItems.none { it.enable })
        assertTrue(snapshot.subsItems.all { it.enableUpdate })
    }

}
