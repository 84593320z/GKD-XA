package li.gkd.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_7_8_Impl : Migration {
  private val callback: AutoMigrationSpec = ActivityLog.ActivityLogV2Spec()

  public constructor() : super(7, 8)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("DROP TABLE `activity_log`")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `activity_log_v2` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ctime` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT)")
    callback.onPostMigrate(connection)
  }
}
