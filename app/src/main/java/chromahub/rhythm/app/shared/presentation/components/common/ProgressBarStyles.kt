package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Progress bar style options for MiniPlayer and Player
 * Following Compose December 2025 best practices with optimized animations
 */
enum class ProgressStyle {
    NORMAL,     // Standard LinearProgressIndicator
    WAVY,       // Animated wavy line
    ROUNDED,    // Rounded pill-shaped progress
    THIN,       // Thin elegant line
    THICK,      // Thick bold progress bar
    GRADIENT,   // Gradient colored progress
    SEGMENTED,  // Segmented/dotted progress
    DOTS        // Dots indicator
}

/**
 * Thumb style options for progress bar slider
 */
enum class ThumbStyle {
    NONE,       // No thumb
    CIRCLE,     // Standard circular thumb
    PILL,       // Pill-shaped vertical thumb
    DIAMOND,    // Diamond/rhombus shaped thumb
    LINE,       // Vertical line thumb
    SQUARE,     // Rounded square thumb
    GLOW,       // Circle with glow effect
    ARROW,      // Arrow/chevron pointing right
    DOT         // Small dot thumb
}

/**
 * Helper function to draw different thumb styles
 */
private fun DrawScope.drawThumb(
    thumbStyle: ThumbStyle,
    progressColor: Color,
    thumbSize: Float,
    position: Offset
) {
    when (thumbStyle) {
        ThumbStyle.NONE -> { /* No thumb */ }
        ThumbStyle.CIRCLE -> {
            // Multi-layered glow effect
            for (i in 4 downTo 0) {
                val alpha = 0.08f * (5 - i)
                val radius = thumbSize / 2 + (i * 3f)
                drawCircle(
                    color = progressColor.copy(alpha = alpha),
                    radius = radius,
                    center = position
                )
            }
            // Main circle with gradient
            drawCircle(
                color = progressColor,
                radius = thumbSize / 2,
                center = position
            )
            // Inner highlight with radial gradient effect
            val highlightRadius = thumbSize / 2.2f
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = highlightRadius,
                center = Offset(position.x - thumbSize / 5, position.y - thumbSize / 5)
            )
            // Subtle inner shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.1f),
                radius = thumbSize / 3,
                center = Offset(position.x + thumbSize / 6, position.y + thumbSize / 6)
            )
        }
        ThumbStyle.PILL -> {
            val pillWidth = thumbSize * 0.9f
            val pillHeight = thumbSize * 1.6f
            val cornerRadius = pillHeight / 2

            // Soft shadow with multiple layers
            for (i in 2 downTo 0) {
                val alpha = 0.1f * (3 - i)
                val offset = i * 2f
                drawRoundRect(
                    color = progressColor.copy(alpha = alpha),
                    topLeft = Offset(position.x - pillWidth / 2 - offset, position.y - pillHeight / 2 - offset),
                    size = Size(pillWidth + offset * 2, pillHeight + offset * 2),
                    cornerRadius = CornerRadius(cornerRadius + offset)
                )
            }

            // Main pill shape
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(position.x - pillWidth / 2, position.y - pillHeight / 2),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(cornerRadius)
            )

            // Highlight stripe
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(position.x - pillWidth / 2 + 3f, position.y - pillHeight / 2 + 3f),
                size = Size(pillWidth - 6f, pillHeight / 2.5f),
                cornerRadius = CornerRadius((pillHeight / 2.5f) / 2)
            )
        }
        ThumbStyle.DIAMOND -> {
            val diamondSize = thumbSize * 1.1f

            // Outer glow
            val glowPath = Path().apply {
                moveTo(position.x, position.y - diamondSize / 2 - 4f)
                lineTo(position.x + diamondSize / 2 + 4f, position.y)
                lineTo(position.x, position.y + diamondSize / 2 + 4f)
                lineTo(position.x - diamondSize / 2 - 4f, position.y)
                close()
            }
            drawPath(glowPath, progressColor.copy(alpha = 0.2f))

            // Main diamond
            val mainPath = Path().apply {
                moveTo(position.x, position.y - diamondSize / 2)
                lineTo(position.x + diamondSize / 2, position.y)
                lineTo(position.x, position.y + diamondSize / 2)
                lineTo(position.x - diamondSize / 2, position.y)
                close()
            }
            drawPath(mainPath, progressColor)

            // Inner highlight facets
            val highlightPath = Path().apply {
                moveTo(position.x, position.y - diamondSize / 2 + 4f)
                lineTo(position.x + diamondSize / 2 - 4f, position.y)
                lineTo(position.x, position.y - 2f)
                close()
            }
            drawPath(highlightPath, Color.White.copy(alpha = 0.4f))

            // Bottom shadow facet
            val shadowPath = Path().apply {
                moveTo(position.x, position.y + diamondSize / 2 - 4f)
                lineTo(position.x + diamondSize / 2 - 4f, position.y)
                lineTo(position.x, position.y + 2f)
                close()
            }
            drawPath(shadowPath, Color.Black.copy(alpha = 0.2f))
        }
        ThumbStyle.LINE -> {
            val lineHeight = thumbSize * 3.2f
            val lineWidth = thumbSize * 0.4f

            // Soft glow around the line
            drawRoundRect(
                color = progressColor.copy(alpha = 0.25f),
                topLeft = Offset(position.x - lineWidth - 3f, position.y - lineHeight / 2 - 3f),
                size = Size(lineWidth * 2 + 6f, lineHeight + 6f),
                cornerRadius = CornerRadius(lineWidth + 3f)
            )

            // Main line
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(position.x - lineWidth / 2, position.y - lineHeight / 2),
                size = Size(lineWidth, lineHeight),
                cornerRadius = CornerRadius(lineWidth / 2)
            )

            // Center highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(position.x - lineWidth / 3, position.y - lineHeight / 3),
                size = Size(lineWidth * 0.66f, lineHeight * 0.66f),
                cornerRadius = CornerRadius(lineWidth / 3)
            )
        }
        ThumbStyle.SQUARE -> {
            val squareSize = thumbSize * 1.2f
            val cornerRadius = squareSize * 0.2f

            // Multi-layer shadow effect
            for (i in 3 downTo 0) {
                val alpha = 0.08f * (4 - i)
                val offset = i * 1.5f
                drawRoundRect(
                    color = progressColor.copy(alpha = alpha),
                    topLeft = Offset(position.x - squareSize / 2 - offset, position.y - squareSize / 2 - offset),
                    size = Size(squareSize + offset * 2, squareSize + offset * 2),
                    cornerRadius = CornerRadius(cornerRadius + offset * 0.5f)
                )
            }

            // Main square
            drawRoundRect(
                color = progressColor,
                topLeft = Offset(position.x - squareSize / 2, position.y - squareSize / 2),
                size = Size(squareSize, squareSize),
                cornerRadius = CornerRadius(cornerRadius)
            )

            // Inner highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = Offset(position.x - squareSize / 2 + 4f, position.y - squareSize / 2 + 4f),
                size = Size(squareSize - 8f, squareSize - 8f),
                cornerRadius = CornerRadius(cornerRadius - 2f)
            )

            // Subtle inner shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(position.x - squareSize / 2 + squareSize * 0.65f, position.y - squareSize / 2 + squareSize * 0.65f),
                size = Size(squareSize * 0.3f, squareSize * 0.3f),
                cornerRadius = CornerRadius(cornerRadius * 0.3f)
            )
        }
        ThumbStyle.GLOW -> {
            // Advanced multi-layer glow with varying alphas and sizes
            val glowLayers = 6
            for (i in glowLayers downTo 0) {
                val progress = i.toFloat() / glowLayers
                val alpha = 0.12f * (1f - progress * 0.7f)
                val radius = thumbSize / 2 + (i * 6f)
                drawCircle(
                    color = progressColor.copy(alpha = alpha),
                    radius = radius,
                    center = position
                )
            }

            // Pulsing inner core
            drawCircle(
                color = progressColor.copy(alpha = 0.9f),
                radius = thumbSize / 2f,
                center = position
            )

            // Bright center highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = thumbSize / 3f,
                center = position
            )

            // Subtle color variation in core
            drawCircle(
                color = progressColor.copy(alpha = 0.3f),
                radius = thumbSize / 4.5f,
                center = Offset(position.x - thumbSize / 9, position.y - thumbSize / 9)
            )
        }
        ThumbStyle.ARROW -> {
            val arrowWidth = thumbSize * 1.4f
            val arrowHeight = thumbSize * 1.1f

            // Arrow shadow/glow
            val shadowPath = Path().apply {
                moveTo(position.x - arrowWidth / 3 - 3f, position.y - arrowHeight / 2 - 3f)
                lineTo(position.x + arrowWidth / 2 + 3f, position.y)
                lineTo(position.x - arrowWidth / 3 - 3f, position.y + arrowHeight / 2 + 3f)
                lineTo(position.x - arrowWidth / 6 - 3f, position.y)
                close()
            }
            drawPath(shadowPath, progressColor.copy(alpha = 0.2f))

            // Main arrow
            val arrowPath = Path().apply {
                moveTo(position.x - arrowWidth / 3, position.y - arrowHeight / 2)
                lineTo(position.x + arrowWidth / 2, position.y)
                lineTo(position.x - arrowWidth / 3, position.y + arrowHeight / 2)
                lineTo(position.x - arrowWidth / 6, position.y)
                close()
            }
            drawPath(arrowPath, progressColor)

            // Arrow tip highlight
            val highlightPath = Path().apply {
                moveTo(position.x + arrowWidth / 2 - 4f, position.y)
                lineTo(position.x + arrowWidth / 3, position.y - arrowHeight / 3)
                lineTo(position.x + arrowWidth / 3, position.y + arrowHeight / 3)
                close()
            }
            drawPath(highlightPath, Color.White.copy(alpha = 0.4f))
        }
        ThumbStyle.DOT -> {
            val dotSize = thumbSize * 0.7f

            // Outer ripple rings
            for (i in 2 downTo 0) {
                val alpha = 0.15f * (3 - i)
                val radius = dotSize + (i * 6f)
                drawCircle(
                    color = progressColor.copy(alpha = alpha),
                    radius = radius,
                    center = position,
                    style = Stroke(width = 2f)
                )
            }

            // Main dot with gradient effect
            drawCircle(
                color = progressColor,
                radius = dotSize,
                center = position
            )

            // Inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = dotSize * 0.5f,
                center = Offset(position.x - dotSize * 0.25f, position.y - dotSize * 0.25f)
            )

            // Subtle shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = dotSize * 0.4f,
                center = Offset(position.x + dotSize * 0.2f, position.y + dotSize * 0.2f)
            )
        }
    }
}

