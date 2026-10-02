package li.gkd.db

import androidx.room3.RenameColumn
import androidx.room3.RenameTable
import androidx.room3.migration.AutoMigrationSpec

@RenameTable(fromTableName = "app_config", toTableName = "subs_app_config")
@RenameTable(fromTableName = "category_config", toTableName = "subs_category_config")
@RenameTable(fromTableName = "app_group_config", toTableName = "subs_app_group_config")
@RenameTable(fromTableName = "global_group_config", toTableName = "subs_global_group_config")
@RenameTable(fromTableName = "activity_log_v2", toTableName = "activity_log")
@RenameTable(fromTableName = "app_visit_log", toTableName = "app_last_visit")
@RenameColumn(
    tableName = "app_visit_log",
    fromColumnName = "id",
    toColumnName = "app_id",
)
@RenameColumn(
    tableName = "app_visit_log",
    fromColumnName = "mtime",
    toColumnName = "last_visit_time",
)
@RenameColumn(
    tableName = "a11y_event_log",
    fromColumnName = "appId",
    toColumnName = "app_id",
)
class Migration15To16Spec : AutoMigrationSpec
