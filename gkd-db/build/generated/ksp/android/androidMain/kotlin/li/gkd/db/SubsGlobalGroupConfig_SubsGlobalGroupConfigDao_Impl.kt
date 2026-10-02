package li.gkd.db

import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
import androidx.room3.util.appendPlaceholders
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
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room3.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL", "MemberExtensionConflict"])
internal class SubsGlobalGroupConfig_SubsGlobalGroupConfigDao_Impl(
  __db: RoomDatabase,
) : SubsGlobalGroupConfig.SubsGlobalGroupConfigDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubsGlobalGroupConfig: EntityInsertAdapter<SubsGlobalGroupConfig>

  private val __deleteAdapterOfSubsGlobalGroupConfig:
      EntityDeleteOrUpdateAdapter<SubsGlobalGroupConfig>

  private val __updateAdapterOfSubsGlobalGroupConfig:
      EntityDeleteOrUpdateAdapter<SubsGlobalGroupConfig>

  private val __upsertAdapterOfSubsGlobalGroupConfig: EntityUpsertAdapter<SubsGlobalGroupConfig>
  init {
    this.__db = __db
    this.__insertAdapterOfSubsGlobalGroupConfig = object : EntityInsertAdapter<SubsGlobalGroupConfig>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `subs_global_group_config` (`subs_id`,`group_key`,`enable`,`exclude`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsGlobalGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp.toLong())
        }
        statement.bindText(4, entity.exclude)
      }
    }
    this.__deleteAdapterOfSubsGlobalGroupConfig = object : EntityDeleteOrUpdateAdapter<SubsGlobalGroupConfig>() {
      protected override fun createQuery(): String = "DELETE FROM `subs_global_group_config` WHERE `subs_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsGlobalGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.groupKey.toLong())
      }
    }
    this.__updateAdapterOfSubsGlobalGroupConfig = object : EntityDeleteOrUpdateAdapter<SubsGlobalGroupConfig>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subs_global_group_config` SET `subs_id` = ?,`group_key` = ?,`enable` = ?,`exclude` = ? WHERE `subs_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsGlobalGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp.toLong())
        }
        statement.bindText(4, entity.exclude)
        statement.bindLong(5, entity.subsId)
        statement.bindLong(6, entity.groupKey.toLong())
      }
    }
    this.__upsertAdapterOfSubsGlobalGroupConfig = EntityUpsertAdapter<SubsGlobalGroupConfig>(object : EntityInsertAdapter<SubsGlobalGroupConfig>() {
      protected override fun createQuery(): String = "INSERT INTO `subs_global_group_config` (`subs_id`,`group_key`,`enable`,`exclude`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsGlobalGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp.toLong())
        }
        statement.bindText(4, entity.exclude)
      }
    }, object : EntityDeleteOrUpdateAdapter<SubsGlobalGroupConfig>() {
      protected override fun createQuery(): String = "UPDATE `subs_global_group_config` SET `subs_id` = ?,`group_key` = ?,`enable` = ?,`exclude` = ? WHERE `subs_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsGlobalGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(3)
        } else {
          statement.bindLong(3, _tmp.toLong())
        }
        statement.bindText(4, entity.exclude)
        statement.bindLong(5, entity.subsId)
        statement.bindLong(6, entity.groupKey.toLong())
      }
    })
  }

  public override suspend fun insertOrIgnore(vararg objects: SubsGlobalGroupConfig): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubsGlobalGroupConfig.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override suspend fun delete(vararg objects: SubsGlobalGroupConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSubsGlobalGroupConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun update(vararg objects: SubsGlobalGroupConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfSubsGlobalGroupConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun upsert(vararg objects: SubsGlobalGroupConfig): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSubsGlobalGroupConfig.upsert(_connection, objects)
  }

  public override suspend fun queryAll(): List<SubsGlobalGroupConfig> {
    val _sql: String = "SELECT * FROM subs_global_group_config"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsGlobalGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsGlobalGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _item = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryBySubsId(subsId: Long): Flow<List<SubsGlobalGroupConfig>> {
    val _sql: String = "SELECT * FROM subs_global_group_config WHERE subs_id=?"
    return createFlow(__db, false, arrayOf("subs_global_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsGlobalGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsGlobalGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _item = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryConfig(subsId: Long, groupKey: Int): Flow<SubsGlobalGroupConfig?> {
    val _sql: String = "SELECT * FROM subs_global_group_config WHERE subs_id=? AND group_key=?"
    return createFlow(__db, false, arrayOf("subs_global_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, groupKey.toLong())
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: SubsGlobalGroupConfig?
        if (_stmt.step()) {
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _result = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getConfig(subsId: Long, groupKey: Int): SubsGlobalGroupConfig? {
    val _sql: String = "SELECT * FROM subs_global_group_config WHERE subs_id=? AND group_key=?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, groupKey.toLong())
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: SubsGlobalGroupConfig?
        if (_stmt.step()) {
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _result = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun queryBySubsIds(subsIds: List<Long>): List<SubsGlobalGroupConfig> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_global_group_config WHERE subs_id IN (")
    val _inputSize: Int = subsIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in subsIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsGlobalGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsGlobalGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _item_1 = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryUsedList(): Flow<List<SubsGlobalGroupConfig>> {
    val _sql: String = "SELECT * FROM subs_global_group_config WHERE subs_id IN (SELECT id FROM subs_item WHERE enable = 1)"
    return createFlow(__db, false, arrayOf("subs_global_group_config", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsGlobalGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsGlobalGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _item = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryGlobalConfig(subsIds: List<Long>): Flow<List<SubsGlobalGroupConfig>> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_global_group_config WHERE subs_id IN (")
    val _inputSize: Int = subsIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("subs_global_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in subsIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsGlobalGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsGlobalGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpGroupKey: Int
          _tmpGroupKey = _stmt.getLong(_columnIndexOfGroupKey).toInt()
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpExclude: String
          _tmpExclude = _stmt.getText(_columnIndexOfExclude)
          _item_1 = SubsGlobalGroupConfig(_tmpSubsId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGroups(subsId: Long, keys: List<Int>): Int {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM subs_global_group_config WHERE subs_id=")
    _stringBuilder.append("?")
    _stringBuilder.append(" AND group_key IN (")
    val _inputSize: Int = keys.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        for (_item: Int in keys) {
          _stmt.bindLong(_argIndex, _item.toLong())
          _argIndex++
        }
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