/**
 * Unified progress bar composable that renders different styles
 */
@Composable
fun StyledProgressBar(
    progress: Float,
    style: ProgressStyle,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    height: Dp = 4.dp,
    isPlaying: Boolean = true,
    animated: Boolean = true,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 12.dp,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    waveLength: Dp = 40.dp
) {
    when (style) {
        ProgressStyle.NORMAL -> NormalProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize
        )
        ProgressStyle.WAVY -> WavyProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            isPlaying = isPlaying && animated,
            waveAmplitudeWhenPlaying = waveAmplitudeWhenPlaying,
            waveLength = waveLength
        )
        ProgressStyle.ROUNDED -> RoundedProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize
        )
        ProgressStyle.THIN -> ThinProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize
        )
        ProgressStyle.THICK -> ThickProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize
        )
        ProgressStyle.GRADIENT -> GradientProgressBar(
            progress = progress,
            modifier = modifier,
            trackColor = trackColor,
            height = height,
            showThumb = showThumb,
            thumbStyle = thumbStyle,
            thumbSize = thumbSize
        )
        ProgressStyle.SEGMENTED -> SegmentedProgressBar(
            progress = progress,
            modifier = modifier,
            progressColor = progressColor,
            trackColor = trackColor,
            height = height
        )
        ProgressStyle.DOTS -> DotsProgressBar(
            progress = progress,
            modifier = modifier,
            activeColor = progressColor,
            inactiveColor = trackColor
        )
    }
}

