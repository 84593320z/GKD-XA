package li.gkd.app.data.subscription

import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import li.gkd.app.data.RawSubscription
import li.gkd.app.text.UiStrings
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.LogUtils
import li.gkd.db.Db
import li.gkd.db.SubsItem

object SubscriptionPersistence {
    enum class DeleteStage { File, Database }

    class DeleteException(val stage: DeleteStage, cause: Throwable) : Exception(cause.message, cause)

    data class DeleteResult(val ids: Set<Long>, val count: Int)

    internal data class UrlEdit(val expected: SubsItem, val value: String)

    private fun journal() = SubscriptionFileMutationJournal(FolderUtils.subsFolder) { directory ->
        val descriptor = Os.open(directory.absolutePath, OsConstants.O_RDONLY, 0)
        try {
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
    }

    private suspend fun committedMtimes(): Map<Long, Long> =
        Db.subsItemDao.queryAll().associate { it.id to it.mtime }

    /** Run before publishing the repository or starting rule evaluation. */
    internal suspend fun recoverInterruptedMutation() = journal().recover(::committedMtimes)

    /** Caller holds the subscription writer lock; this owns the outermost Room transaction. */
    internal suspend fun <T> withBodyTransaction(
        block: suspend (SubscriptionFileMutationJournal) -> T,
    ): T = withContext(NonCancellable + Dispatchers.IO) {
        val journal = journal()
        journal.recover(::committedMtimes)
        val result = try {
            Db.withTransaction { block(journal) }
        } catch (error: Throwable) {
            // Read the durable row state only after the outermost transaction has ended.
            runCatching { journal.recover(::committedMtimes) }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
        runCatching { journal.finish() }.exceptionOrNull()?.let {
            LogUtils.d("下次启动或写入时清理订阅事务日志", it.toString())
        }
        result
    }

    internal suspend fun save(
        subscription: RawSubscription,
        initialItem: SubsItem? = null,
        mustBeNew: Boolean = false,
        urlEdit: UrlEdit? = null,
    ) = withBodyTransaction { journal ->
        val id = subscription.id
        val dao = Db.subsItemDao
        val previous = dao.queryAll().firstOrNull { it.id == id }
        val mtime = nextSubscriptionMtime(previous?.mtime)
        check(!mustBeNew || previous == null) { UiStrings.subscription_content_conflict }
        when {
            urlEdit != null -> {
                val expected = urlEdit.expected
                require(expected.id == id)
                val affected = dao.updateUrlIfUnchanged(
                    id = id,
                    expectedCtime = expected.ctime,
                    expectedMtime = expected.mtime,
                    expectedUpdateUrl = expected.updateUrl,
                    updateUrl = urlEdit.value,
                    mtime = mtime,
                )
                check(affected == 1) { UiStrings.subscription_content_conflict }
            }
            previous == null -> {
                check(initialItem != null && initialItem.id == id) { UiStrings.subscription_missing }
                dao.upsert(initialItem.copy(mtime = mtime))
            }
            else -> check(dao.updateMtime(id, mtime) == 1) { UiStrings.subscription_content_conflict }
        }
        cleanupConfigs(subscription)
        journal.stageAndInstall(listOf(SubscriptionBodyChange(id, previous?.mtime, mtime, subscription)))
    }

    suspend fun delete(requestedIds: LongArray): DeleteResult = try {
        withBodyTransaction { journal ->
            val requested = requestedIds.toSet()
            val targets = Db.subsItemDao.queryAll().filter { it.id in requested }
            if (targets.isEmpty()) return@withBodyTransaction DeleteResult(emptySet(), 0)
            val ids = targets.map { it.id }.toLongArray()
            val count = Db.subsItemDao.deleteById(*ids)
            check(count == targets.size) { UiStrings.subscription_content_conflict }
            Db.actionLogDao.deleteBySubsId(*ids)
            try {
                journal.stageAndInstall(targets.map { SubscriptionBodyChange(it.id, it.mtime, null, null) })
            } catch (error: Exception) {
                throw DeleteException(DeleteStage.File, error)
            }
            DeleteResult(ids.toSet(), count)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: DeleteException) {
        throw error
    } catch (error: Exception) {
        throw DeleteException(DeleteStage.Database, error)
    }

    private suspend fun cleanupConfigs(subscription: RawSubscription): Int {
        val globalKeys = subscription.globalGroups.mapTo(mutableSetOf()) { it.key }
        val appKeys = subscription.apps.associate { app ->
            app.id to app.groups.mapTo(mutableSetOf()) { it.key }
        }
        val appConfigs = Db.subsAppGroupConfigDao.queryBySubsIds(listOf(subscription.id))
        val globalConfigs = Db.subsGlobalGroupConfigDao.queryBySubsIds(listOf(subscription.id))
        val obsoleteApps = appConfigs.filter { appKeys[it.appId]?.contains(it.groupKey) != true }
        val obsoleteGlobals = globalConfigs.filter { it.groupKey !in globalKeys }
        val categoryKeys = subscription.categories.mapTo(mutableSetOf()) { it.key }
        val obsoleteCategories = Db.subsCategoryConfigDao.querySubsItemConfig(listOf(subscription.id))
            .filter { it.categoryKey !in categoryKeys }
        val size = obsoleteApps.size + obsoleteGlobals.size + obsoleteCategories.size
        if (size == 0) return 0
        Db.subsAppGroupConfigDao.delete(*obsoleteApps.toTypedArray())
        Db.subsGlobalGroupConfigDao.delete(*obsoleteGlobals.toTypedArray())
        Db.subsCategoryConfigDao.delete(*obsoleteCategories.toTypedArray())
        LogUtils.d(
            "清理已移除规则配置",
            "subsId=${subscription.id}, delete=$size",
        )
        return size
    }
}

/** Distinguish before and after even for writes in the same millisecond. */
internal fun nextSubscriptionMtime(previous: Long?, now: Long = System.currentTimeMillis()): Long =
    when (previous) {
        null -> now
        Long.MAX_VALUE -> if (now == previous) previous - 1 else now
        else -> maxOf(now, previous + 1)
    }
