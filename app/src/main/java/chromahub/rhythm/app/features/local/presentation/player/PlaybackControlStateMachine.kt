package chromahub.rhythm.app.features.local.presentation.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The single, atomic state consumed by every play/pause control surface.
 *
 * [showPause] represents playback intent rather than ExoPlayer's transient `isPlaying` value.
 * During a track change Media3 briefly reports `isPlaying = false`; keeping that detail out of
 * the UI state prevents the play icon from flashing between buffering and resumed playback.
 */
data class PlaybackControlUiState(
    val mediaId: String? = null,
    val isLoading: Boolean = false,
    val showPause: Boolean = false,
)

enum class PlaybackReadiness {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR,
}

/**
 * Serializes Media3 readiness and playback intent into [PlaybackControlUiState].
 *
 * Each media transition starts a new generation. Delayed completion checks both generation and
 * media id, so a previous track can never finish the current track's loading state.
 */
class PlaybackControlStateMachine(
    private val scope: CoroutineScope,
    private val minimumLoadingDurationMs: Long = 400L,
    private val elapsedRealtimeMs: () -> Long,
) {
    private val lock = Any()
    private val _state = MutableStateFlow(PlaybackControlUiState())
    val state: StateFlow<PlaybackControlUiState> = _state.asStateFlow()

    private var generation = 0L
    private var loadingStartedAtMs = 0L
    private var readyCompletionJob: Job? = null
    private var latestReadiness = PlaybackReadiness.IDLE
    private var latestPlayWhenReady = false

    /** Starts (or replaces) the loading window and returns its generation token. */
    fun beginLoading(mediaId: String?, playWhenReady: Boolean): Long = synchronized(lock) {
        generation += 1L
        readyCompletionJob?.cancel()
        readyCompletionJob = null
        loadingStartedAtMs = elapsedRealtimeMs()
        latestReadiness = PlaybackReadiness.BUFFERING
        latestPlayWhenReady = playWhenReady
        _state.value = PlaybackControlUiState(
            mediaId = mediaId,
            isLoading = true,
            showPause = playWhenReady,
        )
        generation
    }

    /**
     * Applies one atomic player snapshot. A different media id is ignored while a generation is
     * active; legitimate changes first call [beginLoading] from `onMediaItemTransition` or the
     * explicit skip command.
     */
    fun update(
        mediaId: String?,
        readiness: PlaybackReadiness,
        playWhenReady: Boolean,
    ) {
        synchronized(lock) {
            val activeMediaId = _state.value.mediaId
            if (activeMediaId != null && mediaId != activeMediaId) return

            if (activeMediaId == null && mediaId != null) {
                generation += 1L
                loadingStartedAtMs = elapsedRealtimeMs()
                _state.value = PlaybackControlUiState(
                    mediaId = mediaId,
                    isLoading = readiness != PlaybackReadiness.READY,
                    showPause = playWhenReady,
                )
            }

            latestReadiness = readiness
            latestPlayWhenReady = playWhenReady

            when (readiness) {
                PlaybackReadiness.BUFFERING -> {
                    readyCompletionJob?.cancel()
                    readyCompletionJob = null
                    if (!_state.value.isLoading) {
                        generation += 1L
                        loadingStartedAtMs = elapsedRealtimeMs()
                    }
                    _state.value = PlaybackControlUiState(mediaId, isLoading = true, showPause = playWhenReady)
                }

                PlaybackReadiness.READY -> finishLoadingWhenAllowedLocked(mediaId)

                PlaybackReadiness.IDLE,
                PlaybackReadiness.ENDED,
                PlaybackReadiness.ERROR,
                -> {
                    generation += 1L
                    readyCompletionJob?.cancel()
                    readyCompletionJob = null
                    _state.value = PlaybackControlUiState(mediaId, isLoading = false, showPause = false)
                }
            }
        }
    }

    fun clear() = synchronized(lock) {
        generation += 1L
        readyCompletionJob?.cancel()
        readyCompletionJob = null
        latestReadiness = PlaybackReadiness.IDLE
        latestPlayWhenReady = false
        _state.value = PlaybackControlUiState()
    }

    private fun finishLoadingWhenAllowedLocked(mediaId: String?) {
        if (!_state.value.isLoading) {
            _state.value = PlaybackControlUiState(mediaId, isLoading = false, showPause = latestPlayWhenReady)
            return
        }

        val activeGeneration = generation
        val remainingMs = (minimumLoadingDurationMs - (elapsedRealtimeMs() - loadingStartedAtMs))
            .coerceAtLeast(0L)
        readyCompletionJob?.cancel()

        if (remainingMs == 0L) {
            completeReadyLocked(activeGeneration, mediaId)
            return
        }

        readyCompletionJob = scope.launch {
            delay(remainingMs)
            synchronized(lock) {
                completeReadyLocked(activeGeneration, mediaId)
            }
        }
    }

    private fun completeReadyLocked(expectedGeneration: Long, expectedMediaId: String?) {
        if (generation != expectedGeneration || _state.value.mediaId != expectedMediaId) return
        if (latestReadiness != PlaybackReadiness.READY) return
        readyCompletionJob = null
        _state.value = PlaybackControlUiState(
            mediaId = expectedMediaId,
            isLoading = false,
            showPause = latestPlayWhenReady,
        )
    }
}
