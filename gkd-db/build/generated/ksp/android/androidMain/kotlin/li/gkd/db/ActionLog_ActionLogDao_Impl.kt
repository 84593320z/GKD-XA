package li.gkd.db

import androidx.paging.PagingSource
import androidx.room3.EntityInsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.coroutines.createFlow
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.room3.util.appendPlaceholders
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class ActionLog_ActionLogDao_Impl(
  __db: RoomDatabase,
) : ActionLog.ActionLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfActionLog: EntityInsertAdapter<ActionLog>

  private val __pagingSourceDaoReturnTypeConverter: PagingSourceDaoReturnTypeConverter =
      PagingSourceDaoReturnTypeConverter()
  init {
    this.__db = __db
    this.__insertAdapterOfActionLog = object : EntityInsertAdapter<ActionLog>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `action_log` (`id`,`ctime`,`app_id`,`activity_id`,`subs_id`,`subs_version`,`group_key`,`group_type`,`rule_index`,`rule_key`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ActionLog) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.ctime)
        statement.bindText(3, entity.appId)
        val _tmpActivityId: String? = entity.activityId
        if (_tmpActivityId == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpActivityId)
        }
        statement.bindLong(5, entity.subsId)
        statement.bindLong(6, entity.subsVersion.toLong())
        statement.bindLong(7, entity.groupKey.toLong())
        statement.bindLong(8, entity.groupType.toLong())
        statement.bindLong(9, entity.ruleIndex.toLong())
        val _tmpRuleKey: Int? = entity.ruleKey
        if (_tmpRuleKey == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpRuleKey.toLong())
        }
      }
    }
  }

  public override suspend fun insert(vararg objects: ActionLog): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfActionLog.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override fun query(): Flow<List<ActionLog>> {
    val _sql: String = "SELECT * FROM action_log ORDER BY id DESC LIMIT 1000"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
        val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
        val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
        val _result: MutableList<ActionLog> = mutableListOf()
        while (_stmt.step()) {
          val _item: ActionLog
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
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpSubsVersion: Int
          _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpGroupType: Int
          _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
          val _tmpRuleIndex: Int
          _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
          val _tmpRuleKey: Int?
          if (_stmt.isNull(_columnIndexOfRuleKey)) {
            _tmpRuleKey = null
          } else {
            _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
          }
          _item = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun pagingSource(): PagingSource<Int, ActionLog> {
    val _sql: String = "SELECT * FROM action_log ORDER BY id DESC "
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return __pagingSourceDaoReturnTypeConverter.convert(__db, arrayOf("action_log"), _rawQuery) { _converterQuery ->
      performSuspending<MutableList<ActionLog>>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
          val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
          val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
          val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
          val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
          val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
          val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
          val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
          val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
          val _result: MutableList<ActionLog> = mutableListOf()
          while (_stmt.step()) {
            val _item: ActionLog
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
            val _tmpSubsId: Long
            _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
            val _tmpSubsVersion: Int
            _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
            val _tmpGroupKey: Int
            _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
            val _tmpGroupType: Int
            _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
            val _tmpRuleIndex: Int
            _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
            val _tmpRuleKey: Int?
            if (_stmt.isNull(_columnIndexOfRuleKey)) {
              _tmpRuleKey = null
            } else {
              _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
            }
            _item = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun pagingSubsSource(subsId: Long): PagingSource<Int, ActionLog> {
    val _sql: String = "SELECT * FROM action_log WHERE subs_id=? ORDER BY id DESC "
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql) { _stmt ->
      var _argIndex: Int = 1
      _stmt.bindLong(_argIndex, subsId)
    }
    return __pagingSourceDaoReturnTypeConverter.convert(__db, arrayOf("action_log"), _rawQuery) { _converterQuery ->
      performSuspending<MutableList<ActionLog>>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
          val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
          val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
          val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
          val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
          val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
          val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
          val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
          val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
          val _result: MutableList<ActionLog> = mutableListOf()
          while (_stmt.step()) {
            val _item: ActionLog
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
            val _tmpSubsId: Long
            _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
            val _tmpSubsVersion: Int
            _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
            val _tmpGroupKey: Int
            _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
            val _tmpGroupType: Int
            _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
            val _tmpRuleIndex: Int
            _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
            val _tmpRuleKey: Int?
            if (_stmt.isNull(_columnIndexOfRuleKey)) {
              _tmpRuleKey = null
            } else {
              _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
            }
            _item = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun pagingAppSource(appId: String): PagingSource<Int, ActionLog> {
    val _sql: String = "SELECT * FROM action_log WHERE app_id=? ORDER BY id DESC "
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql) { _stmt ->
      var _argIndex: Int = 1
      _stmt.bindText(_argIndex, appId)
    }
    return __pagingSourceDaoReturnTypeConverter.convert(__db, arrayOf("action_log"), _rawQuery) { _converterQuery ->
      performSuspending<MutableList<ActionLog>>(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(_converterQuery.sql)
        try {
          _converterQuery.getBindingFunction().invoke(_stmt)
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
          val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
          val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
          val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
          val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
          val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
          val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
          val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
          val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
          val _result: MutableList<ActionLog> = mutableListOf()
          while (_stmt.step()) {
            val _item: ActionLog
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
            val _tmpSubsId: Long
            _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
            val _tmpSubsVersion: Int
            _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
            val _tmpGroupKey: Int
            _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
            val _tmpGroupType: Int
            _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
            val _tmpRuleIndex: Int
            _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
            val _tmpRuleKey: Int?
            if (_stmt.isNull(_columnIndexOfRuleKey)) {
              _tmpRuleKey = null
            } else {
              _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
            }
            _item = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
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
    val _sql: String = "SELECT COUNT(*) FROM action_log"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
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

  public override fun queryLatest(): Flow<ActionLog?> {
    val _sql: String = "SELECT * FROM action_log ORDER BY id DESC LIMIT 1"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
        val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
        val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
        val _result: ActionLog?
        if (_stmt.step()) {
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
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpSubsVersion: Int
          _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpGroupType: Int
          _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
          val _tmpRuleIndex: Int
          _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
          val _tmpRuleKey: Int?
          if (_stmt.isNull(_columnIndexOfRuleKey)) {
            _tmpRuleKey = null
          } else {
            _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
          }
          _result = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryLatestByAppId(appId: String): Flow<List<ActionLog>> {
    val _sql: String = """
        |
        |            SELECT cl.* FROM action_log AS cl
        |            INNER JOIN (
        |                SELECT subs_id, group_type, group_key, MAX(id) AS max_id FROM action_log
        |                WHERE app_id = ? AND subs_id IN (SELECT si.id FROM subs_item si WHERE si.enable = 1)
        |                GROUP BY subs_id, group_type, group_key
        |            ) AS latest_log ON cl.subs_id = latest_log.subs_id 
        |            AND cl.group_type = latest_log.group_type
        |            AND cl.group_key = latest_log.group_key
        |            AND cl.id = latest_log.max_id
        |        
        """.trimMargin()
    return createFlow(__db, false, arrayOf("action_log", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, appId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfActivityId: Int = getColumnIndexOrThrow(_stmt, "activity_id")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfSubsVersion: Int = getColumnIndexOrThrow(_stmt, "subs_version")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfGroupType: Int = getColumnIndexOrThrow(_stmt, "group_type")
        val _columnIndexOfRuleIndex: Int = getColumnIndexOrThrow(_stmt, "rule_index")
        val _columnIndexOfRuleKey: Int = getColumnIndexOrThrow(_stmt, "rule_key")
        val _result: MutableList<ActionLog> = mutableListOf()
        while (_stmt.step()) {
          val _item: ActionLog
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
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpSubsVersion: Int
          _tmpSubsVersion = _stmt.getLong(_columnIndexOfSubsVersion).toInt()
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpGroupType: Int
          _tmpGroupType = _stmt.getLong(_columnIndexOfGroupType).toInt()
          val _tmpRuleIndex: Int
          _tmpRuleIndex = _stmt.getLong(_columnIndexOfRuleIndex).toInt()
          val _tmpRuleKey: Int?
          if (_stmt.isNull(_columnIndexOfRuleKey)) {
            _tmpRuleKey = null
          } else {
            _tmpRuleKey = _stmt.getLong(_columnIndexOfRuleKey).toInt()
          }
          _item = ActionLog(_tmpId,_tmpCtime,_tmpAppId,_tmpActivityId,_tmpSubsId,_tmpSubsVersion,_tmpGroupKey,_tmpGroupType,_tmpRuleIndex,_tmpRuleKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryLatestUniqueAppIds(): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT app_id FROM action_log ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
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

  public override fun queryLatestUniqueAppIds(subsItemId: Long): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT app_id FROM action_log WHERE subs_id=? AND group_type=2 ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsItemId)
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

  public override fun queryLatestUniqueAppIds(subsItemId: Long, globalGroupKey: Int): Flow<List<String>> {
    val _sql: String = "SELECT DISTINCT app_id FROM action_log WHERE subs_id=? AND group_key=? AND group_type=3 ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("action_log")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsItemId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, globalGroupKey.toLong())
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

  public override suspend fun deleteBySubsId(vararg subsIds: Long): Int {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM action_log WHERE subs_id IN (")
    val _inputSize: Int = subsIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in subsIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteKeepLatest(): Int {
    val _sql: String = """
        |
        |            DELETE FROM action_log
        |            WHERE (
        |                    SELECT COUNT(*)
        |                    FROM action_log
        |                ) > 500
        |                AND id <= (
        |                    SELECT id
        |                    FROM action_log
        |                    ORDER BY id DESC
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
