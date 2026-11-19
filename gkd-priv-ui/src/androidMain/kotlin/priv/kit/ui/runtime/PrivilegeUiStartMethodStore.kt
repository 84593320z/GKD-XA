package priv.kit.ui.runtime

import android.content.Context
import priv.kit.shared.PrivilegeBinaryFileStore
import priv.kit.shared.PrivilegeStoragePaths
import priv.kit.ui.PrivilegeUiRuntimeStartSource
import priv.kit.ui.PrivilegeUiStartMethod
import java.nio.charset.StandardCharsets

private const val EXTERNAL_METHOD_ID_PREFIX = "external:"

internal val PrivilegeUiStartMethod.methodId: String
    get() = when (this) {
        PrivilegeUiStartMethod.Root -> "root"
        PrivilegeUiStartMethod.AdbWireless -> "adb-wireless"
        PrivilegeUiStartMethod.AdbTcpip -> "adb-tcpip"
        is PrivilegeUiStartMethod.External -> EXTERNAL_METHOD_ID_PREFIX + providerId
    }

internal fun parsePrivilegeUiStartMethod(methodId: String): PrivilegeUiStartMethod? =
    when {
        methodId == PrivilegeUiStartMethod.Root.methodId -> PrivilegeUiStartMethod.Root
        methodId == PrivilegeUiStartMethod.AdbWireless.methodId -> PrivilegeUiStartMethod.AdbWireless
        methodId == PrivilegeUiStartMethod.AdbTcpip.methodId -> PrivilegeUiStartMethod.AdbTcpip
        methodId.startsWith(EXTERNAL_METHOD_ID_PREFIX) ->
            PrivilegeUiStartMethod.External(methodId.removePrefix(EXTERNAL_METHOD_ID_PREFIX))
        else -> null
    }

internal class PrivilegeUiStartMethodStore(context: Context) {
    private val file = PrivilegeStoragePaths.file(
        context = context,
        fileName = START_METHOD_FILE_NAME,
    )

    fun read(): PrivilegeUiStartMethod? =
        synchronized(lock) {
            PrivilegeBinaryFileStore.readIfExists(file)
                ?.let { bytes ->
                    parsePrivilegeUiStartMethod(
                        String(bytes, StandardCharsets.UTF_8),
                    )
                }
        }

    fun write(method: PrivilegeUiStartMethod) {
        synchronized(lock) {
            PrivilegeBinaryFileStore.writeAtomically(
                file = file,
                bytes = method.methodId.toByteArray(StandardCharsets.UTF_8),
            )
        }
    }

    private companion object {
        const val START_METHOD_FILE_NAME = "ui-start-method"
        val lock = Any()
    }
}

internal fun privilegeUiStartMethod(
    source: PrivilegeUiRuntimeStartSource?,
    providerId: String?,
): PrivilegeUiStartMethod? =
    when (source) {
        PrivilegeUiRuntimeStartSource.ROOT -> PrivilegeUiStartMethod.Root
        PrivilegeUiRuntimeStartSource.ADB_WIRELESS -> PrivilegeUiStartMethod.AdbWireless
        PrivilegeUiRuntimeStartSource.ADB_STATIC_TCP -> PrivilegeUiStartMethod.AdbTcpip
        PrivilegeUiRuntimeStartSource.EXTERNAL -> providerId
            ?.let(PrivilegeUiStartMethod::External)
        null -> null
    }
