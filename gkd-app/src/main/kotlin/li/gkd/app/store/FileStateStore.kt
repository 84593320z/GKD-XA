package li.gkd.app.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.gkd.app.util.FolderUtils
import li.gkd.app.util.json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileStateStore {
    private fun readText(file: File): String? = file.run {
        if (exists()) readText() else null
    }

    private fun writeText(file: File, text: String) {
        val tempFile = File("${file.absolutePath}.tmp")
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
    ): MutableStateFlow<T> {
        val stateFlow = MutableStateFlow(decode(readText(file)))
        // Subscribe before returning the flow. StateFlow itself conflates later writes.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            stateFlow.drop(1).collect {
                withContext(Dispatchers.IO) {
                    writeText(file, encode(it))
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
