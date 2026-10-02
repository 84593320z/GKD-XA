package li.gkd.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_9_10_Impl : Migration {
  private val callback: AutoMigrationSpec = Migration9To10Spec()

  public constructor() : super(9, 10)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `subs_id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, PRIMARY KEY(`id`))")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_subs_config` (`id` INTEGER NOT NULL, `type` INTEGER NOT NULL, `enable` INTEGER, `subs_id` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `group_key` INTEGER NOT NULL, `exclude` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`id`))")
    connection.execSQL("INSERT INTO `_new_subs_config` (`id`,`type`,`enable`,`subs_id`,`app_id`,`group_key`,`exclude`) SELECT `id`,`type`,`enable`,`subs_item_id`,`app_id`,`group_key`,`exclude` FROM `subs_config`")
    connection.execSQL("DROP TABLE `subs_config`")
    connection.execSQL("ALTER TABLE `_new_subs_config` RENAME TO `subs_config`")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_category_config` (`id` INTEGER NOT NULL, `enable` INTEGER, `subs_id` INTEGER NOT NULL, `category_key` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    connection.execSQL("INSERT INTO `_new_category_config` (`id`,`enable`,`subs_id`,`category_key`) SELECT `id`,`enable`,`subs_item_id`,`category_key` FROM `category_config`")
    connection.execSQL("DROP TABLE `category_config`")
    connection.execSQL("ALTER TABLE `_new_category_config` RENAME TO `category_config`")
    callback.onPostMigrate(connection)
  }
}
