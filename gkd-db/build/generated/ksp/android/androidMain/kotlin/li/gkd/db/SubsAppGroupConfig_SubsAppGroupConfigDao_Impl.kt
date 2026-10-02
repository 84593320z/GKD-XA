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
internal class SubsAppGroupConfig_SubsAppGroupConfigDao_Impl(
  __db: RoomDatabase,
) : SubsAppGroupConfig.SubsAppGroupConfigDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubsAppGroupConfig: EntityInsertAdapter<SubsAppGroupConfig>

  private val __deleteAdapterOfSubsAppGroupConfig: EntityDeleteOrUpdateAdapter<SubsAppGroupConfig>

  private val __updateAdapterOfSubsAppGroupConfig: EntityDeleteOrUpdateAdapter<SubsAppGroupConfig>

  private val __upsertAdapterOfSubsAppGroupConfig: EntityUpsertAdapter<SubsAppGroupConfig>
  init {
    this.__db = __db
    this.__insertAdapterOfSubsAppGroupConfig = object : EntityInsertAdapter<SubsAppGroupConfig>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `subs_app_group_config` (`subs_id`,`app_id`,`group_key`,`enable`,`exclude`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
        statement.bindLong(3, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmp.toLong())
        }
        statement.bindText(5, entity.exclude)
      }
    }
    this.__deleteAdapterOfSubsAppGroupConfig = object : EntityDeleteOrUpdateAdapter<SubsAppGroupConfig>() {
      protected override fun createQuery(): String = "DELETE FROM `subs_app_group_config` WHERE `subs_id` = ? AND `app_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
        statement.bindLong(3, entity.groupKey.toLong())
      }
    }
    this.__updateAdapterOfSubsAppGroupConfig = object : EntityDeleteOrUpdateAdapter<SubsAppGroupConfig>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subs_app_group_config` SET `subs_id` = ?,`app_id` = ?,`group_key` = ?,`enable` = ?,`exclude` = ? WHERE `subs_id` = ? AND `app_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
        statement.bindLong(3, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmp.toLong())
        }
        statement.bindText(5, entity.exclude)
        statement.bindLong(6, entity.subsId)
        statement.bindText(7, entity.appId)
        statement.bindLong(8, entity.groupKey.toLong())
      }
    }
    this.__upsertAdapterOfSubsAppGroupConfig = EntityUpsertAdapter<SubsAppGroupConfig>(object : EntityInsertAdapter<SubsAppGroupConfig>() {
      protected override fun createQuery(): String = "INSERT INTO `subs_app_group_config` (`subs_id`,`app_id`,`group_key`,`enable`,`exclude`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
        statement.bindLong(3, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmp.toLong())
        }
        statement.bindText(5, entity.exclude)
      }
    }, object : EntityDeleteOrUpdateAdapter<SubsAppGroupConfig>() {
      protected override fun createQuery(): String = "UPDATE `subs_app_group_config` SET `subs_id` = ?,`app_id` = ?,`group_key` = ?,`enable` = ?,`exclude` = ? WHERE `subs_id` = ? AND `app_id` = ? AND `group_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppGroupConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
        statement.bindLong(3, entity.groupKey.toLong())
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmp.toLong())
        }
        statement.bindText(5, entity.exclude)
        statement.bindLong(6, entity.subsId)
        statement.bindText(7, entity.appId)
        statement.bindLong(8, entity.groupKey.toLong())
      }
    })
  }

  public override suspend fun insertOrIgnore(vararg objects: SubsAppGroupConfig): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubsAppGroupConfig.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override suspend fun delete(vararg objects: SubsAppGroupConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSubsAppGroupConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun update(vararg objects: SubsAppGroupConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfSubsAppGroupConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun upsert(vararg objects: SubsAppGroupConfig): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSubsAppGroupConfig.upsert(_connection, objects)
  }

  public override suspend fun queryAll(): List<SubsAppGroupConfig> {
    val _sql: String = "SELECT * FROM subs_app_group_config"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryBySubsId(subsId: Long): Flow<List<SubsAppGroupConfig>> {
    val _sql: String = "SELECT * FROM subs_app_group_config WHERE subs_id=?"
    return createFlow(__db, false, arrayOf("subs_app_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryConfig(
    subsId: Long,
    appId: String,
    groupKey: Int,
  ): Flow<SubsAppGroupConfig?> {
    val _sql: String = "SELECT * FROM subs_app_group_config WHERE subs_id=? AND app_id=? AND group_key=?"
    return createFlow(__db, false, arrayOf("subs_app_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindText(_argIndex, appId)
        _argIndex = 3
        _stmt.bindLong(_argIndex, groupKey.toLong())
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: SubsAppGroupConfig?
        if (_stmt.step()) {
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _result = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getConfig(
    subsId: Long,
    appId: String,
    groupKey: Int,
  ): SubsAppGroupConfig? {
    val _sql: String = "SELECT * FROM subs_app_group_config WHERE subs_id=? AND app_id=? AND group_key=?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindText(_argIndex, appId)
        _argIndex = 3
        _stmt.bindLong(_argIndex, groupKey.toLong())
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: SubsAppGroupConfig?
        if (_stmt.step()) {
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _result = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun queryBySubsIds(subsIds: List<Long>): List<SubsAppGroupConfig> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_app_group_config WHERE subs_id IN (")
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
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item_1 = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryUsedList(): Flow<List<SubsAppGroupConfig>> {
    val _sql: String = "SELECT * FROM subs_app_group_config WHERE subs_id IN (SELECT id FROM subs_item WHERE enable = 1)"
    return createFlow(__db, false, arrayOf("subs_app_group_config", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryByAppId(subsId: Long, appId: String): Flow<List<SubsAppGroupConfig>> {
    val _sql: String = "SELECT * FROM subs_app_group_config WHERE subs_id=? AND app_id=?"
    return createFlow(__db, false, arrayOf("subs_app_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindText(_argIndex, appId)
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryAppConfig(subsIds: List<Long>, appId: String): Flow<List<SubsAppGroupConfig>> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_app_group_config WHERE app_id=")
    _stringBuilder.append("?")
    _stringBuilder.append(" AND subs_id IN (")
    val _inputSize: Int = subsIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("subs_app_group_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, appId)
        _argIndex = 2
        for (_item: Long in subsIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _columnIndexOfGroupKey: Int = getColumnIndexOrThrow(_stmt, "group_key")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfExclude: Int = getColumnIndexOrThrow(_stmt, "exclude")
        val _result: MutableList<SubsAppGroupConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsAppGroupConfig
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
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
          _item_1 = SubsAppGroupConfig(_tmpSubsId,_tmpAppId,_tmpGroupKey,_tmpEnable,_tmpExclude)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAppConfig(subsId: Long, appId: String): Int {
    val _sql: String = "DELETE FROM subs_app_group_config WHERE subs_id=? AND app_id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindText(_argIndex, appId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGroups(
    subsId: Long,
    appId: String,
    keys: List<Int>,
  ): Int {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM subs_app_group_config WHERE subs_id=")
    _stringBuilder.append("?")
    _stringBuilder.append(" AND app_id=")
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
        _stmt.bindText(_argIndex, appId)
        _argIndex = 3
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
