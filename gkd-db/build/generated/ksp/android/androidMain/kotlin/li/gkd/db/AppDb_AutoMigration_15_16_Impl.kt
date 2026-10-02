package li.gkd.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_15_16_Impl : Migration {
  private val callback: AutoMigrationSpec = Migration15To16Spec()

  public constructor() : super(15, 16)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `app_group_config` RENAME TO `subs_app_group_config`")
    connection.execSQL("ALTER TABLE `global_group_config` RENAME TO `subs_global_group_config`")
    connection.execSQL("ALTER TABLE `category_config` RENAME TO `subs_category_config`")
    connection.execSQL("ALTER TABLE `activity_log_v2` RENAME TO `activity_log`")
    connection.execSQL("ALTER TABLE `app_config` RENAME TO `subs_app_config`")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_app_last_visit` (`app_id` TEXT NOT NULL, `last_visit_time` INTEGER NOT NULL, PRIMARY KEY(`app_id`))")
    connection.execSQL("INSERT INTO `_new_app_last_visit` (`app_id`,`last_visit_time`) SELECT `id`,`mtime` FROM `app_visit_log`")
    connection.execSQL("DROP TABLE `app_visit_log`")
    connection.execSQL("ALTER TABLE `_new_app_last_visit` RENAME TO `app_last_visit`")
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_a11y_event_log` (`id` INTEGER NOT NULL, `ctime` INTEGER NOT NULL, `type` INTEGER NOT NULL, `app_id` TEXT NOT NULL, `name` TEXT NOT NULL, `desc` TEXT, `text` TEXT NOT NULL, PRIMARY KEY(`id`))")
    connection.execSQL("INSERT INTO `_new_a11y_event_log` (`id`,`ctime`,`type`,`app_id`,`name`,`desc`,`text`) SELECT `id`,`ctime`,`type`,`appId`,`name`,`desc`,`text` FROM `a11y_event_log`")
    connection.execSQL("DROP TABLE `a11y_event_log`")
    connection.execSQL("ALTER TABLE `_new_a11y_event_log` RENAME TO `a11y_event_log`")
    callback.onPostMigrate(connection)
  }
}
