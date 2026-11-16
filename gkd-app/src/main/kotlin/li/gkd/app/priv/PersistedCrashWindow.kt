package li.gkd.app.priv

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Small synchronous state, independent of application storage migration and settings. */
internal class PersistedCrashWindow(
    private val file: File,
    private val windowMillis: Long = 5 * 60 * 1000L,
    private val threshold: Int = 3,
) {
    private data class State(val blocked: Boolean, val times: List<Long>)
    private var state = readState()
    val isBlocked: Boolean @Synchronized get() = state.blocked

    private fun readState(): State {
        if (!file.exists()) return State(false, emptyList())
        return runCatching {
            require(file.length() in 1..4096)
            val fields = file.readText().trim().split('\n')
            require(fields.size == 3 && fields[0] == "1")
            val blocked = fields[1].toBooleanStrict()
            val times = if (fields[2] == "-") emptyList() else fields[2].split(',').map { it.toLong() }
            require(times.size <= threshold && times.all { it >= 0L })
            State(blocked, times)
        }.getOrElse { State(true, emptyList()) } // Corrupt state must not re-enable an endless restart loop.
    }

    @Synchronized
    fun recordCrash(nowMillis: Long): Boolean {
        val recent = (state.times.filter { it > nowMillis || nowMillis - it <= windowMillis } + nowMillis).takeLast(threshold)
        state = State(state.blocked || recent.size >= threshold, recent)
        persist()
        return state.blocked
    }

    @Synchronized
    fun reset() {
        val previous = state
        state = State(false, emptyList())
        try { persist() } catch (error: Exception) { state = previous; throw error }
    }

    private fun persist() {
        val parent = requireNotNull(file.parentFile)
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Unable to create crash state directory")
        val pending = File(parent, file.name + ".pending")
        val text = "1\n" + state.blocked + "\n" + state.times.joinToString(",").ifEmpty { "-" } + "\n"
        FileOutputStream(pending).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        try {
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
