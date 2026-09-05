package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorePlaybackCompletionTrackerTest {
    @Test
    fun naturalCompletionIsConsumedOnlyOnce() {
        val tracker = ScorePlaybackCompletionTracker()

        tracker.markFinished()

        assertTrue(tracker.consumeFinished())
        assertFalse(tracker.consumeFinished())
    }

    @Test
    fun resetClearsNaturalCompletion() {
        val tracker = ScorePlaybackCompletionTracker()

        tracker.markFinished()
        tracker.reset()

        assertFalse(tracker.consumeFinished())
    }
}
