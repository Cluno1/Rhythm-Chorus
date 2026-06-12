package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import android.net.Uri

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.RhythmSortMenuContent
import chromahub.rhythm.app.shared.presentation.components.common.RhythmSortOption
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.shared.presentation.components.common.AutoScrollingTextOnDemand
import chromahub.rhythm.app.shared.presentation.components.AudioQualityBadges
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AlbumSortOrder {
    TRACK_NUMBER,
    TITLE_ASC,
    TITLE_DESC,
    DURATION_ASC,
    DURATION_DESC
}

private data class AlbumSongDisplayState(
    val visibleSongs: List<Song> = emptyList(),
    val availableDiscs: List<Int> = emptyList(),
    val selectedDisc: Int = 0,
    val totalDuration: Long = 0L,
    val tracksDuration: Long = 0L,
    val hasMultipleDiscs: Boolean = false,
    val isPreparing: Boolean = true
)

private fun prepareAlbumSongDisplayState(
    songs: List<Song>,
    sortOrder: AlbumSortOrder,
    libraryCombineDiscs: Boolean,
    savedDiscFilter: Int
): AlbumSongDisplayState {
    val trackComparator = Comparator<Song> { a, b ->
        when {
            a.trackNumber > 0 && b.trackNumber > 0 -> a.trackNumber.compareTo(b.trackNumber)
            a.trackNumber > 0 -> -1
            b.trackNumber > 0 -> 1
            else -> a.title.compareTo(b.title, ignoreCase = true)
        }
    }

    fun sortByOrder(albumSongs: List<Song>): List<Song> {
        return when (sortOrder) {
            AlbumSortOrder.TRACK_NUMBER -> albumSongs.sortedWith(trackComparator)
            AlbumSortOrder.TITLE_ASC -> albumSongs.sortedBy { it.title.lowercase() }
            AlbumSortOrder.TITLE_DESC -> albumSongs.sortedByDescending { it.title.lowercase() }
            AlbumSortOrder.DURATION_ASC -> albumSongs.sortedBy { it.duration }
            AlbumSortOrder.DURATION_DESC -> albumSongs.sortedByDescending { it.duration }
        }
    }

    val sortedSongs = if (libraryCombineDiscs) {
        sortByOrder(songs)
    } else {
        songs
            .groupBy { it.discNumber.coerceAtLeast(1) }
            .toSortedMap()
            .values
            .flatMap { discSongs -> sortByOrder(discSongs) }
    }

    val availableDiscs = songs
        .map { it.discNumber.coerceAtLeast(1) }
        .distinct()
        .sorted()
    val shouldShowDiscFilter = !libraryCombineDiscs && availableDiscs.size > 1
    val selectedDisc = if (shouldShowDiscFilter && savedDiscFilter in availableDiscs) {
        savedDiscFilter
    } else {
        0
    }
    val visibleSongs = if (selectedDisc == 0) {
        sortedSongs
    } else {
        sortedSongs.filter { it.discNumber.coerceAtLeast(1) == selectedDisc }
    }
    val hasMultipleDiscs = !libraryCombineDiscs &&
        sortedSongs.asSequence()
            .map { it.discNumber.coerceAtLeast(1) }
            .distinct()
            .take(2)
            .count() > 1

    return AlbumSongDisplayState(
        visibleSongs = visibleSongs,
        availableDiscs = availableDiscs,
        selectedDisc = selectedDisc,
        totalDuration = sortedSongs.sumOf { it.duration },
        tracksDuration = visibleSongs.sumOf { it.duration },
        hasMultipleDiscs = hasMultipleDiscs,
        isPreparing = false
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AlbumBottomSheet(
    album: Album,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onPlayerClick: () -> Unit,
    sheetState: SheetState,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPlayNext: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit = {},
    favoriteSongs: Set<String> = emptySet(),
    onShowSongInfo: (Song) -> Unit = {},
    onAddToBlacklist: (Song) -> Unit = {},
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    showPlayNextAction: Boolean = true,
    showAddToQueueAction: Boolean = true,
    showToggleFavoriteAction: Boolean = true,
    showAddToPlaylistAction: Boolean = true,
    showSongInfoAction: Boolean = true,
    showAddToBlacklistAction: Boolean = true,
    onAddToQueueAll: ((List<Song>) -> Unit)? = null,
    onEditAlbum: ((
        title: String,
        artist: String,
        artworkUri: Uri?,
        removeArtwork: Boolean,
        onProgress: (Int, Int) -> Unit,
        onComplete: (successCount: Int, failCount: Int) -> Unit
    ) -> Unit)? = null
) {
    var showEditAlbumSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Detect tablet mode
    val isTablet = configuration.screenWidthDp >= 600

    // App settings for persistence
    val appSettings = remember { AppSettings.getInstance(context) }
    val savedSortOrder by appSettings.albumSortOrder.collectAsState()
    val savedDiscFilter by appSettings.albumBottomSheetDiscFilter.collectAsState()
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
    val albumBottomSheetGradientBlur by appSettings.albumBottomSheetGradientBlur.collectAsState()
    val libraryCombineDiscs by appSettings.libraryCombineDiscs.collectAsState()
    val albumArtworkShape = rememberExpressiveShapeFor(
        target = ExpressiveShapeTarget.ALBUM_ART,
        fallbackShape = RoundedCornerShape(24.dp)
    )
    val compactAlbumArtworkShape = rememberExpressiveShapeFor(
        target = ExpressiveShapeTarget.ALBUM_ART,
        fallbackShape = RoundedCornerShape(20.dp)
    )
    val songArtworkShape = rememberExpressiveShapeFor(
        target = ExpressiveShapeTarget.SONG_ART,
        fallbackShape = RoundedCornerShape(12.dp)
    )
    val tabletAlbumTitleStyle = when {
        album.title.length >= 52 -> MaterialTheme.typography.titleLarge
        album.title.length >= 34 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.headlineMedium
    }.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    val phoneAlbumTitleStyle = when {
        album.title.length >= 44 -> MaterialTheme.typography.titleMedium
        album.title.length >= 30 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.headlineSmall
    }.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    // Sort order state
    var sortOrder by remember {
        mutableStateOf(
            try {
                AlbumSortOrder.valueOf(savedSortOrder)
            } catch (e: Exception) {
                AlbumSortOrder.TRACK_NUMBER
            }
        )
    }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDiscMenu by remember { mutableStateOf(false) }

    // Save sort order when changed
    LaunchedEffect(sortOrder, savedSortOrder) {
        if (savedSortOrder != sortOrder.name) {
            appSettings.setAlbumSortOrder(sortOrder.name)
        }
    }

    // Staggered animation states
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Infinite shimmer animation for artwork background - loops continuously
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerOffset"
    )

    // Header animations
    val headerScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headerScale"
    )

    val headerAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "headerAlpha"
    )

    // Content slide animation
    val contentOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contentOffset"
    )

    var songDisplayState by remember(album.id) { mutableStateOf(AlbumSongDisplayState()) }
    val albumSongs = album.songs
    val firstAlbumSongId = albumSongs.firstOrNull()?.id
    val lastAlbumSongId = albumSongs.lastOrNull()?.id

    LaunchedEffect(
        album.id,
        album.numberOfSongs,
        album.dateModified,
        firstAlbumSongId,
        lastAlbumSongId,
        sortOrder,
        libraryCombineDiscs,
        savedDiscFilter
    ) {
        songDisplayState = withContext(Dispatchers.Default) {
            prepareAlbumSongDisplayState(
                songs = albumSongs,
                sortOrder = sortOrder,
                libraryCombineDiscs = libraryCombineDiscs,
                savedDiscFilter = savedDiscFilter
            )
        }
    }

    val visibleSongs = songDisplayState.visibleSongs
    val availableDiscs = songDisplayState.availableDiscs
    val selectedDiscFilterForAlbum = songDisplayState.selectedDisc
    val totalDuration = songDisplayState.totalDuration
    val tracksDuration = songDisplayState.tracksDuration
    val hasMultipleDiscs = songDisplayState.hasMultipleDiscs
    val isPreparingSongs = songDisplayState.isPreparing
    val shouldShowDiscFilter = !libraryCombineDiscs && availableDiscs.size > 1
    val showDiscSections = hasMultipleDiscs && selectedDiscFilterForAlbum == 0
    val selectedDiscLabel = if (selectedDiscFilterForAlbum == 0) {
        context.getString(R.string.bottomsheet_all_discs)
    } else {
        context.getString(R.string.bottomsheet_disc_option, selectedDiscFilterForAlbum)
    }

    LaunchedEffect(shouldShowDiscFilter) {
        if (!shouldShowDiscFilter) {
            showDiscMenu = false
        }
    }

    // Lazy list state for scroll-based effects
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    if (isTablet) {
        // Tablet layout: Dialog with side-by-side layout
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .navigationBarsPadding()
                ) {
                    // Background artwork with blur effect for tablet (conditional)
                    if (album.artworkUri != null && albumBottomSheetGradientBlur) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .apply(
                                    ImageUtils.buildImageRequest(
                                        album.artworkUri,
                                        album.title,
                                        context.cacheDir,
                                        M3PlaceholderType.ALBUM
                                    )
                                )
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(25.dp)
                                .graphicsLayer { alpha = 0.4f }
                        )
                    }

                    // Animated gradient shimmer overlay (conditional)
                    if (albumBottomSheetGradientBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.03f),
                                                Color.Transparent
                                            ),
                                            start = Offset(shimmerOffset - 500f, 0f),
                                            end = Offset(shimmerOffset, size.height)
                                        )
                                    )
                                }
                        )
                    }

                    // Multi-layer gradient overlay for depth (conditional)
                    if (albumBottomSheetGradientBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                                alpha = 0.4f
                                            ),
                                            0.3f to Color.Transparent,
                                            0.7f to MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            1f to MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )
                    }

                    // Radial highlight accent (conditional)
                    if (albumBottomSheetGradientBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = Offset(0.7f, 0.2f),
                                        radius = 1000f
                                    )
                                )
                        )
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side: Album info and artwork
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Album artwork
                                Surface(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .graphicsLayer {
                                            alpha = headerAlpha
                                            scaleX = headerScale
                                            scaleY = headerScale
                                        },
                                    shape = albumArtworkShape,
                                    shadowElevation = 16.dp,
                                    tonalElevation = 8.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (album.artworkUri != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .apply(
                                                        ImageUtils.buildImageRequest(
                                                            album.artworkUri,
                                                            album.title,
                                                            context.cacheDir,
                                                            M3PlaceholderType.ALBUM
                                                        )
                                                    )
                                                    .build(),
                                                contentDescription = stringResource(R.string.album_artwork_description, album.title),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primaryContainer,
                                                                MaterialTheme.colorScheme.tertiaryContainer
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.AlbumFilled,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.7f
                                                    ),
                                                    modifier = Modifier.size(80.dp)
                                                )
                                            }
                                        }

                                        if (onEditAlbum != null) {
                                            FilledIconButton(
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.MEDIUM)
                                                    showEditAlbumSheet = true
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(12.dp)
                                                    .size(36.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.Edit,
                                                    contentDescription = "Edit Album",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Album info
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.weight(1f, fill = false)) {
                                            AutoScrollingTextOnDemand(
                                                text = album.title,
                                                style = tabletAlbumTitleStyle,
                                                gradientEdgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = true,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metadata
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (album.year > 0) {
                                            MetadataChip(
                                                text = "${album.year}",
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        if (album.songs.isNotEmpty()) {
                                            AudioQualityBadges(song = album.songs.first())
                                        }

                                        MetadataChip(
                                            text = formatDuration(totalDuration, useHoursFormat),
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Action buttons - Grouped button design
                                var shufflePressed by remember { mutableStateOf(false) }
                                var playAllPressed by remember { mutableStateOf(false) }
                                var addToQueuePressed by remember { mutableStateOf(false) }
                                
                                val shuffleScale by animateFloatAsState(
                                    targetValue = if (shufflePressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "shuffleScale"
                                )
                                
                                val playAllScale by animateFloatAsState(
                                    targetValue = if (playAllPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "playAllScale"
                                )

                                val addToQueueScale by animateFloatAsState(
                                    targetValue = if (addToQueuePressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "addToQueueScale"
                                )
                                
                                LaunchedEffect(shufflePressed) {
                                    if (shufflePressed) {
                                        delay(150)
                                        shufflePressed = false
                                    }
                                }
                                
                                LaunchedEffect(playAllPressed) {
                                    if (playAllPressed) {
                                        delay(150)
                                        playAllPressed = false
                                    }
                                }
                                
                                LaunchedEffect(addToQueuePressed) {
                                    if (addToQueuePressed) {
                                        delay(150)
                                        addToQueuePressed = false
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Play All Button - Equal sizing with text
                                    Button(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticType.HEAVY
                                            )
                                            playAllPressed = true
                                            onPlayAll(visibleSongs)
                                            onDismiss()
                                            onPlayerClick()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .graphicsLayer {
                                                scaleX = playAllScale
                                                scaleY = playAllScale
                                            },
                                        shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Play,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = context.getString(R.string.bottomsheet_play_all),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Shuffle Button - Equal sizing with text
                                    FilledTonalButton(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticType.HEAVY
                                            )
                                            shufflePressed = true
                                            onShufflePlay(visibleSongs)
                                            onDismiss()
                                            onPlayerClick()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .graphicsLayer {
                                                scaleX = shuffleScale
                                                scaleY = shuffleScale
                                            },
                                        shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Shuffle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Shuffle",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Divider

                        // Right side: Songs list
                        Surface(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Header with sort
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.bottomsheet_tracks),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Sort and Close buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (shouldShowDiscFilter) {
                                                Box {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            showSortMenu = false
                                                            showDiscMenu = true
                                                        },
                                                        shape = RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                                        modifier = Modifier.height(40.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedDiscLabel,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Icon(
                                                            imageVector = RhythmIcons.ArrowDropDown,
                                                            contentDescription = context.getString(R.string.bottomsheet_disc_filter)
                                                        )
                                                    }

                                                    DiscFilterDropdownMenu(
                                                        expanded = showDiscMenu,
                                                        onDismissRequest = { showDiscMenu = false },
                                                        selectedDisc = selectedDiscFilterForAlbum,
                                                        availableDiscs = availableDiscs,
                                                        onSelectDisc = { discNumber ->
                                                            HapticUtils.performHapticFeedback(
                                                                context,
                                                                haptics,
                                                                HapticType.HEAVY
                                                            )
                                                            appSettings.setAlbumBottomSheetDiscFilter(discNumber)
                                                            showDiscMenu = false
                                                        }
                                                    )
                                                }
                                            }

                                            // Sort button
                                            Box {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        showDiscMenu = false
                                                        showSortMenu = true
                                                    },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = RhythmIcons.Actions.Sort,
                                                        contentDescription = stringResource(R.string.content_desc_sort_songs),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false },
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier
                                                        .widthIn(min = 250.dp)
                                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                                        .padding(8.dp)
                                                ) {
                                                    val sortOptions = remember(context) {
                                                        listOf(
                                                            RhythmSortOption("TRACK_NUMBER", "Track Number", RhythmIcons.FormatListNumbered),
                                                            RhythmSortOption("TITLE", "Title", RhythmIcons.SortByAlpha),
                                                            RhythmSortOption("DURATION", "Duration", RhythmIcons.AccessTime)
                                                        )
                                                    }
                                                    val currentKey = when (sortOrder) {
                                                        AlbumSortOrder.TRACK_NUMBER -> "TRACK_NUMBER"
                                                        AlbumSortOrder.TITLE_ASC, AlbumSortOrder.TITLE_DESC -> "TITLE"
                                                        AlbumSortOrder.DURATION_ASC, AlbumSortOrder.DURATION_DESC -> "DURATION"
                                                    }
                                                    val isAscending = when (sortOrder) {
                                                        AlbumSortOrder.TITLE_DESC, AlbumSortOrder.DURATION_DESC -> false
                                                        else -> true
                                                    }
                                                    RhythmSortMenuContent(
                                                        selectedKey = currentKey,
                                                        isAscending = isAscending,
                                                        options = sortOptions,
                                                        onKeySelected = { key ->
                                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                                            val newOrder = when (key) {
                                                                "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                                                                "TITLE" -> if (isAscending) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                                                                "DURATION" -> if (isAscending) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                                                                else -> AlbumSortOrder.TRACK_NUMBER
                                                            }
                                                            if (sortOrder != newOrder) {
                                                                sortOrder = newOrder
                                                                appSettings.setAlbumSortOrder(newOrder.name)
                                                            }
                                                            showSortMenu = false
                                                        },
                                                        onDirectionToggled = { asc ->
                                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                                            val newOrder = when (currentKey) {
                                                                "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                                                                "TITLE" -> if (asc) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                                                                "DURATION" -> if (asc) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                                                                else -> AlbumSortOrder.TRACK_NUMBER
                                                            }
                                                            if (sortOrder != newOrder) {
                                                                sortOrder = newOrder
                                                                appSettings.setAlbumSortOrder(newOrder.name)
                                                            }
                                                            showSortMenu = false
                                                        }
                                                    )
                                                }
                                            }

                                            // Close button on tablet
                                            IconButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.Close,
                                                    contentDescription = stringResource(R.string.ui_close),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                    // Songs list
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                            .graphicsLayer { translationY = contentOffset }
                                    ) {
                                        if (isPreparingSongs) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        } else if (visibleSongs.isNotEmpty()) {
                                            LazyColumn(
                                                state = listState,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(
                                                    top = 8.dp,
                                                    bottom = 24.dp
                                                ),
                                                userScrollEnabled = true
                                            ) {
                                                itemsIndexed(
                                                    items = visibleSongs,
                                                    key = { index, song -> "album_song_${song.id}_$index" }
                                                ) { index, song ->
                                                    val currentDisc = song.discNumber.coerceAtLeast(1)
                                                    val previousDisc = visibleSongs.getOrNull(index - 1)?.discNumber?.coerceAtLeast(1)
                                                    val nextDisc = visibleSongs.getOrNull(index + 1)?.discNumber?.coerceAtLeast(1)
                                                    val isFirstInDiscGroup = showDiscSections && previousDisc != currentDisc
                                                    val isLastInDiscGroup = showDiscSections && nextDisc != currentDisc

                                                    if (isFirstInDiscGroup) {
                                                        DiscSectionHeader(
                                                            discNumber = currentDisc,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                                        )
                                                    }

                                                    ExpressiveSongItem(
                                                        song = song,
                                                        index = index + 1,
                                                        itemShape = when {
                                                            showDiscSections && isFirstInDiscGroup && isLastInDiscGroup -> RoundedCornerShape(24.dp)
                                                            showDiscSections && isFirstInDiscGroup -> RoundedCornerShape(
                                                                topStart = 24.dp,
                                                                topEnd = 24.dp,
                                                                bottomStart = 6.dp,
                                                                bottomEnd = 6.dp
                                                            )
                                                            showDiscSections && isLastInDiscGroup -> RoundedCornerShape(
                                                                topStart = 6.dp,
                                                                topEnd = 6.dp,
                                                                bottomStart = 24.dp,
                                                                bottomEnd = 24.dp
                                                            )
                                                            visibleSongs.size == 1 -> RoundedCornerShape(24.dp)
                                                            index == 0 -> RoundedCornerShape(
                                                                topStart = 24.dp,
                                                                topEnd = 24.dp,
                                                                bottomStart = 6.dp,
                                                                bottomEnd = 6.dp
                                                            )
                                                            index == visibleSongs.lastIndex -> RoundedCornerShape(
                                                                topStart = 6.dp,
                                                                topEnd = 6.dp,
                                                                bottomStart = 24.dp,
                                                                bottomEnd = 24.dp
                                                            )
                                                            else -> RoundedCornerShape(6.dp)
                                                        },
                                                        baseContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                                        horizontalPadding = 4.dp,
                                                        verticalPadding = 2.dp,
                                                        onClick = {
                                                            onSongClick(song)
                                                            onDismiss()
                                                            onPlayerClick()
                                                        },
                                                        onAddToQueue = { onAddToQueue(song) },
                                                        onAddToPlaylist = {
                                                            onAddSongToPlaylist(
                                                                song
                                                            )
                                                        },
                                                        onPlayNext = { onPlayNext(song) },
                                                        onToggleFavorite = {
                                                            onToggleFavorite(
                                                                song
                                                            )
                                                        },
                                                        isFavorite = favoriteSongs.contains(song.id),
                                                        onShowSongInfo = { onShowSongInfo(song) },
                                                        onAddToBlacklist = {
                                                            onAddToBlacklist(
                                                                song
                                                            )
                                                        },
                                                        currentSong = currentSong,
                                                        isPlaying = isPlaying,
                                                        useHoursFormat = useHoursFormat,
                                                        songArtShape = songArtworkShape,
                                                        haptics = haptics,
                                                        modifier = Modifier.animateItem()
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.size(80.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = RhythmIcons.MusicOff,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.6f
                                                                ),
                                                                modifier = Modifier.size(40.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = context.getString(R.string.bottomsheet_no_songs_album),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center
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
        }
    } else {
        // Phone layout: Bottom sheet
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxHeight()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ═══════════════════════════════════════════════════════════════════
                    // EXPRESSIVE HEADER SECTION
                    // ═══════════════════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .graphicsLayer {
                                alpha = headerAlpha
                                scaleX = headerScale
                                scaleY = headerScale
                            }
                    ) {
                        // Background artwork with blur effect (conditional)
                        if (album.artworkUri != null && albumBottomSheetGradientBlur) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .apply(
                                        ImageUtils.buildImageRequest(
                                            album.artworkUri,
                                            album.title,
                                            context.cacheDir,
                                            M3PlaceholderType.ALBUM
                                        )
                                    )
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(20.dp)
                                    .graphicsLayer { alpha = 0.5f }
                            )
                        }

                        // Animated gradient shimmer overlay (conditional)
                        if (albumBottomSheetGradientBlur) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.05f),
                                                    Color.Transparent
                                                ),
                                                start = Offset(shimmerOffset - 500f, 0f),
                                                end = Offset(shimmerOffset, size.height)
                                        )
                                    )
                                }
                        )
                    }

                    // Multi-layer gradient overlay for depth (conditional)
                    if (albumBottomSheetGradientBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                                alpha = 0.3f
                                            ),
                                            0.4f to Color.Transparent,
                                            0.7f to MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            1f to MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )
                    }

                    // Radial highlight accent (conditional)
                    if (albumBottomSheetGradientBlur) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            Color.Transparent
                                        ),
                                        center = Offset(0.3f, 0.3f),
                                        radius = 800f
                                    )
                                )
                        )
                    }

                    // Drag handle with animation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ) {}
                    }

                    // Close button
