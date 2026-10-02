package li.gkd.db

import androidx.room3.withWriteTransaction

object Db {
    private var createDatabase: (() -> AppDb)? = null

    internal fun initialize(createDatabase: () -> AppDb) {
        check(this.createDatabase == null) { "Db is already initialized" }
        this.createDatabase = createDatabase
    }

    private val database by lazy {
        checkNotNull(createDatabase) { "Db is not initialized" }.invoke()
    }

    val subscriptionConfigStore by lazy { SubscriptionConfigStore(database) }

    val subsItemDao get() = database.subsItemDao()
    val subsAppGroupConfigDao get() = database.subsAppGroupConfigDao()
    val subsGlobalGroupConfigDao get() = database.subsGlobalGroupConfigDao()
    val snapshotDao get() = database.snapshotDao()
    val actionLogDao get() = database.actionLogDao()
    val subsCategoryConfigDao get() = database.subsCategoryConfigDao()
    val activityLogDao get() = database.activityLogDao()
    val subsAppConfigDao get() = database.subsAppConfigDao()
    val appLastVisitDao get() = database.appLastVisitDao()
    val a11yEventLogDao get() = database.a11yEventLogDao()

    suspend fun <T> withTransaction(block: suspend () -> T): T =
        database.withWriteTransaction { block() }
}
