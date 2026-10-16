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
    // 与 Upgrade.kt 的 parseVersionCode / tag 判定保持同一套宽松度，
    // 否则 v1.4.5-beta-1 这类预发布 tag 会被本测试误判成字段不一致。
    private val VERSION_NAME = Regex("""(\d+)\.(\d+)\.(\d+)""")
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

            val parts = VERSION_NAME.findAll(version.versionName).firstOrNull()?.destructured
                ?: error("$name.json versionName 解析不出 x.y.z: ${version.versionName}")
            assertEquals(
                "$name.json versionCode 与 versionName 对不上（改了一个忘了另一个）",
                parts.component1().toInt() * 10000 + parts.component2().toInt() * 100 +
                    parts.component3().toInt(),
                version.versionCode,
            )

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
