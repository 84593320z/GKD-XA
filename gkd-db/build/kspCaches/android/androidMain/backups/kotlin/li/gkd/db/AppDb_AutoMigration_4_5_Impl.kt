package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_4_5_Impl : Migration {
  public constructor() : super(4, 5)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `subs_config` ADD COLUMN `exclude` TEXT NOT NULL DEFAULT ''")
  }
}
