package li.gkd.app.data.backup

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import li.gkd.app.data.RawSubscription
import li.gkd.app.data.subscription.SubscriptionBodyChange
import li.gkd.app.data.subscription.SubscriptionFileStore
import li.gkd.app.data.subscription.SubscriptionPersistence
import li.gkd.app.data.subscription.SubscriptionRepository
import li.gkd.app.data.subscription.nextSubscriptionMtime
import li.gkd.app.store.AppStore
import li.gkd.app.text.UiStrings
import li.gkd.app.util.ExportFileNames
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.LogUtils
import li.gkd.app.util.ZipUtils
import li.gkd.app.util.json
import li.gkd.db.Db
import li.gkd.db.LOCAL_SUBS_ID
import li.gkd.db.SubscriptionConfigSnapshot
import java.io.File
import java.io.IOException

private data class PreparedBackup(
    val dbData: SubscriptionConfigSnapshot?,
    val storeEntries: Map<String, String>,
    val subscriptions: List<RawSubscription>,
)

object BackupManager {
    private val mutationMutex = Mutex()

    suspend fun exportData(): File = mutationMutex.withLock {
        withContext(Dispatchers.IO) { exportLocked(FolderUtils.sharedDir) }
    }

    /** A point-in-time settings/subscription/DB image; no live SQLite or WAL files are copied. */
    suspend fun captureEntries(): Map<String, String> =
        mutationMutex.withLock { captureEntriesLocked() }

    private suspend fun captureEntriesLocked(): Map<String, String> =
        SubscriptionRepository.withBackupSnapshot { snapshot ->
            // The transaction prevents DB writers while the settings lock captures all values.
            Db.withTransaction {
                val database = Db.subscriptionConfigStore.capture()
                buildMap {
                    AppStore.exportBackupEntries().forEach { (filename, text) -> put("store/$filename", text) }
                    put("db.json", BackupFormat.encode(BackupDatabaseData.fromSnapshot(database)))
                    snapshot.subscriptions.values.forEach { subs ->
                        put("subscription/${subs.id}.json", json.encodeToString(subs))
                    }
                }
            }
        }

    /** Diagnostics never build the lossless backup Map or query unbounded config tables. */
    internal suspend fun writeDiagnostics(
        directory: File, budget: DiagnosticBudget, snapshotSince: Long?, capturedAt: Long,
    ) = mutationMutex.withLock {
        // Already published immutable values; each value is encoded under the remaining budget.
        budget.writeJson(directory.resolve("store/store.json"), json, AppStore.storeFlow.value)
        budget.writeJson(directory.resolve("store/action_count.txt"), json, AppStore.actionCountFlow.value)
        budget.writeJson(directory.resolve("store/block_match_app_list.json"), json, AppStore.blockMatchAppListFlow.value)
        budget.writeJson(directory.resolve("store/block_a11y_app_list.json"), json, AppStore.blockA11yAppListFlow.value)
        budget.writeJson(directory.resolve("store/a11y_scope_app_list.json"), json, AppStore.a11yScopeAppListFlow.value)
        Db.withTransaction {
            // Five bounded tables, at most two 1024-character fields per row.
            val configRows = minOf(50L, budget.availableTextBytes / (5L * 16_384)).toInt()
            if (configRows == 0) budget.omit() else {
                val config = Db.subscriptionConfigStore.captureForDiagnostics(configRows)
                budget.writeJson(directory.resolve("db.json"), json, BackupDatabaseData.fromSnapshot(config))
            }
            writeDiagnosticPages(directory, "action_log", budget) { limit, offset ->
                Db.actionLogDao.queryForDiagnostics(limit, offset)
            }
            writeDiagnosticPages(directory, "activity_log", budget) { limit, offset ->
                Db.activityLogDao.queryForDiagnostics(limit, offset)
            }
            writeDiagnosticPages(directory, "a11y_event_log", budget) { limit, offset ->
                Db.a11yEventLogDao.queryForDiagnostics(limit, offset)
            }
            if (snapshotSince != null) writeDiagnosticPages(directory, "snapshot", budget) { limit, offset ->
                Db.snapshotDao.queryForDiagnostics(limit, snapshotSince, capturedAt, offset)
            }
        }
        // A published snapshot does not reload/encode all subscription files before the budget applies.
        SubscriptionRepository.awaitSnapshot().subscriptions.values.forEach { subs ->
            budget.writeJson(directory.resolve("subscription/${subs.id}.json"), json, subs)
        }
    }

