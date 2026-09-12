package io.github.cluno1.sonorus.features.chorus.data

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class ChorusTrimResult(
    val durationMs: Long,
    val peakSamples: List<Float>,
)

/** Non-destructive editor for the recorder's 48 kHz mono 16-bit PCM WAV master. */
internal object ChorusPcmEditor {
    const val MIN_TRIM_DURATION_MS = 500L

    private const val SAMPLE_RATE = 48_000
    private const val CHANNEL_COUNT = 1
    private const val BYTES_PER_SAMPLE = 2
    private const val WAV_HEADER_SIZE = 44
    private const val MAX_PEAKS = 400
    private const val FADE_SAMPLES = 240

    fun trimWav(
        source: File,
        destination: File,
        startMs: Long,
        endMs: Long,
    ): ChorusTrimResult {
        require(source.isFile) { "recording WAV is missing" }
        val header = ByteArray(WAV_HEADER_SIZE)
        RandomAccessFile(source, "r").use { input -> input.readFully(header) }
        require(header.copyOfRange(0, 4).contentEquals("RIFF".toByteArray())) {
            "recording is not a RIFF WAV"
        }
        require(header.copyOfRange(8, 12).contentEquals("WAVE".toByteArray())) {
            "recording is not a WAV file"
        }
        val format = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        require(format.getShort(22).toInt() == CHANNEL_COUNT) {
            "recording must be mono"
        }
        require(format.getInt(24) == SAMPLE_RATE && format.getShort(34).toInt() == 16) {
            "recording must be 48 kHz 16-bit PCM"
        }

        val declaredDataBytes = format.getInt(40).toLong() and 0xffff_ffffL
        val availableDataBytes = min(declaredDataBytes, source.length() - WAV_HEADER_SIZE)
            .coerceAtLeast(0L)
        val availableSamples = availableDataBytes / BYTES_PER_SAMPLE
        val startSample = (startMs.coerceAtLeast(0L) * SAMPLE_RATE / 1_000)
            .coerceAtMost(availableSamples)
        val endSample = (endMs.coerceAtLeast(0L) * SAMPLE_RATE / 1_000)
            .coerceAtMost(availableSamples)
        val sampleCount = endSample - startSample
        require(sampleCount >= SAMPLE_RATE * MIN_TRIM_DURATION_MS / 1_000) {
            "保留的录音不能短于 0.5 秒"
        }

        destination.parentFile?.mkdirs()
        destination.delete()
        val actualDurationMs = sampleCount * 1_000 / SAMPLE_RATE
        val peakCount = min(MAX_PEAKS, max(1, (actualDurationMs / 50).toInt()))
        val peaks = IntArray(peakCount)
        RandomAccessFile(source, "r").use { input ->
            input.seek(WAV_HEADER_SIZE + startSample * BYTES_PER_SAMPLE)
            FileOutputStream(destination).buffered().use { output ->
                output.write(wavHeader(sampleCount * BYTES_PER_SAMPLE))
                val buffer = ByteArray(16 * 1024)
                var copiedSamples = 0L
                while (copiedSamples < sampleCount) {
                    val remainingBytes = (sampleCount - copiedSamples) * BYTES_PER_SAMPLE
                    val requested = min(buffer.size.toLong(), remainingBytes).toInt()
                    val read = input.read(buffer, 0, requested)
                    check(read > 0) { "recording WAV ended before the selected range" }
                    var byteIndex = 0
                    while (byteIndex + 1 < read) {
                        val position = copiedSamples + byteIndex / BYTES_PER_SAMPLE
                        val raw = ((buffer[byteIndex].toInt() and 0xff) or
                            (buffer[byteIndex + 1].toInt() shl 8)).toShort().toInt()
                        val edge = minOf(position + 1, sampleCount - position, FADE_SAMPLES.toLong())
                        val edited = if (edge < FADE_SAMPLES) {
                            (raw * (edge.toFloat() / FADE_SAMPLES)).toInt()
                        } else {
                            raw
                        }.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        buffer[byteIndex] = (edited and 0xff).toByte()
                        buffer[byteIndex + 1] = (edited shr 8).toByte()
                        val peakIndex = min(
                            peakCount - 1,
                            (position * peakCount / sampleCount).toInt(),
                        )
                        peaks[peakIndex] = max(peaks[peakIndex], abs(edited))
                        byteIndex += BYTES_PER_SAMPLE
                    }
                    output.write(buffer, 0, read)
                    copiedSamples += read / BYTES_PER_SAMPLE
                }
            }
        }
        check(destination.length() == WAV_HEADER_SIZE + sampleCount * BYTES_PER_SAMPLE) {
            "trimmed recording has an unexpected size"
        }
        return ChorusTrimResult(
            durationMs = actualDurationMs,
            peakSamples = peaks.map { (it / 32768f).coerceIn(0f, 1f) },
        )
    }

    private fun wavHeader(dataSize: Long): ByteArray = ByteBuffer.allocate(WAV_HEADER_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".toByteArray())
            putInt((36 + dataSize).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(CHANNEL_COUNT.toShort())
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * CHANNEL_COUNT * BYTES_PER_SAMPLE)
            putShort((CHANNEL_COUNT * BYTES_PER_SAMPLE).toShort())
            putShort((BYTES_PER_SAMPLE * 8).toShort())
            put("data".toByteArray())
            putInt(dataSize.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
        .array()
}
