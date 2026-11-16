package li.gkd.app.data.backup

/** The mutation has committed; refreshError must not be presented as a rolled-back import. */
data class BackupCommitResult<T>(val value: T, val refreshError: Throwable? = null)

data class BackupImportResult(val skippedCount: Int, val refreshError: Throwable? = null)
