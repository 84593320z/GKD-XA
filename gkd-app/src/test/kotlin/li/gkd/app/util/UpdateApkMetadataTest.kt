package li.gkd.app.util

import org.junit.Assert.*
import org.junit.Test

class UpdateApkMetadataTest {
    private val installed = UpdateApkIdentity("li.gkd.app", 152, "1.5.1", setOf("trusted signer"))
    private val archive = installed.copy(versionCode = 153, versionName = "1.5.2")

    @Test fun githubTagsNeverInventAndroidVersionCode() {
        val release = GitHubRelease(
            tagName = "v1.5.2", body = "changes",
            assets = listOf(GitHubReleaseAsset("GKD-XA-v1.5.2.apk", 123, "https://example.invalid/update.apk")),
        ).toNewVersion()
        assertNull(release.versionCode)
        assertEquals("1.5.2", release.versionName)
        assertEquals("changes", release.releaseNotes)
        assertEquals(archive, validateUpdateApkIdentity(archive, installed, release.versionCode, release.versionName))
    }

    @Test fun verifiedApkVersionMustMatchExplicitChannelCode() {
        assertEquals(archive, validateUpdateApkIdentity(archive, installed, 153, "1.5.2"))
        assertRejected(archive, 10502)
    }

    @Test fun rejectsWrongPackageSignerEmptySignerAndDowngrade() {
        listOf(
            archive.copy(packageName = "other.package"),
            archive.copy(signerSha256 = setOf("untrusted signer")),
            archive.copy(signerSha256 = emptySet()),
            archive.copy(versionCode = 152),
            archive.copy(versionCode = 151),
            archive.copy(versionName = "1.5.3"),
            archive.copy(versionName = null),
        ).forEach { assertRejected(it) }
    }

    @Test fun githubDigestIsOptionalAndOnlySha256IsForwarded() {
        val asset = GitHubReleaseAsset("app.apk", 123, "https://example.invalid/app.apk", "sha256:" + "ab".repeat(32))
        assertEquals("ab".repeat(32), GitHubRelease("v1.5.2", assets = listOf(asset)).toNewVersion().sha256)
        assertNull(GitHubRelease("v1.5.2", assets = listOf(asset.copy(digest = "sha512:other"))).toNewVersion().sha256)
    }

    private fun assertRejected(candidate: UpdateApkIdentity, code: Int? = null) {
        try { validateUpdateApkIdentity(candidate, installed, code, "1.5.2"); fail("Untrusted APK accepted: $candidate") }
        catch (_: IllegalArgumentException) { }
    }
}
