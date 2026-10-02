package li.gkd.db

import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
import androidx.room3.util.appendPlaceholders
import androidx.room3.util.getColumnIndexOrThrow
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
internal class SubsAppConfig_SubsAppConfigDao_Impl(
  __db: RoomDatabase,
) : SubsAppConfig.SubsAppConfigDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubsAppConfig: EntityInsertAdapter<SubsAppConfig>

  private val __deleteAdapterOfSubsAppConfig: EntityDeleteOrUpdateAdapter<SubsAppConfig>

  private val __updateAdapterOfSubsAppConfig: EntityDeleteOrUpdateAdapter<SubsAppConfig>

  private val __upsertAdapterOfSubsAppConfig: EntityUpsertAdapter<SubsAppConfig>
  init {
    this.__db = __db
    this.__insertAdapterOfSubsAppConfig = object : EntityInsertAdapter<SubsAppConfig>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `subs_app_config` (`enable`,`subs_id`,`app_id`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppConfig) {
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(1, _tmp.toLong())
        statement.bindLong(2, entity.subsId)
        statement.bindText(3, entity.appId)
      }
    }
    this.__deleteAdapterOfSubsAppConfig = object : EntityDeleteOrUpdateAdapter<SubsAppConfig>() {
      protected override fun createQuery(): String = "DELETE FROM `subs_app_config` WHERE `subs_id` = ? AND `app_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindText(2, entity.appId)
      }
    }
    this.__updateAdapterOfSubsAppConfig = object : EntityDeleteOrUpdateAdapter<SubsAppConfig>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subs_app_config` SET `enable` = ?,`subs_id` = ?,`app_id` = ? WHERE `subs_id` = ? AND `app_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppConfig) {
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(1, _tmp.toLong())
        statement.bindLong(2, entity.subsId)
        statement.bindText(3, entity.appId)
        statement.bindLong(4, entity.subsId)
        statement.bindText(5, entity.appId)
      }
    }
    this.__upsertAdapterOfSubsAppConfig = EntityUpsertAdapter<SubsAppConfig>(object : EntityInsertAdapter<SubsAppConfig>() {
      protected override fun createQuery(): String = "INSERT INTO `subs_app_config` (`enable`,`subs_id`,`app_id`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppConfig) {
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(1, _tmp.toLong())
        statement.bindLong(2, entity.subsId)
        statement.bindText(3, entity.appId)
      }
    }, object : EntityDeleteOrUpdateAdapter<SubsAppConfig>() {
      protected override fun createQuery(): String = "UPDATE `subs_app_config` SET `enable` = ?,`subs_id` = ?,`app_id` = ? WHERE `subs_id` = ? AND `app_id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsAppConfig) {
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(1, _tmp.toLong())
        statement.bindLong(2, entity.subsId)
        statement.bindText(3, entity.appId)
        statement.bindLong(4, entity.subsId)
        statement.bindText(5, entity.appId)
      }
    })
  }

  public override suspend fun insertOrIgnore(vararg objects: SubsAppConfig): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubsAppConfig.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override suspend fun delete(vararg objects: SubsAppConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSubsAppConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun update(vararg objects: SubsAppConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfSubsAppConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun upsert(vararg users: SubsAppConfig): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSubsAppConfig.upsert(_connection, users)
  }

  public override suspend fun queryAll(): List<SubsAppConfig> {
    val _sql: String = "SELECT * FROM subs_app_config"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _result: MutableList<SubsAppConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppConfig
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          _item = SubsAppConfig(_tmpEnable,_tmpSubsId,_tmpAppId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryAppTypeConfig(subsId: Long): Flow<List<SubsAppConfig>> {
    val _sql: String = "SELECT * FROM subs_app_config WHERE subs_id=?"
    return createFlow(__db, false, arrayOf("subs_app_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _result: MutableList<SubsAppConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppConfig
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          _item = SubsAppConfig(_tmpEnable,_tmpSubsId,_tmpAppId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryAppUsedList(appId: String): Flow<List<SubsAppConfig>> {
    val _sql: String = "SELECT * FROM subs_app_config WHERE app_id=? AND subs_id IN (SELECT si.id FROM subs_item si WHERE si.enable = 1)"
    return createFlow(__db, false, arrayOf("subs_app_config", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, appId)
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _result: MutableList<SubsAppConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppConfig
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          _item = SubsAppConfig(_tmpEnable,_tmpSubsId,_tmpAppId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryUsedList(): Flow<List<SubsAppConfig>> {
    val _sql: String = "SELECT * FROM subs_app_config WHERE subs_id IN (SELECT si.id FROM subs_item si WHERE si.enable = 1)"
    return createFlow(__db, false, arrayOf("subs_app_config", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _result: MutableList<SubsAppConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsAppConfig
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          _item = SubsAppConfig(_tmpEnable,_tmpSubsId,_tmpAppId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun querySubsItemConfig(subsItemIds: List<Long>): List<SubsAppConfig> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_app_config WHERE subs_id IN (")
    val _inputSize: Int = subsItemIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in subsItemIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfAppId: Int = getColumnIndexOrThrow(_stmt, "app_id")
        val _result: MutableList<SubsAppConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsAppConfig
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpAppId: String
          _tmpAppId = _stmt.getText(_columnIndexOfAppId)
          _item_1 = SubsAppConfig(_tmpEnable,_tmpSubsId,_tmpAppId)
          _result.add(_item_1)
        }
        _result
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
