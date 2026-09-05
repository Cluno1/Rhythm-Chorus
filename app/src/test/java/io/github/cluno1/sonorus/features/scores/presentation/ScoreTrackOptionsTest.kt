package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreTrackOptionsTest {
    @Test
    fun `maps four generic midi parts to satb labels`() {
        val options = buildScoreTrackOptions(
            listOf(
                "SmartMusic\u00A0SoftSynth (Pno)",
                "SmartMusic SoftSynth (Pno)",
                "SmartMusic SoftSynth (Pno)",
                "SmartMusic SoftSynth (Pno)"
            )
        )

        assertEquals(listOf("S", "A", "T", "B"), options.map { it.label })
        assertEquals(listOf(0, 1, 2, 3), options.map { it.index })
    }

    @Test
    fun `keeps explicit part names`() {
        val options = buildScoreTrackOptions(listOf("Soprano", "Alto", "Tenor", "Bass"))

        assertEquals(
            listOf("Soprano", "Alto", "Tenor", "Bass"),
            options.map { it.label }
        )
    }

    @Test
    fun `uses stable numeric labels for generic non satb scores`() {
        val options = buildScoreTrackOptions(
            listOf("[Staff 1]", "[Staff 1]", "[Staff 2]", "[Staff 2]", "[Staff 4]", "[Staff 4]")
        )

        assertEquals(listOf("1", "2", "3", "4", "5", "6"), options.map { it.label })
    }

    @Test
    fun `allows multiple visible parts while keeping at least one selected`() {
        val sopranoOnly = 1 shl 0
        val sopranoAndBass = toggleScoreTrackSelectionMask(
            selectedMask = sopranoOnly,
            trackIndex = 3,
            trackCount = 4
        )

        assertEquals(setOf(0, 3), sopranoAndBass.selectedIndexes(trackCount = 4))
        assertEquals(
            sopranoOnly,
            toggleScoreTrackSelectionMask(
                selectedMask = sopranoAndBass xor (1 shl 3),
                trackIndex = 0,
                trackCount = 4
            )
        )
        assertEquals(1, normalizeScoreTrackSelectionMask(selectedMask = 0, trackCount = 4))
    }

    private fun Int.selectedIndexes(trackCount: Int): Set<Int> =
        (0 until trackCount).filterTo(mutableSetOf()) { index ->
            this and (1 shl index) != 0
        }
}
