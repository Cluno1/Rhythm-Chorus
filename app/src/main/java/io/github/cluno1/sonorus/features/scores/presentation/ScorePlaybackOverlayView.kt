@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.presentation

import alphaTab.model.Beat
import alphaTab.rendering.utils.BoundsLookup
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/** Pulses around active note heads without covering or changing their original glyphs. */
internal class ScorePlaybackOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(225, 29, 72)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val animatedMarker = RectF()
    private var activeBeats: List<Beat> = emptyList()
    private var markers: List<RectF> = emptyList()
    private var pulseProgress = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 520L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = DecelerateInterpolator()
        addUpdateListener { animation ->
            pulseProgress = animation.animatedValue as Float
            invalidate()
        }
    }

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun showBeats(beats: List<Beat>, lookup: BoundsLookup?) {
        activeBeats = beats
        updateMarkers(lookup)
    }

    fun refresh(lookup: BoundsLookup?) {
        updateMarkers(lookup)
    }

    private fun updateMarkers(lookup: BoundsLookup?) {
        markers = if (lookup?.isFinished == true) {
            activeBeats.flatMap { beat ->
                lookup.findBeats(beat)?.toList().orEmpty().flatMap { beatBounds ->
                    beatBounds.notes?.toList().orEmpty().map { noteBounds ->
                        val noteHead = noteBounds.noteHeadBounds
                        val centerX = (noteHead.x + noteHead.w / 2.0) * density
                        val centerY = (noteHead.y + noteHead.h / 2.0) * density
                        // Keep the ring outside the glyph. The minimum size also handles
                        // alphaTab glyphs which report a point-sized note-head bound.
                        val ringGap = 2.5 * density
                        val halfWidth = max(noteHead.w * density / 2.0, 5.0 * density) + ringGap
                        val halfHeight = max(noteHead.h * density / 2.0, 3.5 * density) + ringGap
                        RectF(
                            (centerX - halfWidth).toFloat(),
                            (centerY - halfHeight).toFloat(),
                            (centerX + halfWidth).toFloat(),
                            (centerY + halfHeight).toFloat(),
                        )
                    }
                }
            }
        } else {
            emptyList()
        }
        updatePulseAnimation()
        bringToFront()
        invalidate()
    }

    private fun updatePulseAnimation() {
        if (markers.isEmpty()) {
            pulseAnimator.cancel()
            pulseProgress = 0f
        } else if (isAttachedToWindow && !pulseAnimator.isRunning) {
            pulseAnimator.start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePulseAnimation()
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val expansion = pulseProgress * 2.5f * density
        pulseRingPaint.alpha = (220f - pulseProgress * 80f).toInt()
        markers.forEach { marker ->
            animatedMarker.set(
                marker.left - expansion,
                marker.top - expansion,
                marker.right + expansion,
                marker.bottom + expansion,
            )
            canvas.drawOval(animatedMarker, pulseRingPaint)
        }
    }
}
