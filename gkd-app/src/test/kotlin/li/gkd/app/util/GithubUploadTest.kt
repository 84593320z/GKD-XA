package li.gkd.app.util

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GithubUploadTest {
    @Test
    fun uploadValidationRejectsOversizedArchiveBeforeNetwork() {
        val file = Files.createTempFile("gkd-upload", ".zip").toFile()
        try {
            java.io.RandomAccessFile(file, "rw").use { it.setLength(Github.MAX_UPLOAD_BYTES + 1) }
            assertThrows(IllegalArgumentException::class.java) {
                validateGithubUploadFile(file)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun uploadValidationReturnsKnownSizeForRegularArchive() {
        val file = Files.createTempFile("gkd-upload", ".zip").toFile()
        try {
            file.writeBytes(byteArrayOf(1, 2, 3))
            assertEquals(3L, validateGithubUploadFile(file))
        } finally {
            file.delete()
        }
    }
}
