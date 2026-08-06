package chromahub.rhythm.app.features.local.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeProvider
import chromahub.rhythm.app.ui.theme.festive.FestiveConfig
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeEngine
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeType
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.res.stringResource
import chromahub.rhythm.app.util.windowScreenWidthDp
import chromahub.rhythm.app.util.windowScreenHeightDp

@Composable
fun SplashScreen(
    musicViewModel: MusicViewModel,
    onMediaScanComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }

    // Festive theme configuration
    val festiveEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveTypeString by appSettings.festiveThemeType.collectAsState()
    val festiveAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()
    val appMode by appSettings.appMode.collectAsState()
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
    val expressiveShapePreset by appSettings.expressiveShapePreset.collectAsState()
    val expressiveShapeAlbumArt by appSettings.expressiveShapeAlbumArt.collectAsState()
    val expressiveShapePlayerArt by appSettings.expressiveShapePlayerArt.collectAsState()
    val expressiveShapeSongArt by appSettings.expressiveShapeSongArt.collectAsState()
    val expressiveShapePlaylistArt by appSettings.expressiveShapePlaylistArt.collectAsState()
    val expressiveShapeArtistArt by appSettings.expressiveShapeArtistArt.collectAsState()
    val expressiveShapePlayerControls by appSettings.expressiveShapePlayerControls.collectAsState()
    val expressiveShapeMiniPlayer by appSettings.expressiveShapeMiniPlayer.collectAsState()
    val screenWidthDp = windowScreenWidthDp()
    val screenHeightDp = windowScreenHeightDp()

    val festiveConfig = remember(festiveEnabled, festiveTypeString, festiveAutoDetect) {
        FestiveConfig(
            enabled = festiveEnabled,
            type = try {
                FestiveThemeType.valueOf(festiveTypeString)
            } catch (e: IllegalArgumentException) {
                FestiveThemeType.NONE
            },
            autoDetect = festiveAutoDetect
        )
    }
    val activeFestiveTheme = FestiveThemeEngine.getActiveFestiveTheme(festiveConfig)

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showLoader by remember { mutableStateOf(false) }
    var exitSplash by remember { mutableStateOf(false) }

    // Animatable values
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.8f) }
    val loaderOffsetY = remember { Animatable(100f) } // Start below screen
    val loaderAlpha = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }

    // Subtle pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    val primaryBackdropColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val secondaryBackdropColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val tertiaryBackdropColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    val neutralBackdropColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    val expressiveShapePalette = remember(
        expressiveShapesEnabled,
        expressiveShapePreset,
        expressiveShapeAlbumArt,
        expressiveShapePlayerArt,
        expressiveShapeSongArt,
        expressiveShapePlaylistArt,
        expressiveShapeArtistArt,
        expressiveShapePlayerControls,
        expressiveShapeMiniPlayer
    ) {
        buildList {
            if (expressiveShapesEnabled) {
                addAll(
                    listOf(
                        expressiveShapeAlbumArt,
                        expressiveShapePlayerArt,
                        expressiveShapeSongArt,
                        expressiveShapePlaylistArt,
                        expressiveShapeArtistArt,
                        expressiveShapePlayerControls,
                        expressiveShapeMiniPlayer
                    )
                )
            } else {
                addAll(
                    when (expressiveShapePreset) {
                        "FRIENDLY" -> listOf("CLOVER_8_LEAF", "HEART", "OVAL", "CIRCLE")
                        "MODERN" -> listOf("SLANTED", "DIAMOND", "PENTAGON", "SQUARE")
                        "PLAYFUL" -> listOf("FLOWER", "SOFT_BURST", "SUNNY", "COOKIE_6")
                        "ORGANIC" -> listOf("CLOVER_4_LEAF", "FLOWER", "BUN", "OVAL")
                        "GEOMETRIC" -> listOf("SQUARE", "DIAMOND", "PENTAGON", "CIRCLE")
                        "RETRO" -> listOf("PIXEL_CIRCLE", "PIXEL_TRIANGLE", "SQUARE")
                        "CHEERFUL" -> listOf("FLOWER", "SUNNY", "PUFFY", "HEART")
                        else -> listOf("GHOSTISH", "BUN", "CLOVER_8_LEAF", "COOKIE_12")
                    }
                )
            }
        }.distinct().filter { it.isNotBlank() }
    }

    val splashBackdropShapes = remember(
        screenWidthDp,
        screenHeightDp,
        expressiveShapePalette,
        primaryBackdropColor,
        secondaryBackdropColor,
        tertiaryBackdropColor,
        neutralBackdropColor
    ) {
        val launchSeed = System.nanoTime()
        buildSplashBackdropShapes(
            seed = launchSeed.toInt(),
            shapeIds = expressiveShapePalette,
            preset = expressiveShapePreset,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            primaryColor = primaryBackdropColor,
            secondaryColor = secondaryBackdropColor,
            tertiaryColor = tertiaryBackdropColor,
            neutralColor = neutralBackdropColor
        )
    }

    // Monitor media scanning completion
    val isInitialized by musicViewModel.isInitialized.collectAsState()

    val statusText = remember(appMode, isInitialized) {
        when {
            isInitialized -> context.getString(R.string.splash_ready)
            appMode == AppMode.STREAMING.name -> context.getString(R.string.splash_loading_streaming)
            else -> context.getString(R.string.splash_loading)
        }
    }

    // Entrance animation
    LaunchedEffect(Unit) {
        // Start entrance animations immediately; avoid artificial delays so the system
        // (Android lifecycle) can control visible timing during cold starts.
        showContent = true
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(800, easing = EaseOut))
        }
        launch {
            contentScale.animateTo(1f, animationSpec = spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ))
        }

        // Show loader without extra holds
        showLoader = true
        launch {
            loaderOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )
        }
        loaderAlpha.animateTo(1f, animationSpec = tween(400))
    }

    // Exit animation when ready
    LaunchedEffect(isInitialized) {
        if (isInitialized && !exitSplash) {
            // Proceed immediately once initialization completes; avoid additional holds
            exitSplash = true

            launch {
                exitScale.animateTo(0.9f, animationSpec = tween(400))
            }
            launch {
                exitAlpha.animateTo(0f, animationSpec = tween(400))
            }

            // Notify host right away. The activity/host can decide whether to keep
            // the splash visible longer if necessary.
            onMediaScanComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer {
                scaleX = exitScale.value
                scaleY = exitScale.value
                alpha = exitAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        SplashBackgroundOrbs(
            shapes = splashBackdropShapes
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha.value
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
                .padding(horizontal = 32.dp)
        ) {
            // Logo and App name on same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.rhythm_splash_logo),
                    contentDescription = stringResource(R.string.updates_rhythm_logo_cd),
                    modifier = Modifier
                        .size(100.dp)
                )

                // App name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = context.getString(R.string.common_rhythm),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (appMode == AppMode.STREAMING.name) {
                        Text(
                            text = stringResource(R.string.splashscreen_go),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Tagline
            Text(
                text = context.getString(R.string.splash_tagline),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // Festive greeting
            if (festiveEnabled && activeFestiveTheme != FestiveThemeType.NONE) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = when (activeFestiveTheme) {
                        FestiveThemeType.CHRISTMAS -> context.getString(R.string.festive_greeting_christmas)
                        FestiveThemeType.NEW_YEAR -> context.getString(R.string.festive_greeting_new_year)
                        FestiveThemeType.HALLOWEEN -> context.getString(R.string.festive_greeting_halloween)
                        FestiveThemeType.VALENTINES -> context.getString(R.string.festive_greeting_valentines)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Loading indicator at bottom, sliding up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
                .graphicsLayer {
                    translationY = loaderOffsetY.value
                    alpha = loaderAlpha.value
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (showLoader) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    // Modern loading dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        800,
                                        delayMillis = index * 200,
                                        easing = EaseInOut
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotAlpha$index"
                            )

                            Surface(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(dotAlpha),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SplashBackgroundOrbs(
    shapes: List<SplashBackdropShape>
) {
    shapes.forEachIndexed { index, shape ->
        SplashBackdropShapeItem(
            shape = shape,
            index = index
        )
    }
}

@Composable
private fun BoxScope.SplashBackdropShapeItem(
    shape: SplashBackdropShape,
    index: Int
) {
    val transition = rememberInfiniteTransition(label = "splashBackdropShape_$index")
    val pulse by transition.animateFloat(
        initialValue = shape.pulseMin,
        targetValue = shape.pulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.pulseDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapePulse_$index"
    )
    val driftX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.driftXDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeDriftX_$index"
    )
    val driftY by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.driftYDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeDriftY_$index"
    )
    val rotation by transition.animateFloat(
        initialValue = -shape.rotationDegrees,
        targetValue = shape.rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(shape.rotationDurationMs, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shapeRotation_$index"
    )

    Surface(
        modifier = Modifier
            .align(shape.alignment)
            .offset(x = shape.offsetX, y = shape.offsetY)
            .size(shape.size)
            .graphicsLayer {
                translationX = driftX * shape.driftXPx
                translationY = driftY * shape.driftYPx
                scaleX = pulse
                scaleY = pulse
                rotationZ = rotation
                alpha = shape.alpha
            },
        shape = shape.shape,
        color = shape.color
    ) {}
}

private data class SplashBackdropShape(
    val shape: Shape,
    val alignment: Alignment,
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp,
    val size: androidx.compose.ui.unit.Dp,
    val color: androidx.compose.ui.graphics.Color,
    val alpha: Float,
    val driftXPx: Float,
    val driftYPx: Float,
    val pulseMin: Float,
    val pulseMax: Float,
    val pulseDurationMs: Int,
    val driftXDurationMs: Int,
    val driftYDurationMs: Int,
    val rotationDegrees: Float,
    val rotationDurationMs: Int
)

private fun buildSplashBackdropShapes(
    seed: Int,
    shapeIds: List<String>,
    preset: String?,
    screenWidthDp: Int,
    screenHeightDp: Int,
    primaryColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    tertiaryColor: androidx.compose.ui.graphics.Color,
    neutralColor: androidx.compose.ui.graphics.Color
): List<SplashBackdropShape> {
    val random = Random(seed)
    val colors = listOf(primaryColor, secondaryColor, tertiaryColor, neutralColor)
    val shapePool = if (shapeIds.isNotEmpty()) shapeIds else listOf("CIRCLE")
    val shortestSideDp = minOf(screenWidthDp, screenHeightDp)
    val isCompact = shortestSideDp < 380

    // Preset-driven visual tuning
    val (count, sizeMin, sizeRange, rotMin, rotRange, pulseBaseMin, pulseBaseRange) = when (preset) {
        "MODERN" -> listOf(10, 56, 80, 0, 8, 0.96f, 0.04f)
        "FRIENDLY" -> listOf(10, 88, 110, 6, 18, 0.88f, 0.12f)
        "PLAYFUL" -> listOf(12, 72, 120, 4, 20, 0.86f, 0.18f)
        "GEOMETRIC" -> listOf(9, 48, 72, 0, 6, 0.96f, 0.03f)
        "ORGANIC" -> listOf(11, 84, 130, 6, 22, 0.86f, 0.14f)
        else -> listOf(11, 74, 138, 8, 16, 0.86f, 0.1f)
    }.let { tpl ->
        // convert typed list to strongly typed tuple
        val c = tpl[0] as Int
        val smin = tpl[1] as Int
        val srange = tpl[2] as Int
        val rmin = tpl[3] as Int
        val rrange = tpl[4] as Int
        val pmin = tpl[5] as Float
        val prange = tpl[6] as Float
        val adjustedCount = when {
            shortestSideDp < 340 -> (c - 3).coerceAtLeast(6)
            shortestSideDp < 400 -> (c - 1).coerceAtLeast(6)
            else -> c
        }
        val adjustedSizeMin = when {
            shortestSideDp < 340 -> (smin - 18).coerceAtLeast(42)
            shortestSideDp < 400 -> (smin - 8).coerceAtLeast(42)
            else -> smin
        }
        val adjustedSizeRange = when {
            shortestSideDp < 340 -> (srange - 28).coerceAtLeast(56)
            shortestSideDp < 400 -> (srange - 12).coerceAtLeast(56)
            else -> srange
        }
        Quad(adjustedCount, adjustedSizeMin, adjustedSizeRange, rmin, rrange, pmin, prange)
    }

    val anchors = listOf(
        Pair(0.12f, 0.14f),
        Pair(0.88f, 0.16f),
        Pair(0.14f, 0.84f),
        Pair(0.86f, 0.86f),
        Pair(0.06f, 0.48f),
        Pair(0.94f, 0.52f),
        Pair(0.24f, 0.10f),
        Pair(0.76f, 0.10f),
        Pair(0.18f, 0.92f),
        Pair(0.82f, 0.90f)
    )

    // place shapes avoiding overlaps by rejection sampling in normalized space (0..1)
    val placed = mutableListOf<Pair<Pair<Float, Float>, Float>>() // ((x,y), radius)
    val results = mutableListOf<SplashBackdropShape>()

    // virtual scale for normalizing dp sizes to 0..1 (higher => smaller normalized radii)
    val virtualScale = 420f
    val offsetLimitX = if (isCompact) 18 else 32
    val offsetLimitY = if (isCompact) 22 else 44
    val centerClearMin = if (isCompact) 0.20f else 0.24f
    val centerClearMax = 1f - centerClearMin

    var attemptsTotal = 0
    var placedCount = 0
    var i = 0
    while (i < count && attemptsTotal < count * 30) {
        attemptsTotal++

        val anchor = anchors[random.nextInt(anchors.size)]
        val jitterScale = if (isCompact) 0.05f else 0.08f
        val biasX = (anchor.first + (random.nextFloat() - 0.5f) * jitterScale).coerceIn(0.08f, 0.92f)
        val biasY = (anchor.second + (random.nextFloat() - 0.5f) * jitterScale).coerceIn(0.08f, 0.92f)

        if (biasX in centerClearMin..centerClearMax && biasY in centerClearMin..centerClearMax) {
            continue
        }

        val sizeDp = (sizeMin + random.nextInt(0, sizeRange)).dp
        val sizeVal = sizeDp.value
        val radiusNorm = (sizeVal / virtualScale) / 2f

        // normalized position in 0..1
        val px = biasX
        val py = biasY

        // check overlap
        val safe = placed.all { (pos, r) ->
            val dx = pos.first - px
            val dy = pos.second - py
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            dist > (r + radiusNorm) * 1.05f
        }

        if (!safe) {
            // too close, try again
            if (attemptsTotal % 6 == 0) {
                // occasionally relax by skipping this slot
                i++
            }
            continue
        }

        // accept placement
        placed.add(Pair(Pair(px, py), radiusNorm))

        val alignment = BiasAlignment(px * 2f - 1f, py * 2f - 1f)
        val offsetX = random.nextInt(-offsetLimitX, offsetLimitX).dp
        val offsetY = random.nextInt(-offsetLimitY, offsetLimitY).dp
        val driftXPx = random.nextInt(if (isCompact) 4 else 6, if (isCompact) 22 else 30).toFloat()
        val driftYPx = random.nextInt(if (isCompact) 4 else 6, if (isCompact) 22 else 30).toFloat()
        val pulseMin = pulseBaseMin + random.nextFloat() * pulseBaseRange
        val pulseMax = pulseMin + 0.04f + random.nextFloat() * 0.06f
        val pulseDurationMs = random.nextInt(4200, 10800)
        val driftXDurationMs = random.nextInt(6800, 14800)
        val driftYDurationMs = random.nextInt(7200, 15600)
        val rotationDegrees = (rotMin + random.nextInt(0, rotRange)).toFloat()
        val rotationDurationMs = random.nextInt(7600, 16200)
        val selectedShapeId = shapePool[random.nextInt(shapePool.size)]

        results.add(
            SplashBackdropShape(
                shape = ExpressiveShapeProvider.getShapeById(selectedShapeId, ExpressiveShapes.ExtraLarge),
                alignment = alignment,
                offsetX = offsetX,
                offsetY = offsetY,
                size = sizeDp,
                color = colors[random.nextInt(colors.size)],
                alpha = 0.92f - (placedCount * 0.04f),
                driftXPx = driftXPx,
                driftYPx = driftYPx,
                pulseMin = pulseMin,
                pulseMax = pulseMax,
                pulseDurationMs = pulseDurationMs,
                driftXDurationMs = driftXDurationMs,
                driftYDurationMs = driftYDurationMs,
                rotationDegrees = rotationDegrees,
                rotationDurationMs = rotationDurationMs
            )
        )

        placedCount++
        i++
    }

    // If nothing placed (shouldn't happen), fallback to a simple single shape
    if (results.isEmpty()) {
        val selectedShapeId = shapePool[random.nextInt(shapePool.size)]
        results.add(
            SplashBackdropShape(
                shape = ExpressiveShapeProvider.getShapeById(selectedShapeId, ExpressiveShapes.ExtraLarge),
                alignment = Alignment.TopCenter,
                offsetX = 0.dp,
                offsetY = if (isCompact) 12.dp else 20.dp,
                size = (sizeMin.coerceAtLeast(44)).dp,
                color = colors[0],
                alpha = 0.82f,
                driftXPx = if (isCompact) 8f else 12f,
                driftYPx = if (isCompact) 8f else 12f,
                pulseMin = pulseBaseMin,
                pulseMax = pulseBaseMin + 0.06f,
                pulseDurationMs = 6000,
                driftXDurationMs = 10000,
                driftYDurationMs = 11000,
                rotationDegrees = rotMin.toFloat(),
                rotationDurationMs = 10000
            )
        )
    }

    return results
}

// small helper data holder emulation (no imports required)
private data class Quad(
    val count: Int,
    val sizeMin: Int,
    val sizeRange: Int,
    val rotMin: Int,
    val rotRange: Int,
    val pulseBaseMin: Float,
    val pulseBaseRange: Float
)
