package li.gkd.app.util

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hjq.device.compat.DeviceBrand
import com.hjq.device.compat.DeviceMarketName
import com.hjq.device.compat.DeviceOs
import li.gkd.app.META
import li.gkd.app.app
import li.songe.codeorigin.CallSite
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.days

object LogUtils {
    fun d(
        vararg args: Any?,
        @CallSite loc: String = "",
        @CallSite("{file}") fileName: String = "",
        tag: String = fileName.substringBeforeLast('.'),
    ) {
        val name = Thread.currentThread().name
        val actualLoc = loc.removePrefix("li.gkd.app.")
        val texts = args.map { stringify(it) }
        if (META.debuggable) {
            val msg = buildString {
                append("$name, $actualLoc")
                texts.forEachIndexed { i, text ->
                    if (texts.size == 1) {
                        append("\n")
                    } else {
                        append("\n[$i]: ")
                    }
                    append(text)
                }
            }
            Log.d(tag, msg)
        }
        val t = System.currentTimeMillis()
        logFileExecutor.run {
            logToFile(tag, name, actualLoc, texts, t)
        }
    }
}

private val logFileExecutor = Executors.newSingleThreadExecutor()
private const val MAX_LOG_KEEP_DAYS = 7
val deviceInfoDesc by lazy {
    listOf(
        android.os.Build.MANUFACTURER,
        android.os.Build.MODEL,
        DeviceBrand.getBrandName(),
        DeviceOs.getOsName() + DeviceOs.getOsVersionName() + DeviceOs.getOsBigVersionCode(),
        DeviceMarketName.getMarketName(app)
    ).joinToString("/")
}
private val deviceInfoText by lazy {
    buildString {
        append("Android: ${android.os.Build.VERSION.RELEASE} (${android.os.Build.VERSION.SDK_INT})\n")
        append("Device: ${deviceInfoDesc}\n")
        append("App: ${META.versionName} (${META.versionCode})\n")
    }
}

private const val MAX_LOG_ENTRY_BYTES = 64 * 1024
private const val MAX_LOG_FILE_BYTES = 2 * 1024 * 1024L
private const val MAX_LOG_TOTAL_BYTES = 16 * 1024 * 1024L
private const val LOG_TRUNCATED_MARK = "\n...[单条日志超过 64KB，已截断]...\n"

private fun logToFile(tag: String, name: String, loc: String, texts: List<String>, t: Long) {
    val dateText = t.format("yyyyMMdd")
    val baseFile = FolderUtils.logFolder.resolve("gkd-$dateText.log")
    if (!baseFile.exists()) {
        // 沿用原清理逻辑：新一天首次写时，删 7 天前的文件（按 lastModified 判断，
        // 分片文件同样命中，不会漏删）
        val files = FolderUtils.logFolder.listFiles()
        if (files != null && files.size >= MAX_LOG_KEEP_DAYS) {
            files.forEach {
                if (t - it.lastModified() > MAX_LOG_KEEP_DAYS.days.inWholeMilliseconds) {
                    it.delete()
                }
            }
        }
    }
    val sb = StringBuilder()
    sb.append(t.format("HH:mm:ss.SSS"))
    sb.append(" $tag, $name, $loc")
    if (texts.size == 1) {
        sb.append('\n')
        sb.append(texts[0])
    } else {
        texts.forEachIndexed { i, text ->
            sb.append("\n[$i]: ")
            sb.append(text)
        }
    }
    sb.append("\n\n")
    var entry = sb.toString()
    // 单条上限 64KB（按 UTF-8 字节计），超出截断并留标记
    var entryBytes = entry.toByteArray(Charsets.UTF_8)
    if (entryBytes.size > MAX_LOG_ENTRY_BYTES) {
        val markBytes = LOG_TRUNCATED_MARK.toByteArray(Charsets.UTF_8)
        entry = String(
            entryBytes,
            0,
            MAX_LOG_ENTRY_BYTES - markBytes.size,
            Charsets.UTF_8,
        ) + LOG_TRUNCATED_MARK
        entryBytes = entry.toByteArray(Charsets.UTF_8)
    }
    // 单文件上限 2MB：当天基础文件写满后滚动到 gkd-yyyyMMdd-2.log、-3.log……
    var file = baseFile
    var shard = 1
    while (file.exists() && file.length() + entryBytes.size > MAX_LOG_FILE_BYTES) {
        shard++
        file = FolderUtils.logFolder.resolve("gkd-$dateText-$shard.log")
    }
    if (!file.exists()) {
        file.appendText(buildString {
            append("=== Log ===\n")
            append("Date: ${t.format("yyyy-MM-dd HH:mm:ss.SSS")}\n")
            append(deviceInfoText)
            append("=== Log ===\n\n")
        })
    }
    file.appendText(entry)
    trimLogFolderTotal(keep = file)
}

// 日志目录总量上限 16MB：超出时从最旧文件开始删，刚写入的文件不删
private fun trimLogFolderTotal(keep: File) {
    val files = FolderUtils.logFolder.listFiles()?.filter { it.isFile } ?: return
    var totalBytes = files.sumOf { it.length() }
    if (totalBytes <= MAX_LOG_TOTAL_BYTES) return
    files.filter { it.absolutePath != keep.absolutePath }
        .sortedBy { it.lastModified() }
        .forEach { oldFile ->
            if (totalBytes <= MAX_LOG_TOTAL_BYTES) return
            val len = oldFile.length()
            if (oldFile.delete()) {
                totalBytes -= len
            }
        }
}

private fun stringify(arg: Any?): String = when (arg) {
    is Bundle -> {
        val sb = StringBuilder()
        sb.append("Bundle{")
        val keys = arg.keySet()
        keys.forEachIndexed { index, key ->
            @Suppress("DEPRECATION")
            val value = arg.get(key)
            sb.append("$key=${stringify(value)}")
            if (index < keys.size - 1) {
                sb.append(",")
            }
        }
        sb.append("}")
        sb.toString()
    }

    is Intent -> {
        val sb = StringBuilder()
        sb.append("Intent{")
        arg.action?.let { sb.append("action=$it,") }
        arg.data?.let { sb.append("data=$it,") }
        arg.type?.let { sb.append("type=$it,") }
        arg.component?.let { sb.append("component=$it,") }
        arg.categories?.let { sb.append("categories=$it,") }
        arg.extras?.let { sb.append("extras=${stringify(it)}") }
        sb.append("}")
        sb.toString()
    }

    is Throwable -> Log.getStackTraceString(arg)

    else -> arg.toString()
}
