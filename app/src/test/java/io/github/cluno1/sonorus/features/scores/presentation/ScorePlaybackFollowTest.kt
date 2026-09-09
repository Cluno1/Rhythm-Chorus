package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScorePlaybackFollowTest {
    @Test
    fun scrollOffsetKeepsCurrentSystemInUpperViewport() {
        assertEquals(
            -288.0,
            scoreFollowScrollOffset(viewportHeightPx = 1600, density = 2f),
            0.0001,
        )
    }

    @Test
    fun invalidViewportDoesNotCreateAnOffset() {
        assertEquals(0.0, scoreFollowScrollOffset(viewportHeightPx = 0, density = 2f), 0.0)
        assertEquals(0.0, scoreFollowScrollOffset(viewportHeightPx = 1600, density = 0f), 0.0)
    }
}
