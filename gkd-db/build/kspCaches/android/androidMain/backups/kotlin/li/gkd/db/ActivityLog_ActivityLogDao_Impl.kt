package li.gkd.db

import androidx.paging.PagingSource
import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.coroutines.createFlow
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.getTotalChangedRows
import androidx.room3.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
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
internal class ActivityLog_ActivityLogDao_Impl(
  __db: RoomDatabase,
) : ActivityLog.ActivityLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfActivityLog: EntityInsertAdapter<ActivityLog>

  private val __pagingSourceDaoReturnTypeConverter: PagingSourceDaoReturnTypeConverter =
      PagingSourceDaoReturnTypeConverter()
  init {
    this.__db = __db
    this.__insertAdapterOfActivityLog = object : EntityInsertAdapter<ActivityLog>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `activity_log` (`id`,`ctime`,`app_id`,`activity_id`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ActivityLog) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.ctime)
        statement.bindText(3, entity.appId)
        val _tmpActivityId: String? = entity.activityId
        if (_tmpActivityId == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpActivityId)
        }
      }
    }
  }

  public override suspend fun insert(vararg objects: ActivityLog): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfActivityLog.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override fun pagingSource(): PagingSource<Int, ActivityLog> {
    val _sql: String = "SELECT * FROM activity_log ORDER BY ctime DESC "
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return __pagingSourceDaoReturnTypeConverter.convert(__db, arrayOf("activity_log"), _rawQuery) { _converterQuery ->
      performSuspending<MutableList<ActivityLog>>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
          val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
          val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
          val _result: MutableList<ActivityLog> = mutableListOf()
          while (_stmt.step()) {
            val _item: ActivityLog
            val _tmpId: Int
            _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
            val _tmpCtime: Long
            _tmpCtime = _stmt.getLong(_columnIndexOfCtime)
            val _tmpAppId: String
            _tmpAppId = _stmt.getText(_columnIndexOfAppId)
            val _tmpActivityId: String?
            if (_stmt.isNull(_columnIndexOfActivityId)) {
              _tmpActivityId = null
            } else {
              _tmpActivityId = _stmt.getText(_columnIndexOfActivityId)
            }
            _item = ActivityLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun count(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM activity_log"
    return createFlow(__db, false, arrayOf("activity_log")) { _connection ->
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

  public override suspend fun deleteKeepLatest(): Int {
    val _sql: String = """
        |
        |            DELETE FROM activity_log
        |            WHERE (
        |                    SELECT COUNT(*)
        |                    FROM activity_log
        |                ) > 500
        |                AND ctime <= (
        |                    SELECT ctime
        |                    FROM activity_log
        |                    ORDER BY ctime DESC
        |                    LIMIT 1 OFFSET 500
        |                )
        |        
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
