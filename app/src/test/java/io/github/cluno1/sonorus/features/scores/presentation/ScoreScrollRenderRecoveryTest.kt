package io.github.cluno1.sonorus.features.scores.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreScrollRenderRecoveryTest {
    @Test
    fun recoveryOnlyRunsAfterTheViewportIsStableForAFrame() {
        val detector = ScoreScrollSettleDetector()

        assertFalse(detector.observe(100))
        assertFalse(detector.observe(240))
        assertFalse(detector.observe(410))
        assertTrue(detector.observe(410))

        // A completed observation is consumed so a new scroll needs its own stable frame.
        assertFalse(detector.observe(410))
    }

    @Test
    fun resetRequiresASecondStableObservation() {
        val detector = ScoreScrollSettleDetector()
        assertFalse(detector.observe(300))

        detector.reset()

        assertFalse(detector.observe(300))
        assertTrue(detector.observe(300))
    }
}
