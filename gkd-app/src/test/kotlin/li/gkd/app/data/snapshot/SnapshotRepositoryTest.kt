package li.gkd.app.data.snapshot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import li.gkd.db.Snapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files

class SnapshotRepositoryTest {
    @Test
    fun uploadLinkIsRecordedWhenScreenshotIsUnchanged() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-upload-test").toFile()
        try {
            val screenshot = screenshot(root)
            val dao = FakeSnapshotDao()
            val repository = SnapshotStore(dao, root)

            assertTrue(repository.markUploaded(1, 42, screenshot.lastModified()))
            assertEquals(42, dao.markedAssetId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun uploadLinkIsNotRecordedAfterScreenshotChanges() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-upload-changed-test").toFile()
        try {
            val screenshot = screenshot(root)
            val originalModifiedAt = screenshot.lastModified()
            assertTrue(screenshot.setLastModified(originalModifiedAt + 10_000))
            val dao = FakeSnapshotDao()
            val repository = SnapshotStore(dao, root)

            assertFalse(repository.markUploaded(1, 42, originalModifiedAt))
            assertEquals(null, dao.markedAssetId)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun screenshot(root: java.io.File) = root.resolve("1").apply { mkdirs() }
        .resolve("1.png").apply {
            writeBytes(byteArrayOf(
                0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            ))
            check(setLastModified(10_000))
        }

    @Test
    fun recentSnapshotIdQueryNeverReturnsMoreThanOneHundred() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-id-budget").toFile()
        try {
            val ids = (1L..150L).toMutableSet()
            val repository = SnapshotStore(FakeSnapshotDao(snapshotIds = ids), root)

            val result = repository.queryRecentSnapshotIds(1_000)

            assertEquals(100, result.size)
            assertEquals((150L downTo 51L).toList(), result)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun oversizedSnapshotIsRejectedBeforeReadingTheFullJson() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-byte-budget").toFile()
        try {
            val directory = root.resolve("1").apply { mkdirs() }
            directory.resolve("1.json").writeText("{\"payload\":\"" + "x".repeat(4_096) + "\"}")
            val repository = SnapshotStore(FakeSnapshotDao(), root)

            assertEquals(null, repository.getMinSnapshot(1, maxSourceBytes = 128))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryClearsRowsWhenSnapshotRootIsConfirmedAbsent() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-missing-root").toFile()
        val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1, 2))
        try {
            assertTrue(root.delete())

            SnapshotStore(dao, root).recoverInterruptedMutations()

            assertTrue(dao.snapshotIds.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryRestoresStagedDeleteWhenRoomRowStillExists() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-recover-delete-rollback").toFile()
        val staged = root.resolve(".1.delete-01234567-abcd-1234-abcd-0123456789ab")
        try {
            completeSnapshotDirectory(staged, 1)
            val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1))
            val repository = SnapshotStore(dao, root)

            repository.recoverInterruptedMutations()

            assertTrue(root.resolve("1/1.json").isFile)
            assertTrue(root.resolve("1/1.png").isFile)
            assertFalse(staged.exists())
            assertEquals(setOf(1L), dao.snapshotIds)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryFinishesDeleteWhenRoomRowWasAlreadyRemoved() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-recover-delete-commit").toFile()
        val staged = root.resolve(".1.delete-01234567-abcd-1234-abcd-0123456789ab")
        try {
            completeSnapshotDirectory(staged, 1)
            val repository = SnapshotStore(FakeSnapshotDao(snapshotIds = mutableSetOf()), root)

            repository.recoverInterruptedMutations()

            assertFalse(staged.exists())
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryRemovesOrphanPublishedAndTemporaryDirsAndDanglingRows() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-recover-mismatch").toFile()
        try {
            completeSnapshotDirectory(root.resolve("2"), 2)
            root.resolve(".3.tmp").apply { mkdirs(); resolve("partial").writeText("partial") }
            val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1, 3))
            val repository = SnapshotStore(dao, root)

            repository.recoverInterruptedMutations()

            assertTrue(root.listFiles().orEmpty().isEmpty())
            assertTrue(dao.snapshotIds.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryRestoresStagedWebpAndInvalidatesPotentiallyStaleUpload() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-recover-screenshot").toFile()
        try {
            val directory = root.resolve("1").apply { mkdirs() }
            directory.resolve("1.json").writeText("{}")
            val original = directory.resolve(".1.webp.replace-01234567-abcd-1234-abcd-0123456789ab")
            val oldWebp = "RIFF\u0004\u0000\u0000\u0000WEBP".encodeToByteArray()
            original.writeBytes(oldWebp)
            val pending = directory.resolve(".1.webp.123.tmp").apply { writeText("partial") }
            val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1))
            val repository = SnapshotStore(dao, root)

            repeat(2) { repository.recoverInterruptedMutations() }

            assertArrayEquals(oldWebp, repository.screenshotFile(1).readBytes())
            assertFalse(original.exists())
            assertFalse(pending.exists())
            assertEquals(listOf(1L), dao.invalidatedUploads)
            assertEquals(setOf(1L), dao.snapshotIds)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryRetainsIncompleteSnapshotDataForExistingRow() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-preserve-incomplete").toFile()
        try {
            val directory = root.resolve("1").apply { mkdirs() }
            val source = directory.resolve("1.json").apply { writeText("{}") }
            val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1))

            SnapshotStore(dao, root).recoverInterruptedMutations()

            assertTrue(source.isFile)
            assertEquals(setOf(1L), dao.snapshotIds)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recoveryKeepsStagedCopyWhenThePublishedSnapshotIsIncomplete() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-keep-staged").toFile()
        try {
            val staged = root.resolve(".1.delete-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            completeSnapshotDirectory(staged, 1)
            val published = root.resolve("1").apply { mkdirs() }
            val source = published.resolve("1.json").apply { writeText("{}") }
            val dao = FakeSnapshotDao(snapshotIds = mutableSetOf(1))

            SnapshotStore(dao, root).recoverInterruptedMutations()

            assertTrue(staged.resolve("1.json").isFile)
            assertTrue(source.isFile)
            assertEquals(setOf(1L), dao.snapshotIds)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun boundedSummaryUsesSourceWhenCacheCannotBeWritten() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-cache-unavailable").toFile()
        try {
            val directory = root.resolve("1").apply { mkdirs() }
            directory.resolve("1.json").writeText("""{"id":1,"nodes":[{}]}""")
            assertTrue(directory.resolve("1.min.json").mkdirs())

            val summary = SnapshotStore(FakeSnapshotDao(), root).getMinSnapshot(1, 128)

            assertEquals("""{"id":1,"nodes":[]}""", summary.toString())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun completeSnapshotDirectory(directory: java.io.File, id: Long) {
        check(directory.mkdirs())
        directory.resolve("$id.json").writeText("{}")
        directory.resolve("$id.png").writeBytes(byteArrayOf(
            0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
        ))
    }

    @Test
    fun deleteRestoresDirectoryWhenDatabaseDeleteFails() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-delete-test").toFile()
        val snapshot = snapshot()
        val directory = root.resolve(snapshot.id.toString()).apply { mkdirs() }
        directory.resolve("data").writeText("value")
        try {
            val repository = SnapshotStore(FakeSnapshotDao(deleteFailure = IOException("db")), root)

            val result = runCatching { repository.delete(snapshot) }

            assertTrue(result.isFailure)
            assertTrue(directory.resolve("data").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun deleteRemovesStagedDirectoryAfterDatabaseDeleteSucceeds() = runBlocking {
        val root = Files.createTempDirectory("gkd-snapshot-delete-success-test").toFile()
        val snapshot = snapshot()
        val directory = root.resolve(snapshot.id.toString()).apply { mkdirs() }
        directory.resolve("data").writeText("value")
        try {
            val repository = SnapshotStore(FakeSnapshotDao(), root)

            repository.delete(snapshot)

            assertFalse(directory.exists())
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun snapshot() = Snapshot(
        id = 1,
        appId = "app.id",
        activityId = null,
        screenHeight = 1,
        screenWidth = 1,
        isLandscape = false,
    )

    private class FakeSnapshotDao(
        private val deleteFailure: Exception? = null,
        val snapshotIds: MutableSet<Long> = mutableSetOf(1),
    ) : Snapshot.SnapshotDao {
        var markedAssetId: Int? = null

        override suspend fun insert(vararg users: Snapshot): List<Long> = users.map { it.id }

        override suspend fun delete(vararg users: Snapshot): Int {
            deleteFailure?.let { throw it }
            users.forEach { snapshotIds.remove(it.id) }
            return users.size
        }

        override fun query(): Flow<List<Snapshot>> = flowOf(emptyList())

        override suspend fun queryForDiagnostics(limit: Int, since: Long, until: Long, offset: Int): List<Snapshot> = emptyList()

        override suspend fun queryIds(): List<Long> = snapshotIds.toList()

        override suspend fun queryRecentIds(limit: Int): List<Long> = snapshotIds.sortedDescending().take(limit)

        override suspend fun deleteById(id: Long): Int = if (snapshotIds.remove(id)) 1 else 0

        val invalidatedUploads = mutableListOf<Long>()

        override suspend fun deleteGithubAssetId(id: Long) {
            invalidatedUploads += id
        }

        override suspend fun markUploadedIfPending(id: Long, assetId: Int): Int {
            markedAssetId = assetId
            return 1
        }

        override fun count(): Flow<Int> = flowOf(0)
    }
}
