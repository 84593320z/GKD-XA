package li.gkd.db

import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class AppDb_AutoMigration_10_11_Impl : Migration {
  private val callback: AutoMigrationSpec = Migration10To11Spec()

  public constructor() : super(10, 11)

  public override suspend fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_snapshot` (`id` INTEGER NOT NULL, `app_id` TEXT, `activity_id` TEXT, `screen_height` INTEGER NOT NULL, `screen_width` INTEGER NOT NULL, `is_landscape` INTEGER NOT NULL, `github_asset_id` INTEGER, PRIMARY KEY(`id`))")
    connection.execSQL("INSERT INTO `_new_snapshot` (`id`,`app_id`,`activity_id`,`screen_height`,`screen_width`,`is_landscape`,`github_asset_id`) SELECT `id`,`app_id`,`activity_id`,`screen_height`,`screen_width`,`is_landscape`,`github_asset_id` FROM `snapshot`")
    connection.execSQL("DROP TABLE `snapshot`")
    connection.execSQL("ALTER TABLE `_new_snapshot` RENAME TO `snapshot`")
    callback.onPostMigrate(connection)
  }
}
