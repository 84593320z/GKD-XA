package li.gkd.db

import androidx.room3.ColumnInfo
import kotlinx.serialization.Serializable

/** Whole bounded JSON or an omission marker; never a sliced JSON array or a List<String> conversion. */
@Serializable
data class DiagnosticA11yEventLog(
    val id: Int,
    val ctime: Long,
    val type: Int,
    @ColumnInfo(name = "app_id") val appId: String,
    val name: String,
    val desc: String?,
    @ColumnInfo(name = "text_preview") val textPreview: String,
)
