package li.gkd.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

object BuildSnapshotRegression {
    @JvmStatic
    fun main(args: Array<String>) {
        val repo = Files.createTempDirectory("gkd-buildsrc-regression-").toFile()
        try {
            git(repo, "init", "-q")
            git(repo, "config", "core.autocrlf", "false")
            File(repo, ".gitignore").writeText("/build/\n/.gradle/\n")
            val tracked = File(repo, "Tracked.kt").apply { writeText("clean!\n") }
            git(repo, "add", ".gitignore", tracked.name)
            git(repo, "-c", "user.name=Build Test", "-c", "user.email=test@example.invalid",
                "-c", "commit.gpgsign=false", "-c", "core.hooksPath=.git/no-hooks", "commit", "-qm", "Fixture")
            val commit = git(repo, "rev-parse", "HEAD")
            check(readRepositoryStateId(repo.absolutePath, commit) == commit)

            tracked.writeText("before\n")
            dirtyKeyReadsBytesBeyondSizeAndMtime(repo, tracked, commit)
            regenerationRemovesStaleOutputs(repo, tracked, commit)
            println("Build snapshot regression checks passed")
        } finally {
            repo.deleteRecursively()
        }
    }

    private fun dirtyKeyReadsBytesBeyondSizeAndMtime(repo: File, tracked: File, commit: String) {
        val path = tracked.toPath()
        // Use a fixed, old timestamp so Git's first status sees the tracked file as dirty.
        Files.setLastModifiedTime(path, FileTime.fromMillis(1_700_000_000_000))
        val size = tracked.length()
        val mtime = Files.getLastModifiedTime(path)
        val status = readGitStatus(repo.absolutePath)
        check(status.isNotEmpty()) { "Fixture must start dirty" }
        val key = readRepositoryStateId(repo.absolutePath, commit)
        tracked.writeText("after!\n")
        Files.setLastModifiedTime(path, mtime)
        check(tracked.length() == size && Files.getLastModifiedTime(path) == mtime)
        check(readGitStatus(repo.absolutePath).contentEquals(status))
        val newKey = readRepositoryStateId(repo.absolutePath, commit)
        check(newKey != key) { "Same-size dirty bytes with an unchanged mtime must change the key" }
        check(readRepositoryStateId(repo.absolutePath, commit) == newKey)
    }

    private fun regenerationRemovesStaleOutputs(repo: File, tracked: File, commit: String) {
        val project = ProjectBuilder.builder().withProjectDir(repo).build()
        val task = project.registerSourcePathsTask(commit).get()
        val assets = task.outputDirectory.get().asFile.apply { mkdirs() }
        val report = File(assets, "working-tree.txt")
        val snapshot = task.snapshotDirectory.get().asFile
        val oldFile = File(repo, "Old.kt").apply { writeText("old\n") }

        task.reportDetail.set("full")
        task.generate()
        check(report.readText().contains(oldFile.name))
        check(File(snapshot, "modified/Tracked.kt").readText() == tracked.readText())
        check(File(snapshot, "untracked/Old.kt").readText() == oldFile.readText())
        check(File(snapshot, "dirty.patch").length() > 0)

        git(repo, "restore", "--", tracked.name)
        check(oldFile.delete())
        val newFile = File(repo, "New.kt").apply { writeText("new\n") }
        // Use a fresh ValueSource, as Gradle would in the next build.
        task.workingTreeEntries.set(project.providers.of(WorkingTreeEntriesValueSource::class.java) {
            parameters.repositoryDirectory.set(project.layout.projectDirectory)
        })
        task.reportDetail.set("off")
        task.generate()
        check(!report.exists()) { "Turning the report off must remove the old report" }
        check(File(assets, "source-paths.txt").readText() == "Tracked.kt\n")
        val snapshotPaths = snapshot.walkTopDown().filter(File::isFile).map { it.relativeTo(snapshot).invariantSeparatorsPath }.toList()
        check(snapshotPaths == listOf("untracked/New.kt")) { "Stale snapshot paths: " + snapshotPaths }
        check(File(snapshot, snapshotPaths.single()).readText() == newFile.readText())
    }

    private fun git(repo: File, vararg args: String): String =
        runGitCommandBytes(repo.absolutePath, args.toList()).toString(Charsets.UTF_8).trim()
}
