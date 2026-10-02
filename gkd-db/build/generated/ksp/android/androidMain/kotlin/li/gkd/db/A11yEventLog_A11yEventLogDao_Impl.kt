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
internal class A11yEventLog_A11yEventLogDao_Impl(
  __db: RoomDatabase,
) : A11yEventLog.A11yEventLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfA11yEventLog: EntityInsertAdapter<A11yEventLog>

  private val __dbConverters: DbConverters = DbConverters()

  private val __pagingSourceDaoReturnTypeConverter: PagingSourceDaoReturnTypeConverter =
      PagingSourceDaoReturnTypeConverter()
  init {
    this.__db = __db
    this.__insertAdapterOfA11yEventLog = object : EntityInsertAdapter<A11yEventLog>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `a11y_event_log` (`id`,`ctime`,`type`,`app_id`,`name`,`desc`,`text`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: A11yEventLog) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.ctime)
        statement.bindLong(3, entity.type.toLong())
        statement.bindText(4, entity.appId)
        statement.bindText(5, entity.name)
        val _tmpDesc: String? = entity.desc
        if (_tmpDesc == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpDesc)
        }
        val _tmp: String = __dbConverters.fromListStringToString(entity.text)
        statement.bindText(7, _tmp)
      }
    }
  }

  public override suspend fun insert(objects: List<A11yEventLog>): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfA11yEventLog.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override fun count(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM a11y_event_log"
    return createFlow(__db, false, arrayOf("a11y_event_log")) { _connection ->
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

  public override fun pagingSource(): PagingSource<Int, A11yEventLog> {
    val _sql: String = "SELECT * FROM a11y_event_log ORDER BY ctime DESC "
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return __pagingSourceDaoReturnTypeConverter.convert(__db, arrayOf("a11y_event_log"), _rawQuery) { _converterQuery ->
      performSuspending<MutableList<A11yEventLog>>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
          val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
          val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
          val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
          val _columnIndexOfDesc: Int = getColumnIndexOrThrow(_stmt, "desc")
          val _columnIndexOfText: Int = getColumnIndexOrThrow(_stmt, "text")
          val _result: MutableList<A11yEventLog> = mutableListOf()
          while (_stmt.step()) {
            val _item: A11yEventLog
            val _tmpId: Int
            _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
            val _tmpCtime: Long
            _tmpCtime = _stmt.getLong(_columnIndexOfCtime)
            val _tmpType: Int
            _tmpType = _stmt.getLong(_columnIndexOfType).toInt()
            val _tmpAppId: String
            _tmpAppId = _stmt.getText(_columnIndexOfAppId)
            val _tmpName: String
            _tmpName = _stmt.getText(_columnIndexOfName)
            val _tmpDesc: String?
            if (_stmt.isNull(_columnIndexOfDesc)) {
              _tmpDesc = null
            } else {
              _tmpDesc = _stmt.getText(_columnIndexOfDesc)
            }
            val _tmpText: List<String>
            val _tmp: String
            _tmp = _stmt.getText(_columnIndexOfText)
            _tmpText = __dbConverters.fromStringToList(_tmp)
            _item = A11yEventLog(_tmpId,_tmpCtime,_tmpType,_tmpAppId,_tmpName,_tmpDesc,_tmpText)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override suspend fun maxId(): Int? {
    val _sql: String = "SELECT MAX(id) FROM a11y_event_log"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
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
        |            DELETE FROM a11y_event_log
        |            WHERE (
        |                    SELECT COUNT(*)
        |                    FROM a11y_event_log
        |                ) > 1000
        |                AND id <= (
        |                    SELECT id
        |                    FROM a11y_event_log
        |                    ORDER BY id DESC
        |                    LIMIT 1 OFFSET 1000
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