/**
 * Standard Material3 LinearProgressIndicator
 */
@Composable
private fun NormalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 12.dp
) {
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(height.coerceAtLeast(thumbSize))
        ) {
            val progressWidth = size.width * progress.coerceIn(0f, 1f)
            val centerY = size.height / 2
            val trackHeight = height.toPx()
            
            // Draw track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )
            
            // Draw progress
            if (progressWidth > 0) {
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = androidx.compose.ui.geometry.Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
            }
            
            // Draw thumb based on style
            if (progressWidth > 0) {
                drawThumb(
                    thumbStyle = thumbStyle,
                    progressColor = progressColor,
                    thumbSize = thumbSize.toPx(),
                    position = Offset(progressWidth, centerY)
                )
            }
        }
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            color = progressColor,
            trackColor = trackColor
        )
    }
}

/**
 * Wavy animated progress bar - playful and musical
 * Enhanced with smooth amplitude transitions and bezier curve smoothing
 */
@Composable
private fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    isPlaying: Boolean,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    waveLength: Dp = 40.dp
) {
    // Smooth wave amplitude animation - only show wave when playing
    val animatedAmplitude by animateDpAsState(
        targetValue = if (isPlaying) waveAmplitudeWhenPlaying else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "WaveAmplitudeAnim"
    )
    
    // Conditional phase animation - only when wave should show
    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val fullRotation = (2 * PI).toFloat()
            while (isPlaying) {
                val start = (phaseShiftAnim.value % fullRotation).let { 
                    if (it < 0f) it + fullRotation else it 
                }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.coerceAtLeast(8.dp))
    ) {
        val width = size.width
        val centerY = size.height / 2
        val progressWidth = width * progress.coerceIn(0f, 1f)
        val waveAmplitude = animatedAmplitude.toPx().coerceAtLeast(0f)
        val strokeWidth = (size.height / 2).coerceIn(2f, 6f)
        
        // Draw track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Draw wavy progress
        if (progressWidth > 0) {
            if (waveAmplitude > 0.01f) {
                // Draw wavy line
                val path = Path()
                val waveLengthPx = waveLength.toPx()
                val waveFrequency = if (waveLengthPx > 0f) {
                    ((2 * PI) / waveLengthPx).toFloat()
                } else {
                    0f
                }
                
                val waveStartDrawX = 0f
                val waveEndDrawX = progressWidth.coerceAtLeast(waveStartDrawX)
                
                if (waveEndDrawX > waveStartDrawX) {
                    val periodPx = ((2 * PI) / waveFrequency).toFloat()
                    val samplesPerCycle = 20f
                    val waveStep = (periodPx / samplesPerCycle).coerceAtLeast(1.2f).coerceAtMost(strokeWidth)

                    fun yAt(x: Float): Float {
                        val s = sin(waveFrequency * x + phaseShift)
                        return (centerY + waveAmplitude * s).coerceIn(
                            centerY - waveAmplitude - strokeWidth / 2f,
                            centerY + waveAmplitude + strokeWidth / 2f
                        )
                    }

                    var prevX = waveStartDrawX
                    var prevY = yAt(prevX)
                    path.moveTo(prevX, prevY)

                    var x = prevX + waveStep
                    while (x < waveEndDrawX) {
                        val y = yAt(x)
                        val midX = (prevX + x) * 0.5f
                        val midY = (prevY + y) * 0.5f
                        path.quadraticTo(prevX, prevY, midX, midY)
                        prevX = x
                        prevY = y
                        x += waveStep
                    }
                    val endY = yAt(waveEndDrawX)
                    path.quadraticTo(prevX, prevY, waveEndDrawX, endY)

                    drawPath(
                        path = path,
                        color = progressColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            miter = 1f
                        )
                    )
                }
            } else {
                // Draw straight line when paused
                drawLine(
                    color = progressColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressWidth, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Rounded pill-shaped progress bar
 */
@Composable
private fun RoundedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 12.dp
) {
    val actualHeight = height.coerceAtLeast(6.dp)
    
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight.coerceAtLeast(thumbSize))
        ) {
            val progressWidth = size.width * progress.coerceIn(0f, 1f)
            val centerY = size.height / 2
            val trackHeight = actualHeight.toPx()
            
            // Draw track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )
            
            // Draw progress
            if (progressWidth > 0) {
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Draw thumb
                drawThumb(
                    thumbStyle = thumbStyle,
                    progressColor = progressColor,
                    thumbSize = thumbSize.toPx(),
                    position = Offset(progressWidth, centerY)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(actualHeight)
                    .clip(RoundedCornerShape(50))
                    .background(progressColor)
            )
        }
    }
}

