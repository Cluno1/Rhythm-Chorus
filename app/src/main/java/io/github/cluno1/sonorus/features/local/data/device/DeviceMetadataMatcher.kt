/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.data.device

import java.text.Normalizer
import kotlin.math.abs

data class DeviceMatchInput(val title: String, val artist: String, val album: String, val durationMs: Long)

object DeviceMetadataMatcher {
    internal fun similarityForTesting(left: String?, right: String?): Double = similarity(left, right)

    fun isAutomaticMatch(best: Double, runnerUp: Double?, minimum: Double = 0.72, margin: Double = 0.03): Boolean =
        best >= minimum && (runnerUp == null || best - runnerUp >= margin)

    fun score(input: DeviceMatchInput, title: String?, artist: String?, album: String?, durationSeconds: Double?): Double {
        val titleScore = similarity(input.title, title)
        val artistScore = similarity(input.artist, artist)
        val albumScore = if (input.album.isBlank() || album.isNullOrBlank()) 0.5 else similarity(input.album, album)
        val durationScore = durationSeconds?.let {
            val delta = abs(input.durationMs / 1000.0 - it)
            when {
                delta <= 2.0 -> 1.0
                delta <= 5.0 -> 0.8
                delta <= 10.0 -> 0.45
                else -> 0.0
            }
        } ?: 0.5
        val raw = titleScore * 0.35 + artistScore * 0.25 + albumScore * 0.20 + durationScore * 0.20
        val hasReliableDurations = input.durationMs > 0L && durationSeconds != null && durationSeconds > 20.0
        val severeDurationMismatch = hasReliableDurations && abs(input.durationMs / 1000.0 - durationSeconds) > 15.0
        val versionConflict = isLive(input.title, input.album) != isLive(title, album)
        val penalized = when {
            severeDurationMismatch -> raw * 0.55
            versionConflict -> raw - 0.20
            else -> raw
        }
        return penalized.coerceIn(0.0, 1.0)
    }

    private fun similarity(left: String?, right: String?): Double {
        val a = normalize(left)
        val b = normalize(right)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.82
        val alignedCjk = alignedCjkVariantSimilarity(a.replace(" ", ""), b.replace(" ", ""))
        if (alignedCjk != null) return alignedCjk
        val charSimilarity = levenshteinSimilarity(a.replace(" ", ""), b.replace(" ", ""))
        if (charSimilarity >= 0.66) return charSimilarity
        val at = a.split(' ').filter(String::isNotBlank).toSet()
        val bt = b.split(' ').filter(String::isNotBlank).toSet()
        if (at.isEmpty() || bt.isEmpty()) return 0.0
        return at.intersect(bt).size.toDouble() / at.union(bt).size
    }

    private fun normalize(value: String?): String = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private fun isLive(title: String?, album: String?): Boolean = sequenceOf(title, album)
        .filterNotNull()
        .map { Normalizer.normalize(it, Normalizer.Form.NFKC).lowercase() }
        .any { it.contains("live") || it.contains("演唱会") || it.contains("演唱會") }

    private fun levenshteinSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            current.copyInto(previous)
        }
        return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
    }

    /**
     * Simplified/traditional variants often differ at aligned Han characters only. This keeps
     * those candidates comparable without shipping a large transliteration dictionary.
     */
    private fun alignedCjkVariantSimilarity(a: String, b: String): Double? {
        if (a.length < 3 || a.length != b.length) return null
        var exact = 0
        var weighted = 0.0
        var cjkDifferences = 0
        for (index in a.indices) {
            when {
                a[index] == b[index] -> {
                    exact++
                    weighted += 1.0
                }
                a[index].isCjk() && b[index].isCjk() -> {
                    cjkDifferences++
                    weighted += 0.65
                }
            }
        }
        val minimumExactRatio = if (a.length <= 4) 1.0 / 3.0 else 0.55
        if (cjkDifferences == 0 || exact.toDouble() / a.length < minimumExactRatio) return null
        return weighted / a.length
    }

    private fun Char.isCjk(): Boolean = code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF
}
