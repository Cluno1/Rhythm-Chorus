@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.presentation

import alphaTab.ICursorHandler
import alphaTab.midi.MidiTickLookupFindBeatResultCursorMode
import alphaTab.platform.Cursors
import alphaTab.platform.IContainer
import alphaTab.rendering.utils.BeatBounds

internal data class ScoreCursorTransition(
    val targetX: Double,
    val durationMillis: Long,
)

internal fun stableScoreCursorTransition(
    startX: Double,
    endX: Double,
    durationMillis: Double,
    cursorMode: MidiTickLookupFindBeatResultCursorMode,
    hasFollowingBeat: Boolean = true,
): ScoreCursorTransition {
    val factor = if (
        cursorMode == MidiTickLookupFindBeatResultCursorMode.ToNextBext && hasFollowingBeat
    ) {
        2.0
    } else {
        1.0
    }
    val target = startX + (endX - startX) * factor
    val duration = durationMillis * factor
    return ScoreCursorTransition(
        targetX = target.takeIf(Double::isFinite) ?: startX,
        durationMillis = duration.takeIf { it.isFinite() && it > 0.0 }
            ?.toLong()
            ?.coerceAtMost(MAX_SCORE_CURSOR_TRANSITION_MS)
            ?: 0L,
    )
}

/** Draws the playback line in our overlay, avoiding Android View Animation rendering stalls. */
internal class ScorePlaybackCursorHandler(
    private val overlay: ScorePlaybackOverlayView,
) : ICursorHandler {
    override fun onAttach(cursors: Cursors) = Unit

    override fun onDetach(cursors: Cursors) {
        overlay.hidePlaybackLine()
    }

    override fun placeBarCursor(barCursor: IContainer, beatBounds: BeatBounds) {
        val bar = beatBounds.barBounds.masterBarBounds.visualBounds
        barCursor.setBounds(bar.x, bar.y, bar.w, bar.h)
    }

    override fun placeBeatCursor(
        beatCursor: IContainer,
        beatBounds: BeatBounds,
        startBeatX: Double,
    ) {
        val bar = beatBounds.barBounds.masterBarBounds.visualBounds
        overlay.placePlaybackLine(startBeatX, bar.y, bar.h)
    }

    override fun transitionBeatCursor(
        beatCursor: IContainer,
        beatBounds: BeatBounds,
        startBeatX: Double,
        nextBeatX: Double,
        duration: Double,
        cursorMode: MidiTickLookupFindBeatResultCursorMode,
    ) {
        val transition = stableScoreCursorTransition(
            startX = startBeatX,
            endX = nextBeatX,
            durationMillis = duration,
            cursorMode = cursorMode,
            hasFollowingBeat = beatBounds.beat.nextBeat != null,
        )
        val bar = beatBounds.barBounds.masterBarBounds.visualBounds
        overlay.animatePlaybackLine(
            targetX = transition.targetX.coerceIn(bar.x, bar.x + bar.w),
            durationMillis = transition.durationMillis,
        )
    }
}

private const val MAX_SCORE_CURSOR_TRANSITION_MS = 120_000L
