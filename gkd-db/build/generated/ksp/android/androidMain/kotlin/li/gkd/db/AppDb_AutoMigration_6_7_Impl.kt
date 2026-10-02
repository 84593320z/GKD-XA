package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_6_7_Impl : Migration {
  public constructor() : super(6, 7)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `activity_log` (`id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `activity_id` TEXT, PRIMARY KEY(`id`))")
  }
}
