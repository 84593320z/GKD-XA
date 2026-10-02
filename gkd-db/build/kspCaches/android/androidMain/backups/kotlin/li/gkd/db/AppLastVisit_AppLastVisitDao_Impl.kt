package li.gkd.db

import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
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
internal class AppLastVisit_AppLastVisitDao_Impl(
  __db: RoomDatabase,
) : AppLastVisit.AppLastVisitDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAppLastVisit: EntityInsertAdapter<AppLastVisit>
  init {
    this.__db = __db
    this.__insertAdapterOfAppLastVisit = object : EntityInsertAdapter<AppLastVisit>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `app_last_visit` (`app_id`,`last_visit_time`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AppLastVisit) {
        statement.bindText(1, entity.appId)
        statement.bindLong(2, entity.lastVisitTime)
      }
    }
  }

  public override suspend fun insert(vararg objects: AppLastVisit): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfAppLastVisit.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override fun query(): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT app_id FROM app_last_visit ORDER BY last_visit_time DESC"
    return createFlow(__db, false, arrayOf("app_last_visit")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
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
        |            DELETE FROM app_last_visit
        |            WHERE (
        |                    SELECT COUNT(*)
        |                    FROM app_last_visit
        |                ) > 500
        |                AND last_visit_time <= (
        |                    SELECT last_visit_time
        |                    FROM app_last_visit
        |                    ORDER BY last_visit_time DESC
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
