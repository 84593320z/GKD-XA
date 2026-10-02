package li.gkd.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_5_6_Impl : Migration {
  public constructor() : super(5, 6)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_subs_config` (`id` INTEGER NOT NULL, `type` INTEGER NOT NULL, `enable` INTEGER, `subs_item_id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `group_key` INTEGER NOT NULL, `exclude` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))")
    connection.execSQL("INSERT INTO `_new_subs_config` (`id`,`type`,`enable`,`subs_item_id`,`app_id`,`group_key`,`exclude`) SELECT `id`,`type`,`enable`,`subs_item_id`,`app_id`,`group_key`,`exclude` FROM `subs_config`")
    connection.execSQL("DROP TABLE `subs_config`")
    connection.execSQL("ALTER TABLE `_new_subs_config` RENAME TO `subs_config`")
  }
}
