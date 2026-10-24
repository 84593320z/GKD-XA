package li.gkd.app.data.subscription

/** 新增订阅时会经过的四个阶段，用于在对话框里显示进度而不是干等。 */
enum class SubscriptionAddStage {
    Downloading,
    Parsing,
    Checking,
    Saving,
}

sealed interface SubscriptionResult {
    data object Busy : SubscriptionResult

    data class Success(
        val kind: SuccessKind = SuccessKind.None,
        val count: Int = 0,
    ) : SubscriptionResult

    data class Failure(
        val reason: FailureReason,
        val detail: String? = null,
        val cause: Throwable? = null,
    ) : SubscriptionResult

    enum class SuccessKind {
        None,
        Deleted,
        Added,
        Modified,
        Refreshed,
    }

    enum class FailureReason {
        DeleteData,
        DeleteFile,
        DuplicateUrl,
        Download,
        Parse,
        AlreadyExists,
        IdMismatch,
        InvalidId,
        Save,
        NetworkUnavailable,
    }
}