    private suspend inline fun <reified T> writeDiagnosticPages(
        directory: File, name: String, budget: DiagnosticBudget,
        query: (limit: Int, offset: Int) -> List<T>,
    ) {
        var offset = 0
        while (offset < 500) {
            // Reserve a conservative query materialization allowance before invoking Room.
            val limit = minOf(50L, (500 - offset).toLong(), budget.availableTextBytes / 32_768).toInt()
            if (limit == 0) { budget.omit(); return }
            val rows = query(limit, offset)
            budget.writeJson(directory.resolve("diagnostics/$name-$offset.json"), json, rows)
            if (rows.size < limit) return
            offset += rows.size
        }
    }

    suspend fun createPreUpgradeBackup(): File = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            val directory = li.gkd.app.app.noBackupFilesDir.resolve("pre-upgrade-backups")
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create pre-upgrade backup directory" }
            val backup = exportLocked(directory)
            // Retention runs only after the new archive is completely written.
            directory.listFiles()?.filter { it.isFile && it.extension == "zip" && it != backup }
                ?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach { it.delete() }
            backup
        }
    }

    private suspend fun exportLocked(directory: File): File {
        val entries = captureEntriesLocked()
        val tempDir = FolderUtils.createGkdTempDir()
        try {
            entries.forEach { (path, text) ->
                tempDir.resolve(path).apply { parentFile?.mkdirs(); writeText(text) }
            }
            val file = ExportFileNames.reserve(
                directory, "gkd-backup-${ExportFileNames.timestamp(System.currentTimeMillis())}", "zip",
            )
            try {
                if (!ZipUtils.zipFiles(tempDir.listFiles().orEmpty().toList(), file)) {
                    throw IOException(UiStrings.backup_compress_failed)
                }
                // Ensure installation never starts before the safety archive reaches storage.
                java.io.RandomAccessFile(file, "rw").use { it.fd.sync() }
                return file
            } catch (e: Throwable) {
                file.delete()
                throw e
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun importData(uri: Uri): BackupImportResult = mutationMutex.withLock {
        var committed: BackupImportResult? = null
        try {
            withContext(Dispatchers.IO) {
                val tempDir = FolderUtils.createGkdTempDir()
                try {
                    val zipFile = tempDir.resolve("file.zip")
                    val unzipDir = tempDir.resolve("unzip")
                    try {
                        BackupArchiveReader.extract(uri, zipFile, unzipDir)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SecurityException) {
                        LogUtils.d("importBackUpData.openFile", e)
                        throw IllegalArgumentException(UiStrings.backup_reselect_file, e)
                    } catch (e: Exception) {
                        LogUtils.d("importBackUpData.unzipFile", e)
                        throw IllegalArgumentException(UiStrings.backup_invalid_archive, e)
                    }
                    zipFile.delete()

                    val prepared = prepareBackup(unzipDir)
                    currentCoroutineContext().ensureActive()
                    withContext(NonCancellable) {
                        val result = applyPreparedBackup(prepared)
                        BackupImportResult(result.value, result.refreshError).also { committed = it }
                    }
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        } catch (cancelled: CancellationException) {
            // Dispatch back to a cancelled UI must not turn a durable commit into a reported failure.
            committed ?: throw cancelled
        }
    }

    private suspend fun applyPreparedBackup(prepared: PreparedBackup): BackupCommitResult<Int> =
        SubscriptionRepository.withBackupTransaction(prepared.subscriptions) { archivedSubscriptions ->
            AppStore.withBackupRestore(prepared.storeEntries) {
                val skipped = SubscriptionPersistence.withBodyTransaction { journal ->
                    val current = Db.subscriptionConfigStore.capture()
                    val plan = planSubscriptionRestore(
                        database = completeSubscriptionMetadata(prepared.dbData, archivedSubscriptions),
                        subscriptions = archivedSubscriptions,
                        existing = current,
                        hasExistingBody = journal::hasExistingBody,
                        readLocalPlaceholder = {
                            if (journal.hasLegacySidecar(LOCAL_SUBS_ID)) null
                            else SubscriptionFileStore.readSmallBody(LOCAL_SUBS_ID, maxBytes = 4096)
                        },
                    )
                    if (plan.conflictingIds.isNotEmpty()) {
                        LogUtils.d("保留现有同 ID 订阅及配置", "count=" + plan.conflictingIds.size)
                    }
                    val currentItems = current.subsItems.associateBy { it.id }
                    val importItems = plan.database.subsItems.map { archived ->
                        val previous = currentItems[archived.id] ?: return@map archived
                        // Only the automatically-created pristine local placeholder may be replaced.
                        archived.copy(
                            ctime = previous.ctime,
                            order = previous.order,
                            mtime = nextSubscriptionMtime(previous.mtime),
                        ).also { Db.subsItemDao.upsert(it) }
                    }
                    val itemsById = importItems.associateBy { it.id }
                    val skippedConfigs = Db.subscriptionConfigStore.merge(plan.database.copy(
                        subsItems = importItems.filterNot { it.id in currentItems },
                    ))
                    journal.stageAndInstall(plan.subscriptions.map { body ->
                        SubscriptionBodyChange(
                            id = body.id,
                            previousMtime = currentItems[body.id]?.mtime,
                            nextMtime = itemsById.getValue(body.id).mtime,
                            body = body,
                        )
                    })
                    plan.skippedOrphanConfigs + skippedConfigs
                }
                skipped
            }
        }

    private suspend fun prepareBackup(unzipDir: File): PreparedBackup =
        withContext(Dispatchers.IO) {
            val dbFile = unzipDir.resolve("db.json")
            val dbData = if (dbFile.exists() && dbFile.isFile) {
                val text = dbFile.readText()
                withContext(Dispatchers.Default) { BackupFormat.decode(text).toSnapshot() }
            } else {
                null
            }
            val storeEntries = AppStore.backupFilenames.mapNotNull { filename ->
                val file = unzipDir.resolve("store/$filename")
                if (!file.exists() || !file.isFile) return@mapNotNull null
                filename to file.readText()
            }
            val subsDir = unzipDir.resolve("subscription")
            val subscriptions = if (subsDir.exists() && subsDir.isDirectory) {
                (subsDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".json")
                } ?: emptyArray()).filterNotNull().sortedBy { it.name }.map { file ->
                    val fileId = file.nameWithoutExtension.toLongOrNull()
                        ?: error(UiStrings.subscription_invalid_filename(file.name))
                    val text = file.readText()
                    withContext(Dispatchers.Default) { json.decodeFromString<RawSubscription>(text) }.also { subscription ->
                        require(subscription.id == fileId) {
                            UiStrings.subscription_file_id_mismatch_detail(fileId, subscription.id)
                        }
                    }
                }.also { list ->
                    require(list.map { it.id }.distinct().size == list.size) {
                        UiStrings.backup_duplicate_subscription_id
                    }
                }
            } else {
                emptyList()
            }
            PreparedBackup(
                dbData = dbData,
                storeEntries = storeEntries.toMap(),
                subscriptions = subscriptions,
            )
        }
}
