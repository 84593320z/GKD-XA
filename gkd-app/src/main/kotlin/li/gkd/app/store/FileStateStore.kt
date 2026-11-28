package li.gkd.app.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.LogUtils
import li.gkd.app.util.json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Retry transient I/O only; the caller keeps the latest in-memory value and owns ordering. */
internal suspend fun retryFileWrite(
    maxAttempts: Int = 4,
    waitBeforeRetry: suspend (attempt: Int) -> Unit = { attempt -> delay(100L shl attempt) },
    write: () -> Unit,
) {
    require(maxAttempts > 0)
    for (attempt in 0 until maxAttempts) {
        currentCoroutineContext().ensureActive()
        try {
            write()
            return
        } catch (error: IOException) {
            if (attempt + 1 == maxAttempts) throw error
            waitBeforeRetry(attempt)

        }
    }
}

internal fun writeTextAtomically(file: File, text: String) {
    val tempFile = File("${file.absolutePath}.tmp")
    try {
        tempFile.outputStream().use {
            it.write(text.toByteArray(Charsets.UTF_8))
            it.fd.sync()
        }
        Files.move(
            tempFile.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } finally {
        tempFile.delete()
    }
}

object FileStateStore {
    private fun readText(file: File): String? = file.run {
        if (exists()) readText() else null
    }

    fun <T> createTextFlow(
        key: String,
        decode: (String?) -> T,
        encode: (T) -> String,
        scope: CoroutineScope,
        private: Boolean = false,
    ): MutableStateFlow<T> {
        val filename = if (key.contains('.')) key else "$key.txt"
        val folder = if (private) FolderUtils.privateStoreFolder else FolderUtils.storeFolder
        val file = folder.resolve(filename)
        return createTextFlow(file, decode, encode, scope)
    }

    internal fun <T> createTextFlow(
        file: File,
        decode: (String?) -> T,
        encode: (T) -> String,
        scope: CoroutineScope,
        writeText: (File, String) -> Unit = ::writeTextAtomically,
        waitBeforeRetry: suspend (Int) -> Unit = { attempt -> delay(100L shl attempt) },
    ): MutableStateFlow<T> {
        val stateFlow = MutableStateFlow(decode(readText(file)))
        // Subscribe before returning the flow. StateFlow itself conflates later writes.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateFlow.drop(1).collect { value ->
                try {
                    withContext(Dispatchers.IO) {
                        val text = encode(value)
                        retryFileWrite(waitBeforeRetry = waitBeforeRetry) { writeText(file, text) }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // Do not terminate the writer (or its owner scope) on a failed cache write.
                    runCatching { LogUtils.d("状态文件写入失败: ${file.name}", error) }
                }
            }
        }
        return stateFlow
    }

    inline fun <reified T> createJsonFlow(
        key: String,
        crossinline default: () -> T,
        scope: CoroutineScope,
        private: Boolean = false,
    ): MutableStateFlow<T> = createTextFlow(
        key = "$key.json",
        decode = { text ->
            val value = text?.let {
                runCatching { json.decodeFromString<T>(it) }.getOrNull()
            }
            value ?: default()
        },
        encode = { json.encodeToString(it) },
        scope = scope,
        private = private,
    )
}
