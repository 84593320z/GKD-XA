package priv.kit.ui.runtime

import java.io.File
import java.io.IOException
import priv.kit.shared.PrivilegeBinaryFileStore

private val externalShellScriptLock = Any()

internal fun requireValidManualShellScriptPath(scriptPath: String?) {
    if (scriptPath == null) return
    require(
        scriptPath.split('/').all { it.isNotBlank() && it != "." && it != ".." } &&
            scriptPath.none { it == '\\' || it.isISOControl() },
    ) {
        "scriptPath must be a relative file path without empty, . or .. components, " +
            "backslashes or control characters"
    }
}

internal fun preparePrivilegeUiExternalShellScript(
    nativeStarterCommand: () -> String,
    ownerUserId: Int,
    scriptPath: String?,
    externalFilesDirectory: () -> File?,
    onScriptFailure: (Exception) -> Unit = {},
): File? {
    if (ownerUserId != 0 || scriptPath == null) return null
    val command = nativeStarterCommand()
    try {
        // App preloading and UI initialization share the content check and write.
        synchronized(externalShellScriptLock) {
            val directory = externalFilesDirectory()?.parentFile ?: return null
            val script = File(directory, scriptPath).absoluteFile
            val rootPath = directory.canonicalFile.toPath()
            val scriptCanonicalPath = script.canonicalFile.toPath()
            if (scriptCanonicalPath == rootPath || !scriptCanonicalPath.startsWith(rootPath)) {
                throw IOException("Manual startup script must stay inside the app's external directory")
            }
            val content = "#!/system/bin/sh\n${command.trim()}\n".toByteArray(Charsets.UTF_8)
            if (!script.isFile || script.length() != content.size.toLong() || !script.readBytes().contentEquals(content)) {
                PrivilegeBinaryFileStore.writeAtomically(script, content)
            }
            return script
        }
    } catch (exception: Exception) {
        if (exception !is IOException && exception !is SecurityException) throw exception
        onScriptFailure(exception)
        return null
    }
}
