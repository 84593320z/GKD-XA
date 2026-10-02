package li.gkd.app.data

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: Int,
    val name: String?,
)
