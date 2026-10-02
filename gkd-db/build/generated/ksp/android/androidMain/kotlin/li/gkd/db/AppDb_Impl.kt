package li.gkd.db

import androidx.room3.InvalidationTracker
import androidx.room3.RoomOpenDelegate
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.room3.util.TableInfo
import androidx.room3.util.TableInfo.Companion.read
import androidx.room3.util.dropFtsSyncTriggers
import androidx.room3.util.performClear
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_Impl : AppDb() {
  private val _subsItem: Lazy<SubsItem.SubsItemDao> = lazy {
    SubsItem_SubsItemDao_Impl(this)
  }

  private val _snapshot: Lazy<Snapshot.SnapshotDao> = lazy {
    Snapshot_SnapshotDao_Impl(this)
  }

  private val _subsAppGroupConfig: Lazy<SubsAppGroupConfig.SubsAppGroupConfigDao> = lazy {
    SubsAppGroupConfig_SubsAppGroupConfigDao_Impl(this)
  }

  private val _subsGlobalGroupConfig: Lazy<SubsGlobalGroupConfig.SubsGlobalGroupConfigDao> = lazy {
    SubsGlobalGroupConfig_SubsGlobalGroupConfigDao_Impl(this)
  }

  private val _subsAppConfig: Lazy<SubsAppConfig.SubsAppConfigDao> = lazy {
    SubsAppConfig_SubsAppConfigDao_Impl(this)
  }

  private val _subsCategoryConfig: Lazy<SubsCategoryConfig.SubsCategoryConfigDao> = lazy {
    SubsCategoryConfig_SubsCategoryConfigDao_Impl(this)
  }

  private val _actionLog: Lazy<ActionLog.ActionLogDao> = lazy {
    ActionLog_ActionLogDao_Impl(this)
  }

  private val _activityLog: Lazy<ActivityLog.ActivityLogDao> = lazy {
    ActivityLog_ActivityLogDao_Impl(this)
  }

  private val _appLastVisit: Lazy<AppLastVisit.AppLastVisitDao> = lazy {
    AppLastVisit_AppLastVisitDao_Impl(this)
  }

  private val _a11yEventLog: Lazy<A11yEventLog.A11yEventLogDao> = lazy {
    A11yEventLog_A11yEventLogDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(16, "3f8468f8c021868fb0740932f96df404", "163d33a7ba1ccfd2ac6d8dfadf4a0fa7") {
      public override suspend fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subs_item` (`id` INTEGER NOT NULL, `ctime` INTEGER NOT NULL, `mtime` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `enable_update` INTEGER NOT NULL, `order` INTEGER NOT NULL, `update_url` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `snapshot` (`id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT, `screen_height` INTEGER NOT NULL, `screen_width` INTEGER NOT NULL, `is_landscape` INTEGER NOT NULL, `github_asset_id` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subs_app_group_config` (`subs_id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `group_key` INTEGER NOT NULL, `enable` INTEGER, `exclude` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`subs_id`, `app_id`, `group_key`), FOREIGN KEY(`subs_id`) REFERENCES `subs_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subs_global_group_config` (`subs_id` INTEGER NOT NULL, `group_key` INTEGER NOT NULL, `enable` INTEGER, `exclude` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`subs_id`, `group_key`), FOREIGN KEY(`subs_id`) REFERENCES `subs_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subs_category_config` (`enable` INTEGER, `subs_id` INTEGER NOT NULL, `category_key` INTEGER NOT NULL, PRIMARY KEY(`subs_id`, `category_key`), FOREIGN KEY(`subs_id`) REFERENCES `subs_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `action_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ctime` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT, `subs_id` INTEGER NOT NULL, `subs_version` INTEGER NOT NULL DEFAULT 0, `group_key` INTEGER NOT NULL, `group_type` INTEGER NOT NULL DEFAULT 2, `rule_index` INTEGER NOT NULL, `rule_key` INTEGER)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `activity_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ctime` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subs_app_config` (`enable` INTEGER NOT NULL, `subs_id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, PRIMARY KEY(`subs_id`, `app_id`), FOREIGN KEY(`subs_id`) REFERENCES `subs_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `app_last_visit` (`app_id` TEXT NOT NULL, `last_visit_time` INTEGER NOT NULL, PRIMARY KEY(`app_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `a11y_event_log` (`id` INTEGER NOT NULL, `ctime` INTEGER NOT NULL, `type` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `name` TEXT NOT NULL, `desc` TEXT, `text` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3f8468f8c021868fb0740932f96df404')")
      }

      public override suspend fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `subs_item`")
        connection.execSQL("DROP TABLE IF EXISTS `snapshot`")
        connection.execSQL("DROP TABLE IF EXISTS `subs_app_group_config`")
        connection.execSQL("DROP TABLE IF EXISTS `subs_global_group_config`")
        connection.execSQL("DROP TABLE IF EXISTS `subs_category_config`")
        connection.execSQL("DROP TABLE IF EXISTS `action_log`")
        connection.execSQL("DROP TABLE IF EXISTS `activity_log`")
        connection.execSQL("DROP TABLE IF EXISTS `subs_app_config`")
        connection.execSQL("DROP TABLE IF EXISTS `app_last_visit`")
        connection.execSQL("DROP TABLE IF EXISTS `a11y_event_log`")
      }

      public override suspend fun onCreate(connection: SQLiteConnection) {
      }

      public override suspend fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override suspend fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override suspend fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override suspend fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsSubsItem: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubsItem.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("ctime", TableInfo.Column("ctime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("mtime", TableInfo.Column("mtime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("enable", TableInfo.Column("enable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("enable_update", TableInfo.Column("enable_update", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("order", TableInfo.Column("order", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsItem.put("update_url", TableInfo.Column("update_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubsItem: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSubsItem: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubsItem: TableInfo = TableInfo("subs_item", _columnsSubsItem, _foreignKeysSubsItem, _indicesSubsItem)
        val _existingSubsItem: TableInfo = read(connection, "subs_item")
        if (!_infoSubsItem.equals(_existingSubsItem)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subs_item(li.gkd.db.SubsItem).
              | Expected:
              |""".trimMargin() + _infoSubsItem + """
              |
              | Found:
              |""".trimMargin() + _existingSubsItem)
        }
        val _columnsSnapshot: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSnapshot.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("app_id", TableInfo.Column("app_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("activity_id", TableInfo.Column("activity_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("screen_height", TableInfo.Column("screen_height", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("screen_width", TableInfo.Column("screen_width", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("is_landscape", TableInfo.Column("is_landscape", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSnapshot.put("github_asset_id", TableInfo.Column("github_asset_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSnapshot: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSnapshot: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSnapshot: TableInfo = TableInfo("snapshot", _columnsSnapshot, _foreignKeysSnapshot, _indicesSnapshot)
        val _existingSnapshot: TableInfo = read(connection, "snapshot")
        if (!_infoSnapshot.equals(_existingSnapshot)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |snapshot(li.gkd.db.Snapshot).
              | Expected:
              |""".trimMargin() + _infoSnapshot + """
              |
              | Found:
              |""".trimMargin() + _existingSnapshot)
        }
        val _columnsSubsAppGroupConfig: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubsAppGroupConfig.put("subs_id", TableInfo.Column("subs_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppGroupConfig.put("app_id", TableInfo.Column("app_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppGroupConfig.put("group_key", TableInfo.Column("group_key", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppGroupConfig.put("enable", TableInfo.Column("enable", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppGroupConfig.put("exclude", TableInfo.Column("exclude", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubsAppGroupConfig: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSubsAppGroupConfig.add(TableInfo.ForeignKey("subs_item", "CASCADE", "NO ACTION", listOf("subs_id"), listOf("id")))
        val _indicesSubsAppGroupConfig: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubsAppGroupConfig: TableInfo = TableInfo("subs_app_group_config", _columnsSubsAppGroupConfig, _foreignKeysSubsAppGroupConfig, _indicesSubsAppGroupConfig)
        val _existingSubsAppGroupConfig: TableInfo = read(connection, "subs_app_group_config")
        if (!_infoSubsAppGroupConfig.equals(_existingSubsAppGroupConfig)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subs_app_group_config(li.gkd.db.SubsAppGroupConfig).
              | Expected:
              |""".trimMargin() + _infoSubsAppGroupConfig + """
              |
              | Found:
              |""".trimMargin() + _existingSubsAppGroupConfig)
        }
        val _columnsSubsGlobalGroupConfig: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubsGlobalGroupConfig.put("subs_id", TableInfo.Column("subs_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsGlobalGroupConfig.put("group_key", TableInfo.Column("group_key", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsGlobalGroupConfig.put("enable", TableInfo.Column("enable", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsGlobalGroupConfig.put("exclude", TableInfo.Column("exclude", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubsGlobalGroupConfig: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSubsGlobalGroupConfig.add(TableInfo.ForeignKey("subs_item", "CASCADE", "NO ACTION", listOf("subs_id"), listOf("id")))
        val _indicesSubsGlobalGroupConfig: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubsGlobalGroupConfig: TableInfo = TableInfo("subs_global_group_config", _columnsSubsGlobalGroupConfig, _foreignKeysSubsGlobalGroupConfig, _indicesSubsGlobalGroupConfig)
        val _existingSubsGlobalGroupConfig: TableInfo = read(connection, "subs_global_group_config")
        if (!_infoSubsGlobalGroupConfig.equals(_existingSubsGlobalGroupConfig)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subs_global_group_config(li.gkd.db.SubsGlobalGroupConfig).
              | Expected:
              |""".trimMargin() + _infoSubsGlobalGroupConfig + """
              |
              | Found:
              |""".trimMargin() + _existingSubsGlobalGroupConfig)
        }
        val _columnsSubsCategoryConfig: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubsCategoryConfig.put("enable", TableInfo.Column("enable", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsCategoryConfig.put("subs_id", TableInfo.Column("subs_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsCategoryConfig.put("category_key", TableInfo.Column("category_key", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubsCategoryConfig: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSubsCategoryConfig.add(TableInfo.ForeignKey("subs_item", "CASCADE", "NO ACTION", listOf("subs_id"), listOf("id")))
        val _indicesSubsCategoryConfig: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubsCategoryConfig: TableInfo = TableInfo("subs_category_config", _columnsSubsCategoryConfig, _foreignKeysSubsCategoryConfig, _indicesSubsCategoryConfig)
        val _existingSubsCategoryConfig: TableInfo = read(connection, "subs_category_config")
        if (!_infoSubsCategoryConfig.equals(_existingSubsCategoryConfig)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subs_category_config(li.gkd.db.SubsCategoryConfig).
              | Expected:
              |""".trimMargin() + _infoSubsCategoryConfig + """
              |
              | Found:
              |""".trimMargin() + _existingSubsCategoryConfig)
        }
        val _columnsActionLog: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsActionLog.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("ctime", TableInfo.Column("ctime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("app_id", TableInfo.Column("app_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("activity_id", TableInfo.Column("activity_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("subs_id", TableInfo.Column("subs_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("subs_version", TableInfo.Column("subs_version", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("group_key", TableInfo.Column("group_key", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("group_type", TableInfo.Column("group_type", "INTEGER", true, 0, "2", TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("rule_index", TableInfo.Column("rule_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActionLog.put("rule_key", TableInfo.Column("rule_key", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysActionLog: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesActionLog: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoActionLog: TableInfo = TableInfo("action_log", _columnsActionLog, _foreignKeysActionLog, _indicesActionLog)
        val _existingActionLog: TableInfo = read(connection, "action_log")
        if (!_infoActionLog.equals(_existingActionLog)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |action_log(li.gkd.db.ActionLog).
              | Expected:
              |""".trimMargin() + _infoActionLog + """
              |
              | Found:
              |""".trimMargin() + _existingActionLog)
        }
        val _columnsActivityLog: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsActivityLog.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActivityLog.put("ctime", TableInfo.Column("ctime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActivityLog.put("app_id", TableInfo.Column("app_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsActivityLog.put("activity_id", TableInfo.Column("activity_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysActivityLog: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesActivityLog: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoActivityLog: TableInfo = TableInfo("activity_log", _columnsActivityLog, _foreignKeysActivityLog, _indicesActivityLog)
        val _existingActivityLog: TableInfo = read(connection, "activity_log")
        if (!_infoActivityLog.equals(_existingActivityLog)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |activity_log(li.gkd.db.ActivityLog).
              | Expected:
              |""".trimMargin() + _infoActivityLog + """
              |
              | Found:
              |""".trimMargin() + _existingActivityLog)
        }
        val _columnsSubsAppConfig: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubsAppConfig.put("enable", TableInfo.Column("enable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppConfig.put("subs_id", TableInfo.Column("subs_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubsAppConfig.put("app_id", TableInfo.Column("app_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubsAppConfig: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSubsAppConfig.add(TableInfo.ForeignKey("subs_item", "CASCADE", "NO ACTION", listOf("subs_id"), listOf("id")))
        val _indicesSubsAppConfig: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubsAppConfig: TableInfo = TableInfo("subs_app_config", _columnsSubsAppConfig, _foreignKeysSubsAppConfig, _indicesSubsAppConfig)
        val _existingSubsAppConfig: TableInfo = read(connection, "subs_app_config")
        if (!_infoSubsAppConfig.equals(_existingSubsAppConfig)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subs_app_config(li.gkd.db.SubsAppConfig).
              | Expected:
              |""".trimMargin() + _infoSubsAppConfig + """
              |
              | Found:
              |""".trimMargin() + _existingSubsAppConfig)
        }
        val _columnsAppLastVisit: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAppLastVisit.put("app_id", TableInfo.Column("app_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAppLastVisit.put("last_visit_time", TableInfo.Column("last_visit_time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAppLastVisit: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAppLastVisit: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAppLastVisit: TableInfo = TableInfo("app_last_visit", _columnsAppLastVisit, _foreignKeysAppLastVisit, _indicesAppLastVisit)
        val _existingAppLastVisit: TableInfo = read(connection, "app_last_visit")
        if (!_infoAppLastVisit.equals(_existingAppLastVisit)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |app_last_visit(li.gkd.db.AppLastVisit).
              | Expected:
              |""".trimMargin() + _infoAppLastVisit + """
              |
              | Found:
              |""".trimMargin() + _existingAppLastVisit)
        }
        val _columnsA11yEventLog: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsA11yEventLog.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("ctime", TableInfo.Column("ctime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("type", TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("app_id", TableInfo.Column("app_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("desc", TableInfo.Column("desc", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsA11yEventLog.put("text", TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysA11yEventLog: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesA11yEventLog: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoA11yEventLog: TableInfo = TableInfo("a11y_event_log", _columnsA11yEventLog, _foreignKeysA11yEventLog, _indicesA11yEventLog)
        val _existingA11yEventLog: TableInfo = read(connection, "a11y_event_log")
        if (!_infoA11yEventLog.equals(_existingA11yEventLog)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |a11y_event_log(li.gkd.db.A11yEventLog).
              | Expected:
              |""".trimMargin() + _infoA11yEventLog + """
              |
              | Found:
              |""".trimMargin() + _existingA11yEventLog)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "subs_item", "snapshot", "subs_app_group_config", "subs_global_group_config", "subs_category_config", "action_log", "activity_log", "subs_app_config", "app_last_visit", "a11y_event_log")
  }

  public override suspend fun clearAllTables() {
    performClear(this, true, "subs_item", "snapshot", "subs_app_group_config", "subs_global_group_config", "subs_category_config", "action_log", "activity_log", "subs_app_config", "app_last_visit", "a11y_event_log")
  }

  protected override fun getRequiredColumnTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _columnTypeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _columnTypeConvertersMap.put(SubsItem.SubsItemDao::class, SubsItem_SubsItemDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(Snapshot.SnapshotDao::class, Snapshot_SnapshotDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(SubsAppGroupConfig.SubsAppGroupConfigDao::class, SubsAppGroupConfig_SubsAppGroupConfigDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(SubsGlobalGroupConfig.SubsGlobalGroupConfigDao::class, SubsGlobalGroupConfig_SubsGlobalGroupConfigDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(SubsAppConfig.SubsAppConfigDao::class, SubsAppConfig_SubsAppConfigDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(SubsCategoryConfig.SubsCategoryConfigDao::class, SubsCategoryConfig_SubsCategoryConfigDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(ActionLog.ActionLogDao::class, ActionLog_ActionLogDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(ActivityLog.ActivityLogDao::class, ActivityLog_ActivityLogDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(AppLastVisit.AppLastVisitDao::class, AppLastVisit_AppLastVisitDao_Impl.getRequiredColumnConverters())
    _columnTypeConvertersMap.put(A11yEventLog.A11yEventLogDao::class, A11yEventLog_A11yEventLogDao_Impl.getRequiredColumnConverters())
    return _columnTypeConvertersMap
  }

  protected override fun getRequiredDaoReturnTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _daoReturnTypeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _daoReturnTypeConvertersMap.put(SubsItem.SubsItemDao::class, SubsItem_SubsItemDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(Snapshot.SnapshotDao::class, Snapshot_SnapshotDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(SubsAppGroupConfig.SubsAppGroupConfigDao::class, SubsAppGroupConfig_SubsAppGroupConfigDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(SubsGlobalGroupConfig.SubsGlobalGroupConfigDao::class, SubsGlobalGroupConfig_SubsGlobalGroupConfigDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(SubsAppConfig.SubsAppConfigDao::class, SubsAppConfig_SubsAppConfigDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(SubsCategoryConfig.SubsCategoryConfigDao::class, SubsCategoryConfig_SubsCategoryConfigDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(ActionLog.ActionLogDao::class, ActionLog_ActionLogDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(ActivityLog.ActivityLogDao::class, ActivityLog_ActivityLogDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(AppLastVisit.AppLastVisitDao::class, AppLastVisit_AppLastVisitDao_Impl.getRequiredDaoReturnTypeConverters())
    _daoReturnTypeConvertersMap.put(A11yEventLog.A11yEventLogDao::class, A11yEventLog_A11yEventLogDao_Impl.getRequiredDaoReturnTypeConverters())
    return _daoReturnTypeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    _autoMigrations.add(AppDb_AutoMigration_1_2_Impl())
    _autoMigrations.add(AppDb_AutoMigration_2_3_Impl())
    _autoMigrations.add(AppDb_AutoMigration_3_4_Impl())
    _autoMigrations.add(AppDb_AutoMigration_4_5_Impl())
    _autoMigrations.add(AppDb_AutoMigration_5_6_Impl())
    _autoMigrations.add(AppDb_AutoMigration_6_7_Impl())
    _autoMigrations.add(AppDb_AutoMigration_7_8_Impl())
    _autoMigrations.add(AppDb_AutoMigration_8_9_Impl())
    _autoMigrations.add(AppDb_AutoMigration_9_10_Impl())
    _autoMigrations.add(AppDb_AutoMigration_10_11_Impl())
    _autoMigrations.add(AppDb_AutoMigration_11_12_Impl())
    _autoMigrations.add(AppDb_AutoMigration_12_13_Impl())
    _autoMigrations.add(AppDb_AutoMigration_13_14_Impl())
    _autoMigrations.add(AppDb_AutoMigration_15_16_Impl())
    return _autoMigrations
  }

  public override fun subsItemDao(): SubsItem.SubsItemDao = _subsItem.value

  public override fun snapshotDao(): Snapshot.SnapshotDao = _snapshot.value

  public override fun subsAppGroupConfigDao(): SubsAppGroupConfig.SubsAppGroupConfigDao = _subsAppGroupConfig.value

  public override fun subsGlobalGroupConfigDao(): SubsGlobalGroupConfig.SubsGlobalGroupConfigDao = _subsGlobalGroupConfig.value

  public override fun subsAppConfigDao(): SubsAppConfig.SubsAppConfigDao = _subsAppConfig.value

  public override fun subsCategoryConfigDao(): SubsCategoryConfig.SubsCategoryConfigDao = _subsCategoryConfig.value

  public override fun actionLogDao(): ActionLog.ActionLogDao = _actionLog.value

  public override fun activityLogDao(): ActivityLog.ActivityLogDao = _activityLog.value

  public override fun appLastVisitDao(): AppLastVisit.AppLastVisitDao = _appLastVisit.value

  public override fun a11yEventLogDao(): A11yEventLog.A11yEventLogDao = _a11yEventLog.value
}