//                    Surface(
//                        onClick = {
//                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
//                            onDismiss()
//                        },
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .padding(WindowInsets.statusBars.asPaddingValues())
//                            .padding(16.dp)
//                            .size(44.dp),
//                        shape = CircleShape,
//                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
//                        shadowElevation = 0.dp
//                    ) {
//                        Box(contentAlignment = Alignment.Center) {
//                            Icon(
//                                imageVector = RhythmIcons.Close,
//                                contentDescription = "Close",
//                                tint = MaterialTheme.colorScheme.onSurface,
//                                modifier = Modifier.size(22.dp)
//                            )
//                        }
//                    }

                        // Main content layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(48.dp))

                            // Floating album artwork card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Album art with animated elevation
                                val artworkScale by animateFloatAsState(
                                    targetValue = if (isVisible) 1f else 0.8f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                        visibilityThreshold = 0.001f
                                    ),
                                    label = "artworkScale"
                                )

                                Surface(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .graphicsLayer {
                                            scaleX = artworkScale
                                            scaleY = artworkScale
                                        },
                                    shape = compactAlbumArtworkShape,
                                    shadowElevation = 16.dp,
                                    tonalElevation = 8.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (album.artworkUri != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .apply(
                                                        ImageUtils.buildImageRequest(
                                                            album.artworkUri,
                                                            album.title,
                                                            context.cacheDir,
                                                            M3PlaceholderType.ALBUM
                                                        )
                                                    )
                                                    .build(),
                                                contentDescription = stringResource(R.string.album_artwork_description, album.title),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            // Gradient fallback
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primaryContainer,
                                                                MaterialTheme.colorScheme.tertiaryContainer
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.AlbumFilled,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                        alpha = 0.7f
                                                    ),
                                                    modifier = Modifier.size(64.dp)
                                                )
                                            }
                                        }

                                        // Shine overlay effect
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.15f),
                                                            Color.Transparent,
                                                            Color.Transparent
                                                        ),
                                                        start = Offset(0f, 0f),
                                                        end = Offset(300f, 300f)
                                                    )
                                                )
                                        )

                                        if (onEditAlbum != null) {
                                            FilledIconButton(
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.MEDIUM)
                                                    showEditAlbumSheet = true
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .size(32.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = RhythmIcons.Edit,
                                                    contentDescription = "Edit Album",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Album info with staggered animations
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Album title with entrance animation
                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = fadeIn(tween(400, delayMillis = 100)) +
                                                slideInHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    ),
                                                    initialOffsetX = { 50 }
                                                )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = album.title,
                                                    style = phoneAlbumTitleStyle,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Start
                                                )
                                            }
                                        }
                                    }

                                    // Artist name
                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = fadeIn(tween(400, delayMillis = 200)) +
                                                slideInHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    ),
                                                    initialOffsetX = { 50 }
                                                )
                                    ) {
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Metadata chips row
                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = fadeIn(tween(400, delayMillis = 300)) +
                                                slideInHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    ),
                                                    initialOffsetX = { 50 }
                                                )
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (album.year > 0) {
                                                MetadataChip(
                                                    text = "${album.year}",
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }

                                            if (album.songs.isNotEmpty()) {
                                                AudioQualityBadges(song = album.songs.first())
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ═══════════════════════════════════════════════════════════════════
                            // ACTION BUTTONS ROW - Grouped button design
                            // ═══════════════════════════════════════════════════════════════════
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(tween(500, delayMillis = 400)) +
                                        slideInVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            initialOffsetY = { 40 }
                                        )
                            ) {
                                var shufflePressed by remember { mutableStateOf(false) }
                                var playAllPressed by remember { mutableStateOf(false) }
                                var addToQueuePressed by remember { mutableStateOf(false) }
                                
                                val shuffleScale by animateFloatAsState(
                                    targetValue = if (shufflePressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "shuffleScale"
                                )
                                
                                val playAllScale by animateFloatAsState(
                                    targetValue = if (playAllPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "playAllScale"
                                )

                                val addToQueueScale by animateFloatAsState(
                                    targetValue = if (addToQueuePressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "addToQueueScale"
                                )
                                
                                LaunchedEffect(shufflePressed) {
                                    if (shufflePressed) {
                                        delay(150)
                                        shufflePressed = false
                                    }
                                }
                                
                                LaunchedEffect(playAllPressed) {
                                    if (playAllPressed) {
                                        delay(150)
                                        playAllPressed = false
                                    }
                                }

                                LaunchedEffect(addToQueuePressed) {
                                    if (addToQueuePressed) {
                                        delay(150)
                                        addToQueuePressed = false
                                    }
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        // Play All Button - Equal sizing with text
                                        Button(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(
                                                    context,
                                                    haptics,
                                                    HapticType.HEAVY
                                                )
                                                playAllPressed = true
                                                onPlayAll(visibleSongs)
                                                scope.launch {
                                                    sheetState.hide()
                                                }.invokeOnCompletion {
                                                    if (!sheetState.isVisible) {
                                                        onDismiss()
                                                        onPlayerClick()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .graphicsLayer {
                                                    scaleX = playAllScale
                                                    scaleY = playAllScale
                                                },
                                            shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Play,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = context.getString(R.string.bottomsheet_play_all),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        // Shuffle Button - Equal sizing with text
                                        FilledTonalButton(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(
                                                    context,
                                                    haptics,
                                                    HapticType.HEAVY
                                                )
                                                shufflePressed = true
                                                onShufflePlay(visibleSongs)
                                                scope.launch {
                                                    sheetState.hide()
                                                }.invokeOnCompletion {
                                                    if (!sheetState.isVisible) {
                                                        onDismiss()
                                                        onPlayerClick()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .graphicsLayer {
                                                    scaleX = shuffleScale
                                                    scaleY = shuffleScale
                                                },
                                            shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Shuffle,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Shuffle",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (onAddToQueueAll != null) {
                                        FilledTonalButton(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(
                                                    context,
                                                    haptics,
                                                    HapticType.HEAVY
                                                )
                                                addToQueuePressed = true
                                                onAddToQueueAll(visibleSongs)
                                                scope.launch {
                                                    sheetState.hide()
                                                }.invokeOnCompletion {
                                                    if (!sheetState.isVisible) {
                                                        onDismiss()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .graphicsLayer {
                                                    scaleX = addToQueueScale
                                                    scaleY = addToQueueScale
                                                },
                                            shape = RoundedCornerShape(26.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Queue,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = context.getString(R.string.action_add_to_queue),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════════════════
                    // SONGS SECTION HEADER
                    // ═══════════════════════════════════════════════════════════════════
                    AnimatedVisibility(
                        visible = isVisible && visibleSongs.isNotEmpty(),
                        enter = fadeIn(tween(500, delayMillis = 500)) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    initialOffsetY = { 30 }
                                )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = context.getString(R.string.bottomsheet_tracks),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatDuration(
                                            tracksDuration,
                                            useHoursFormat
                                        ) + " total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Song count badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${visibleSongs.size}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }

                                    if (shouldShowDiscFilter) {
                                        Box {
                                            FilledTonalButton(
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticType.LIGHT
                                                    )
                                                    showSortMenu = false
                                                    showDiscMenu = true
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text(
                                                    text = selectedDiscLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Icon(
                                                    imageVector = RhythmIcons.ArrowDropDown,
                                                    contentDescription = context.getString(R.string.bottomsheet_disc_filter)
                                                )
                                            }

                                            DiscFilterDropdownMenu(
                                                expanded = showDiscMenu,
                                                onDismissRequest = { showDiscMenu = false },
                                                selectedDisc = selectedDiscFilterForAlbum,
                                                availableDiscs = availableDiscs,
                                                onSelectDisc = { discNumber ->
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticType.HEAVY
                                                    )
                                                    appSettings.setAlbumBottomSheetDiscFilter(discNumber)
                                                    showDiscMenu = false
                                                }
                                            )
                                        }
                                    }

                                    // Sort button
                                    Box {
                                        var sortButtonPressed by remember { mutableStateOf(false) }
                                        val sortButtonScale by animateFloatAsState(
                                            targetValue = if (sortButtonPressed) 0.85f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "sortButtonScale"
                                        )

                                        FilledTonalIconButton(
                                            onClick = {
                                                HapticUtils.performHapticFeedback(
                                                    context,
                                                    haptics,
                                                    HapticType.LIGHT
                                                )
                                                showDiscMenu = false
                                                showSortMenu = true
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .graphicsLayer {
                                                    scaleX = sortButtonScale
                                                    scaleY = sortButtonScale
                                                }
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            sortButtonPressed = true
                                                            tryAwaitRelease()
                                                            sortButtonPressed = false
                                                        }
                                                    )
                                                },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Actions.Sort,
                                                contentDescription = stringResource(R.string.content_desc_sort_songs),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .widthIn(min = 250.dp)
                                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                                .padding(8.dp)
                                        ) {
                                            val sortOptions = remember(context) {
                                                listOf(
                                                    RhythmSortOption("TRACK_NUMBER", "Track Number", RhythmIcons.FormatListNumbered),
                                                    RhythmSortOption("TITLE", "Title", RhythmIcons.SortByAlpha),
                                                    RhythmSortOption("DURATION", "Duration", RhythmIcons.AccessTime)
                                                )
                                            }
                                            val currentKey = when (sortOrder) {
                                                AlbumSortOrder.TRACK_NUMBER -> "TRACK_NUMBER"
                                                AlbumSortOrder.TITLE_ASC, AlbumSortOrder.TITLE_DESC -> "TITLE"
                                                AlbumSortOrder.DURATION_ASC, AlbumSortOrder.DURATION_DESC -> "DURATION"
                                            }
                                            val isAscending = when (sortOrder) {
                                                AlbumSortOrder.TITLE_DESC, AlbumSortOrder.DURATION_DESC -> false
                                                else -> true
                                            }
                                            RhythmSortMenuContent(
                                                selectedKey = currentKey,
                                                isAscending = isAscending,
                                                options = sortOptions,
                                                onKeySelected = { key ->
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                                    val newOrder = when (key) {
                                                        "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                                                        "TITLE" -> if (isAscending) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                                                        "DURATION" -> if (isAscending) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                                                        else -> AlbumSortOrder.TRACK_NUMBER
                                                    }
                                                    if (sortOrder != newOrder) {
                                                        sortOrder = newOrder
                                                        appSettings.setAlbumSortOrder(newOrder.name)
                                                    }
                                                    showSortMenu = false
                                                },
                                                onDirectionToggled = { asc ->
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                                    val newOrder = when (currentKey) {
                                                        "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                                                        "TITLE" -> if (asc) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                                                        "DURATION" -> if (asc) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                                                        else -> AlbumSortOrder.TRACK_NUMBER
                                                    }
                                                    if (sortOrder != newOrder) {
                                                        sortOrder = newOrder
                                                        appSettings.setAlbumSortOrder(newOrder.name)
                                                    }
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════════════════════════════════════════
                    // SONGS LIST
                    // ═══════════════════════════════════════════════════════════════════
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .graphicsLayer { translationY = contentOffset }
                    ) {
                        if (isPreparingSongs) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (visibleSongs.isNotEmpty()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                                userScrollEnabled = true
                            ) {
                                itemsIndexed(
                                    items = visibleSongs,
                                    key = { index, song -> "album_song_${song.id}_$index" }
                                ) { index, song ->
                                    val currentDisc = song.discNumber.coerceAtLeast(1)
                                    val previousDisc = visibleSongs.getOrNull(index - 1)?.discNumber?.coerceAtLeast(1)
                                    val nextDisc = visibleSongs.getOrNull(index + 1)?.discNumber?.coerceAtLeast(1)
                                    val isFirstInDiscGroup = showDiscSections && previousDisc != currentDisc
                                    val isLastInDiscGroup = showDiscSections && nextDisc != currentDisc

                                    if (isFirstInDiscGroup) {
                                        DiscSectionHeader(
                                            discNumber = currentDisc,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                        )
                                    }

                                    ExpressiveSongItem(
                                        song = song,
                                        index = index + 1,
                                        itemShape = when {
                                            showDiscSections && isFirstInDiscGroup && isLastInDiscGroup -> RoundedCornerShape(24.dp)
                                            showDiscSections && isFirstInDiscGroup -> RoundedCornerShape(
                                                topStart = 24.dp,
                                                topEnd = 24.dp,
                                                bottomStart = 6.dp,
                                                bottomEnd = 6.dp
                                            )
                                            showDiscSections && isLastInDiscGroup -> RoundedCornerShape(
                                                topStart = 6.dp,
                                                topEnd = 6.dp,
                                                bottomStart = 24.dp,
                                                bottomEnd = 24.dp
                                            )
                                            visibleSongs.size == 1 -> RoundedCornerShape(24.dp)
                                            index == 0 -> RoundedCornerShape(
                                                topStart = 24.dp,
                                                topEnd = 24.dp,
                                                bottomStart = 6.dp,
                                                bottomEnd = 6.dp
                                            )
                                            index == visibleSongs.lastIndex -> RoundedCornerShape(
                                                topStart = 6.dp,
                                                topEnd = 6.dp,
                                                bottomStart = 24.dp,
                                                bottomEnd = 24.dp
                                            )
                                            else -> RoundedCornerShape(6.dp)
                                        },
                                        baseContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        horizontalPadding = 4.dp,
                                        verticalPadding = 2.dp,
                                        onClick = {
                                            onSongClick(song)
                                            scope.launch {
                                                sheetState.hide()
                                            }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismiss()
                                                    onPlayerClick()
                                                }
                                            }
                                        },
                                        onAddToQueue = { onAddToQueue(song) },
                                        onAddToPlaylist = { onAddSongToPlaylist(song) },
                                        onPlayNext = { onPlayNext(song) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        isFavorite = favoriteSongs.contains(song.id),
                                        onShowSongInfo = { onShowSongInfo(song) },
                                        onAddToBlacklist = { onAddToBlacklist(song) },
                                        showPlayNextAction = showPlayNextAction,
                                        showAddToQueueAction = showAddToQueueAction,
                                        showToggleFavoriteAction = showToggleFavoriteAction,
                                        showAddToPlaylistAction = showAddToPlaylistAction,
                                        showSongInfoAction = showSongInfoAction,
                                        showAddToBlacklistAction = showAddToBlacklistAction,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        useHoursFormat = useHoursFormat,
                                        songArtShape = songArtworkShape,
                                        haptics = haptics,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = RhythmIcons.MusicOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.6f
                                                ),
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = context.getString(R.string.bottomsheet_no_songs_album),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditAlbumSheet && onEditAlbum != null) {
        EditAlbumSheet(
            album = album,
            onDismiss = { showEditAlbumSheet = false },
            onSave = { title, artist, artworkUri, removeArtwork, onProgress, onComplete ->
                onEditAlbum(title, artist, artworkUri, removeArtwork, onProgress, onComplete)
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SUPPORTING COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun MetadataChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpressiveSongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPlayNext: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    isFavorite: Boolean = false,
    onShowSongInfo: () -> Unit = {},
    onAddToBlacklist: () -> Unit = {},
    showPlayNextAction: Boolean = true,
    showAddToQueueAction: Boolean = true,
    showToggleFavoriteAction: Boolean = true,
    showAddToPlaylistAction: Boolean = true,
    showSongInfoAction: Boolean = true,
    showAddToBlacklistAction: Boolean = true,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    useHoursFormat: Boolean = false,
    songArtShape: Shape = RoundedCornerShape(12.dp),
    itemShape: Shape = RoundedCornerShape(16.dp),
    baseContainerColor: Color = Color.Transparent,
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 3.dp
) {
    val context = LocalContext.current
    var showDropdown by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val isCurrentSong = currentSong?.id == song.id

    // Animated properties for current song highlight
    val containerColor by animateColorAsState(
        targetValue = when {
            isCurrentSong -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            isPressed -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> baseContainerColor
        },
        animationSpec = tween(250),
        label = "containerColor"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "titleColor"
    )

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "itemScale"
    )

    val hasOverflowActions =
        showPlayNextAction ||
            showAddToQueueAction ||
            showToggleFavoriteAction ||
            showAddToPlaylistAction ||
            showSongInfoAction ||
            showAddToBlacklistAction

    // No pulse animation - removed per user request

    Surface(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = itemShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Track number
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (song.trackNumber > 0) "${song.trackNumber}" else "$index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentSong)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Album artwork thumbnail
            Surface(
                shape = songArtShape,
                modifier = Modifier.size(50.dp),
                tonalElevation = 2.dp,
                shadowElevation = if (isCurrentSong) 4.dp else 1.dp
            ) {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .apply(ImageUtils.buildImageRequest(
                                song.artworkUri,
                                song.title,
                                context.cacheDir,
                                M3PlaceholderType.TRACK
                            ))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Playing overlay with EQ icon
                    if (isCurrentSong && isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingEqIcon(
                                modifier = Modifier.size(width = 24.dp, height = 20.dp),
                                color = Color.White,
                                isPlaying = isPlaying,
                                bars = 3
                            )
                        }
                    }
                }
            }

            // Song info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Favorite indicator
                    AnimatedVisibility(
                        visible = isFavorite,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            imageVector = RhythmIcons.FavoriteFilled,
                            contentDescription = stringResource(R.string.player_chip_favorite),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Duration
                Text(
                    text = formatDuration(song.duration, useHoursFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // More options button
            if (hasOverflowActions) {
                Box {
                var moreButtonPressed by remember { mutableStateOf(false) }
                val moreButtonScale by animateFloatAsState(
                    targetValue = if (moreButtonPressed) 0.85f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "moreButtonScale"
                )

                FilledTonalIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        showDropdown = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .graphicsLayer {
                            scaleX = moreButtonScale
                            scaleY = moreButtonScale
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    moreButtonPressed = true
                                    tryAwaitRelease()
                                    moreButtonPressed = false
                                }
                            )
                        },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.More,
                        contentDescription = stringResource(R.string.content_desc_more_options),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        showDropdown = false
                    },
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(5.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (showPlayNextAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Play next",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.SkipNext,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onPlayNext()
                                }
                            )
                        }
                    }

                    if (showAddToQueueAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add to queue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Queue,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onAddToQueue()
                                }
                            )
                        }
                    }

                    if (showToggleFavoriteAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFavorite) "Remove from favorites" else "Add to favorites",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) RhythmIcons.FavoriteFilled else RhythmIcons.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onToggleFavorite()
                                }
                            )
                        }
                    }

                    if (showAddToPlaylistAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add to playlist",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.AddToPlaylist,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onAddToPlaylist()
                                }
                            )
                        }
                    }

                    if (showSongInfoAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Song info",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onShowSongInfo()
                                }
                            )
                        }
                    }

                    if (showAddToBlacklistAction) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add to blacklist",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Block,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    showDropdown = false
                                    onAddToBlacklist()
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun DiscFilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedDisc: Int,
    availableDiscs: List<Int>,
    onSelectDisc: (Int) -> Unit
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        val selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        val selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
        val defaultTextColor = MaterialTheme.colorScheme.onSurface
        val defaultIconColor = MaterialTheme.colorScheme.onSurfaceVariant

        Surface(
            color = if (selectedDisc == 0) selectedContainerColor else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = context.getString(R.string.bottomsheet_all_discs),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedDisc == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedDisc == 0) selectedTextColor else defaultTextColor
                    )
                },
                leadingIcon = {
                    if (selectedDisc == 0) {
                        Icon(
                            imageVector = RhythmIcons.Check,
                            contentDescription = null,
                            tint = selectedTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = { onSelectDisc(0) },
                colors = androidx.compose.material3.MenuDefaults.itemColors(
                    textColor = if (selectedDisc == 0) selectedTextColor else defaultTextColor
                )
            )
        }

        availableDiscs.forEach { discNumber ->
            val isSelected = selectedDisc == discNumber
            Surface(
                color = if (isSelected) selectedContainerColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = context.getString(R.string.bottomsheet_disc_option, discNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) selectedTextColor else defaultTextColor
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = RhythmIcons.Check,
                                contentDescription = null,
                                tint = selectedTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = RhythmIcons.MusicNote,
                                contentDescription = null,
                                tint = defaultIconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onClick = { onSelectDisc(discNumber) },
                    colors = androidx.compose.material3.MenuDefaults.itemColors(
                        textColor = if (isSelected) selectedTextColor else defaultTextColor
                    )
                )
            }
        }
    }
}

@Composable
private fun DiscSectionHeader(
    discNumber: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.bottomsheet_disc_option, discNumber),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

// Keep backward compatibility alias
@Composable
fun EnhancedAlbumSongItem(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPlayNext: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    isFavorite: Boolean = false,
    onShowSongInfo: () -> Unit = {},
    onAddToBlacklist: () -> Unit = {},
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    useHoursFormat: Boolean = false
) {
    ExpressiveSongItem(
        song = song,
        index = 1,
        onClick = onClick,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = onAddToPlaylist,
        modifier = modifier,
        haptics = haptics,
        onPlayNext = onPlayNext,
        onToggleFavorite = onToggleFavorite,
        isFavorite = isFavorite,
        onShowSongInfo = onShowSongInfo,
        onAddToBlacklist = onAddToBlacklist,
        currentSong = currentSong,
        isPlaying = isPlaying,
        useHoursFormat = useHoursFormat
    )
}