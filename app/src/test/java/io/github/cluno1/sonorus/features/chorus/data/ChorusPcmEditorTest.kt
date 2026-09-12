package io.github.cluno1.sonorus.features.chorus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChorusPcmEditorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun trimsRequestedRangeWithoutChangingSource() {
        val source = temporaryFolder.newFile("source.wav")
        writeTestWav(source, durationMs = 2_000)
        val originalSize = source.length()
        val destination = temporaryFolder.newFile("trimmed.wav")

        val result = ChorusPcmEditor.trimWav(source, destination, 500, 1_500)

        assertEquals(1_000L, result.durationMs)
        assertEquals(44L + 48_000L * 2, destination.length())
        assertEquals(originalSize, source.length())
        assertTrue(result.peakSamples.isNotEmpty())
        assertTrue(result.peakSamples.max() > 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSelectionsShorterThanHalfASecond() {
        val source = temporaryFolder.newFile("source.wav")
        writeTestWav(source, durationMs = 1_000)

        ChorusPcmEditor.trimWav(
            source,
            temporaryFolder.newFile("trimmed.wav"),
            100,
            400,
        )
    }

    private fun writeTestWav(destination: File, durationMs: Long) {
        val samples = 48_000L * durationMs / 1_000
        val dataSize = samples * 2
        FileOutputStream(destination).buffered().use { output ->
            output.write(
                ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                    put("RIFF".toByteArray())
                    putInt((36 + dataSize).toInt())
                    put("WAVE".toByteArray())
                    put("fmt ".toByteArray())
                    putInt(16)
                    putShort(1.toShort())
                    putShort(1.toShort())
                    putInt(48_000)
                    putInt(96_000)
                    putShort(2.toShort())
                    putShort(16.toShort())
                    put("data".toByteArray())
                    putInt(dataSize.toInt())
                }.array(),
            )
            val sample = byteArrayOf(0x20, 0x4e)
            repeat(samples.toInt()) { output.write(sample) }
        }
    }
}
