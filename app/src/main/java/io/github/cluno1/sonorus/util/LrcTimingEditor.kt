package io.github.cluno1.sonorus.util

import java.util.Locale
import kotlin.math.roundToLong

data class LrcStampResult(
    val text: String,
    val stampedLineIndex: Int,
    val nextLineIndex: Int?,
)

data class LrcTimingTarget(
    val lineIndex: Int,
    val ordinal: Int,
    val total: Int,
    val text: String,
)

object LrcTimingEditor {
    private val timestamp = Regex("^\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?](.*)$")

    fun generateTemplate(text: String, durationMs: Long?, estimateFromDuration: Boolean): String {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val editable = lines.indices.filter { stripTimestamp(lines[it]).isNotBlank() }
        if (editable.isEmpty()) return text
        val duration = durationMs?.coerceAtLeast(0L) ?: 0L
        val denominator = (editable.size - 1).coerceAtLeast(1)
        val positionByLine = editable.withIndex().associate { (order, lineIndex) ->
            val position = if (estimateFromDuration && duration > 0L) {
                (duration.toDouble() * order / denominator).roundToLong().coerceAtMost(duration)
            } else {
                0L
            }
            lineIndex to position
        }
        return lines.mapIndexed { index, line ->
            val content = stripTimestamp(line)
            positionByLine[index]?.let { "${formatTimestamp(it)}$content" } ?: line
        }.joinToString("\n")
    }

    fun stampLine(
        text: String,
        requestedLineIndex: Int,
        playbackPositionMs: Long,
        durationMs: Long? = null,
    ): LrcStampResult? {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
        val editable = lines.indices.filter { stripTimestamp(lines[it]).isNotBlank() }
        if (editable.isEmpty()) return null
        val target = editable.firstOrNull { it >= requestedLineIndex } ?: editable.last()
        val previousTime = editable.asSequence()
            .filter { it < target }
            .mapNotNull { parseTimestampMs(lines[it]) }
            .lastOrNull()
            ?: 0L
        val max = durationMs?.takeIf { it > 0L } ?: Long.MAX_VALUE
        val position = playbackPositionMs.coerceAtLeast(previousTime).coerceAtMost(max)
        lines[target] = "${formatTimestamp(position)}${stripTimestamp(lines[target])}"
        return LrcStampResult(
            text = lines.joinToString("\n"),
            stampedLineIndex = target,
            nextLineIndex = editable.firstOrNull { it > target },
        )
    }

    fun previousEditableLine(text: String, lineIndex: Int): Int? =
        text.lines().indices.lastOrNull { it < lineIndex && stripTimestamp(text.lines()[it]).isNotBlank() }

    fun timingTarget(text: String, requestedLineIndex: Int): LrcTimingTarget? {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val editable = lines.indices.filter { stripTimestamp(lines[it]).isNotBlank() }
        if (editable.isEmpty()) return null
        val lineIndex = editable.firstOrNull { it >= requestedLineIndex } ?: editable.last()
        return LrcTimingTarget(
            lineIndex = lineIndex,
            ordinal = editable.indexOf(lineIndex) + 1,
            total = editable.size,
            text = stripTimestamp(lines[lineIndex]),
        )
    }

    fun loopEnd(
        startMs: Long,
        requestedEndMs: Long,
        durationMs: Long?,
        minimumDurationMs: Long = 500L,
    ): Long? {
        val maximum = durationMs?.takeIf { it > 0L } ?: Long.MAX_VALUE
        val minimumEnd = startMs.coerceAtLeast(0L) + minimumDurationMs.coerceAtLeast(1L)
        if (minimumEnd > maximum) return null
        return requestedEndMs.coerceAtLeast(minimumEnd).coerceAtMost(maximum)
    }

    fun lineStartOffset(text: String, lineIndex: Int): Int = text.lineSequence()
        .take(lineIndex.coerceAtLeast(0))
        .sumOf { it.length + 1 }
        .coerceAtMost(text.length)

    fun lineIndexAtOffset(text: String, offset: Int): Int =
        text.take(offset.coerceIn(0, text.length)).count { it == '\n' }

    fun formatTimestamp(positionMs: Long): String {
        val safe = positionMs.coerceAtLeast(0L)
        val minutes = safe / 60_000
        val seconds = (safe % 60_000) / 1_000
        val millis = safe % 1_000
        return String.format(Locale.ROOT, "[%02d:%02d.%03d]", minutes, seconds, millis)
    }

    private fun stripTimestamp(line: String): String = timestamp.matchEntire(line)?.groupValues?.get(4) ?: line

    private fun parseTimestampMs(line: String): Long? = timestamp.matchEntire(line)?.let { match ->
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1_000 + fraction
    }
}
