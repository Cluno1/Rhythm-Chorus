@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package chromahub.rhythm.app.features.scores.presentation

import alphaTab.model.Beat
import alphaTab.rendering.utils.BoundsLookup
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max

/** Recolors active note heads without asking alphaTab to rerender the score every beat. */
internal class ScorePlaybackOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val noteHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(225, 29, 72)
        style = Paint.Style.FILL
    }
    private var activeBeats: List<Beat> = emptyList()
    private var markers: List<RectF> = emptyList()

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
                        // Some alphaTab glyphs report a point-sized note-head bound. Keep a
                        // compact oval fallback so it covers the glyph instead of becoming
                        // invisible, while remaining much smaller than the old pulse ring.
                        val halfWidth = max(noteHead.w * density / 2.0, 5.0 * density)
                        val halfHeight = max(noteHead.h * density / 2.0, 3.5 * density)
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
        bringToFront()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        markers.forEach { marker ->
            canvas.drawOval(marker, noteHeadPaint)
        }
    }
}
