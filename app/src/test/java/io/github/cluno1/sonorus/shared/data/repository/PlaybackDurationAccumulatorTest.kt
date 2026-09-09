package io.github.cluno1.sonorus.shared.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackDurationAccumulatorTest {
    @Test
    fun `only running intervals are accumulated`() {
        val tracker = PlaybackDurationAccumulator()

        tracker.resume(1_000)
        tracker.pause(4_000)
        tracker.resume(10_000)

        assertEquals(5_000, tracker.elapsedMs(12_000))
    }

    @Test
    fun `duplicate lifecycle callbacks are idempotent`() {
        val tracker = PlaybackDurationAccumulator()

        tracker.resume(1_000)
        tracker.resume(2_000)
        tracker.pause(4_000)
        tracker.pause(8_000)

        assertEquals(3_000, tracker.consume(10_000))
        assertEquals(0, tracker.consume(11_000))
        assertFalse(tracker.isRunning)
    }

    @Test
    fun `consume closes an active interval and resets tracker`() {
        val tracker = PlaybackDurationAccumulator()

        tracker.resume(5_000)
        assertTrue(tracker.isRunning)
        assertEquals(4_000, tracker.consume(9_000))
        assertEquals(0, tracker.elapsedMs(10_000))
    }
}
