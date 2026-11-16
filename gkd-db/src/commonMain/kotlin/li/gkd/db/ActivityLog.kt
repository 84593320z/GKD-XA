package li.gkd.db

import androidx.paging.PagingSource
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.DeleteTable
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "activity_log",
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "ctime") val ctime: Long,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "activity_id") val activityId: String? = null,
) {
    @Dao
    @DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
    interface ActivityLogDao {
        @Insert
        suspend fun insert(vararg objects: ActivityLog): List<Long>

        @Query("SELECT id, ctime, CASE WHEN length(app_id) > 1024 THEN '[\"[omitted: diagnostic text exceeds 1024 characters]\"]' ELSE app_id END AS app_id, CASE WHEN length(activity_id) > 1024 THEN '[\"[omitted: diagnostic text exceeds 1024 characters]\"]' ELSE activity_id END AS activity_id FROM activity_log ORDER BY ctime DESC, id DESC LIMIT min(max(:limit, 0), 50) OFFSET max(:offset, 0)")
        suspend fun queryForDiagnostics(limit: Int, offset: Int = 0): List<ActivityLog>

        @Query("SELECT * FROM activity_log ORDER BY ctime DESC ")
        fun pagingSource(): PagingSource<Int, ActivityLog>

        @Query("SELECT COUNT(*) FROM activity_log")
        fun count(): Flow<Int>

        @Query(
            """
            DELETE FROM activity_log
            WHERE (
                    SELECT COUNT(*)
                    FROM activity_log
                ) > 500
                AND ctime <= (
                    SELECT ctime
                    FROM activity_log
                    ORDER BY ctime DESC
                    LIMIT 1 OFFSET 500
                )
        """
        )
        suspend fun deleteKeepLatest(): Int
    }


    @DeleteTable.Entries(
        DeleteTable(tableName = "activity_log")
    )
    class ActivityLogV2Spec : AutoMigrationSpec
}
