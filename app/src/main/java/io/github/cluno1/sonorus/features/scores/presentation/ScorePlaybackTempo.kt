package io.github.cluno1.sonorus.features.scores.presentation

import kotlin.math.roundToInt

internal const val MIN_SCORE_PLAYBACK_BPM = 30
internal const val MAX_SCORE_PLAYBACK_BPM = 300
private const val FALLBACK_SCORE_PLAYBACK_BPM = 120.0

internal fun normalizedScoreSourceBpm(sourceBpm: Double): Double =
    sourceBpm.takeIf { it.isFinite() && it > 0.0 } ?: FALLBACK_SCORE_PLAYBACK_BPM

internal fun displayedScoreSourceBpm(sourceBpm: Double): Int =
    normalizedScoreSourceBpm(sourceBpm).roundToInt().coerceAtLeast(1)

internal fun scorePlaybackSpeedForTargetBpm(
    sourceBpm: Double,
    targetBpm: Int,
): Double = targetBpm.coerceIn(MIN_SCORE_PLAYBACK_BPM, MAX_SCORE_PLAYBACK_BPM) /
    normalizedScoreSourceBpm(sourceBpm)
