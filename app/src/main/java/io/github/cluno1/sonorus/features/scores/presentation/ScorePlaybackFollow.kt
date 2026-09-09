@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.presentation

import alphaTab.AlphaTabView
import alphaTab.IScrollHandler
import alphaTab.midi.MidiTickLookupFindBeatResultCursorMode
import alphaTab.rendering.utils.BeatBounds
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import android.widget.ScrollView
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val SCORE_FOLLOW_VIEWPORT_FRACTION = 0.36
private const val MAX_SCORE_FOLLOW_ANIMATION_MS = 120_000L

internal fun scoreFollowScrollOffset(
    viewportHeightPx: Int,
    density: Float,
): Double = if (viewportHeightPx > 0 && density > 0f) {
    -(viewportHeightPx / density * SCORE_FOLLOW_VIEWPORT_FRACTION)
} else {
    0.0
}

/**
 * Extra scrollable space needed to keep the final system at the same reading position as
 * every other system. alphaTab 1.8.4's Android render surface ignores its own padding while
 * measuring, so this must be applied to the wrapper's minimum height instead.
 */
internal fun scoreFollowTailSpace(viewportHeightPx: Int): Int =
    if (viewportHeightPx > 0) {
        (viewportHeightPx * (1.0 - SCORE_FOLLOW_VIEWPORT_FRACTION)).roundToInt()
    } else {
        0
    }

internal fun scoreFollowContentMinHeight(scoreHeightPx: Int, viewportHeightPx: Int): Int =
    scoreHeightPx.coerceAtLeast(0) + scoreFollowTailSpace(viewportHeightPx)

/**
 * Keeps the score moving continuously through a system without alphaTab's instant jump at
 * every line break. The next system naturally arrives at the same reading position because
 * the previous animation ends at the bottom of its system.
 */
internal class ScorePlaybackScrollHandler(
    private val displayView: AlphaTabView,
    private val scrollView: ScrollView,
) : IScrollHandler {
    private val density = displayView.resources.displayMetrics.density
    private var scrollAnimator: ObjectAnimator? = null
    private var lastSystemIndex: Int? = null

    override fun forceScrollTo(currentBeatBounds: BeatBounds) {
        scrollAnimator?.cancel()
        scrollAnimator = null
        scrollView.scrollTo(0, systemStartScroll(currentBeatBounds))
        lastSystemIndex = null
    }

    override fun onBeatCursorUpdating(
        startBeat: BeatBounds,
        endBeat: BeatBounds?,
        cursorMode: MidiTickLookupFindBeatResultCursorMode,
        actualBeatCursorStartX: Double,
        actualBeatCursorEndX: Double,
        actualBeatCursorTransitionDuration: Double,
    ) {
        val system = startBeat.barBounds.masterBarBounds.staffSystemBounds ?: return
        val systemIndex = system.index.toInt()
        if (lastSystemIndex == systemIndex && actualBeatCursorTransitionDuration > 0.0) return

        val startScroll = systemStartScroll(startBeat)
        val duration = systemDurationMillis(startBeat)
        val looksLikeSeek = abs(scrollView.scrollY - startScroll) > scrollView.height * 1.5
        if (lastSystemIndex == null || duration <= 0L || looksLikeSeek) {
            scrollAnimator?.cancel()
            scrollView.scrollTo(0, startScroll)
        }

        lastSystemIndex = systemIndex
        if (duration <= 0L) return

        val offset = scoreFollowScrollOffset(scrollView.height, density)
        val target = ((system.realBounds.y + system.realBounds.h + offset) * density)
            .roundToInt()
            .coerceAtLeast(0)
        scrollAnimator?.cancel()
        scrollAnimator = ObjectAnimator.ofInt(scrollView, "scrollY", target).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            start()
        }
    }

    override fun close() {
        scrollAnimator?.cancel()
        scrollAnimator = null
    }

    private fun systemStartScroll(beatBounds: BeatBounds): Int {
        val offset = scoreFollowScrollOffset(scrollView.height, density)
        return ((beatBounds.barBounds.masterBarBounds.staffSystemBounds?.realBounds?.y
            ?: beatBounds.barBounds.masterBarBounds.realBounds.y) + offset)
            .times(density)
            .roundToInt()
            .coerceAtLeast(0)
    }

    private fun systemDurationMillis(beatBounds: BeatBounds): Long {
        val systemBars = beatBounds.barBounds.masterBarBounds.staffSystemBounds?.bars?.toList()
            ?: return 0L
        val score = displayView.api.score ?: return 0L
        val tickCache = displayView.api.tickCache ?: return 0L
        var duration = 0.0

        systemBars.forEach { barBounds ->
            val masterBar = score.masterBars[barBounds.index.toInt()]
            val lookup = tickCache.getMasterBar(masterBar)
            val tempoChanges = lookup.tempoChanges.toList()
            if (tempoChanges.isEmpty()) {
                duration += scoreTicksToMillis(lookup.end - lookup.start, lookup.tempo)
                return@forEach
            }

            var tempo = tempoChanges.first().tempo
            var tick = tempoChanges.first().tick
            tempoChanges.drop(1).forEach { change ->
                duration += scoreTicksToMillis(change.tick - tick, tempo)
                tempo = change.tempo
                tick = change.tick
            }
            duration += scoreTicksToMillis(lookup.end - tick, tempo)
        }

        val playbackSpeed = displayView.api.playbackSpeed.takeIf { it > 0.0 } ?: 1.0
        return (duration / playbackSpeed)
            .takeIf { it.isFinite() && it > 0.0 }
            ?.roundToLong()
            ?.coerceAtMost(MAX_SCORE_FOLLOW_ANIMATION_MS)
            ?: 0L
    }
}

private fun scoreTicksToMillis(ticks: Double, tempo: Double): Double =
    if (ticks > 0.0 && tempo > 0.0) ticks * (60_000.0 / (tempo * 960.0)) else 0.0
