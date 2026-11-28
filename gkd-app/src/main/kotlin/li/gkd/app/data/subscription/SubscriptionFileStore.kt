package li.gkd.app.data.subscription

import li.gkd.app.text.UiStrings
import li.gkd.app.data.RawSubscription
import li.gkd.app.util.FolderUtils
import li.gkd.db.LOCAL_HTTP_SUBS_ID
import li.gkd.db.LOCAL_SUBS_ID
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS

object SubscriptionFileStore {
    fun load(id: Long): RawSubscription {
        val file = file(id)
        if (!file.exists()) {
            return when (id) {
                LOCAL_SUBS_ID -> RawSubscription(id = id, name = UiStrings.subscription_local, version = 0)
                LOCAL_HTTP_SUBS_ID -> RawSubscription(id = id, name = UiStrings.subscription_memory, version = 0)
                else -> error(UiStrings.subscription_file_missing)
            }
        }
        val subscription = try {
            RawSubscription.parse(file.readText(), json5 = false)
        } catch (e: Exception) {
            throw Exception(UiStrings.subscription_file_parse_failed, e)
        }
        if (subscription.id != id) error(UiStrings.subscription_file_id_mismatch)
        return subscription
    }

    fun hasBody(id: Long): Boolean = file(id).isFile

    /** Read only a tiny bootstrap file; never load an arbitrary local rules file for detection. */
    internal fun readSmallBody(id: Long, maxBytes: Long): String? {
        val source = file(id)
        if (!Files.isRegularFile(source.toPath(), NOFOLLOW_LINKS) || source.length() !in 1..maxBytes) return null
        return try {
            source.readText()
        } catch (_: IOException) {
            // Cannot prove the body is a default placeholder; leave it untouched.
            null
        }
    }

    private fun file(id: Long): File = FolderUtils.subsFolder.resolve("$id.json")
}
