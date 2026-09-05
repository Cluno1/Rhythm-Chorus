package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScorePartColorsTest {
    @Test
    fun mergedSaMapsVoicesBackToOriginalParts() {
        assertEquals(
            listOf(0, 1),
            resolveScoreVoicePartIndexes(
                trackName = "S+A",
                trackIndex = 0,
                voiceCount = 2,
                notationLayout = ScoreNotationLayout.MERGED_STAVES
            )
        )
    }

    @Test
    fun mergedTbMapsVoicesBackToOriginalParts() {
        assertEquals(
            listOf(2, 3),
            resolveScoreVoicePartIndexes(
                trackName = "T+B",
                trackIndex = 1,
                voiceCount = 2,
                notationLayout = ScoreNotationLayout.MERGED_STAVES
            )
        )
    }

    @Test
    fun defaultIsBlackAndEnhancedUsesSatbPalette() {
        assertEquals("#000000", scorePartColorHex(1, ScorePartColorMode.DEFAULT))
        assertEquals(
            listOf("#4f6bff", "#ed168c", "#f59f00", "#12b886"),
            (0..3).map { scorePartColorHex(it, ScorePartColorMode.ENHANCED) }
        )
    }
}
