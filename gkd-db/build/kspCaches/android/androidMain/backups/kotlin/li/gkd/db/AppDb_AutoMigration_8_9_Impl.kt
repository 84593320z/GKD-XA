package li.gkd.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_8_9_Impl : Migration {
  private val callback: AutoMigrationSpec = ActionLog.ActionLogSpec()

  public constructor() : super(8, 9)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("DROP TABLE `click_log`")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `action_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ctime` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT, `subs_id` INTEGER NOT NULL, `subs_version` INTEGER NOT NULL DEFAULT 0, `group_key` INTEGER NOT NULL, `group_type` INTEGER NOT NULL DEFAULT 2, `rule_index` INTEGER NOT NULL, `rule_key` INTEGER)")
    callback.onPostMigrate(connection)
  }
}
