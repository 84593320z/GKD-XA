package li.gkd.app.util

/** Values read from PackageManager, never derived from a release tag. */
internal data class UpdateApkIdentity(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
    val signerSha256: Set<String>,
)

internal fun validateUpdateApkIdentity(
    archive: UpdateApkIdentity,
    installed: UpdateApkIdentity,
    expectedVersionCode: Int?,
    expectedVersionName: String,
): UpdateApkIdentity {
    require(archive.packageName == installed.packageName) { "更新包包名与当前应用不符" }
    require(archive.signerSha256.isNotEmpty() && archive.signerSha256 == installed.signerSha256) {
        "更新包签名与当前应用不符"
    }
    require(archive.versionCode > installed.versionCode) { "更新包版本不高于已安装版本" }
    require(expectedVersionCode == null || archive.versionCode == expectedVersionCode.toLong()) {
        "更新包 versionCode 与发布信息不符"
    }
    require(expectedVersionName.isNotBlank() && archive.versionName == expectedVersionName) {
        "更新包版本名称与发布信息不符"
    }
    return archive
}
