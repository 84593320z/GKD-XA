package li.gkd.app.data.subscription

import kotlinx.serialization.Serializable
import li.gkd.app.data.RawSubscription
import li.gkd.app.util.json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal data class SubscriptionBodyChange(
    val id: Long,
    val previousMtime: Long?,
    /** Null denotes a committed deletion. */
    val nextMtime: Long?,
    val body: RawSubscription?,
)

/**
 * One pending transaction under the repository's writer lock. Publish complete, fsynced bodies
 * before Room commits; the old and new mtimes then determine which files to publish on restart.
 * Immutable staged links keep recovery idempotent and avoid retaining every deleted body in RAM.
 */
internal class SubscriptionFileMutationJournal(
    private val root: File,
    private val syncDirectory: (File) -> Unit,
) {
    @Serializable
    private data class Entry(
        val id: Long,
        val previousMtime: Long?,
        val nextMtime: Long?,
        val existingSuffixes: List<String>,
    )

    private val preparing = root.resolve(".subscription-mutation.preparing")
    private val active = root.resolve(".subscription-mutation.active")
    private val retired = root.resolve(".subscription-mutation.done")
    private val suffixes = listOf("", ".bak", ".new")

    fun hasExistingBody(id: Long): Boolean = suffixes.any { live(id, it).existsOnDisk() }

    fun hasLegacySidecar(id: Long): Boolean = suffixes.drop(1).any { live(id, it).existsOnDisk() }

    fun stageAndInstall(changes: List<SubscriptionBodyChange>) {
        if (changes.isEmpty()) return
        require(changes.size <= MAX_CHANGES && changes.distinctBy { it.id }.size == changes.size)
        require(changes.all {
            it.previousMtime != it.nextMtime &&
                if (it.body == null) it.nextMtime == null else it.body.id == it.id && it.nextMtime != null
        })
        check(!active.existsOnDisk()) { "Subscription recovery is pending" }
        Files.createDirectory(preparing.toPath())
        val entries = changes.map { change ->
            val existing = suffixes.filter { suffix ->
                val source = live(change.id, suffix)
                if (!source.existsOnDisk()) return@filter false
                source.requireRegularFile()
                Files.createLink(before(preparing, change.id, suffix).toPath(), source.toPath())
                true
            }
            change.body?.let {
                durableWrite(after(preparing, change.id), json.encodeToString(it))
            }
            Entry(change.id, change.previousMtime, change.nextMtime, existing)
        }
        durableWrite(preparing.resolve("manifest.json"), json.encodeToString(entries))
        syncDirectory(preparing)
        Files.move(preparing.toPath(), active.toPath(), ATOMIC_MOVE)
        syncDirectory(root)
        install(entries, committed = true)
    }

    suspend fun recover(readMtimes: suspend () -> Map<Long, Long>) {
        if (active.existsOnDisk()) {
            check(Files.isDirectory(active.toPath(), NOFOLLOW_LINKS)) { "Invalid subscription journal" }
            val manifest = active.resolve("manifest.json").requireRegularFile()
            check(manifest.length() in 1..MAX_MANIFEST_BYTES) { "Invalid subscription journal size" }
            val entries = json.decodeFromString<List<Entry>>(manifest.readText())
            check(entries.size in 1..MAX_CHANGES && entries.distinctBy { it.id }.size == entries.size &&
                entries.all { it.previousMtime != it.nextMtime && suffixes.containsAll(it.existingSuffixes) }) {
                "Invalid subscription journal entries"
            }
            val current = readMtimes()
            val committed = entries.all { current[it.id] == it.nextMtime }
            check(committed || entries.all { current[it.id] == it.previousMtime }) {
                "Subscription journal does not match the database"
            }
            install(entries, committed)
            finish()
        }
        deleteStage(preparing)
        deleteStage(retired)
    }

    /** Retire with a rename before removing any file needed by recovery. */
    fun finish() {
        if (!active.existsOnDisk()) return
        deleteStage(retired)
        Files.move(active.toPath(), retired.toPath(), ATOMIC_MOVE)
        syncDirectory(root)
        deleteStage(retired)
    }

    private fun install(entries: List<Entry>, committed: Boolean) {
        // Validate all sources before changing the live files.
        val files = entries.flatMap { entry ->
            suffixes.map { suffix ->
                val source = if (committed) {
                    after(active, entry.id).takeIf { suffix.isEmpty() && entry.nextMtime != null }
                } else {
                    before(active, entry.id, suffix).takeIf { suffix in entry.existingSuffixes }
                }
                live(entry.id, suffix) to source?.requireRegularFile()
            }
        }
        val link = active.resolve("publish")
        files.forEach { (target, source) ->
            if (source == null) {
                Files.deleteIfExists(target.toPath())
            } else if (!target.existsOnDisk() || !Files.isSameFile(target.toPath(), source.toPath())) {
                Files.deleteIfExists(link.toPath())
                Files.createLink(link.toPath(), source.toPath())
                Files.move(link.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            }
        }
        syncDirectory(root)
    }

    private fun live(id: Long, suffix: String) = root.resolve("$id.json$suffix")
    private fun before(stage: File, id: Long, suffix: String) = stage.resolve("before-$id.json$suffix")
    private fun after(stage: File, id: Long) = stage.resolve("after-$id.json")
    private fun File.existsOnDisk() = !Files.notExists(toPath(), NOFOLLOW_LINKS)

    private fun File.requireRegularFile(): File {
        check(Files.isRegularFile(toPath(), NOFOLLOW_LINKS)) { "Invalid subscription file: $name" }
        return this
    }

    private fun durableWrite(file: File, value: String) {
        FileOutputStream(file).use {
            it.write(value.encodeToByteArray())
            it.fd.sync()
        }
    }

    private fun deleteStage(stage: File) {
        if (!stage.existsOnDisk()) return
        check(Files.isDirectory(stage.toPath(), NOFOLLOW_LINKS)) { "Invalid subscription journal directory" }
        val files = stage.listFiles() ?: throw IOException("Cannot list subscription journal directory")
        files.forEach { Files.delete(it.requireRegularFile().toPath()) }
        Files.delete(stage.toPath())
    }

    private companion object {
        const val MAX_CHANGES = 10_000
        const val MAX_MANIFEST_BYTES = 2L * 1024 * 1024
    }
}
