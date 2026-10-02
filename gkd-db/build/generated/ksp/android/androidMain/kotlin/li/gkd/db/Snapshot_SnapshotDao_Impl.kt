package li.gkd.db

import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.getTotalChangedRows
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class Snapshot_SnapshotDao_Impl(
  __db: RoomDatabase,
) : Snapshot.SnapshotDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSnapshot: EntityInsertAdapter<Snapshot>

  private val __deleteAdapterOfSnapshot: EntityDeleteOrUpdateAdapter<Snapshot>
  init {
    this.__db = __db
    this.__insertAdapterOfSnapshot = object : EntityInsertAdapter<Snapshot>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `snapshot` (`id`,`app_id`,`activity_id`,`screen_height`,`screen_width`,`is_landscape`,`github_asset_id`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Snapshot) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.appId)
        val _tmpActivityId: String? = entity.activityId
        if (_tmpActivityId == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpActivityId)
        }
        statement.bindLong(4, entity.screenHeight.toLong())
        statement.bindLong(5, entity.screenWidth.toLong())
        val _tmp: Int = if (entity.isLandscape) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        val _tmpGithubAssetId: Int? = entity.githubAssetId
        if (_tmpGithubAssetId == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpGithubAssetId.toLong())
        }
      }
    }
    this.__deleteAdapterOfSnapshot = object : EntityDeleteOrUpdateAdapter<Snapshot>() {
      protected override fun createQuery(): String = "DELETE FROM `snapshot` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Snapshot) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insert(vararg users: Snapshot): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSnapshot.insertAndReturnIdsList(_connection, users)
    _result
  }

  public override suspend fun delete(vararg users: Snapshot): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSnapshot.handleMultipleAndReturnChanges(_connection, users)
    _result
  }

  public override fun query(): Flow<List<Snapshot>> {
    val _sql: String = "SELECT * FROM snapshot ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("snapshot")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
        val _columnIndexOfScreenHeight: Int = getColumnIndexOrThrow(_stmt, "screen_height")
        val _columnIndexOfScreenWidth: Int = getColumnIndexOrThrow(_stmt, "screen_width")
        val _columnIndexOfIsLandscape: Int = getColumnIndexOrThrow(_stmt, "is_landscape")
        val _columnIndexOfGithubAssetId: Int = getColumnIndexOrThrow(_stmt, "github_asset_id")
        val _result: MutableList<Snapshot> = mutableListOf()
        while (_stmt.step()) {
          val _item: Snapshot
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          val _tmpActivityId: String?
          if (_stmt.isNull(_columnIndexOfActivityId)) {
            _tmpActivityId = null
          } else {
            _tmpActivityId = _stmt.getText(_columnIndexOfActivityId)
          }
          val _tmpScreenHeight: Int
          _tmpScreenHeight = _stmt.getLong(_columnIndexOfScreenHeight).toInt()
          val _tmpScreenWidth: Int
          _tmpScreenWidth = _stmt.getLong(_columnIndexOfScreenWidth).toInt()
          val _tmpIsLandscape: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsLandscape).toInt()
          _tmpIsLandscape = _tmp != 0
          val _tmpGithubAssetId: Int?
          if (_stmt.isNull(_columnIndexOfGithubAssetId)) {
            _tmpGithubAssetId = null
          } else {
            _tmpGithubAssetId = _stmt.getLong(_columnIndexOfGithubAssetId).toInt()
          }
          _item = Snapshot(_tmpId,_tmpAppId,_tmpActivityId,_tmpScreenHeight,_tmpScreenWidth,_tmpIsLandscape,_tmpGithubAssetId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun count(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM snapshot"
    return createFlow(__db, false, arrayOf("snapshot")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGithubAssetId(id: Long) {
    val _sql: String = "UPDATE snapshot SET github_asset_id=null WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markUploadedIfPending(id: Long, assetId: Int): Int {
    val _sql: String = "UPDATE snapshot SET github_asset_id=? WHERE id=? AND github_asset_id IS NULL"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, assetId.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredColumnConverters(): List<KClass<*>> = emptyList()

    public fun getRequiredDaoReturnTypeConverters(): List<KClass<*>> = emptyList()
  }
}
