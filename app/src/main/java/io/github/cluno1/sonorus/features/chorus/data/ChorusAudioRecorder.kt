package io.github.cluno1.sonorus.features.chorus.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max

data class ChorusRecordingResult(
    val wavFile: File,
    val uploadFile: File,
    val mediaType: String,
    val durationMs: Long,
    val peakSamples: List<Float>,
)

class ChorusAudioRecorder private constructor(
    context: Context,
    private val projectId: String,
    private val timelineId: String,
    existingRoot: File?,
) {
    constructor(context: Context, projectId: String, timelineId: String) :
        this(context, projectId, timelineId, null)

    private val root = (existingRoot ?: File(
        context.filesDir,
        "chorus-drafts/${UUID.randomUUID()}",
    )).apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recording = AtomicBoolean(false)
    private val totalSamples = AtomicLong(0)
    private val clips = mutableListOf<File>()
    private val jobs = mutableListOf<Job>()
    private val collectedPeaks = mutableListOf<Float>()
    private var audioRecord: AudioRecord? = null
    private val _peak = MutableStateFlow(0f)
    val peak: StateFlow<Float> = _peak.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    val hasAudio: Boolean
        get() = totalSamples.get() > 0

    init {
        File(root, PROJECT_MARKER).writeText(projectId)
        File(root, TIMELINE_MARKER).writeText(timelineId)
        root.listFiles { file -> file.name.startsWith("clip-") && file.extension == "pcm" }
            .orEmpty()
            .sortedBy(File::getName)
            .forEach { clip ->
                clips += clip
                totalSamples.addAndGet(clip.length() / BYTES_PER_SAMPLE)
            }
        _durationMs.value = totalSamples.get() * 1000 / SAMPLE_RATE
    }

    fun existingResult(): ChorusRecordingResult? {
        val wav = File(root, "recording.wav").takeIf(File::isFile) ?: return null
        val m4a = File(root, "recording.m4a").takeIf { it.isFile && it.length() > 0 }
        return ChorusRecordingResult(
            wavFile = wav,
            uploadFile = m4a ?: wav,
            mediaType = if (m4a != null) "audio/mp4" else "audio/wav",
            durationMs = totalSamples.get() * 1000 / SAMPLE_RATE,
            peakSamples = emptyList(),
        )
    }

    fun saveTimelineAnchors(anchors: List<Pair<Long, Long>>) {
        File(root, ANCHOR_MARKER).writeText(
            anchors.joinToString("\n") { (tick, mediaMs) -> "$tick,$mediaMs" },
        )
    }

    fun recoveredTimelineAnchors(): List<Pair<Long, Long>> = File(root, ANCHOR_MARKER)
        .takeIf(File::isFile)
        ?.readLines()
        .orEmpty()
        .mapNotNull { line ->
            val values = line.split(',', limit = 2)
            val tick = values.getOrNull(0)?.toLongOrNull()
            val mediaMs = values.getOrNull(1)?.toLongOrNull()
            if (tick != null && tick >= 0 && mediaMs != null && mediaMs >= 0) tick to mediaMs else null
        }

    fun saveTransportOffset(offsetMs: Long) {
        File(root, OFFSET_MARKER).writeText(offsetMs.toString())
    }

    fun recoveredTransportOffset(): Long = File(root, OFFSET_MARKER)
        .takeIf(File::isFile)
        ?.readText()
        ?.trim()
        ?.toLongOrNull()
        ?: 0L

    @SuppressLint("MissingPermission")
    fun startSegment() {
        check(!recording.get()) { "a recording segment is already active" }
        check(root.usableSpace >= MIN_FREE_SPACE_BYTES) { "录音空间不足，请先释放存储空间" }
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimum > 0) { "this device cannot record 48 kHz mono PCM" }
        val bufferSize = max(minimum, SAMPLE_RATE / 5 * BYTES_PER_SAMPLE)
        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .build()
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "microphone initialization failed" }
        val clip = File(root, "clip-${clips.size.toString().padStart(3, '0')}.pcm")
        clips += clip
        audioRecord = recorder
        recording.set(true)
        recorder.startRecording()
        jobs += scope.launch {
            FileOutputStream(clip).buffered(bufferSize * 2).use { output ->
                val buffer = ByteArray(bufferSize)
                while (isActive && recording.get()) {
                    val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (read <= 0) continue
                    output.write(buffer, 0, read)
                    val sampleCount = read / BYTES_PER_SAMPLE
                    totalSamples.addAndGet(sampleCount.toLong())
                    _durationMs.value = totalSamples.get() * 1000 / SAMPLE_RATE
                    var segmentPeak = 0
                    var index = 0
                    while (index + 1 < read) {
                        val sample = (buffer[index].toInt() and 0xff) or (buffer[index + 1].toInt() shl 8)
                        segmentPeak = max(segmentPeak, abs(sample.toShort().toInt()))
                        index += 2
                    }
                    val normalized = (segmentPeak / 32768f).coerceIn(0f, 1f)
                    _peak.value = normalized
                    if (collectedPeaks.size < MAX_PEAKS) {
                        collectedPeaks += normalized
                    } else {
                        val target = ((_durationMs.value / 50) % MAX_PEAKS).toInt()
                        collectedPeaks[target] = max(collectedPeaks[target], normalized)
                    }
                }
            }
        }
    }

    suspend fun pause() {
        val (recorder, job) = stopCaptureImmediately() ?: return
        job?.join()
        recorder.release()
        _peak.value = 0f
    }

    fun pauseSafely() {
        val (recorder, job) = stopCaptureImmediately() ?: return
        scope.launch {
            job?.join()
            recorder.release()
            _peak.value = 0f
        }
    }

    private fun stopCaptureImmediately(): Pair<AudioRecord, Job?>? {
        if (!recording.compareAndSet(true, false)) return null
        val recorder = audioRecord ?: return null
        audioRecord = null
        runCatching { recorder.stop() }
        return recorder to jobs.lastOrNull()
    }

    suspend fun finish(): ChorusRecordingResult = withContext(Dispatchers.IO) {
        pause()
        jobs.joinAll()
        check(totalSamples.get() > 0) { "recording is empty" }
        val wav = File(root, "recording.wav")
        writeWav(clips, wav, totalSamples.get())
        val m4a = File(root, "recording.m4a")
        val encoded = runCatching { encodeWavToAac(wav, m4a) }.getOrNull()
        ChorusRecordingResult(
            wavFile = wav,
            uploadFile = encoded ?: wav,
            mediaType = if (encoded != null) "audio/mp4" else "audio/wav",
            durationMs = totalSamples.get() * 1000 / SAMPLE_RATE,
            peakSamples = collectedPeaks.toList(),
        )
    }

    suspend fun exportTrimmed(
        result: ChorusRecordingResult,
        startMs: Long,
        endMs: Long,
    ): ChorusRecordingResult = withContext(Dispatchers.IO) {
        val normalizedStart = startMs.coerceIn(0L, result.durationMs)
        val normalizedEnd = endMs.coerceIn(normalizedStart, result.durationMs)
        if (normalizedStart == 0L && normalizedEnd == result.durationMs) {
            return@withContext result
        }
        val wav = File(root, "recording-trimmed.wav")
        val trim = ChorusPcmEditor.trimWav(
            source = result.wavFile,
            destination = wav,
            startMs = normalizedStart,
            endMs = normalizedEnd,
        )
        val m4a = File(root, "recording-trimmed.m4a")
        val encoded = runCatching { encodeWavToAac(wav, m4a) }.getOrNull()
        ChorusRecordingResult(
            wavFile = wav,
            uploadFile = encoded ?: wav,
            mediaType = if (encoded != null) "audio/mp4" else "audio/wav",
            durationMs = trim.durationMs,
            peakSamples = trim.peakSamples,
        )
    }

    suspend fun discard() = withContext(Dispatchers.IO) {
        pause()
        jobs.joinAll()
        root.deleteRecursively()
    }

    private fun writeWav(clips: List<File>, destination: File, samples: Long) {
        val dataSize = samples * BYTES_PER_SAMPLE
        FileOutputStream(destination).buffered().use { output ->
            output.write(wavHeader(dataSize))
            clips.forEach { clip ->
                val clipSamples = clip.length() / BYTES_PER_SAMPLE
                var sampleOffset = 0L
                FileInputStream(clip).buffered().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        var byteIndex = 0
                        while (byteIndex + 1 < read) {
                            val position = sampleOffset + byteIndex / BYTES_PER_SAMPLE
                            val edge = minOf(position + 1, clipSamples - position, FADE_SAMPLES.toLong())
                            if (edge < FADE_SAMPLES) {
                                val raw = ((buffer[byteIndex].toInt() and 0xff) or
                                    (buffer[byteIndex + 1].toInt() shl 8)).toShort()
                                val faded = (raw * (edge.toFloat() / FADE_SAMPLES)).toInt().toShort()
                                buffer[byteIndex] = (faded.toInt() and 0xff).toByte()
                                buffer[byteIndex + 1] = (faded.toInt() shr 8).toByte()
                            }
                            byteIndex += BYTES_PER_SAMPLE
                        }
                        output.write(buffer, 0, read)
                        sampleOffset += read / BYTES_PER_SAMPLE
                    }
                }
            }
        }
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

    private fun encodeWavToAac(source: File, destination: File): File {
        destination.delete()
        val codec = MediaCodec.createEncoderByType(AAC_MIME)
        val format = MediaFormat.createAudioFormat(AAC_MIME, SAMPLE_RATE, CHANNEL_COUNT).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AAC_BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AAC_INPUT_SIZE)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val muxer = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var muxerTrack = -1
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var samplesSubmitted = 0L
        try {
            codec.start()
            RandomAccessFile(source, "r").use { input ->
                input.seek(WAV_HEADER_SIZE.toLong())
                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val buffer = checkNotNull(codec.getInputBuffer(inputIndex)).apply { clear() }
                            val bytes = ByteArray(minOf(buffer.remaining(), AAC_INPUT_SIZE))
                            val read = input.read(bytes)
                            if (read < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    samplesSubmitted * 1_000_000 / SAMPLE_RATE,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                buffer.put(bytes, 0, read)
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    read,
                                    samplesSubmitted * 1_000_000 / SAMPLE_RATE,
                                    0,
                                )
                                samplesSubmitted += read / BYTES_PER_SAMPLE
                            }
                        }
                    }
                    when (val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "AAC output format changed twice" }
                            muxerTrack = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outputIndex >= 0) {
                            val output = checkNotNull(codec.getOutputBuffer(outputIndex))
                            if (info.size > 0) {
                                check(muxerStarted) { "AAC samples arrived before output format" }
                                output.position(info.offset)
                                output.limit(info.offset + info.size)
                                muxer.writeSampleData(muxerTrack, output, info)
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxerStarted) runCatching { muxer.stop() }
            muxer.release()
        }
        check(destination.isFile && destination.length() > 0) { "AAC encoder produced no output" }
        return destination
    }

    companion object {
        private const val PROJECT_MARKER = "project-id.txt"
        private const val TIMELINE_MARKER = "timeline-id.txt"
        private const val ANCHOR_MARKER = "timeline-anchors.csv"
        private const val OFFSET_MARKER = "transport-offset-ms.txt"
        private const val SAMPLE_RATE = 48_000
        private const val CHANNEL_COUNT = 1
        private const val BYTES_PER_SAMPLE = 2
        private const val WAV_HEADER_SIZE = 44
        private const val AAC_MIME = "audio/mp4a-latm"
        private const val AAC_BIT_RATE = 128_000
        private const val AAC_INPUT_SIZE = 16 * 1024
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val MAX_PEAKS = 400
        private const val FADE_SAMPLES = 240
        private const val MIN_FREE_SPACE_BYTES = 32L * 1024 * 1024

        fun recover(
            context: Context,
            projectId: String,
            timelineId: String,
            allowLegacyProjectDraft: Boolean = false,
        ): ChorusAudioRecorder? {
            val root = File(context.filesDir, "chorus-drafts")
            val draft = root.listFiles(File::isDirectory).orEmpty()
                .filter { directory ->
                    val storedTimeline = File(directory, TIMELINE_MARKER)
                        .takeIf(File::isFile)
                        ?.readText()
                    File(directory, PROJECT_MARKER).takeIf(File::isFile)?.readText() == projectId &&
                        (storedTimeline == timelineId ||
                            storedTimeline == null && allowLegacyProjectDraft) &&
                        directory.listFiles { file -> file.extension == "pcm" }.orEmpty().isNotEmpty()
                }
                .maxByOrNull(File::lastModified)
                ?: return null
            return ChorusAudioRecorder(context, projectId, timelineId, draft)
        }
    }
}
