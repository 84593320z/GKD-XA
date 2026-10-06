package li.gkd.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.security.MessageDigest

private const val GIT_UNAVAILABLE_PREFIX = "# git status unavailable"

/**
 * 产出三样东西。
 *
 * 1. `source-paths.txt`（进 APK assets，语义与原来一致）：构建提交里 tracked 且非测试的 `.kt` 清单，
 *    来自 `git ls-tree`，所以它**天生看不见未提交的文件**。
 * 2. `working-tree.txt`（进 APK assets）：`git status` 里未提交的源码文件，带状态、内容 sha256、路径。
 *    补的正是上面那个盲区 —— 万一工作区没了而 APK 还在，至少能知道曾经有哪些文件不在 git 里。
 *    默认只记路径与哈希，不记内容。级别开关：`-Pgkd.workingTreeReport=full|names|off`。
 * 3. `build/gkd-working-tree/`（不进 APK，也不由 CI 上传）：`dirty.patch` + `modified/` + `untracked/`
 *    副本，是唯一能把未提交内容本身救回来的东西。公开仓库的 artifact 谁都能下，所以内容默认不外传。
 *
 * 正确性要点：哈希在 ValueSource 里算，因此任务输入随文件内容变化，`@CacheableTask` 与
 * up-to-date 判断都不会把过期快照判成最新。非源码文件（图片等）也参与哈希，只是不进报告与快照。
 */
@CacheableTask
abstract class GenerateSourcePathsTask : DefaultTask() {
    @get:Input
    abstract val sourcePaths: ListProperty<String>

    /** 每行 `XY <path>\t<sha256>`；git 不可用时是以 `# git status unavailable` 开头的单行。 */
    @get:Input
    abstract val workingTreeEntries: Property<String>

    @get:Input
    abstract val gitCommitId: Property<String>

    @get:Input
    abstract val reportDetail: Property<String>

    @get:Internal
    abstract val repositoryDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val snapshotDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val paths = sourcePaths.get()
        require(paths.isNotEmpty()) { "No tracked Kotlin source files found" }
        require(paths == paths.sorted()) { "Kotlin source paths must be sorted" }
        require(paths.size == paths.distinct().size) { "Duplicate Kotlin source paths found" }
        require(paths.none { '\r' in it || '\n' in it || '\\' in it }) {
            "Kotlin source paths must be single-line repository-relative paths"
        }
        outputDirectory.file("source-paths.txt").get().asFile.writeText(
            paths.joinToString(separator = "\n", postfix = "\n"),
            Charsets.UTF_8,
        )

        val entries = parseEntries(workingTreeEntries.get()).filter { isSourcePath(it.path) }
        writeWorkingTreeReport(entries)
        writeWorkingTreeSnapshot(entries)
        if (entries.isNotEmpty()) {
            logger.warn(
                "GKD working tree is not clean: ${entries.size} uncommitted source file(s) " +
                    "relative to commit ${gitCommitId.get().take(7)}. They are absent from " +
                    "source-paths.txt and only exist in this directory. " +
                    "Backup refreshed at: ${snapshotDirectory.get().asFile}",
            )
        }
    }

    private fun writeWorkingTreeReport(entries: List<StatusEntry>) {
        val detail = reportDetail.get()
        if (detail == "off") return
        val report = outputDirectory.file("working-tree.txt").get().asFile
        if (entries.isEmpty()) {
            report.writeText("# commit=${gitCommitId.get()}\n# clean\n", Charsets.UTF_8)
            return
        }
        val body = entries.joinToString(separator = "\n", postfix = "\n") { entry ->
            if (detail == "full") "${entry.status} ${entry.sha256}  ${entry.path}"
            else "${entry.status} ${entry.path}"
        }
        report.writeText(
            "# commit=${gitCommitId.get()}\n# detail=$detail\n$body",
            Charsets.UTF_8,
        )
    }

    private fun writeWorkingTreeSnapshot(entries: List<StatusEntry>) {
        val snapshotRoot = snapshotDirectory.get().asFile
        val repo = repositoryDirectory.get().asFile
        for (entry in entries) {
            val source = File(repo, entry.path)
            if (!source.isFile) continue
            val kind = if (entry.status.startsWith("??")) "untracked" else "modified"
            val target = File(snapshotRoot, "$kind/${entry.path}")
            target.parentFile.mkdirs()
            source.copyTo(target, overwrite = true)
        }
        val patchFile = File(snapshotRoot, "dirty.patch")
        val diff = runCatching {
            runGitCommandBytes(repo.absolutePath, listOf("diff", "HEAD", "--binary"))
        }.getOrNull()
        if (diff != null && diff.isNotEmpty()) {
            patchFile.writeBytes(diff)
        } else if (patchFile.exists()) {
            patchFile.delete()
        }
    }

    private fun parseEntries(raw: String): List<StatusEntry> {
        if (raw.startsWith(GIT_UNAVAILABLE_PREFIX)) return emptyList()
        // 用「行尾是 tab + 64 位哈希 / missing」锚定，避免路径本身含 tab 时切错。
        val pattern = Regex("""^(.. .*)\t([0-9a-f]{64}|missing)$""")
        return raw.lineSequence()
            .mapNotNull { pattern.matchEntire(it) }
            .map { match ->
                val head = match.groupValues[1]
                StatusEntry(
                    status = head.substring(0, 2),
                    path = head.substring(3),
                    sha256 = match.groupValues[2],
                )
            }
            .sortedBy { it.path }
            .toList()
    }

    private fun isSourcePath(path: String): Boolean =
        path.endsWith(".kt") || path.endsWith(".kts") || path.endsWith(".java") ||
            path.endsWith(".aidl") || path.endsWith(".pro") || path.endsWith(".toml") ||
            path.endsWith(".gradle") || (path.contains("/src/") && path.endsWith(".xml"))

    private data class StatusEntry(val status: String, val path: String, val sha256: String)
}

