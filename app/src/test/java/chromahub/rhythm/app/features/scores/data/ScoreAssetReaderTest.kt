package chromahub.rhythm.app.features.scores.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreAssetReaderTest {
    @Test
    fun `reads all bytes and closes the asset stream`() {
        val expected = "<score-partwise/>".encodeToByteArray()
        val stream = ClosingInputStream(expected)

        val actual = ScoreAssetReader.read { stream }

        assertArrayEquals(expected, actual)
        assertTrue(stream.wasClosed)
    }

    @Test
    fun `rejects an empty asset`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ScoreAssetReader.read { ByteArrayInputStream(byteArrayOf()) }
        }

        assertEquals("Score asset is empty", error.message)
    }

    private class ClosingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var wasClosed = false
            private set

        override fun close() {
            wasClosed = true
            super.close()
        }
    }
}
