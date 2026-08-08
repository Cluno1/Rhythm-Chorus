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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.SplashBackgroundOrbs
import chromahub.rhythm.app.shared.presentation.components.common.buildSplashBackdropShapes
import chromahub.rhythm.app.ui.theme.festive.FestiveConfig
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeEngine
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeType
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            neutralColor = neutralBackdropColor,
            sizeScale = 3f
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.rhythm_splash_logo),
                    contentDescription = stringResource(R.string.updates_rhythm_logo_cd),
                    modifier = Modifier
                        .size(100.dp)
                )

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
