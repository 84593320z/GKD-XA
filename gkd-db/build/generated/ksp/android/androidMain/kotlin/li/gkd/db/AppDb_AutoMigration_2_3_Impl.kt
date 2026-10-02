package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_2_3_Impl : Migration {
  public constructor() : super(2, 3)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `category_config` (`id` INTEGER NOT NULL, `enable` INTEGER, `subs_item_id` INTEGER NOT NULL, `category_key` INTEGER NOT NULL, PRIMARY KEY(`id`))")
  }
}
