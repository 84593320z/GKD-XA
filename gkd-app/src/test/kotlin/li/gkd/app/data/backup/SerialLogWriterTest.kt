package li.gkd.app.data.backup

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialLogWriterTest {
    @Test
    fun executeIsAsynchronousAndFlushWaitsForAllEarlierWrites() {
        val writer = SerialLogWriter()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val values = mutableListOf<Int>()
        try {
            writer.execute { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)); values += 1 }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            writer.execute { values += 2 }
            assertFalse(writer.flush(10))
            release.countDown()
            assertTrue(writer.flush(5_000))
            assertEquals(listOf(1, 2), values)
        } finally {
            release.countDown()
            writer.close()
        }
    }

    @Test
    fun recoveredWriterDrainsAndReportsLostHistoryWithoutBlockingExport() {
        val writer = SerialLogWriter()
        val completed = CountDownLatch(1)
        try {
            writer.execute { error("disk failed") }
            writer.execute { completed.countDown() }
            assertTrue(completed.await(5, TimeUnit.SECONDS))
            val status = writer.flushStatus(5_000)
            assertTrue(status.drained)
            assertEquals(1L, status.failedWrites)
            assertTrue(status.diagnosticWarning()!!.contains("1 log writes failed"))
            assertTrue(writer.flush(5_000))
            assertEquals(1L, writer.flushStatus(5_000).failedWrites)
        } finally { writer.close() }
    }
}
