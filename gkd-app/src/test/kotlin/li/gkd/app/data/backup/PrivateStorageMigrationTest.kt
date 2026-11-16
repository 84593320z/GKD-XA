package li.gkd.app.data.backup

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class PrivateStorageMigrationTest {
    private fun withRoots(block: (File, File) -> Unit) {
        val temp = Files.createTempDirectory("private-storage-migration").toFile()
        try { block(temp.resolve("internal").apply { mkdir() }, temp.resolve("external").apply { mkdir() }) }
        finally { temp.deleteRecursively() }
    }
    private fun File.put(path: String, value: String) = resolve(path).apply { parentFile!!.mkdirs(); writeText(value) }

    @Test fun externalDataAndWalAreCopiedBeforeLegacyCleanup() = withRoots { internal, external ->
        external.put("db/gkd.db", "database")
        external.put("db/gkd.db-wal", "uncheckpointed")
        external.put("store/store.json", "credentials")
        external.put("snapshot/123/layout.json", "snapshot")
        val root = PrivateStorageMigration.migrate(internal, external)
        assertEquals("database", root.resolve("db/gkd.db").readText())
        assertEquals("uncheckpointed", root.resolve("db/gkd.db-wal").readText())
        assertEquals("snapshot", root.resolve("snapshot/123/layout.json").readText())
        assertFalse(external.resolve("store/store.json").exists())
        assertEquals(root, PrivateStorageMigration.migrate(internal, external))
    }

    @Test fun failedCopyPreservesEverySourceAndDoesNotCommit() = withRoots { internal, external ->
        external.put("db/gkd.db", "healthy")
        external.put("sh", "invalid-directory")
        assertTrue(runCatching { PrivateStorageMigration.migrate(internal, external) }.isFailure)
        assertEquals("healthy", external.resolve("db/gkd.db").readText())
        assertEquals("invalid-directory", external.resolve("sh").readText())
        assertFalse(internal.resolve("data-v2").exists())
        assertTrue(internal.listFiles().orEmpty().none { it.name.startsWith(".data-v2-migration-") })
    }

    @Test fun selectedInternalRootWinsAndInactiveDataIsRetained() = withRoots { internal, external ->
        internal.put(".gkd", "")
        internal.put("store/store.json", "active")
        external.put("store/store.json", "inactive")
        val root = PrivateStorageMigration.migrate(internal, external)
        assertEquals("active", root.resolve("store/store.json").readText())
        assertEquals("inactive", root.resolve("legacy-recovery/0/store/store.json").readText())
    }

    @Test fun retryNeverOverwritesNewDataOrDeletesUnverifiedLeftover() = withRoots { internal, external ->
        external.put("store/store.json", "old")
        val root = PrivateStorageMigration.migrate(internal, external)
        external.put("store/store.json", "old")
        root.put("store/store.json", "new-user-edit")
        PrivateStorageMigration.migrate(internal, external)
        assertEquals("new-user-edit", root.resolve("store/store.json").readText())
        assertEquals("old", external.resolve("store/store.json").readText())
    }

    @Test fun directorySyncFailureRetainsOriginalsAndRetryCompletes() = withRoots { internal, external ->
        external.put("store/store.json", "healthy")
        val failure = runCatching {
            PrivateStorageMigration.migrate(internal, external, syncDirectory = { directory ->
                if (directory == internal) throw IOException("directory sync failed")
            })
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals("healthy", external.resolve("store/store.json").readText())
        assertEquals("healthy", internal.resolve("data-v2/store/store.json").readText())
        val root = PrivateStorageMigration.migrate(internal, external)
        assertEquals("healthy", root.resolve("store/store.json").readText())
        assertFalse(external.resolve("store/store.json").exists())
    }

    @Test fun interruptedStageIsReclaimedOnlyWithVerifiedCopy() = withRoots { internal, external ->
        val stage = internal.resolve(".data-v2-migration-12345678-1234-1234-1234-123456789abc")
        external.put("store/store.json", "healthy-original")
        stage.put("store/store.json", "healthy")
        val unrelated = internal.resolve(".data-v2-migration-not-a-uuid").apply { mkdir() }
        unrelated.put("keep.txt", "unique")
        val root = PrivateStorageMigration.migrate(internal, external)
        assertFalse(stage.exists())
        assertEquals("healthy-original", root.resolve("store/store.json").readText())
        assertEquals("unique", unrelated.resolve("keep.txt").readText())
    }

    @Test fun missingOrDifferentSourceRetainsOnlyStageWithoutAllocatingAnother() = withRoots { internal, external ->
        val stage = internal.resolve(".data-v2-migration-12345678-1234-1234-1234-123456789abc")
        stage.put("store/store.json", "only-copy")
        assertTrue(runCatching { PrivateStorageMigration.migrate(internal, null) }.isFailure)
        assertEquals("only-copy", stage.resolve("store/store.json").readText())
        external.put("store/store.json", "different")
        assertTrue(runCatching { PrivateStorageMigration.migrate(internal, external) }.isFailure)
        assertEquals("only-copy", stage.resolve("store/store.json").readText())
        assertEquals(1, internal.listFiles().orEmpty().count { it.name.startsWith(".data-v2-migration-") })
        assertFalse(internal.resolve("data-v2").exists())
    }

    @Test fun committedCopyAllowsStageRecoveryWithoutRewritingDestination() = withRoots { internal, external ->
        external.put("store/store.json", "healthy")
        val root = PrivateStorageMigration.migrate(internal, external)
        val stage = internal.resolve(".data-v2-migration-12345678-1234-1234-1234-123456789abc")
        stage.put("store/store.json", "healthy")
        assertEquals(root, PrivateStorageMigration.migrate(internal, null))
        assertFalse(stage.exists())
        assertEquals("healthy", root.resolve("store/store.json").readText())
    }

    @Test fun failedAttemptRetainsStageWhenSourceChangesBeforeCleanup() = withRoots { internal, external ->
        external.put("store/store.json", "only-before-change")
        val failure = runCatching {
            PrivateStorageMigration.migrate(internal, external, syncDirectory = {
                external.resolve("store/store.json").writeText("replaced-source")
                throw IOException("sync failed after source changed")
            })
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        val stage = internal.listFiles().orEmpty().single { it.name.startsWith(".data-v2-migration-") }
        assertEquals("only-before-change", stage.resolve("store/store.json").readText())
    }

    @Test fun migrationPreservesHistoricalSnapshotDirectoryTime() = withRoots { internal, external ->
        val oldTime = 1_700_000_000_000L
        val nested = external.put("snapshot/1700000000000/nested/layout.json", "{}")
        nested.setLastModified(oldTime)
        nested.parentFile.setLastModified(oldTime)
        nested.parentFile.parentFile.setLastModified(oldTime)
        val root = PrivateStorageMigration.migrate(internal, external)
        assertEquals(oldTime, root.resolve("snapshot/1700000000000").lastModified())
        assertEquals(oldTime, root.resolve("snapshot/1700000000000/nested").lastModified())
    }

    @Test fun unavailableExternalRootCannotSilentlyInitializeOldInstall() = withRoots { internal, _ ->
        assertTrue(runCatching { PrivateStorageMigration.migrate(internal, null) }.exceptionOrNull() is IOException)
        assertFalse(internal.resolve("data-v2").exists())
        assertTrue(PrivateStorageMigration.migrate(internal, null, allowEmptyInitialization = true).isDirectory)
    }
}
