package li.gkd.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SubsItemDaoTest {
    @Test
    fun conditionalUrlUpdateUsesAffectedRowsAndPreservesUnrelatedColumns() = runBlocking {
        val directory = Files.createTempDirectory("subs-item-cas")
        val database = Room.databaseBuilder<AppDb>(directory.resolve("test.db").toString())
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        try {
            val dao = database.subsItemDao()
            val original = SubsItem(
                id = 7,
                ctime = 11,
                mtime = 22,
                enable = true,
                enableUpdate = false,
                order = 4,
                updateUrl = "https://old.example/sub.json",
            )
            dao.upsert(original)
            val affected = dao.updateUrlIfUnchanged(
                id = 7,
                expectedCtime = 11,
                expectedMtime = 22,
                expectedUpdateUrl = original.updateUrl,
                updateUrl = "https://new.example/sub.json",
                mtime = 23,
            )
            assertEquals(1, affected)
            assertEquals(
                original.copy(mtime = 23, updateUrl = "https://new.example/sub.json"),
                dao.queryAll().single(),
            )
            assertEquals(0, dao.updateUrlIfUnchanged(7, 10, 23, "https://new.example/sub.json", "bad", 24))
            assertEquals(0, dao.updateUrlIfUnchanged(7, 11, 22, "https://new.example/sub.json", "bad", 24))
            assertEquals(0, dao.updateUrlIfUnchanged(7, 11, 23, "https://stale.example/sub.json", "bad", 24))
            assertEquals(0, dao.updateUrlIfUnchanged(99, 11, 23, "https://new.example/sub.json", "bad", 24))
            assertEquals(original.copy(mtime = 23, updateUrl = "https://new.example/sub.json"), dao.queryAll().single())
        } finally {
            database.close()
            directory.toFile().deleteRecursively()
        }
    }
}
