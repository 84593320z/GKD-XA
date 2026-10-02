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
internal class SubsCategoryConfig_SubsCategoryConfigDao_Impl(
  __db: RoomDatabase,
) : SubsCategoryConfig.SubsCategoryConfigDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubsCategoryConfig: EntityInsertAdapter<SubsCategoryConfig>

  private val __deleteAdapterOfSubsCategoryConfig: EntityDeleteOrUpdateAdapter<SubsCategoryConfig>

  private val __updateAdapterOfSubsCategoryConfig: EntityDeleteOrUpdateAdapter<SubsCategoryConfig>

  private val __upsertAdapterOfSubsCategoryConfig: EntityUpsertAdapter<SubsCategoryConfig>
  init {
    this.__db = __db
    this.__insertAdapterOfSubsCategoryConfig = object : EntityInsertAdapter<SubsCategoryConfig>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `subs_category_config` (`enable`,`subs_id`,`category_key`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsCategoryConfig) {
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmp.toLong())
        }
        statement.bindLong(2, entity.subsId)
        statement.bindLong(3, entity.categoryKey.toLong())
      }
    }
    this.__deleteAdapterOfSubsCategoryConfig = object : EntityDeleteOrUpdateAdapter<SubsCategoryConfig>() {
      protected override fun createQuery(): String = "DELETE FROM `subs_category_config` WHERE `subs_id` = ? AND `category_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsCategoryConfig) {
        statement.bindLong(1, entity.subsId)
        statement.bindLong(2, entity.categoryKey.toLong())
      }
    }
    this.__updateAdapterOfSubsCategoryConfig = object : EntityDeleteOrUpdateAdapter<SubsCategoryConfig>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subs_category_config` SET `enable` = ?,`subs_id` = ?,`category_key` = ? WHERE `subs_id` = ? AND `category_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsCategoryConfig) {
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmp.toLong())
        }
        statement.bindLong(2, entity.subsId)
        statement.bindLong(3, entity.categoryKey.toLong())
        statement.bindLong(4, entity.subsId)
        statement.bindLong(5, entity.categoryKey.toLong())
      }
    }
    this.__upsertAdapterOfSubsCategoryConfig = EntityUpsertAdapter<SubsCategoryConfig>(object : EntityInsertAdapter<SubsCategoryConfig>() {
      protected override fun createQuery(): String = "INSERT INTO `subs_category_config` (`enable`,`subs_id`,`category_key`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsCategoryConfig) {
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmp.toLong())
        }
        statement.bindLong(2, entity.subsId)
        statement.bindLong(3, entity.categoryKey.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<SubsCategoryConfig>() {
      protected override fun createQuery(): String = "UPDATE `subs_category_config` SET `enable` = ?,`subs_id` = ?,`category_key` = ? WHERE `subs_id` = ? AND `category_key` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsCategoryConfig) {
        val _tmpEnable: Boolean? = entity.enable
        val _tmp: Int? = _tmpEnable?.let { if (it) 1 else 0 }
        if (_tmp == null) {
          statement.bindNull(1)
        } else {
          statement.bindLong(1, _tmp.toLong())
        }
        statement.bindLong(2, entity.subsId)
        statement.bindLong(3, entity.categoryKey.toLong())
        statement.bindLong(4, entity.subsId)
        statement.bindLong(5, entity.categoryKey.toLong())
      }
    })
  }

  public override suspend fun insertOrIgnore(vararg objects: SubsCategoryConfig): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubsCategoryConfig.insertAndReturnIdsList(_connection, objects)
    _result
  }

  public override suspend fun delete(vararg objects: SubsCategoryConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSubsCategoryConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun update(vararg objects: SubsCategoryConfig): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfSubsCategoryConfig.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun upsert(vararg objects: SubsCategoryConfig): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfSubsCategoryConfig.upsert(_connection, objects)
  }

  public override suspend fun queryAll(): List<SubsCategoryConfig> {
    val _sql: String = "SELECT * FROM subs_category_config"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: MutableList<SubsCategoryConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsCategoryConfig
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _item = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryUsedList(): Flow<List<SubsCategoryConfig>> {
    val _sql: String = "SELECT * FROM subs_category_config WHERE subs_id IN (SELECT si.id FROM subs_item si WHERE si.enable = 1)"
    return createFlow(__db, false, arrayOf("subs_category_config", "subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: MutableList<SubsCategoryConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsCategoryConfig
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _item = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryConfig(subsItemId: Long): Flow<List<SubsCategoryConfig>> {
    val _sql: String = "SELECT * FROM subs_category_config WHERE subs_id=?"
    return createFlow(__db, false, arrayOf("subs_category_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsItemId)
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: MutableList<SubsCategoryConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsCategoryConfig
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _item = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryCategoryConfig(subsId: Long, categoryKey: Int): Flow<SubsCategoryConfig?> {
    val _sql: String = "SELECT * FROM subs_category_config WHERE subs_id=? AND category_key=?"
    return createFlow(__db, false, arrayOf("subs_category_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, categoryKey.toLong())
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: SubsCategoryConfig?
        if (_stmt.step()) {
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _result = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun querySubsItemConfig(subsItemIds: List<Long>): List<SubsCategoryConfig> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_category_config WHERE subs_id IN (")
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
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: MutableList<SubsCategoryConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsCategoryConfig
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _item_1 = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun queryBySubsIds(subsItemIds: List<Long>): Flow<List<SubsCategoryConfig>> {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT * FROM subs_category_config WHERE subs_id IN (")
    val _inputSize: Int = subsItemIds.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return createFlow(__db, false, arrayOf("subs_category_config")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in subsItemIds) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfSubsId: Int = getColumnIndexOrThrow(_stmt, "subs_id")
        val _columnIndexOfCategoryKey: Int = getColumnIndexOrThrow(_stmt, "category_key")
        val _result: MutableList<SubsCategoryConfig> = mutableListOf()
        while (_stmt.step()) {
          val _item_1: SubsCategoryConfig
          val _tmpEnable: Boolean?
          val _tmp: Int?
          if (_stmt.isNull(_columnIndexOfEnable)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          }
          _tmpEnable = _tmp?.let { it != 0 }
          val _tmpSubsId: Long
          _tmpSubsId = _stmt.getLong(_columnIndexOfSubsId)
          val _tmpCategoryKey: Int
          _tmpCategoryKey = _stmt.getLong(_columnIndexOfCategoryKey).toInt()
          _item_1 = SubsCategoryConfig(_tmpEnable,_tmpSubsId,_tmpCategoryKey)
          _result.add(_item_1)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBySubsItemId(subsItemId: Long): Int {
    val _sql: String = "DELETE FROM subs_category_config WHERE subs_id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsItemId)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBySubsId(vararg subsIds: Long): Int {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM subs_category_config WHERE subs_id IN (")
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

  public override suspend fun deleteByCategoryKey(subsItemId: Long, categoryKey: Int): Int {
    val _sql: String = "DELETE FROM subs_category_config WHERE subs_id=? AND category_key=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, subsItemId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, categoryKey.toLong())
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
