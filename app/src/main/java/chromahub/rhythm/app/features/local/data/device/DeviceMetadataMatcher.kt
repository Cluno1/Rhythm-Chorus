/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.features.local.data.device

import java.text.Normalizer
import kotlin.math.abs

data class DeviceMatchInput(val title: String, val artist: String, val album: String, val durationMs: Long)

object DeviceMetadataMatcher {
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
        val raw = titleScore * 0.45 + artistScore * 0.30 + albumScore * 0.15 + durationScore * 0.10
        val hasReliableDurations = input.durationMs > 0L && durationSeconds != null && durationSeconds > 20.0
        val severeDurationMismatch = hasReliableDurations && abs(input.durationMs / 1000.0 - durationSeconds) > 15.0
        return (if (severeDurationMismatch) raw * 0.55 else raw).coerceIn(0.0, 1.0)
    }

    private fun similarity(left: String?, right: String?): Double {
        val a = normalize(left)
        val b = normalize(right)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.82
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
}
