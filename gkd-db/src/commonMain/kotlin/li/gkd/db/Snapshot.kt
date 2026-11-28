package li.gkd.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    tableName = "snapshot",
)
@Serializable
data class Snapshot(
    @PrimaryKey @ColumnInfo(name = "id") override val id: Long,

    @ColumnInfo(name = "app_id") override val appId: String,
    @ColumnInfo(name = "activity_id") override val activityId: String?,

    @ColumnInfo(name = "screen_height") override val screenHeight: Int,
    @ColumnInfo(name = "screen_width") override val screenWidth: Int,
    @ColumnInfo(name = "is_landscape") override val isLandscape: Boolean,

    @ColumnInfo(name = "github_asset_id") val githubAssetId: Int? = null,

    ) : BaseSnapshot {
    @Dao
    interface SnapshotDao {
        @Insert
        suspend fun insert(vararg users: Snapshot): List<Long>

        @Delete
        suspend fun delete(vararg users: Snapshot): Int

        @Query("SELECT id, CASE WHEN length(app_id) > 1024 THEN '[\"[omitted: diagnostic text exceeds 1024 characters]\"]' ELSE app_id END AS app_id, CASE WHEN length(activity_id) > 1024 THEN '[\"[omitted: diagnostic text exceeds 1024 characters]\"]' ELSE activity_id END AS activity_id, screen_height, screen_width, is_landscape, github_asset_id FROM snapshot WHERE id >= :since AND id <= :until ORDER BY id DESC LIMIT min(max(:limit, 0), 50) OFFSET max(:offset, 0)")
        suspend fun queryForDiagnostics(limit: Int, since: Long = Long.MIN_VALUE, until: Long = Long.MAX_VALUE, offset: Int = 0): List<Snapshot>

        @Query("SELECT * FROM snapshot ORDER BY id DESC")
        fun query(): Flow<List<Snapshot>>

        @Query("SELECT id FROM snapshot ORDER BY id DESC LIMIT min(max(:limit, 0), 100)")
        suspend fun queryRecentIds(limit: Int): List<Long>

        @Query("SELECT id FROM snapshot")
        suspend fun queryIds(): List<Long>

        @Query("DELETE FROM snapshot WHERE id = :id")
        suspend fun deleteById(id: Long): Int

        @Query("UPDATE snapshot SET github_asset_id=null WHERE id = :id")
        suspend fun deleteGithubAssetId(id: Long)

        @Query("UPDATE snapshot SET github_asset_id=:assetId WHERE id=:id AND github_asset_id IS NULL")
        suspend fun markUploadedIfPending(id: Long, assetId: Int): Int

        @Query("SELECT COUNT(*) FROM snapshot")
        fun count(): Flow<Int>
    }
}
