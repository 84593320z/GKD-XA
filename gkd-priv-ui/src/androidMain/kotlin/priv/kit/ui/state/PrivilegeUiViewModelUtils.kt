package priv.kit.ui.state

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import priv.kit.shared.toPrivilegeAdbDeviceNameText
import priv.kit.shared.toPrivilegeDiagnosticString
import priv.kit.shared.toPrivilegeShellArgument

internal fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun String.toPrivilegeUiAdbDeviceNameText(): String =
    toPrivilegeAdbDeviceNameText()

internal fun privilegeUiManualShellCommand(nativeStarterCommand: String): String =
    ADB_SHELL_PREFIX + nativeStarterCommand.trim()

internal fun privilegeUiManualShellScriptCommand(packageName: String, scriptPath: String): String {
    val shellScriptPath = "/sdcard/Android/data/$packageName/$scriptPath"
    // The POSIX host shell consumes outer quoting; adb passes inner quoting to Android's shell.
    val scriptArgument = shellScriptPath.toPrivilegeShellArgument().toPrivilegeShellArgument()
    return privilegeUiManualShellCommand("sh $scriptArgument")
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
internal fun isPrivilegeUiWirelessAdbSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

internal fun Throwable.failureMessage(): String =
    message ?: javaClass.simpleName

internal fun Throwable.toPrivilegeUiDiagnosticString(): String =
    toPrivilegeDiagnosticString()

private const val ADB_SHELL_PREFIX = "adb shell "
