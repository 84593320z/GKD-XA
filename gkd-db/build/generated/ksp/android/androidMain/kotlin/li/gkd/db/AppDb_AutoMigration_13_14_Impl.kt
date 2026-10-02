package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_13_14_Impl : Migration {
  public constructor() : super(13, 14)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `a11y_event_log` (`id` INTEGER NOT NULL, `ctime` INTEGER NOT NULL, `type` INTEGER NOT NULL, `appId` TEXT NOT NULL, `name` TEXT NOT NULL, `desc` TEXT, `text` TEXT NOT NULL, PRIMARY KEY(`id`))")
  }
}