/**
 * Thin elegant progress line - 2dp height
 */
@Composable
private fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 10.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showThumb && thumbStyle != ThumbStyle.NONE) thumbSize else 2.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2
        
        // Track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Progress
        val progressWidth = width * progress.coerceIn(0f, 1f)
        if (progressWidth > 0) {
            drawLine(
                color = progressColor,
                start = Offset(0f, centerY),
                end = Offset(progressWidth, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw thumb
            if (showThumb && thumbStyle != ThumbStyle.NONE) {
                drawThumb(
                    thumbStyle = thumbStyle,
                    progressColor = progressColor,
                    thumbSize = thumbSize.toPx(),
                    position = Offset(progressWidth, centerY)
                )
            }
        }
    }
}

/**
 * Thick bold progress bar - 8dp height
 */
@Composable
private fun ThickProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 14.dp
) {
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp.coerceAtLeast(thumbSize))
        ) {
            val progressWidth = size.width * progress.coerceIn(0f, 1f)
            val centerY = size.height / 2
            val trackHeight = 8.dp.toPx()
            
            // Draw track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            
            // Draw progress
            if (progressWidth > 0) {
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                
                // Draw thumb
                drawThumb(
                    thumbStyle = thumbStyle,
                    progressColor = progressColor,
                    thumbSize = thumbSize.toPx(),
                    position = Offset(progressWidth, centerY)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(progressColor)
            )
        }
    }
}

