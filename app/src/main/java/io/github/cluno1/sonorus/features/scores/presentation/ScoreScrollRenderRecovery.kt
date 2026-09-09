package io.github.cluno1.sonorus.features.scores.presentation

import android.view.View
import android.view.ViewTreeObserver
import android.widget.ScrollView

/** Detects the first animation frame where a scrolling viewport has stopped moving. */
internal class ScoreScrollSettleDetector {
    private var previousPosition: Int? = null

    fun observe(position: Int): Boolean {
        if (previousPosition == position) {
            previousPosition = null
            return true
        }
        previousPosition = position
        return false
    }

    fun reset() {
        previousPosition = null
    }
}

/**
 * Gives alphaTab's Android lazy-render surface one final layout pass after a fling settles.
 *
 * AlphaTab recycles bitmap partials outside the viewport. During a fast reverse fling its
 * single scroll listener can coalesce the final scroll event while a previous layout is still
 * pending, leaving the final viewport without a request to restore its recycled partials.
 */
internal class ScoreScrollRenderRecovery(
    private val renderSurface: View,
    private val scrollView: ScrollView,
) : ViewTreeObserver.OnScrollChangedListener {
    private val settleDetector = ScoreScrollSettleDetector()
    private var started = false
    private var monitoringSettle = false

    private val settleRunnable = object : Runnable {
        override fun run() {
            if (!started || !scrollView.isAttachedToWindow) {
                monitoringSettle = false
                settleDetector.reset()
                return
            }
            if (settleDetector.observe(scrollView.scrollY)) {
                monitoringSettle = false
                forceVisiblePartialCheck()
            } else {
                scrollView.postOnAnimation(this)
            }
        }
    }

    fun start() {
        if (started) return
        started = true
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)
    }

    override fun onScrollChanged() {
        if (!started || monitoringSettle) return
        monitoringSettle = true
        settleDetector.reset()
        scrollView.postOnAnimation(settleRunnable)
    }

    fun close() {
        if (!started) return
        started = false
        monitoringSettle = false
        settleDetector.reset()
        scrollView.removeCallbacks(settleRunnable)
        val observer = scrollView.viewTreeObserver
        if (observer.isAlive) observer.removeOnScrollChangedListener(this)
    }

    private fun forceVisiblePartialCheck() {
        if (!renderSurface.isAttachedToWindow) return
        // forceLayout guarantees AlphaTabRenderSurface.onMeasure() refreshes its private
        // visible-rect dirty flag before onLayout() requests any missing lazy partials.
        renderSurface.forceLayout()
        renderSurface.requestLayout()
        renderSurface.postInvalidateOnAnimation()
    }
}
