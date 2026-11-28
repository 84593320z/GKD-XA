package li.gkd.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.io.Serializable
import java.security.MessageDigest

private const val GIT_INFO_SERVICE_NAME = "gkdGitInfo"

internal fun runGitCommandBytes(
    repositoryDirectory: String,
    arguments: List<String>,
): ByteArray {
    val process = ProcessBuilder(
        listOf("git", "-C", repositoryDirectory) + arguments,
    ).redirectErrorStream(true).start()
    val output = process.inputStream.readBytes()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("Command failed with exit code $exitCode: ${output.toString(Charsets.UTF_8)}")
    }
    return output
}

private fun runGitCommand(
    repositoryDirectory: String,
    arguments: List<String>,
): String {
    return runGitCommandBytes(repositoryDirectory, arguments)
        .toString(Charsets.UTF_8)
        .trim()
}

data class GitInfo(
    val commitId: String,
    val commitTime: String,
    val tagName: String?,
) : Serializable {
    val versionNameSuffix get() = if (tagName == null) ("-" + commitId.take(7)) else null
}

abstract class GitInfoValueSource : ValueSource<GitInfo, GitInfoValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repositoryDirectory: DirectoryProperty
    }

    override fun obtain(): GitInfo {
        return readGitInfo(parameters.repositoryDirectory.get().asFile.absolutePath)
    }
}

abstract class GitOutputValueSource : ValueSource<String, GitOutputValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repositoryDirectory: DirectoryProperty
        val arguments: ListProperty<String>
    }

    override fun obtain(): String {
        return runGitCommandBytes(
            parameters.repositoryDirectory.get().asFile.absolutePath,
            parameters.arguments.get(),
        ).toString(Charsets.UTF_8)
    }
}

abstract class GitInfoService : BuildService<GitInfoService.Parameters> {
    interface Parameters : BuildServiceParameters {
        val gitInfo: Property<GitInfo>
    }

    val gitInfo: GitInfo by lazy {
        parameters.gitInfo.get()
    }
}

val Project.gitInfo: GitInfo
    get() = gradle.sharedServices.registerIfAbsent(
        GIT_INFO_SERVICE_NAME,
        GitInfoService::class.java,
    ) {
        parameters.gitInfo.set(
            providers.of(GitInfoValueSource::class.java) {
                parameters.repositoryDirectory.set(rootProject.layout.projectDirectory)
            },
        )
    }.get().gitInfo

fun Project.releaseBuildKey(
    flavor: String,
    commitId: String,
): Provider<String> =
    providers.of(GitBuildKeyValueSource::class.java) {
        parameters.repositoryDirectory.set(rootProject.layout.projectDirectory)
        parameters.flavor.set(flavor)
        parameters.commitId.set(commitId)
    }

abstract class GitBuildKeyValueSource :
    ValueSource<String, GitBuildKeyValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repositoryDirectory: DirectoryProperty
        val flavor: Property<String>
        val commitId: Property<String>
    }

    override fun obtain(): String {
        val repositoryStateId = readRepositoryStateId(
            parameters.repositoryDirectory.get().asFile.absolutePath,
            parameters.commitId.get(),
        )
        return "${parameters.flavor.get()}-${repositoryStateId.take(16)}"
    }
}

private fun readGitInfo(repositoryDirectory: String): GitInfo {
    val commitId = runGitCommand(repositoryDirectory, listOf("rev-parse", "HEAD"))
    return GitInfo(
        commitId = commitId,
        commitTime = runGitCommand(repositoryDirectory, listOf("log", "-1", "--format=%ct")) + "000",
        tagName = runCatching {
            runGitCommand(repositoryDirectory, listOf("describe", "--tags", "--exact-match"))
        }.getOrNull(),
    )
}

internal fun readGitStatus(repositoryDirectory: String): ByteArray = runGitCommandBytes(
    repositoryDirectory,
    listOf("status", "--porcelain=v1", "-z", "--untracked-files=all"),
)

internal data class GitStatusEntry(val status: String, val path: String)

internal fun gitStatusEntries(status: ByteArray): List<GitStatusEntry> = buildList {
    val records = status.toString(Charsets.UTF_8).splitToSequence('\u0000').iterator()
    while (records.hasNext()) {
        val record = records.next()
        if (record.length < 4) continue
        val statusCode = record.substring(0, 2)
        add(GitStatusEntry(statusCode, record.substring(3)))
        // Rename/copy records include a second (source) path without a status prefix.
        if ((statusCode.contains('R') || statusCode.contains('C')) && records.hasNext()) {
            records.next()
        }
    }
}

internal fun sha256OfFile(file: File): ByteArray? {
    if (!file.isFile) return null
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest()
}

internal fun ByteArray.toHexDigest(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xff)
}

internal fun readRepositoryStateId(
    repositoryDirectory: String,
    commitId: String,
): String {
    val status = readGitStatus(repositoryDirectory)
    if (status.isEmpty()) return commitId

    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(commitId.toByteArray(Charsets.UTF_8))
    digest.update(0)
    digest.update(status)
    // Include staged blob IDs as the index can differ from the working copy.
    digest.update(
        runGitCommandBytes(
            repositoryDirectory,
            listOf("diff", "--cached", "--raw", "--no-abbrev", "-z", commitId),
        ),
    )
    for ((_, path) in gitStatusEntries(status).sortedBy { it.path }) {
        digest.update(0)
        digest.update(path.toByteArray(Charsets.UTF_8))
        digest.update(0)
        // Size and mtime can both stay the same while the bytes change.
        val contentDigest = sha256OfFile(File(repositoryDirectory, path))
        if (contentDigest != null) digest.update(contentDigest)
    }
    return digest.digest().toHexDigest()
}