/**
 * Gradient colored progress bar
 */
@Composable
private fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color,
    height: Dp,
    showThumb: Boolean = false,
    thumbStyle: ThumbStyle = ThumbStyle.CIRCLE,
    thumbSize: Dp = 12.dp
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    
    val actualHeight = height.coerceAtLeast(4.dp)
    
    if (showThumb && thumbStyle != ThumbStyle.NONE) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight.coerceAtLeast(thumbSize))
        ) {
            val progressWidth = size.width * progress.coerceIn(0f, 1f)
            val centerY = size.height / 2
            val trackHeight = actualHeight.toPx()
            
            // Draw track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )
            
            // Draw gradient progress
            if (progressWidth > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(gradientColors),
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2)
                )
                
                // Draw thumb with tertiary color (end of gradient)
                drawThumb(
                    thumbStyle = thumbStyle,
                    progressColor = gradientColors.last(),
                    thumbSize = thumbSize.toPx(),
                    position = Offset(progressWidth, centerY)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(actualHeight)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(actualHeight)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
            )
        }
    }
}

/**
 * Segmented progress bar with gaps
 */
@Composable
private fun SegmentedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color,
    trackColor: Color,
    height: Dp
) {
    val segments = 20
    val actualHeight = height.coerceAtLeast(4.dp)
    val filledSegments = (progress * segments).toInt()
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(actualHeight)
    ) {
        val segmentWidth = (size.width - (segments - 1) * 3.dp.toPx()) / segments
        val cornerRadius = CornerRadius(size.height / 2)
        
        for (i in 0 until segments) {
            val x = i * (segmentWidth + 3.dp.toPx())
            val color = if (i < filledSegments) progressColor else trackColor
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
    }
}

/**
 * Dots progress indicator
 */
@Composable
private fun DotsProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color
) {
    val dotCount = 12
    val activeDots = (progress * dotCount).toInt()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (i < activeDots) activeColor else inactiveColor)
            )
        }
    }
}

/**
 * Compact mini progress bar for MiniPlayer - optimized for small spaces
 */
@Composable
fun MiniProgressBar(
    progress: Float,
    style: String,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    isPlaying: Boolean = true
) {
    val progressStyle = try {
        ProgressStyle.valueOf(style.uppercase())
    } catch (e: IllegalArgumentException) {
        ProgressStyle.NORMAL
    }
    
    StyledProgressBar(
        progress = progress,
        style = progressStyle,
        modifier = modifier,
        progressColor = progressColor,
        trackColor = trackColor,
        height = when (progressStyle) {
            ProgressStyle.THIN -> 2.dp
            ProgressStyle.THICK -> 6.dp
            ProgressStyle.WAVY -> 8.dp
            ProgressStyle.DOTS -> 6.dp
            ProgressStyle.SEGMENTED -> 4.dp
            else -> 4.dp
        },
        isPlaying = isPlaying,
        animated = true
    )
}

/**
 * Circular styled progress bar that wraps around content (like play/pause button)
 * Supports all progress styles including wavy, segmented, dots, etc.
 * The cornerRadius parameter allows the progress to adapt to button shape changes
 * (e.g., from circle when paused to rounded rect when playing)
 */
