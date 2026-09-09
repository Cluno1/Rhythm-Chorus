package io.github.cluno1.sonorus.shared.presentation.components.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmIconsTest {
    @Test
    fun lyricsIconUsesFilledVariantOnlyWhileLyricsAreVisible() {
        val inactive = RhythmIcons.Player.lyrics(isActive = false)
        val active = RhythmIcons.Player.lyrics(isActive = true)

        assertEquals("lyrics", inactive.name)
        assertEquals("lyrics", active.name)
        assertFalse(inactive.filled)
        assertTrue(active.filled)
    }

    @Test
    fun scoreIconUsesAudioFileInsteadOfScoreboardSymbol() {
        assertEquals("audio_file", RhythmIcons.Score.name)
        assertFalse(RhythmIcons.Score.filled)
    }
}
