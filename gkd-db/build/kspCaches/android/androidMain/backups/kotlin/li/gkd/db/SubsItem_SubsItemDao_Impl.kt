package li.gkd.db

import androidx.room3.EntityDeleteOrUpdateAdapter
import androidx.room3.EntityInsertAdapter
import androidx.room3.EntityUpsertAdapter
import androidx.room3.RoomDatabase
import androidx.room3.coroutines.createFlow
import androidx.room3.util.appendPlaceholders
import androidx.room3.util.getColumnIndexOrThrow
import androidx.room3.util.getTotalChangedRows
import androidx.room3.util.performInTransactionSuspending
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
internal class SubsItem_SubsItemDao_Impl(
  __db: RoomDatabase,
) : SubsItem.SubsItemDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubsItem: EntityInsertAdapter<SubsItem>

  private val __deleteAdapterOfSubsItem: EntityDeleteOrUpdateAdapter<SubsItem>

  private val __updateAdapterOfSubsItem: EntityDeleteOrUpdateAdapter<SubsItem>

  private val __upsertAdapterOfSubsItem: EntityUpsertAdapter<SubsItem>
  init {
    this.__db = __db
    this.__insertAdapterOfSubsItem = object : EntityInsertAdapter<SubsItem>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `subs_item` (`id`,`ctime`,`mtime`,`enable`,`enable_update`,`order`,`update_url`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsItem) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.ctime)
        statement.bindLong(3, entity.mtime)
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.enableUpdate) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.order.toLong())
        val _tmpUpdateUrl: String? = entity.updateUrl
        if (_tmpUpdateUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpUpdateUrl)
        }
      }
    }
    this.__deleteAdapterOfSubsItem = object : EntityDeleteOrUpdateAdapter<SubsItem>() {
      protected override fun createQuery(): String = "DELETE FROM `subs_item` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsItem) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfSubsItem = object : EntityDeleteOrUpdateAdapter<SubsItem>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `subs_item` SET `id` = ?,`ctime` = ?,`mtime` = ?,`enable` = ?,`enable_update` = ?,`order` = ?,`update_url` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsItem) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.ctime)
        statement.bindLong(3, entity.mtime)
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.enableUpdate) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.order.toLong())
        val _tmpUpdateUrl: String? = entity.updateUrl
        if (_tmpUpdateUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpUpdateUrl)
        }
        statement.bindLong(8, entity.id)
      }
    }
    this.__upsertAdapterOfSubsItem = EntityUpsertAdapter<SubsItem>(object : EntityInsertAdapter<SubsItem>() {
      protected override fun createQuery(): String = "INSERT INTO `subs_item` (`id`,`ctime`,`mtime`,`enable`,`enable_update`,`order`,`update_url`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubsItem) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.ctime)
        statement.bindLong(3, entity.mtime)
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.enableUpdate) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.order.toLong())
        val _tmpUpdateUrl: String? = entity.updateUrl
        if (_tmpUpdateUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpUpdateUrl)
        }
      }
    }, object : EntityDeleteOrUpdateAdapter<SubsItem>() {
      protected override fun createQuery(): String = "UPDATE `subs_item` SET `id` = ?,`ctime` = ?,`mtime` = ?,`enable` = ?,`enable_update` = ?,`order` = ?,`update_url` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SubsItem) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.ctime)
        statement.bindLong(3, entity.mtime)
        val _tmp: Int = if (entity.enable) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.enableUpdate) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
        statement.bindLong(6, entity.order.toLong())
        val _tmpUpdateUrl: String? = entity.updateUrl
        if (_tmpUpdateUrl == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpUpdateUrl)
        }
        statement.bindLong(8, entity.id)
      }
    })
  }

  public override suspend fun insertOrIgnore(vararg users: SubsItem): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfSubsItem.insertAndReturnIdsList(_connection, users)
    _result
  }

  public override suspend fun delete(vararg users: SubsItem): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfSubsItem.handleMultipleAndReturnChanges(_connection, users)
    _result
  }

  public override suspend fun update(vararg objects: SubsItem): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfSubsItem.handleMultipleAndReturnChanges(_connection, objects)
    _result
  }

  public override suspend fun batchUpdateOrder(subsItems: List<SubsItem>): Unit = performInTransactionSuspending(__db) {
    super@SubsItem_SubsItemDao_Impl.batchUpdateOrder(subsItems)
  }

  public override suspend fun upsert(vararg users: SubsItem): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __upsertAdapterOfSubsItem.upsertAndReturnIdsList(_connection, users)
    _result
  }

  public override fun query(): Flow<List<SubsItem>> {
    val _sql: String = "SELECT * FROM subs_item ORDER BY `order`"
    return createFlow(__db, false, arrayOf("subs_item")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
        val _columnIndexOfMtime: Int = getColumnIndexOrThrow(_stmt, "mtime")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfEnableUpdate: Int = getColumnIndexOrThrow(_stmt, "enable_update")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdateUrl: Int = getColumnIndexOrThrow(_stmt, "update_url")
        val _result: MutableList<SubsItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsItem
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCtime: Long
          _tmpCtime = _stmt.getLong(_columnIndexOfCtime)
          val _tmpMtime: Long
          _tmpMtime = _stmt.getLong(_columnIndexOfMtime)
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpEnableUpdate: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnableUpdate).toInt()
          _tmpEnableUpdate = _tmp_1 != 0
          val _tmpOrder: Int
          _tmpOrder = _stmt.getLong(_columnIndexOfOrder).toInt()
          val _tmpUpdateUrl: String?
          if (_stmt.isNull(_columnIndexOfUpdateUrl)) {
            _tmpUpdateUrl = null
          } else {
            _tmpUpdateUrl = _stmt.getText(_columnIndexOfUpdateUrl)
          }
          _item = SubsItem(_tmpId,_tmpCtime,_tmpMtime,_tmpEnable,_tmpEnableUpdate,_tmpOrder,_tmpUpdateUrl)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun queryAll(): List<SubsItem> {
    val _sql: String = "SELECT * FROM subs_item ORDER BY `order`"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCtime: Int = getColumnIndexOrThrow(_stmt, "ctime")
        val _columnIndexOfMtime: Int = getColumnIndexOrThrow(_stmt, "mtime")
        val _columnIndexOfEnable: Int = getColumnIndexOrThrow(_stmt, "enable")
        val _columnIndexOfEnableUpdate: Int = getColumnIndexOrThrow(_stmt, "enable_update")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdateUrl: Int = getColumnIndexOrThrow(_stmt, "update_url")
        val _result: MutableList<SubsItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubsItem
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCtime: Long
          _tmpCtime = _stmt.getLong(_columnIndexOfCtime)
          val _tmpMtime: Long
          _tmpMtime = _stmt.getLong(_columnIndexOfMtime)
          val _tmpEnable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnable).toInt()
          _tmpEnable = _tmp != 0
          val _tmpEnableUpdate: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfEnableUpdate).toInt()
          _tmpEnableUpdate = _tmp_1 != 0
          val _tmpOrder: Int
          _tmpOrder = _stmt.getLong(_columnIndexOfOrder).toInt()
          val _tmpUpdateUrl: String?
          if (_stmt.isNull(_columnIndexOfUpdateUrl)) {
            _tmpUpdateUrl = null
          } else {
            _tmpUpdateUrl = _stmt.getText(_columnIndexOfUpdateUrl)
          }
          _item = SubsItem(_tmpId,_tmpCtime,_tmpMtime,_tmpEnable,_tmpEnableUpdate,_tmpOrder,_tmpUpdateUrl)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateEnable(id: Long, enable: Boolean): Int {
    val _sql: String = "UPDATE subs_item SET enable=? WHERE id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enable) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateOrder(id: Long, order: Int): Int {
    val _sql: String = "UPDATE subs_item SET `order`=? WHERE id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, order.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateMtime(id: Long, mtime: Long): Int {
    val _sql: String = "UPDATE subs_item SET mtime=? WHERE id=?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, mtime)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(vararg ids: Long): Int {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM subs_item WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
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

  public companion object {
    public fun getRequiredColumnConverters(): List<KClass<*>> = emptyList()

    public fun getRequiredDaoReturnTypeConverters(): List<KClass<*>> = emptyList()
  }
}