@Composable
fun CircularStyledProgressBar(
    progress: Float,
    style: ProgressStyle,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    strokeWidth: Dp = 3.dp,
    isPlaying: Boolean = true,
    cornerRadius: Dp = 50.dp, // 50.dp = circle, lower values = more rounded rect
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            ProgressStyle.WAVY -> WavyCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                isPlaying = isPlaying,
                cornerRadius = cornerRadius
            )
            ProgressStyle.SEGMENTED -> SegmentedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.DOTS -> DottedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.GRADIENT -> GradientCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.THIN -> ThinCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth * 0.6f,
                cornerRadius = cornerRadius
            )
            ProgressStyle.THICK -> ThickCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth * 1.5f,
                cornerRadius = cornerRadius
            )
            ProgressStyle.ROUNDED -> RoundedCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
            ProgressStyle.NORMAL -> NormalCircularProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )
        }
        
        content()
    }
}

@Composable
private fun WavyCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    isPlaying: Boolean,
    cornerRadius: Dp = 50.dp
) {
    // Conditional phase animation - only when playing
    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value
    
    // Wave amplitude animation - animates to 0 when paused (flat circle)
    val waveAmplitudeAnim by animateFloatAsState(
        targetValue = if (isPlaying) 0.3f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "waveAmplitude"
    )
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val fullRotation = (2 * PI).toFloat()
            while (isPlaying) {
                val start = (phaseShiftAnim.value % fullRotation).let { 
                    if (it < 0f) it + fullRotation else it 
                }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            // Draw rounded rectangle track and progress
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = true,
                waveOffset = phaseShift,
                waveAmplitude = waveAmplitudeAnim
            )
        } else {
            // Original circular implementation
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw wavy progress (wave flattens to circle when paused)
            if (progress > 0f) {
                val path = Path()
                val sweepAngle = 360f * progress
                val steps = 200
                
                var prevX = 0f
                var prevY = 0f
                
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * sweepAngle
                    if (angle > sweepAngle) break
                    
                    val angleRad = Math.toRadians((angle - 90).toDouble())
                    val wave = sin((angle / 360f * 12 * PI) + phaseShift).toFloat() * stroke * waveAmplitudeAnim
                    val currentRadius = radius + wave
                    
                    val x = center.x + (currentRadius * kotlin.math.cos(angleRad)).toFloat()
                    val y = center.y + (currentRadius * kotlin.math.sin(angleRad)).toFloat()
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                        prevX = x
                        prevY = y
                    } else {
                        // Use quadratic bezier for smoother curves
                        val midX = (prevX + x) * 0.5f
                        val midY = (prevY + y) * 0.5f
                        path.quadraticTo(prevX, prevY, midX, midY)
                        prevX = x
                        prevY = y
                    }
                }
                
                drawPath(
                    path = path,
                    color = progressColor,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        miter = 1f
                    )
                )
            }
        }
    }
}

