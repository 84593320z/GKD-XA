package li.gkd.app.data.subscription

import kotlinx.coroutines.runBlocking
import li.gkd.app.data.RawSubscription
import li.gkd.app.util.json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SubscriptionFileMutationJournalTest {
    @Test
    fun committedSaveSurvivesRestartAndRetiresJournal() = runBlocking {
        val root = Files.createTempDirectory("subscription-save-committed").toFile()
        try {
            val body = RawSubscription(id = 3, name = "new", version = 2)
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(SubscriptionBodyChange(3, null, 12, body)))
            journal.recover { mapOf(3L to 12L) }
            assertTrue(root.resolve("3.json").readText().contains("new"))
            assertFalse(root.resolve(".subscription-mutation.active").exists())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun uncommittedSaveRestoresBodyAndExistingSidecars() = runBlocking {
        val root = Files.createTempDirectory("subscription-save-rollback").toFile()
        try {
            val old = RawSubscription(id = 4, name = "old", version = 1)
            root.resolve("4.json").writeText(json.encodeToString(old))
            root.resolve("4.json.bak").writeText("legacy backup")
            root.resolve("4.json.new").writeText("legacy temp")
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(SubscriptionBodyChange(4, 5, 6, RawSubscription(4, "new", 2))))
            journal.recover { mapOf(4L to 5L) }
            assertTrue(root.resolve("4.json").readText().contains("old"))
            assertEquals("legacy backup", root.resolve("4.json.bak").readText())
            assertEquals("legacy temp", root.resolve("4.json.new").readText())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun uncommittedDeleteRestoresBody() = runBlocking {
        val root = Files.createTempDirectory("subscription-delete-rollback").toFile()
        try {
            root.resolve("5.json").writeText("keep on rollback")
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(SubscriptionBodyChange(5, 7, null, null)))
            journal.recover { mapOf(5L to 7L) }
            assertEquals("keep on rollback", root.resolve("5.json").readText())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun committedDeleteRemainsDeletedAfterRestart() = runBlocking {
        val root = Files.createTempDirectory("subscription-delete-committed").toFile()
        try {
            root.resolve("5.json").writeText("old")
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(SubscriptionBodyChange(5, 7, null, null)))
            journal.recover { emptyMap() }
            assertFalse(root.resolve("5.json").exists())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun completesPartiallyInstalledCommitAfterRestart() = runBlocking {
        val root = Files.createTempDirectory("subscription-partial-install").toFile()
        try {
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(
                SubscriptionBodyChange(6, null, 8, RawSubscription(6, "six", 1)),
                SubscriptionBodyChange(7, null, 9, RawSubscription(7, "seven", 1)),
            ))
            root.resolve("7.json").delete()
            journal.recover { mapOf(6L to 8L, 7L to 9L) }
            assertTrue(root.resolve("6.json").isFile)
            assertTrue(root.resolve("7.json").readText().contains("seven"))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun refusesMixedRoomStateBeforeChangingAnyLiveBody() = runBlocking {
        val root = Files.createTempDirectory("subscription-mixed-state").toFile()
        try {
            val journal = SubscriptionFileMutationJournal(root) {}
            journal.stageAndInstall(listOf(
                SubscriptionBodyChange(6, null, 8, RawSubscription(6, "six", 1)),
                SubscriptionBodyChange(7, null, 9, RawSubscription(7, "seven", 1)),
            ))
            assertThrows(IllegalStateException::class.java) {
                runBlocking { journal.recover { mapOf(6L to 8L) } }
            }
            assertTrue(root.resolve("6.json").isFile)
            assertTrue(root.resolve("7.json").isFile)
            assertTrue(root.resolve(".subscription-mutation.active").isDirectory)
        } finally { root.deleteRecursively() }
    }
}
