package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScorePlaybackTempoTest {
    @Test
    fun targetBpmIsConvertedToAnExactPlaybackRatio() {
        assertEquals(
            75.0 / 110.0,
            scorePlaybackSpeedForTargetBpm(sourceBpm = 110.0, targetBpm = 75),
            0.000_001,
        )
    }

    @Test
    fun invalidSourceTempoFallsBackToStandardBpm() {
        assertEquals(120.0, normalizedScoreSourceBpm(0.0), 0.0)
        assertEquals(120.0, normalizedScoreSourceBpm(Double.NaN), 0.0)
        assertEquals(120, displayedScoreSourceBpm(Double.POSITIVE_INFINITY))
    }

    @Test
    fun customBpmIsClampedToTheSupportedInputRange() {
        assertEquals(0.25, scorePlaybackSpeedForTargetBpm(120.0, 1), 0.0)
        assertEquals(2.5, scorePlaybackSpeedForTargetBpm(120.0, 999), 0.0)
    }
}