@Composable
private fun SegmentedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectSegmentedProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            val segments = 20
            val segmentAngle = 360f / segments
            val gapAngle = 4f
            
            for (i in 0 until segments) {
                val startAngle = i * segmentAngle - 90f
                val segmentProgress = ((progress * segments) - i).coerceIn(0f, 1f)
                
                drawArc(
                    color = if (segmentProgress > 0) progressColor else trackColor,
                    startAngle = startAngle + gapAngle / 2,
                    sweepAngle = (segmentAngle - gapAngle) * segmentProgress.coerceAtLeast(0.01f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun DottedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectDottedProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            val dots = 24
            val dotRadius = stroke * 0.8f
            
            for (i in 0 until dots) {
                val angle = (i.toFloat() / dots) * 360f - 90f
                val angleRad = Math.toRadians(angle.toDouble())
                val dotProgress = ((progress * dots) - i).coerceIn(0f, 1f)
                
                val x = center.x + (radius * kotlin.math.cos(angleRad)).toFloat()
                val y = center.y + (radius * kotlin.math.sin(angleRad)).toFloat()
                
                drawCircle(
                    color = if (dotProgress > 0) progressColor else trackColor,
                    radius = dotRadius * (0.5f + dotProgress * 0.5f),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun GradientCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectGradientProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw gradient progress
            if (progress > 0f) {
                val sweepAngle = 360f * progress
                
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            progressColor,
                            progressColor.copy(alpha = 0.7f),
                            progressColor.copy(alpha = 0.9f),
                            progressColor
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun NormalCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = false,
                waveOffset = 0f
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw progress arc
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

@Composable
private fun ThinCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    NormalCircularProgress(
        progress = progress,
        progressColor = progressColor,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius
    )
}

@Composable
private fun ThickCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    NormalCircularProgress(
        progress = progress,
        progressColor = progressColor,
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius
    )
}

@Composable
private fun RoundedCircularProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp = 50.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val rectCornerRadius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2)
        val isRoundedRect = rectCornerRadius < size.minDimension / 2 - 1
        
        if (isRoundedRect) {
            drawRoundedRectProgress(
                progress = progress,
                progressColor = progressColor,
                trackColor = trackColor,
                strokeWidth = stroke,
                cornerRadius = rectCornerRadius,
                isWavy = false,
                waveOffset = 0f,
                useRoundCap = true
            )
        } else {
            val radius = (size.minDimension / 2) - stroke
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )
            
            // Draw progress arc with round cap
            if (progress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

// Helper function to draw rounded rectangle progress
private fun DrawScope.drawRoundedRectProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float,
    isWavy: Boolean = false,
    waveOffset: Float = 0f,
    useRoundCap: Boolean = false,
    waveAmplitude: Float = 0.2f
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track as rounded rect stroke
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Draw progress - trace the rounded rect perimeter
    if (progress > 0f) {
        val path = createRoundedRectProgressPath(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            cornerRadius = cr,
            progress = progress,
            isWavy = isWavy,
            waveOffset = waveOffset,
            strokeWidth = strokeWidth,
            waveAmplitude = waveAmplitude
        )
        
        drawPath(
            path = path,
            color = progressColor,
            style = Stroke(
                width = strokeWidth,
                cap = if (useRoundCap) StrokeCap.Round else StrokeCap.Butt
            )
        )
    }
}

private fun DrawScope.drawRoundedRectSegmentedProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Calculate perimeter for segmentation
    val segments = 20
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cr)
    val segmentLength = perimeter / segments
    val gapLength = segmentLength * 0.15f
    
    for (i in 0 until segments) {
        val segmentProgress = ((progress * segments) - i).coerceIn(0f, 1f)
        if (segmentProgress > 0) {
            val startOffset = i * segmentLength + gapLength / 2
            val endOffset = startOffset + (segmentLength - gapLength) * segmentProgress
            
            val path = createRoundedRectSegmentPath(
                left = left, top = top, right = right, bottom = bottom,
                cornerRadius = cr, startOffset = startOffset, endOffset = endOffset
            )
            
            drawPath(
                path = path,
                color = progressColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    miter = 1f
                )
            )
        }
    }
}

private fun DrawScope.drawRoundedRectDottedProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    val dots = 24
    val dotRadius = strokeWidth * 0.8f
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cr)
    
    for (i in 0 until dots) {
        val offset = (i.toFloat() / dots) * perimeter
        val point = getPointOnRoundedRect(left, top, right, bottom, cr, offset)
        val dotProgress = ((progress * dots) - i).coerceIn(0f, 1f)
        
        drawCircle(
            color = if (dotProgress > 0) progressColor else trackColor,
            radius = dotRadius * (0.5f + dotProgress * 0.5f),
            center = point
        )
    }
}

private fun DrawScope.drawRoundedRectGradientProgress(
    progress: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val halfStroke = strokeWidth / 2
    val left = halfStroke
    val top = halfStroke
    val right = size.width - halfStroke
    val bottom = size.height - halfStroke
    val cr = cornerRadius.coerceAtMost((size.minDimension - strokeWidth) / 2)
    
    // Draw track
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(cr),
        style = Stroke(width = strokeWidth)
    )
    
    // Draw gradient progress
    if (progress > 0f) {
        val path = createRoundedRectProgressPath(
            left = left, top = top, right = right, bottom = bottom,
            cornerRadius = cr, progress = progress,
            isWavy = false, waveOffset = 0f, strokeWidth = strokeWidth
        )
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    progressColor,
                    progressColor.copy(alpha = 0.7f),
                    progressColor.copy(alpha = 0.9f),
                    progressColor
                )
            ),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                miter = 1f
            )
        )
    }
}

