package li.gkd.app.util

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * version/stable.json 与 version/beta.json 既是发版流程的输入，也是「测试版」更新通道实际返回的响应体。
 * 它们靠手工拼装、CI 只回填 fileSize，所以把「改了一个字段忘了另一个」钉成构建门禁。
 */
class UpdateChannelMetadataTest {
    // CHANGELOG.md 顶部小节标题允许带预发布后缀（## v1.4.5-beta-1（2026-…）），
    // 但不能把紧跟的中文括号日期吞进去。
    private val CHANGELOG_SECTION = Regex("^## (v\\d+\\.\\d+\\.\\d+[^\\s（]*)", RegexOption.MULTILINE)

    private val channelJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val repoRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "version/stable.json").isFile && File(it, "CHANGELOG.md").isFile }
            ?: error("找不到同时包含 version/stable.json 与 CHANGELOG.md 的仓库根目录")
    }

    private fun readChannel(name: String): Pair<NewVersion, String> {
        val text = File(repoRoot, "version/$name.json").readText()
        val element = channelJson.parseToJsonElement(text)
        val version: NewVersion = channelJson.decodeFromString(text)
        val changelog = element.jsonObject["changelog"]?.jsonPrimitive?.contentOrNull ?: ""
        return version to changelog
    }

    @Test
    fun everyChannelFieldIsSelfConsistent() {
        listOf("stable", "beta").forEach { name ->
            val (version, changelog) = readChannel(name)
            val tag = "v${version.versionName}"

            assertTrue("$name.json fileSize 不像真实 APK: ${version.fileSize}",
                version.fileSize > 1_000_000L)

            assertTrue("$name.json downloadUrl 与 versionName 不是同一个 tag",
                version.downloadUrl.contains("/releases/download/$tag/"))
            assertTrue("$name.json downloadUrl 附件名不符合 GKD-XA-<tag>.apk",
                version.downloadUrl.endsWith("GKD-XA-$tag.apk"))

            // 真正的「改了一个忘另一个」是 JSON 与 build.gradle.kts 不一致：
            // JSON 里的 versionCode 必须等于 META.versionCode（gradle 手工 bump 的那个数），
            // 用户侧比较用的就是这个数；tag 解析出的 10404 是另一套编号，不能拿来当断言。
            val gradle = File(repoRoot, "gkd-app/build.gradle.kts").readText()
            val gradleCode = Regex("""versionCode\s*=\s*(\d+)""").find(gradle)
                ?.groupValues?.get(1)?.toInt()
            val gradleName = Regex("""versionName\s*=\s*"([^"]+)"""").find(gradle)
                ?.groupValues?.get(1)
            assertEquals("$name.json versionCode 与 gkd-app/build.gradle.kts 不一致",
                gradleCode, version.versionCode)
            assertEquals("$name.json versionName 与 gkd-app/build.gradle.kts 不一致",
                gradleName, version.versionName)

            val head = version.versionLogs.firstOrNull()
            assertTrue("$name.json versionLogs 为空", head != null)
            assertEquals("$name.json 更新日志首条不是当前版本", version.versionName, head!!.name)
            assertEquals("$name.json 更新日志首条 code 不是当前 versionCode",
                version.versionCode, head.code)
            assertTrue("$name.json 的 changelog 首行没写当前版本（复制上一版忘了改）",
                changelog.startsWith(tag))

            val codes = version.versionLogs.map { it.code }
            assertEquals("$name.json versionLogs 必须按 versionCode 倒序",
                codes.sortedDescending(), codes)
            assertEquals("$name.json versionLogs 有重复版本", codes.distinct().size, codes.size)
        }
    }

    @Test
    fun changelogTopSectionMatchesStableChannelVersion() {
        val (version, _) = readChannel("stable")
        val header = CHANGELOG_SECTION.find(File(repoRoot, "CHANGELOG.md").readText())
            ?.groupValues?.get(1)
        assertEquals("CHANGELOG.md 顶部小节与 stable.json 版本不一致",
            "v${version.versionName}", header)
    }
}
