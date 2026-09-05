package chromahub.rhythm.app.features.local.presentation.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackControlStateMachineTest {
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun fastReadyKeepsLoadingForMinimumWindowThenShowsPause() {
        val startedAt = System.nanoTime()
        val machine = machine(minimumMs = 50L)

        machine.beginLoading("song-b", playWhenReady = true)
        machine.update("song-b", PlaybackReadiness.READY, playWhenReady = true)

        assertTrue(machine.state.value.isLoading)
        Thread.sleep(90L)

        assertFalse(machine.state.value.isLoading)
        assertTrue(machine.state.value.showPause)
        assertTrue((System.nanoTime() - startedAt) / 1_000_000L >= 50L)
    }

    @Test
    fun slowBufferingWaitsForRealReadyEvenAfterMinimumWindow() {
        val machine = machine(minimumMs = 30L)

        machine.beginLoading("song-b", playWhenReady = true)
        Thread.sleep(60L)

        assertTrue(machine.state.value.isLoading)
        machine.update("song-b", PlaybackReadiness.READY, playWhenReady = true)

        assertFalse(machine.state.value.isLoading)
        assertTrue(machine.state.value.showPause)
    }

    @Test
    fun newerGenerationCannotBeCompletedByPreviousReadyDelay() {
        val machine = machine(minimumMs = 60L)

        machine.beginLoading("song-a", playWhenReady = true)
        machine.update("song-a", PlaybackReadiness.READY, playWhenReady = true)
        Thread.sleep(15L)
        machine.beginLoading("song-b", playWhenReady = true)
        Thread.sleep(70L)

        assertEquals("song-b", machine.state.value.mediaId)
        assertTrue(machine.state.value.isLoading)
        machine.update("song-a", PlaybackReadiness.READY, playWhenReady = true)
        assertEquals("song-b", machine.state.value.mediaId)
        assertTrue(machine.state.value.isLoading)
    }

    @Test
    fun pauseIntentWhileLoadingEndsAtPlayIconOnceReady() {
        val machine = machine(minimumMs = 30L)

        machine.beginLoading("song-b", playWhenReady = true)
        machine.update("song-b", PlaybackReadiness.BUFFERING, playWhenReady = false)
        machine.update("song-b", PlaybackReadiness.READY, playWhenReady = false)
        Thread.sleep(60L)

        assertFalse(machine.state.value.isLoading)
        assertFalse(machine.state.value.showPause)
    }

    @Test
    fun failureCancelsLoadingAndShowsPlayEvenWhenMedia3IntentIsStale() {
        val machine = machine(minimumMs = 100L)

        machine.beginLoading("song-b", playWhenReady = true)
        machine.update("song-b", PlaybackReadiness.ERROR, playWhenReady = true)

        assertFalse(machine.state.value.isLoading)
        assertFalse(machine.state.value.showPause)
    }

    private fun machine(minimumMs: Long) = PlaybackControlStateMachine(
        scope = scope,
        minimumLoadingDurationMs = minimumMs,
        elapsedRealtimeMs = { System.nanoTime() / 1_000_000L },
    )
}
