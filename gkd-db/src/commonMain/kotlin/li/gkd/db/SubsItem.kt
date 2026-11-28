package li.gkd.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val LOCAL_SUBS_ID = -2L
const val LOCAL_HTTP_SUBS_ID = -1L
val LOCAL_SUBS_IDS = arrayOf(LOCAL_SUBS_ID, LOCAL_HTTP_SUBS_ID)

@Serializable
@Entity(
    tableName = "subs_item",
)
data class SubsItem(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "ctime") val ctime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "mtime") val mtime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "enable") val enable: Boolean = false,
    @ColumnInfo(name = "enable_update") val enableUpdate: Boolean = true,
    @ColumnInfo(name = "order") val order: Int,
    @ColumnInfo(name = "update_url") val updateUrl: String? = null,

    ) {

    val isLocal: Boolean
        get() = LOCAL_SUBS_IDS.contains(id)

    @Dao
    interface SubsItemDao {
        @Update
        suspend fun update(vararg objects: SubsItem): Int

        @Query("UPDATE subs_item SET enable=:enable WHERE id=:id")
        suspend fun updateEnable(id: Long, enable: Boolean): Int

        @Query("UPDATE subs_item SET `order`=:order WHERE id=:id")
        suspend fun updateOrder(id: Long, order: Int): Int

        /** Updates only URL metadata if the row still matches the editor snapshot. */
        @Query(
            "UPDATE subs_item SET update_url=:updateUrl, mtime=:mtime " +
                "WHERE id=:id AND ctime=:expectedCtime AND mtime=:expectedMtime " +
                "AND (update_url=:expectedUpdateUrl OR (update_url IS NULL AND :expectedUpdateUrl IS NULL))"
        )
        suspend fun updateUrlIfUnchanged(
            id: Long,
            expectedCtime: Long,
            expectedMtime: Long,
            expectedUpdateUrl: String?,
            updateUrl: String?,
            mtime: Long,
        ): Int

        @Transaction
        suspend fun batchUpdateOrder(subsItems: List<SubsItem>) {
            subsItems.forEach { subsItem ->
                updateOrder(subsItem.id, subsItem.order)
            }
        }

        @Upsert
        suspend fun upsert(vararg users: SubsItem): List<Long>

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insertOrIgnore(vararg users: SubsItem): List<Long>

        @Delete
        suspend fun delete(vararg users: SubsItem): Int

        @Query("UPDATE subs_item SET mtime=:mtime WHERE id=:id")
        suspend fun updateMtime(id: Long, mtime: Long = System.currentTimeMillis()): Int

        @Query("SELECT * FROM subs_item ORDER BY `order`")
        fun query(): Flow<List<SubsItem>>

        @Query("SELECT * FROM subs_item ORDER BY `order`")
        suspend fun queryAll(): List<SubsItem>

        @Query("SELECT id, ctime, mtime, enable, enable_update, `order`, CASE WHEN length(update_url) > 1024 THEN '[\"[omitted: diagnostic text exceeds 1024 characters]\"]' ELSE update_url END AS update_url FROM subs_item ORDER BY `order`, id LIMIT min(max(:limit, 0), 50)")
        suspend fun queryForDiagnostics(limit: Int): List<SubsItem>

        @Query("DELETE FROM subs_item WHERE id IN (:ids)")
        suspend fun deleteById(vararg ids: Long): Int
    }

}
