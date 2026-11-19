package priv.kit.ui.runtime

import android.content.Context
import priv.kit.shared.PrivilegeBinaryFileStore
import priv.kit.shared.PrivilegeStoragePaths

internal class PrivilegeUiDesiredEnabledStore(context: Context) {
    private val file = PrivilegeStoragePaths.file(
        context = context,
        fileName = DESIRED_ENABLED_FILE_NAME,
    )

    fun read(): Boolean =
        synchronized(fileLock) {
            PrivilegeBinaryFileStore.readIfExists(file)
                ?.contentEquals(ENABLED_BYTES) == true
        }

    fun write(enabled: Boolean) {
        synchronized(fileLock) {
            PrivilegeBinaryFileStore.writeAtomically(
                file = file,
                bytes = if (enabled) ENABLED_BYTES else DISABLED_BYTES,
            )
        }
    }

    private companion object {
        const val DESIRED_ENABLED_FILE_NAME = "ui-desired-enabled"
        val ENABLED_BYTES = byteArrayOf('1'.code.toByte())
        val DISABLED_BYTES = byteArrayOf('0'.code.toByte())
        val fileLock = Any()
    }
}
