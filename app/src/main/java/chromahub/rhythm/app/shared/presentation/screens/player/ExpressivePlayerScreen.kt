package chromahub.rhythm.app.shared.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.ExtraControlBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AddToPlaylistBottomSheet

import chromahub.rhythm.app.shared.presentation.components.bottomsheets.ArtistBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.PlaybackBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.QueueBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaybackPitchDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaybackSpeedDialog
import chromahub.rhythm.app.shared.presentation.components.player.SleepTimerBottomSheetNew
import chromahub.rhythm.app.shared.presentation.components.lyrics.LyricsEditorBottomSheet
import chromahub.rhythm.app.shared.presentation.components.lyrics.SyncedLyricsView
import chromahub.rhythm.app.shared.presentation.components.lyrics.WordByWordLyricsView
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.features.local.presentation.navigation.Screen
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.PlaybackLocation
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.common.AutoScrollingTextOnDemand
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.FixedHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.M3LinearLoader
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.PlaybackBufferingLoader
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.shared.presentation.components.common.WaveSlider
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.common.M3CircularLoader
import androidx.compose.material3.ContainedLoadingIndicator
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.util.M3ImageUtils
import chromahub.rhythm.app.util.LrcUtils
import chromahub.rhythm.app.network.CanvasArtwork
import chromahub.rhythm.app.shared.presentation.components.player.CanvasArtworkPlayer
import chromahub.rhythm.app.util.SemanticLyrics
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import kotlin.math.abs
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    canvasArtwork: CanvasArtwork? = null,
    canvasLoading: Boolean = false,
    progress: () -> Float,
    currentTimeStr: String,
    totalTimeStr: String,
    queuePosition: Int,
    queueTotal: Int,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    showLyricsView: Boolean,
    showLyrics: Boolean,
    lyrics: LyricsData?,
    isLoadingLyrics: Boolean,
    onlineOnlyLyrics: Boolean,
    onLyricsSeek: ((Long) -> Unit)?,
    onRetryLyrics: () -> Unit,
    onShowLyricsEditor: () -> Unit,
    onPickLyricsFile: () -> Unit,
    isMediaLoading: Boolean,
    isSeeking: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLyrics: () -> Unit,
    onSongInfoClick: () -> Unit,
    onShowAlbumBottomSheet: () -> Unit,
    onShowArtistBottomSheet: () -> Unit,
    onMoreClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onBack: () -> Unit,
    location: PlaybackLocation?,
    appSettings: AppSettings,
    onOpenFullScreenLyrics: () -> Unit = {},
    swipeToDismissEnabled: Boolean = true,
    expansionFraction: Float = 1f,
    modifier: Modifier = Modifier
) {
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkScale"
    )

    val artworkCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 32.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkCornerRadius"
    )

    val playerArtworkShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.PLAYER_ART,
        RoundedCornerShape(artworkCornerRadius)
    )

    val playerControlShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.PLAYER_CONTROLS,
        CircleShape
    )

    val playerProgressStyle by appSettings.playerProgressStyle.collectAsState()
    val playerProgressThumbStyle by appSettings.playerProgressThumbStyle.collectAsState()
    val enhancedSeekingEnabled by appSettings.enhancedSeekingEnabled.collectAsState()
    val playerLyricsTextSize by appSettings.playerLyricsTextSize.collectAsState()
    val showLyricsTranslation by appSettings.showLyricsTranslation.collectAsState()
    val showLyricsRomanization by appSettings.showLyricsRomanization.collectAsState()
    val playerLyricsTransition by appSettings.playerLyricsTransition.collectAsState()
    val tapLyricsToFullScreen by appSettings.tapLyricsToFullScreen.collectAsState()
    val playerLyricsAlignment by appSettings.playerLyricsAlignment.collectAsState()
    val keepScreenOnLyrics by appSettings.keepScreenOnLyrics.collectAsState()

    val lyricsTextAlign = when (playerLyricsAlignment) {
        "START" -> TextAlign.Start
        "END" -> TextAlign.End
        else -> TextAlign.Center
    }

    val onTapLyricsView = if (tapLyricsToFullScreen) onOpenFullScreenLyrics else null
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    val progressValue = progress().coerceIn(0f, 1f)
    val currentTimeMs = (progressValue * (song?.duration ?: 0L)).toLong()
    val lyricsVisible = showLyricsView && showLyrics
    val showBuffering = isMediaLoading || isSeeking
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Keep screen awake while lyrics are visible
    val shouldKeepScreenOn = keepScreenOnLyrics && lyricsVisible
    val activity = context as? android.app.Activity
    DisposableEffect(shouldKeepScreenOn) {
        if (shouldKeepScreenOn && activity != null) {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Entry animation states - staggered
    var showHeader by remember { mutableStateOf(true) }
    var showAlbumArt by remember { mutableStateOf(true) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var showBottomButtons by remember { mutableStateOf(true) }

    val localEntranceFraction = if (swipeToDismissEnabled) {
        var animateEntrance by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateEntrance = true
        }
        animateFloatAsState(
            targetValue = if (animateEntrance) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "localEntranceFraction"
        ).value
    } else {
        expansionFraction
    }

    val line1Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.0f) / 0.5f).coerceIn(0f, 1f))
    val line2Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.1f) / 0.5f).coerceIn(0f, 1f))
    val line3Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.2f) / 0.5f).coerceIn(0f, 1f))
    val line4Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.3f) / 0.5f).coerceIn(0f, 1f))
    val line5Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.4f) / 0.5f).coerceIn(0f, 1f))
    val line6Fraction = androidx.compose.animation.core.FastOutSlowInEasing.transform(((localEntranceFraction - 0.5f) / 0.5f).coerceIn(0f, 1f))

    val line1Alpha = line1Fraction
    val line1TranslationY = with(LocalDensity.current) { -16.dp.toPx() * (1f - line1Fraction) }

    val line2Alpha = line2Fraction
    val line2TranslationY = with(LocalDensity.current) { 32.dp.toPx() * (1f - line2Fraction) }

    val line3Alpha = line3Fraction
    val line4Alpha = line4Fraction
    val line5Alpha = line5Fraction
    val line6Alpha = line6Fraction

    val artworkClipShape = if (lyricsVisible) {
        RoundedCornerShape(artworkCornerRadius)
    } else {
        playerArtworkShape
    }

    val controlsContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryPillColor = MaterialTheme.colorScheme.primaryContainer
    val primaryPillOnColor = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryButtonColor = MaterialTheme.colorScheme.secondaryContainer
    val secondaryButtonOnColor = MaterialTheme.colorScheme.onSecondaryContainer

    val configuration = LocalConfiguration.current
    val isCompactWidth = configuration.screenWidthDp < 360
    val isCompactHeight = configuration.screenHeightDp < 640
    val songTitle = song?.title ?: stringResource(R.string.unknown_track)
    val songArtist = song?.artist ?: stringResource(R.string.unknown_artist)
    val titleLength = songTitle.length
    val titleLetterSpacing = when {
        isCompactWidth || titleLength > 32 -> (-0.6).sp
        titleLength > 24 -> (-1.0).sp
        else -> (-1.5).sp
    }
    val titleTextStyle = when {
        isCompactWidth -> MaterialTheme.typography.headlineSmall
        isCompactHeight -> MaterialTheme.typography.headlineMedium
        titleLength > 32 -> MaterialTheme.typography.headlineSmall
        titleLength > 24 -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.displaySmall
    }.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = titleLetterSpacing
    )

    val coroutineScope = rememberCoroutineScope()
    val screenHeightPx = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }

    var swipeOffsetY by remember { mutableStateOf(0f) }
    var isDraggingSwipe by remember { mutableStateOf(false) }
    var isSwipeMinimizing by remember { mutableStateOf(false) }

    val swipeDismissThreshold = screenHeightPx * 0.16f
    val swipeDismissTarget = screenHeightPx * 1.05f
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetY,
        animationSpec = when {
            isDraggingSwipe -> spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
            isSwipeMinimizing -> tween(durationMillis = 160, easing = EaseInOut)
            else -> spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "rhythmPlayerSwipeOffset"
    )

    val swipeCornerRadius by animateFloatAsState(
        targetValue = when {
            isDraggingSwipe || isSwipeMinimizing -> 64f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "swipeCornerRadius"
    )
    val clampedSwipeCornerRadius = swipeCornerRadius.coerceAtLeast(0f)

    val swipeMinimizeModifier = if (swipeToDismissEnabled) {
        modifier
            .graphicsLayer {
                val swipeProgress = (animatedSwipeOffset / screenHeightPx).coerceIn(0f, 1f)
                translationY = animatedSwipeOffset
                val scaleTarget = 1f - (swipeProgress * 0.15f)
                scaleX = scaleTarget
                scaleY = scaleTarget
                alpha = (1f - (swipeProgress * 1.5f)).coerceIn(0f, 1f)
                clip = true
                shape = RoundedCornerShape(clampedSwipeCornerRadius.dp)
            }
            .pointerInput(screenHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDraggingSwipe = true
                        isSwipeMinimizing = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f) {
                            change.consume()
                            val currentSwipeProgress = (swipeOffsetY / screenHeightPx).coerceIn(0f, 1f)
                            val dragResistance = (1f - (currentSwipeProgress * 0.5f)).coerceAtLeast(0.4f)
                            swipeOffsetY = (swipeOffsetY + dragAmount * dragResistance).coerceIn(0f, swipeDismissTarget)
                        }
                    },
                    onDragEnd = {
                        isDraggingSwipe = false
                        if (swipeOffsetY > swipeDismissThreshold) {
                            HapticUtils.performHapticFeedback(
                                context,
                                haptic,
                                HapticType.HEAVY
                            )
                            isSwipeMinimizing = true
                            swipeOffsetY = swipeDismissTarget
                            coroutineScope.launch {
                                delay(180)
                                onBack()
                                isSwipeMinimizing = false
                                swipeOffsetY = 0f
                            }
                        } else {
                            isSwipeMinimizing = false
                            swipeOffsetY = 0f
                        }
                    },
                    onDragCancel = {
                        isDraggingSwipe = false
                        isSwipeMinimizing = false
                        swipeOffsetY = 0f
                    }
                )
            }
    } else {
        modifier
    }

    FixedHeaderScreen(
        title = "",
        showBackButton = true,
        onBackClick = onBack,
        actions = {
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = line1Alpha
                            translationY = line1TranslationY
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    song?.album?.takeIf { it.isNotBlank() }?.let { albumName ->
                        AutoScrollingTextOnDemand(
                            text = albumName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            gradientEdgeColor = MaterialTheme.colorScheme.surface,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .widthIn(max = 140.dp)
                                .clickable { onShowAlbumBottomSheet() },
                            respectGlobalSetting = true
                        )
                    }
                    ExpressiveButtonGroup() {
                        ExpressiveGroupButton(
                            onClick = onSongInfoClick,
                            isStart = true,
                            isEnd = false,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = stringResource(R.string.action_song_info),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        ExpressiveGroupButton(
                            onClick = onMoreClick,
                            isStart = false,
                            isEnd = true,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.More,
                                contentDescription = stringResource(R.string.expressiveplayerscreen_more),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        screenModifier = swipeMinimizeModifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) { contentModifier ->
        BoxWithConstraints(
            modifier = contentModifier
                .fillMaxSize()
                .padding(
                    top = 8.dp,
                    bottom = 0.dp
                )
        ) {
            val containerMaxWidth = maxWidth
            val containerMaxHeight = maxHeight
            val isPortraitLocal = containerMaxHeight > containerMaxWidth

            val controlButtonSize = if (isPortraitLocal) {
                val base = (containerMaxWidth * 0.2f)
                if (isCompactHeight) base.coerceIn(48.dp, 56.dp) else base.coerceIn(48.dp, 80.dp)
            } else {
                if (isCompactHeight) 48.dp else 72.dp
            }

            val smallControlSize = if (isPortraitLocal) {
                val base = (containerMaxWidth * 0.16f)
                if (isCompactHeight) base.coerceIn(32.dp, 48.dp) else base.coerceIn(40.dp, 64.dp)
            } else {
                if (isCompactHeight) 40.dp else 56.dp
            }

            val isTablet = configuration.screenWidthDp >= 600
            val isLandscapeTablet = isTablet && configuration.screenWidthDp > configuration.screenHeightDp

            var artworkOffsetX by remember { mutableStateOf(0f) }
            val artworkSwipeThreshold = 140f
            val artworkTranslationX by animateFloatAsState(
                targetValue = artworkOffsetX.coerceIn(-200f, 200f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "artworkTranslationX"
            )

            val artworkContent = @Composable { modifier: Modifier ->
                AnimatedVisibility(
                    visible = showAlbumArt,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = modifier
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer {
                                alpha = line2Alpha
                                translationY = line2TranslationY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = lyricsVisible,
                            transitionSpec = {
                                val enterTransition = when (playerLyricsTransition) {
                                    1 -> fadeIn(tween(400, easing = EaseInOut))
                                    2 -> fadeIn(tween(350, easing = EaseInOut)) + scaleIn(tween(350, easing = EaseInOut), initialScale = 0.92f)
                                    3 -> fadeIn(tween(350, easing = EaseInOut)) + slideInVertically(tween(350, easing = EaseInOut)) { it / 2 }
                                    else -> fadeIn(tween(350, easing = EaseInOut)) + slideInVertically(tween(350, easing = EaseInOut)) { -it / 2 }
                                }
                                val exitTransition = when (playerLyricsTransition) {
                                    1 -> fadeOut(tween(300, easing = EaseInOut))
                                    2 -> fadeOut(tween(250, easing = EaseInOut)) + scaleOut(tween(250, easing = EaseInOut), targetScale = 0.92f)
                                    3 -> fadeOut(tween(250, easing = EaseInOut)) + slideOutVertically(tween(250, easing = EaseInOut)) { it / 2 }
                                    else -> fadeOut(tween(250, easing = EaseInOut)) + slideOutVertically(tween(250, easing = EaseInOut)) { -it / 2 }
                                }
                                enterTransition togetherWith exitTransition
                            },
                            label = "lyricsViewTransition",
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { targetLyricsVisible ->
                            if (targetLyricsVisible) {
                                RhythmPlayerLyricsPanel(
                                    lyrics = lyrics,
                                    isLoadingLyrics = isLoadingLyrics,
                                    onlineOnlyLyrics = onlineOnlyLyrics,
                                    currentTimeMs = currentTimeMs,
                                    onLyricsSeek = onLyricsSeek,
                                    onTapLyricsView = onTapLyricsView,
                                    textSizeMultiplier = playerLyricsTextSize,
                                    onRetryLyrics = onRetryLyrics,
                                    onShowLyricsEditor = onShowLyricsEditor,
                                    onPickLyricsFile = onPickLyricsFile,
                                    showTranslation = showLyricsTranslation,
                                    showRomanization = showLyricsRomanization,
                                    textAlignment = lyricsTextAlign,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .padding(horizontal = if (isCompactWidth) 16.dp else 24.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = if (isCompactWidth) 12.dp else 24.dp)
                                        .fillMaxSize(if (isCompactHeight) 0.78f else 0.88f)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            scaleX = artworkScale
                                            scaleY = artworkScale
                                            shadowElevation = if (isPlaying) 0.dp.toPx() else 0.dp.toPx()
                                            translationX = artworkTranslationX
                                            shape = artworkClipShape
                                            clip = true
                                        }
                                        .pointerInput(showLyrics, lyricsVisible) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptic,
                                                        HapticType.HEAVY
                                                    )
                                                    onPlayPause()
                                                },
                                                onTap = {
                                                    if (showLyrics) {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptic,
                                                            HapticType.LIGHT
                                                        )
                                                        onToggleLyrics()
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (artworkOffsetX < -artworkSwipeThreshold) {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptic,
                                                            HapticType.HEAVY
                                                        )
                                                        onSkipNext()
                                                    } else if (artworkOffsetX > artworkSwipeThreshold) {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptic,
                                                            HapticType.HEAVY
                                                        )
                                                        onSkipPrevious()
                                                    }
                                                    artworkOffsetX = 0f
                                                },
                                                onDragCancel = { artworkOffsetX = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    artworkOffsetX += dragAmount.x
                                                }
                                            )
                                        }
                                ) {
                                    M3ImageUtils.M3MediaImage(
                                        data = song?.artworkUri,
                                        contentDescription = stringResource(R.string.content_desc_album_artwork),
                                        modifier = Modifier.fillMaxSize(),
                                        shape = artworkClipShape,
                                        type = M3PlaceholderType.TRACK,
                                        name = song?.title,
                                        expressiveShape = playerArtworkShape
                                    )

                                    if (canvasArtwork?.preferredAnimationUrl != null) {
                                        CanvasArtworkPlayer(
                                            primaryUrl = canvasArtwork.animated,
                                            fallbackUrl = canvasArtwork.videoUrl,
                                            isPlaying = isPlaying,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(artworkClipShape)
                                        )
                                    }

                                    // Canvas loading badge
                                    if (canvasLoading && canvasArtwork == null) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                                                    androidx.compose.foundation.shape.RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                M3CircularLoader(
                                                    modifier = Modifier.size(12.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    strokeWidth = 2f
                                                )
                                                Text(
                                                    text = "Canvas",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val controlsContent = @Composable {
                AnimatedVisibility(
                    visible = showPlayerControls,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isCompactWidth) 12.dp else 24.dp,
                                end = if (isCompactWidth) 12.dp else 24.dp,
                                bottom = if (isCompactHeight) 8.dp else 16.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = line3Alpha
                                }
                                .padding(bottom = if (isCompactHeight) 8.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                AutoScrollingTextOnDemand(
                                    text = songTitle,
                                    style = titleTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                    gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth(),
                                    respectGlobalSetting = true
                                )
                                AutoScrollingTextOnDemand(
                                    text = songArtist,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    gradientEdgeColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onShowArtistBottomSheet() },
                                    respectGlobalSetting = true
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            val lyricsContainerColor by animateColorAsState(
                                targetValue = if (showLyricsView) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "LyricsContainerColor"
                            )
                            val lyricsContentColor by animateColorAsState(
                                targetValue = if (showLyricsView) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "LyricsContentColor"
                            )
                            val favContainerColor by animateColorAsState(
                                targetValue = if (isFavorite) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "FavContainerColor"
                            )
                            val favContentColor by animateColorAsState(
                                targetValue = if (isFavorite) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "FavContentColor"
                            )

                            ExpressiveButtonGroup() {
                                ExpressiveGroupButton(
                                    onClick = onToggleLyrics,
                                    onLongClick = onOpenFullScreenLyrics,
                                    isStart = true,
                                    isEnd = false,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = lyricsContainerColor,
                                        contentColor = lyricsContentColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Player.Lyrics,
                                        contentDescription = stringResource(R.string.player_chip_lyrics),
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                                ExpressiveGroupButton(
                                    onClick = onToggleFavorite,
                                    isStart = false,
                                    isEnd = true,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = favContainerColor,
                                        contentColor = favContentColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) RhythmIcons.FavoriteFilled else RhythmIcons.Favorite,
                                        contentDescription = stringResource(R.string.cd_toggle_favorite),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = controlsContainerColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(if (isCompactWidth) 12.dp else 20.dp)
                            ) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .graphicsLayer {
                                             alpha = line4Alpha
                                         },
                                     horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = onPlayPause,
                                        shape = CircleShape,
                                        color = primaryPillColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(controlButtonSize)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (showBuffering) {
                                                PlaybackBufferingLoader(
                                                    modifier = Modifier.size(40.dp),
                                                    color = primaryPillOnColor
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                                    style = MaterialTheme.typography.headlineMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = if (isCompactWidth) 20.sp else MaterialTheme.typography.headlineMedium.fontSize
                                                    ),
                                                    color = primaryPillOnColor
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        onClick = onSkipNext,
                                        shape = playerControlShape,
                                        color = secondaryButtonColor,
                                        modifier = Modifier.size(controlButtonSize)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Player.SkipNext,
                                            contentDescription = stringResource(R.string.cd_next_track),
                                            modifier = Modifier.padding(if (isCompactWidth) 16.dp else 24.dp),
                                            tint = secondaryButtonOnColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .graphicsLayer {
                                             alpha = line5Alpha
                                         },
                                     horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = onSkipPrevious,
                                        shape = playerControlShape,
                                        color = secondaryButtonColor,
                                        modifier = Modifier.size(controlButtonSize)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Player.SkipPrevious,
                                            contentDescription = stringResource(R.string.cd_previous_track),
                                            modifier = Modifier.padding(if (isCompactWidth) 16.dp else 24.dp),
                                            tint = secondaryButtonOnColor
                                        )
                                    }

                                    val canSeek = (song?.duration ?: 0L) > 0L

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (showBuffering) {
                                            M3LinearLoader(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                            )
                                        } else if (playerProgressStyle == "WAVY") {
                                            WaveSlider(
                                                value = if (isScrubbing && enhancedSeekingEnabled) scrubProgress else progressValue,
                                                onValueChange = { newValue ->
                                                    if (canSeek && enhancedSeekingEnabled) {
                                                        isScrubbing = true
                                                        scrubProgress = newValue
                                                    } else if (canSeek) {
                                                        onSeek(newValue)
                                                    }
                                                },
                                                onValueChangeFinished = {
                                                    if (canSeek && enhancedSeekingEnabled && isScrubbing) {
                                                        onSeek(scrubProgress)
                                                        isScrubbing = false
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = canSeek,
                                                isPlaying = isPlaying,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.2f
                                                ),
                                                thumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            val progressStyle = try {
                                                ProgressStyle.valueOf(playerProgressStyle)
                                            } catch (e: IllegalArgumentException) {
                                                ProgressStyle.NORMAL
                                            }
                                            val thumbStyle = try {
                                                ThumbStyle.valueOf(playerProgressThumbStyle)
                                            } catch (e: IllegalArgumentException) {
                                                ThumbStyle.CIRCLE
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledProgressBar(
                                                    progress = progressValue,
                                                    style = progressStyle,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    progressColor = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.2f
                                                    ),
                                                    height = when (progressStyle) {
                                                        ProgressStyle.THIN -> 2.dp
                                                        ProgressStyle.THICK -> 12.dp
                                                        else -> 8.dp
                                                    },
                                                    isPlaying = isPlaying,
                                                    showThumb = thumbStyle != ThumbStyle.NONE,
                                                    thumbStyle = thumbStyle,
                                                    thumbSize = 14.dp,
                                                    waveAmplitudeWhenPlaying = 3.dp,
                                                    waveLength = 60.dp
                                                )

                                                Slider(
                                                    value = progressValue,
                                                    onValueChange = { onSeek(it) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    enabled = canSeek,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.Transparent,
                                                        activeTrackColor = Color.Transparent,
                                                        inactiveTrackColor = Color.Transparent
                                                    )
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = currentTimeStr,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = totalTimeStr,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val bottomButtonsContent = @Composable {
                AnimatedVisibility(
                    visible = showBottomButtons,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = line6Alpha
                            }
                            .padding(
                                start = if (isCompactWidth) 12.dp else 24.dp,
                                end = if (isCompactWidth) 12.dp else 24.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val deviceIcon = when {
                                location?.id?.startsWith("bt_") == true -> RhythmIcons.BluetoothFilled
                                location?.id == "wired_headset" -> RhythmIcons.HeadphonesFilled
                                location?.id == "speaker" -> RhythmIcons.SpeakerFilled
                                else -> RhythmIcons.Location
                            }
                            val queueLabel =
                                if (queueTotal > 0) stringResource(R.string.player_queue_format, queuePosition, queueTotal) else stringResource(R.string.player_queue)

                            Surface(
                                onClick = onDeviceClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = deviceIcon,
                                        contentDescription = stringResource(R.string.expressiveplayerscreen_device),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AutoScrollingTextOnDemand(
                                        text = location?.name ?: "Output",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.widthIn(max = if (isCompactWidth) 80.dp else 160.dp),
                                        respectGlobalSetting = true
                                    )
                                }
                            }

                            Surface(
                                onClick = onQueueClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Queue,
                                        contentDescription = stringResource(R.string.bottomsheet_queue),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AutoScrollingTextOnDemand(
                                        text = queueLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.widthIn(max = if (isCompactWidth) 80.dp else 160.dp),
                                        respectGlobalSetting = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLandscapeTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Artwork/Lyrics
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        artworkContent(Modifier.fillMaxSize())
                    }

                    // Right Side: Info, Controls, Buttons
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        controlsContent()
                        bottomButtonsContent()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    artworkContent(
                        Modifier
                            .weight(1f)
                            .padding(bottom = if (isCompactHeight) 12.dp else 24.dp)
                    )
                    controlsContent()
                    bottomButtonsContent()
                }
            }
        }
    }
}

@Composable
private fun RhythmPlayerLyricsPanel(
    lyrics: LyricsData?,
    isLoadingLyrics: Boolean,
    onlineOnlyLyrics: Boolean,
    currentTimeMs: Long,
    onLyricsSeek: ((Long) -> Unit)?,
    onTapLyricsView: (() -> Unit)? = null,
    textSizeMultiplier: Float,
    onRetryLyrics: () -> Unit,
    onShowLyricsEditor: () -> Unit,
    onPickLyricsFile: () -> Unit,
    showTranslation: Boolean,
    showRomanization: Boolean,
    textAlignment: TextAlign,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasLyrics = lyrics?.hasLyrics() == true && lyrics.isErrorMessage().not()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingLyrics -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    ContainedLoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = context.getString(R.string.player_loading_lyrics),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            !hasLyrics -> {
                val message = if (onlineOnlyLyrics) {
                    stringResource(R.string.lyrics_currently_no_lyrics)
                } else {
                    stringResource(R.string.lyrics_no_lyrics)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = RhythmIcons.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = textAlignment
                    )
                    if (!isLoadingLyrics) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpressiveButtonGroup() {
                            ExpressiveGroupButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptic,
                                        HapticType.HEAVY
                                    )
                                    onRetryLyrics()
                                },
                                isStart = true,
                                isEnd = false
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.updates_retry))
                            }
                            ExpressiveGroupButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptic,
                                        HapticType.HEAVY
                                    )
                                    onShowLyricsEditor()
                                },
                                isStart = false,
                                isEnd = false
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Player.Lyrics,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.button_add))
                            }
                            ExpressiveGroupButton(
                                onClick = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptic,
                                        HapticType.HEAVY
                                    )
                                    onPickLyricsFile()
                                },
                                isStart = false,
                                isEnd = true
                            ) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("file_open", filled = true),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.expressiveplayerscreen_load))
                            }
                        }
                    }
                }
            }

            else -> {
                val localAppSettings = remember { chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context) }
                val translationAutoWord by localAppSettings.translationAutoWord.collectAsState()
                val wordByWordLyrics = remember(lyrics, translationAutoWord) {
                    lyrics.getWordByWordLyricsOrNull() ?: run {
                        if (translationAutoWord && lyrics.syncedLyrics != null) {
                            try {
                                val options = LrcUtils.LrcParserOptions(
                                    trim = true, multiLine = true, errorText = null, autoWordSync = true
                                )
                                val parsed = LrcUtils.parseLyrics(
                                    lyrics.syncedLyrics, audioMimeType = null,
                                    parserOptions = options, format = LrcUtils.LyricFormat.LRC
                                )
                                if (parsed is SemanticLyrics.SyncedLyrics) {
                                    LrcUtils.convertSemanticLyricsToWordByWord(parsed)
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }
                }

                if (wordByWordLyrics != null) {
                    WordByWordLyricsView(
                        wordByWordLyrics = wordByWordLyrics,
                        currentPlaybackTime = currentTimeMs,
                        modifier = Modifier.fillMaxSize(),
                        onSeek = onLyricsSeek,
                        onTapLyricsView = onTapLyricsView,
                        lyricsSource = lyrics.source,
                        textSizeMultiplier = textSizeMultiplier,
                        textAlignment = textAlignment,
                        showTranslation = showTranslation,
                        showRomanization = showRomanization
                    )
                } else {
                    val lyricsText = remember(lyrics) {
                        lyrics.getBestLyrics() ?: ""
                    }
                    val filteredPlainLyricsText = remember(
                        lyricsText,
                        showTranslation,
                        showRomanization
                    ) {
                        filterPlainLyricsByPreference(
                            rawLyrics = lyricsText,
                            showTranslation = showTranslation,
                            showRomanization = showRomanization
                        )
                    }

                    val likelySyncedLyrics = remember(lyricsText) {
                        Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{0,3})?]")
                            .containsMatchIn(lyricsText)
                    }

                    val parsedLyrics by produceState<List<chromahub.rhythm.app.util.LyricLine>?>(
                        initialValue = if (likelySyncedLyrics) null else emptyList(),
                        key1 = lyricsText,
                        key2 = likelySyncedLyrics
                    ) {
                        value = if (!likelySyncedLyrics) {
                            emptyList()
                        } else {
                            withContext(kotlinx.coroutines.Dispatchers.Default) {
                                chromahub.rhythm.app.util.LyricsParser.parseLyrics(
                                    lyricsText
                                )
                            }
                        }
                    }

                    if (parsedLyrics == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            M3CircularLoader(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                strokeWidth = 2f
                            )
                        }
                    } else if (parsedLyrics?.isNotEmpty() == true) {
                        SyncedLyricsView(
                            lyrics = lyricsText,
                            currentPlaybackTime = currentTimeMs,
                            modifier = Modifier.fillMaxSize(),
                            parsedLyricsInput = parsedLyrics,
                            onSeek = onLyricsSeek,
                            onTapLyricsView = onTapLyricsView,
                            showTranslation = showTranslation,
                            showRomanization = showRomanization,
                            lyricsSource = lyrics.source,
                            textSizeMultiplier = textSizeMultiplier,
                            textAlignment = textAlignment
                        )
                    } else {
                        val baseStyle = MaterialTheme.typography.bodyLarge
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = when (textAlignment) {
                                TextAlign.Start -> Alignment.Start
                                TextAlign.End -> Alignment.End
                                else -> Alignment.CenterHorizontally
                            }
                        ) {
                            Text(
                                text = filteredPlainLyricsText,
                                style = baseStyle.copy(
                                    fontSize = baseStyle.fontSize * textSizeMultiplier,
                                    lineHeight = baseStyle.lineHeight * 1.6f * textSizeMultiplier,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlignment,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!lyrics.source.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.lyrics_source_attribution, lyrics.source),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = textAlignment,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun filterPlainLyricsByPreference(
    rawLyrics: String,
    showTranslation: Boolean,
    showRomanization: Boolean
): String {
    if (rawLyrics.isBlank()) return rawLyrics
    if (showTranslation && showRomanization) return rawLyrics

    val filteredLines = mutableListOf<String>()
    var previousMainLineWasNonAscii = false

    rawLyrics.lineSequence().forEach { line ->
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            filteredLines += line
            return@forEach
        }

        val isBracketTranslation = trimmed.startsWith("(") && trimmed.endsWith(")") && trimmed.length > 2
        val isBracketRomanization = trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length > 2
        val hasLettersOrDigits = trimmed.any { it.isLetterOrDigit() }
        val isAsciiOnly = trimmed.all { char ->
            char.code <= 127 || char.isWhitespace()
        }
        val inferredRomanization = hasLettersOrDigits && isAsciiOnly && previousMainLineWasNonAscii

        val shouldHide =
            (!showTranslation && isBracketTranslation) ||
                (!showRomanization && (isBracketRomanization || inferredRomanization))

        if (shouldHide) {
            return@forEach
        }

        filteredLines += line

        if (!isBracketTranslation && !isBracketRomanization && !inferredRomanization) {
            previousMainLineWasNonAscii = trimmed.any { it.code > 127 }
        }
    }

    return filteredLines.joinToString("\n")
}