// Helper function to create path for rounded rect progress
private fun DrawScope.createRoundedRectProgressPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    progress: Float,
    isWavy: Boolean,
    waveOffset: Float,
    strokeWidth: Float,
    waveAmplitude: Float = 0.2f
): Path {
    val path = Path()
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cornerRadius)
    val targetLength = perimeter * progress
    
    var currentLength = 0f
    val steps = 200
    val stepLength = perimeter / steps
    
    for (i in 0..steps) {
        if (currentLength > targetLength) break
        
        val offset = i * stepLength
        var point = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, offset)
        
        if (isWavy && waveAmplitude > 0f) {
            // Add wave effect - amplitude controls waviness (0 = flat circle, 0.2+ = wavy)
            val wave = sin((offset / perimeter * 12 * PI) + waveOffset).toFloat() * strokeWidth * waveAmplitude
            // Apply wave perpendicular to path
            val nextPoint = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, (offset + 1).coerceAtMost(perimeter))
            val dx = nextPoint.x - point.x
            val dy = nextPoint.y - point.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            point = Offset(
                point.x + (-dy / len) * wave,
                point.y + (dx / len) * wave
            )
        }
        
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
        
        currentLength += stepLength
    }
    
    return path
}

private fun DrawScope.createRoundedRectSegmentPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    startOffset: Float,
    endOffset: Float
): Path {
    val path = Path()
    val perimeter = calculateRoundedRectPerimeter(right - left, bottom - top, cornerRadius)
    
    val steps = 20
    val length = endOffset - startOffset
    val stepLength = length / steps
    
    for (i in 0..steps) {
        val offset = (startOffset + i * stepLength).coerceAtMost(perimeter)
        val point = getPointOnRoundedRect(left, top, right, bottom, cornerRadius, offset)
        
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    
    return path
}

private fun calculateRoundedRectPerimeter(width: Float, height: Float, cornerRadius: Float): Float {
    val cornerArc = 2 * PI.toFloat() * cornerRadius / 4 // Quarter circle
    val straightWidth = (width - 2 * cornerRadius).coerceAtLeast(0f)
    val straightHeight = (height - 2 * cornerRadius).coerceAtLeast(0f)
    return 4 * cornerArc + 2 * straightWidth + 2 * straightHeight
}

private fun getPointOnRoundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    offset: Float
): Offset {
    val width = right - left
    val height = bottom - top
    val cr = cornerRadius.coerceAtMost(kotlin.math.min(width, height) / 2)
    
    val cornerArc = PI.toFloat() * cr / 2
    val straightWidth = (width - 2 * cr).coerceAtLeast(0f)
    val straightHeight = (height - 2 * cr).coerceAtLeast(0f)
    val perimeter = 4 * cornerArc + 2 * straightWidth + 2 * straightHeight
    
    var pos = offset % perimeter
    if (pos < 0) pos += perimeter
    
    // Start from top center, go clockwise
    val topCenterX = left + width / 2
    
    // Top edge (right half)
    val topRightStraight = straightWidth / 2
    if (pos < topRightStraight) {
        return Offset(topCenterX + pos, top)
    }
    pos -= topRightStraight
    
    // Top-right corner
    if (pos < cornerArc) {
        val angle = -PI.toFloat() / 2 + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            right - cr + cr * kotlin.math.cos(angle),
            top + cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Right edge
    if (pos < straightHeight) {
        return Offset(right, top + cr + pos)
    }
    pos -= straightHeight
    
    // Bottom-right corner
    if (pos < cornerArc) {
        val angle = 0f + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            right - cr + cr * kotlin.math.cos(angle),
            bottom - cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Bottom edge
    if (pos < straightWidth) {
        return Offset(right - cr - pos, bottom)
    }
    pos -= straightWidth
    
    // Bottom-left corner
    if (pos < cornerArc) {
        val angle = PI.toFloat() / 2 + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            left + cr + cr * kotlin.math.cos(angle),
            bottom - cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Left edge
    if (pos < straightHeight) {
        return Offset(left, bottom - cr - pos)
    }
    pos -= straightHeight
    
    // Top-left corner
    if (pos < cornerArc) {
        val angle = PI.toFloat() + (pos / cornerArc) * (PI.toFloat() / 2)
        return Offset(
            left + cr + cr * kotlin.math.cos(angle),
            top + cr + cr * kotlin.math.sin(angle)
        )
    }
    pos -= cornerArc
    
    // Top edge (left half)
    return Offset(left + cr + pos, top)
}