fun Project.registerSourcePathsTask(commitId: String): TaskProvider<GenerateSourcePathsTask> {
    val trackedKotlinSourcePaths = providers.of(GitOutputValueSource::class.java) {
        parameters.repositoryDirectory.set(rootProject.layout.projectDirectory)
        parameters.arguments.set(
            listOf(
                "ls-tree",
                "-r",
                "-z",
                "--name-only",
                commitId,
            ),
        )
    }.map { output ->
        output
            .split('\u0000')
            .filter { it.endsWith(".kt") }
            .filterNot { path ->
                "/jvmTest/kotlin/" in path ||
                    "/androidTest/kotlin/" in path ||
                    "/test/kotlin/" in path ||
                    "/li/songe/gradle/" in path
            }
            .sorted()
    }

    val workingTreeEntries = providers.of(WorkingTreeEntriesValueSource::class.java) {
        parameters.repositoryDirectory.set(rootProject.layout.projectDirectory)
    }

    val generatedAssetsDirectory = layout.buildDirectory.dir("generated/assets/sourcePaths")
    return tasks.register("generateSourcePaths", GenerateSourcePathsTask::class.java) {
        sourcePaths.set(trackedKotlinSourcePaths)
        workingTreeEntries.set(workingTreeEntries)
        gitCommitId.set(commitId)
        reportDetail.set(
            providers.gradleProperty("gkd.workingTreeReport").getOrElse("full"),
        )
        repositoryDirectory.set(rootProject.layout.projectDirectory)
        snapshotDirectory.set(layout.buildDirectory.dir("gkd-working-tree"))
        outputDirectory.set(generatedAssetsDirectory)
    }
}

/**
 * `git status --porcelain=v1 -z --untracked-files=all` + 每个文件内容的 sha256。
 *
 * - `--untracked-files=all`：否则新建目录里的文件会被折叠成目录名，看不见具体文件。
 * - git 不可用（源码 tarball 构建、无 git 环境）时返回标记字符串而不是抛异常，构建照常继续。
 * - 重命名在 `-z` 下是两条记录（新路径 / 旧路径），这里按新路径处理。
 * - 编码按行分隔，因此路径含换行的条目会在解析时被跳过（项目约定路径不含换行，
 *   `source-paths.txt` 也直接 require 了这一点）；这类文件不会进报告也不会进快照。
 */
abstract class WorkingTreeEntriesValueSource :
    ValueSource<String, WorkingTreeEntriesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repositoryDirectory: DirectoryProperty
    }

    override fun obtain(): String = runCatching {
        val repoDir = parameters.repositoryDirectory.get().asFile
        val raw = runGitCommandBytes(
            repoDir.absolutePath,
            listOf("status", "--porcelain=v1", "-z", "--untracked-files=all"),
        ).toString(Charsets.UTF_8)
        raw.split('\u0000')
            .filter(String::isNotEmpty)
            .filter { it.length >= 4 }
            .joinToString(separator = "\n", postfix = "\n") { record ->
                val path = record.substring(3)
                "${record.substring(0, 2)} $path\t${sha256OfFile(File(repoDir, path))}"
            }
    }.getOrElse { error ->
        val reason = error.message?.lineSequence()?.firstOrNull() ?: error.toString()
        "$GIT_UNAVAILABLE_PREFIX: $reason"
    }

    private fun sha256OfFile(file: File): String {
        if (!file.isFile) return "missing"
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }
}
