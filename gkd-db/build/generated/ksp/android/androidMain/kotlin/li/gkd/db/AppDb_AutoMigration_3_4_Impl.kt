package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_3_4_Impl : Migration {
  public constructor() : super(3, 4)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `click_log` ADD COLUMN `subs_version` INTEGER NOT NULL DEFAULT 0")
    connection.execSQL("ALTER TABLE `click_log` ADD COLUMN `group_type` INTEGER NOT NULL DEFAULT 2")
  }
}
