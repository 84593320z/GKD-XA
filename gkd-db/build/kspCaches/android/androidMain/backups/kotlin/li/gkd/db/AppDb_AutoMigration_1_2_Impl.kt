package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_1_2_Impl : Migration {
  public constructor() : super(1, 2)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `snapshot` ADD COLUMN `github_asset_id` INTEGER DEFAULT NULL")
  }
}
