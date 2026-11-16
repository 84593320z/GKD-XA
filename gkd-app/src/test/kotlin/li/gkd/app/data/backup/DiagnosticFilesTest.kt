package li.gkd.app.data.backup

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class DiagnosticFilesTest {
    @Test fun redactsActualHeaderObjectsAndNestedText() {
        val text = """{"headers":[{"name":"X-Custom","value":"custom-secret"}],"apiKey":"key-secret","events":["Bearer bearer-secret"],"status":"ready"}"""
        val result = DiagnosticFiles.redact(text)
        listOf("custom-secret", "key-secret", "bearer-secret").forEach { assertFalse(result.contains(it)) }
        assertTrue(result.contains("ready"))
        assertTrue(result.contains("X-Custom"))
    }

    @Test fun wholeNestedEventJsonAndSqlOmissionMarkerRemainRedactable() {
        val secret = "SYNTHETIC_PREVIEW_SECRET"
        val inner = kotlinx.serialization.json.buildJsonObject {
            put("apiKey", kotlinx.serialization.json.JsonPrimitive(secret))
            put("status", kotlinx.serialization.json.JsonPrimitive("ready"))
        }.toString()
        val databaseText = kotlinx.serialization.json.Json.encodeToString(listOf(inner))
        val document = kotlinx.serialization.json.buildJsonObject {
            put("textPreview", kotlinx.serialization.json.JsonPrimitive(databaseText))
        }.toString()
        val result = DiagnosticFiles.redact(document)
        assertFalse(result.contains(secret))
        assertTrue(result.contains("ready"))
        // The companion diagnostic_sql_regression.py executes the production CASE projection
        // with 256 KiB nested JSON, and verifies it returns this whole marker rather than a slice.
        val marker = "[\"[omitted: diagnostic text exceeds 1024 characters]\"]"
        val omitted = kotlinx.serialization.json.buildJsonObject {
            put("textPreview", kotlinx.serialization.json.JsonPrimitive(marker))
        }.toString()
        assertTrue(DiagnosticFiles.redact(omitted).contains("omitted"))
        assertFalse(DiagnosticFiles.redact(omitted).contains(secret))

        val temp = Files.createTempDirectory("diagnostic-dto-redaction").toFile()
        try {
            for ((name, text) in listOf("short" to databaseText, "omitted" to marker)) {
                val dto = li.gkd.db.DiagnosticA11yEventLog(
                    id = 1, ctime = 1, type = 1, appId = "test.app", name = "TestActivity",
                    desc = null, textPreview = text,
                )
                val target = temp.resolve("$name.json")
                DiagnosticBudget().writeJson(target, kotlinx.serialization.json.Json, listOf(dto))
                val output = target.readText()
                assertFalse(output.contains(secret))
                assertTrue(output.contains(if (name == "short") "ready" else "omitted"))
            }
        } finally { temp.deleteRecursively() }
    }

    @Test fun recursiveSnapshotCopyExcludesRecoveryAndSecretsAndIncludesSelectedImage() {
        val temp = Files.createTempDirectory("diagnostic-copy").toFile()
        try {
            val source = temp.resolve("source").apply { mkdir() }
            source.resolve("nested").mkdir()
            source.resolve("nested/layout.json").writeText("""{"apiKey":"hidden","nodes":3}""")
            source.resolve("nested/screen.png").writeBytes(byteArrayOf(1, 2, 3))
            listOf("store.json.corrupt-1", "state.tmp.json", "cookie.txt", "bad.json").forEach {
                source.resolve(it).writeText("do-not-export")
            }
            val target = temp.resolve("target")
            val budget = DiagnosticBudget()
            DiagnosticFiles.copyTree(source, target, allowImages = true, budget = budget)
            assertTrue(target.resolve("nested/layout.json").isFile)
            assertFalse(target.resolve("nested/layout.json").readText().contains("hidden"))
            assertArrayEquals(byteArrayOf(1, 2, 3), target.resolve("nested/screen.png").readBytes())
            assertEquals(4, budget.omittedFiles)
            assertTrue(target.listFiles().orEmpty().all { it.name == "nested" })
        } finally { temp.deleteRecursively() }
    }

    @Test fun snapshotSelectionUsesIdAfterMigrationAndRejectsUnknownFutureAndTemporaryNames() {
        val temp = Files.createTempDirectory("diagnostic-snapshot-range").toFile()
        try {
            val internal = temp.resolve("internal").apply { mkdir() }
            val external = temp.resolve("external").apply { mkdir() }
            val now = 1_800_000_000_000L
            val old = now - 40L * 86_400_000L
            val recent = now - 1_000
            val names = listOf(old.toString(), recent.toString(), (now + 1).toString(), "unknown", ".$recent.tmp", "0$recent")
            names.forEach { name ->
                external.resolve("snapshot/$name/screen.png").apply { parentFile.mkdirs(); writeBytes(byteArrayOf(1)) }
            }
            val root = PrivateStorageMigration.migrate(internal, external)
            root.resolve("snapshot/$old").setLastModified(now)
            val target = temp.resolve("selected")
            DiagnosticFiles.copySnapshots(root.resolve("snapshot"), target, now - 86_400_000L, now, DiagnosticBudget())
            assertEquals(listOf(recent.toString()), target.listFiles().orEmpty().map { it.name })
            val none = temp.resolve("none")
            DiagnosticFiles.copySnapshots(root.resolve("snapshot"), none, null, now, DiagnosticBudget())
            assertFalse(none.exists())
        } finally { temp.deleteRecursively() }
    }

    @Test fun producerStopsAtBudgetBeforeCreatingWholeDocumentAndDoesNotPublishPartialJson() {
        val temp = Files.createTempDirectory("diagnostic-producer-budget").toFile()
        try {
            val budget = DiagnosticBudget(maxTextBytes = 32, maxTotalBytes = 32)
            var produced = 0
            budget.writeProducedText(temp.resolve("too-large.json")) { output ->
                repeat(1_000_000) { output.write('x'.code); produced++ }
            }
            assertEquals(32, produced)
            assertEquals(1, budget.omittedFiles)
            assertFalse(temp.resolve("too-large.json").exists())
            budget.writeText(temp.resolve("valid.txt"), "ok")
            assertEquals("ok", temp.resolve("valid.txt").readText())
        } finally { temp.deleteRecursively() }
    }

    @Test fun jsonPreflightStopsBeforeSerializingFollowingLargeValues() {
        val temp = Files.createTempDirectory("diagnostic-json-budget").toFile()
        try {
            val budget = DiagnosticBudget(maxTextBytes = 128, maxTotalBytes = 128)
            var visited = 0
            val serializer = object : kotlinx.serialization.SerializationStrategy<Unit> {
                override val descriptor = kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()).descriptor
                override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Unit) {
                    val collection = encoder.beginCollection(descriptor, 500)
                    repeat(500) { index ->
                        visited++
                        collection.encodeStringElement(descriptor, index, "x".repeat(256 * 1024))
                    }
                    collection.endStructure(descriptor)
                }
            }
            budget.writeJson(temp.resolve("events.json"), kotlinx.serialization.json.Json, serializer, Unit)
            assertEquals(1, visited)
            assertFalse(temp.resolve("events.json").exists())
            assertEquals(1, budget.omittedFiles)
            assertTrue(budget.summary().contains("1024 characters"))
        } finally { temp.deleteRecursively() }
    }

    @Test fun textAndTotalBudgetsApplyToActualWrittenFiles() {
        val temp = Files.createTempDirectory("diagnostic-budget").toFile()
        try {
            val budget = DiagnosticBudget(maxTextBytes = 8, maxTotalBytes = 10)
            budget.writeText(temp.resolve("one.txt"), "123456")
            budget.writeText(temp.resolve("two.txt"), "abcdef")
            budget.writeText(temp.resolve("large.txt"), "123456789")
            assertEquals("123456", temp.resolve("one.txt").readText())
            assertFalse(temp.resolve("two.txt").exists())
            assertFalse(temp.resolve("large.txt").exists())
            assertEquals(2, budget.omittedFiles)
        } finally { temp.deleteRecursively() }
    }
}
