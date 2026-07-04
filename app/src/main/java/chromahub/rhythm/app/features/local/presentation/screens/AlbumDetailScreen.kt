package chromahub.rhythm.app.features.local.presentation.screens

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import chromahub.rhythm.app.R

import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.AudioQualityIcon
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.util.M3ImageUtils
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.components.player.CanvasArtworkPlayer
import chromahub.rhythm.app.network.AppleMusicCanvasProvider
import chromahub.rhythm.app.network.WikipediaProvider
import chromahub.rhythm.app.network.CanvasArtwork
import chromahub.rhythm.app.shared.data.model.CanvasNetworkMode
import chromahub.rhythm.app.core.utils.NetworkUtils
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.ArtistChooserBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.PlaylistSongOptionsBottomSheet
import chromahub.rhythm.app.shared.presentation.components.common.RhythmSortMenuContent
import chromahub.rhythm.app.shared.presentation.components.common.RhythmSortOption
import chromahub.rhythm.app.util.ArtistSeparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class AlbumSortOrder {
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
    val totalDuration: Long = 0L
)

private fun String.toAlbumSortOrder(): AlbumSortOrder {
    return runCatching { AlbumSortOrder.valueOf(this) }.getOrDefault(AlbumSortOrder.TRACK_NUMBER)
}

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
    return AlbumSongDisplayState(
        visibleSongs = visibleSongs,
        availableDiscs = availableDiscs,
        selectedDisc = selectedDisc,
        totalDuration = sortedSongs.sumOf { it.duration }
    )
}

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    albumName: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onPlayerClick: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit = {},
    favoriteSongs: Set<String> = emptySet(),
    onShowSongInfo: (Song) -> Unit = {},
    onAddToBlacklist: (Song) -> Unit = {},
    onShare: (Song) -> Unit = {},
    onGoToAlbum: (Song) -> Unit = {},
    onGoToArtist: (Song) -> Unit = {},
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    albumOverride: Album? = null,
    songsOverride: List<Song>? = null,
    isContentLoadingOverride: Boolean? = null,
    onEditAlbum: ((
        title: String,
        artist: String,
        artworkUri: Uri?,
        removeArtwork: Boolean,
        onProgress: (Int, Int) -> Unit,
        onComplete: (successCount: Int, failCount: Int) -> Unit
    ) -> Unit)? = null,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val appSettings = remember { AppSettings.getInstance(context) }
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()
    val savedSortOrder by appSettings.albumSortOrder.collectAsState()
    val savedDiscFilter by appSettings.albumBottomSheetDiscFilter.collectAsState()
    val libraryCombineDiscs by appSettings.libraryCombineDiscs.collectAsState()

    val allAlbums by viewModel.albums.collectAsState()
    val allArtists by viewModel.artists.collectAsState()
    val album = remember(allAlbums, albumId, albumOverride) {
        albumOverride ?: allAlbums.find { it.id == albumId }
    }

    val allDisplaySongs = remember(album, songsOverride) {
        songsOverride ?: album?.songs ?: emptyList()
    }

    val sortOrder = remember(savedSortOrder) { savedSortOrder.toAlbumSortOrder() }
    val songDisplayState = remember(allDisplaySongs, sortOrder, libraryCombineDiscs, savedDiscFilter) {
        prepareAlbumSongDisplayState(
            songs = allDisplaySongs,
            sortOrder = sortOrder,
            libraryCombineDiscs = libraryCombineDiscs,
            savedDiscFilter = savedDiscFilter
        )
    }
    val displaySongs = songDisplayState.visibleSongs
    val availableDiscs = songDisplayState.availableDiscs
    val selectedDisc = songDisplayState.selectedDisc
    val shouldShowDiscFilter = !libraryCombineDiscs && availableDiscs.size > 1

    // Multi-artist picker state
    var showArtistPicker by remember { mutableStateOf(false) }
    var artistPickerCandidates by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var artistPickerSong by remember { mutableStateOf<Song?>(null) }

    val effectiveDelimiters = artistSeparatorDelimiters.ifBlank { "/;,+&" }

    fun handleArtistTap(song: Song) {
        val candidates = ArtistSeparator.splitArtistNames(
            song.artist,
            delimiters = effectiveDelimiters,
            enabled = artistSeparatorEnabled
        )
        if (candidates.size > 1) {
            artistPickerCandidates = candidates.map { artistName ->
                allArtists.firstOrNull { it.name.equals(artistName, ignoreCase = true) }
                    ?: Artist(
                        id = artistName,
                        name = artistName,
                        songs = listOf(song),
                        numberOfTracks = 1
                    )
            }
            artistPickerSong = song
            showArtistPicker = true
        } else {
            onGoToArtist(song)
        }
    }

    var description by remember(albumId) { mutableStateOf<String?>(null) }
    var isDescriptionLoading by remember(albumId) { mutableStateOf(false) }

    LaunchedEffect(albumId, albumName, album?.artist) {
        val artistName = album?.artist
        if (albumName.isNotBlank() && artistName != null && artistName.isNotBlank()) {
            isDescriptionLoading = true
            withContext(Dispatchers.IO) {
                var desc = AppleMusicCanvasProvider.getAlbumDescription(albumName, artistName)
                if (desc.isNullOrBlank()) {
                    desc = WikipediaProvider.getAlbumDescription(albumName, artistName)
                }
                withContext(Dispatchers.Main) {
                    description = desc
                    isDescriptionLoading = false
                }
            }
        }
    }

    val appleCanvasEnabled by appSettings.appleCanvasEnabled.collectAsState()
    val appleCanvasNetworkMode by appSettings.appleCanvasNetworkMode.collectAsState()
    var canvasArtwork by remember(albumId) { mutableStateOf<CanvasArtwork?>(null) }
    var canvasLoading by remember(albumId) { mutableStateOf(false) }

    LaunchedEffect(albumId, albumName, album?.artist, appleCanvasEnabled, appleCanvasNetworkMode) {
        canvasArtwork = null
        canvasLoading = false

        val artistName = album?.artist
        if (albumName.isNotBlank() && artistName != null && appleCanvasEnabled) {
            val hasNetwork = if (appleCanvasNetworkMode == CanvasNetworkMode.WIFI_ONLY) {
                NetworkUtils.isWifiConnected(context)
            } else {
                NetworkUtils.isNetworkAvailable(context)
            }
            if (hasNetwork) {
                canvasLoading = true
                val result = withContext(Dispatchers.IO) {
                    AppleMusicCanvasProvider.getByAlbumArtist(album = albumName, artist = artistName)
                }
                canvasLoading = false
                canvasArtwork = result
            }
        }
    }

    var showSongOptionsSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    var playAllPressed by remember { mutableStateOf(false) }
    var shufflePressed by remember { mutableStateOf(false) }
    val playAllScale by animateFloatAsState(targetValue = if (playAllPressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "playAllScale")
    val shuffleScale by animateFloatAsState(targetValue = if (shufflePressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "shuffleScale")

    LaunchedEffect(playAllPressed) { if (playAllPressed) { delay(150); playAllPressed = false } }
    LaunchedEffect(shufflePressed) { if (shufflePressed) { delay(150); shufflePressed = false } }

    val totalDuration = songDisplayState.totalDuration
    val displayArtist = album?.artist ?: "Unknown Artist"
    val displayArtworkUri = album?.artworkUri
    val hasCanvas = appleCanvasEnabled && canvasArtwork != null
    val backgroundColor = MaterialTheme.colorScheme.background

    if (isTablet) {
        // Animated infinite transition for backdrop orbs (like full-screen lyrics view)
        val infiniteTransition = rememberInfiniteTransition(label = "tabletBackdrop")
        val translationX1 by infiniteTransition.animateFloat(
            initialValue = -60f, targetValue = 60f,
            animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
            label = "tx1"
        )
        val translationY1 by infiniteTransition.animateFloat(
            initialValue = -40f, targetValue = 40f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
            label = "ty1"
        )
        val pulseScale1 by infiniteTransition.animateFloat(
            initialValue = 0.92f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
            label = "ps1"
        )
        val pulseScale2 by infiniteTransition.animateFloat(
            initialValue = 1.05f, targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Reverse),
            label = "ps2"
        )
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
            label = "rot"
        )

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-screen blurred backdrop (like FullScreenLyricsView)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(56.dp)
                        .alpha(0.68f)
                ) {
                    // Blurred base album art
                    if (displayArtworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).apply(ImageUtils.buildImageRequest(displayArtworkUri, albumName, context.cacheDir, M3PlaceholderType.ALBUM)).build(),
                            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Animated canvas blurred along with backdrop
                    if (hasCanvas) {
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork?.animated,
                            fallbackUrl = canvasArtwork?.videoUrl,
                            alwaysPlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Moving gradient aura 1
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .align(Alignment.TopStart)
                            .graphicsLayer {
                                translationX = translationX1
                                translationY = translationY1
                                scaleX = pulseScale1
                                scaleY = pulseScale1
                                rotationZ = rotationAngle
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // Moving gradient aura 2
                    Box(
                        modifier = Modifier
                            .size(420.dp)
                            .align(Alignment.BottomEnd)
                            .graphicsLayer {
                                translationX = -translationX1 * 0.8f
                                translationY = -translationY1 * 0.9f
                                scaleX = pulseScale2
                                scaleY = pulseScale2
                                rotationZ = -rotationAngle * 1.2f
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                // Dim layer for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.weight(0.4f).fillMaxHeight(), color = Color.Transparent) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Back,
                                            contentDescription = stringResource(R.string.cd_back),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            // Artwork card — canvas used as full backdrop, not inside the card
                            Surface(
                                modifier = Modifier.size(300.dp),
                                shape = RoundedCornerShape(32.dp),
                                shadowElevation = 16.dp
                            ) {
                                if (displayArtworkUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).apply(ImageUtils.buildImageRequest(displayArtworkUri, albumName, context.cacheDir, M3PlaceholderType.ALBUM)).build(),
                                        contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))))
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Text(text = albumName, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayArtist,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable {
                                    val song = allDisplaySongs.firstOrNull()
                                    if (song != null) handleArtistTap(song)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${allDisplaySongs.size} tracks • ${formatDuration(allDisplaySongs.sumOf { it.duration }, useHoursFormat)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                allDisplaySongs.firstOrNull()?.let { firstSong ->
                                    AudioQualityIcon(
                                        song = firstSong,
                                        iconSize = 20.dp,
                                        padding = 0.dp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        playAllPressed = true
                                        onPlayAll(displaySongs)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
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
                                        "Play All",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        shufflePressed = true
                                        onShufflePlay(displaySongs)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
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
                                        stringResource(R.string.action_shuffle),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Surface(modifier = Modifier.weight(0.6f).fillMaxHeight(), color = Color.Transparent) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 32.dp, bottom = 32.dp)
                        ) {
                            if (description != null) {
                                AboutAlbumSection(
                                    description = description!!,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            AlbumListControls(
                                shouldShowDiscFilter = shouldShowDiscFilter,
                                selectedDisc = selectedDisc,
                                availableDiscs = availableDiscs,
                                onDiscSelected = { disc ->
                                    appSettings.setAlbumBottomSheetDiscFilter(disc)
                                },
                                sortOrder = sortOrder,
                                showSortMenu = showSortMenu,
                                onShowSortMenu = { showSortMenu = true },
                                onDismissSortMenu = { showSortMenu = false },
                                onSortOrderSelected = { newOrder ->
                                    appSettings.setAlbumSortOrder(newOrder.name)
                                    showSortMenu = false
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                            itemsIndexed(displaySongs, key = { _, song -> song.id }) { index, song ->
                                AnimateIn {
                                    AlbumSongItem(
                                        song = song,
                                        index = index,
                                        totalCount = displaySongs.size,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        useHoursFormat = useHoursFormat,
                                        onClick = { onSongClick(song) },
                                        onMoreClick = {
                                            selectedSongForOptions = song
                                            showSongOptionsSheet = true
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
    } else {
        val songListState = rememberLazyListState()
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val expandedHeaderHeight = 450.dp
        val collapsedHeaderHeight = 56.dp + statusBarHeight
        val collapseDistancePx = with(density) { (expandedHeaderHeight - collapsedHeaderHeight).toPx() }

        LaunchedEffect(selectedDisc) {
            songListState.scrollToItem(0)
        }
        val collapseFraction by remember(songListState, collapseDistancePx) {
            derivedStateOf {
                if (songListState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (songListState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
                }
            }
        }
        val headerHeight by animateDpAsState(
            targetValue = lerpDp(expandedHeaderHeight, collapsedHeaderHeight, collapseFraction),
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            label = "albumHeaderHeight"
        )
        val artworkAlpha by animateFloatAsState(
            targetValue = 1f - collapseFraction,
            animationSpec = tween(180),
            label = "albumArtworkAlpha"
        )

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CollapsibleAlbumHeader(
                    albumName = albumName,
                    artist = displayArtist,
                    metadata = "${allDisplaySongs.size} tracks • ${formatDuration(totalDuration, useHoursFormat)}",
                    artworkUri = displayArtworkUri,
                    hasCanvas = hasCanvas,
                    canvasArtwork = canvasArtwork,
                    backgroundColor = backgroundColor,
                    height = headerHeight,
                    collapseFraction = collapseFraction,
                    artworkAlpha = artworkAlpha,
                    sortOrder = sortOrder,
                    showSortMenu = showSortMenu,
                    onShowSortMenu = { showSortMenu = true },
                    onDismissSortMenu = { showSortMenu = false },
                    onSortOrderSelected = { newOrder ->
                        appSettings.setAlbumSortOrder(newOrder.name)
                        showSortMenu = false
                    },
                    onBack = onBack,
                    onArtistClick = {
                        val song = allDisplaySongs.firstOrNull()
                        if (song != null) handleArtistTap(song)
                    },
                    firstSong = allDisplaySongs.firstOrNull()
                )

                AnimatedVisibility(
                    visible = allDisplaySongs.isNotEmpty(),
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut(animationSpec = tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                playAllPressed = true
                                onPlayAll(displaySongs)
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
                                "Play All",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                shufflePressed = true
                                onShufflePlay(displaySongs)
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
                                stringResource(R.string.action_shuffle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (description != null) {
                    AboutAlbumSection(
                        description = description!!,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                if (shouldShowDiscFilter) {
                    AlbumDiscFilterChips(
                        selectedDisc = selectedDisc,
                        availableDiscs = availableDiscs,
                        onDiscSelected = { disc ->
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                            appSettings.setAlbumBottomSheetDiscFilter(disc)
                        },
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                LazyColumn(
                    state = songListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(displaySongs, key = { _, song -> song.id }) { index, song ->
                        AnimateIn {
                            AlbumSongItem(
                                song = song,
                                index = index,
                                totalCount = displaySongs.size,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                useHoursFormat = useHoursFormat,
                                onClick = { onSongClick(song) },
                                onMoreClick = {
                                    selectedSongForOptions = song
                                    showSongOptionsSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Multi-artist picker
    if (showArtistPicker && artistPickerCandidates.isNotEmpty()) {
        ArtistChooserBottomSheet(
            candidateArtists = artistPickerCandidates,
            onDismiss = { showArtistPicker = false },
            onArtistSelected = { artist ->
                showArtistPicker = false
                val song = artistPickerSong
                if (song != null) {
                    onGoToArtist(song.copy(artist = artist.name))
                }
            },
            haptic = haptics
        )
    }

    if (showSongOptionsSheet && selectedSongForOptions != null) {
        PlaylistSongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismiss = { showSongOptionsSheet = false },
            onShare = {
                onShare(selectedSongForOptions!!)
                showSongOptionsSheet = false
            },
            onRemoveFromPlaylist = { }, // Not applicable to Album screen
            onPlayNext = {
                onPlayNext(selectedSongForOptions!!)
                showSongOptionsSheet = false
                Toast.makeText(context, context.getString(R.string.will_play_next, selectedSongForOptions!!.title), Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = {
                onAddToQueue(selectedSongForOptions!!)
                showSongOptionsSheet = false
                Toast.makeText(context, context.getString(R.string.added_to_queue, selectedSongForOptions!!.title), Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = {
                onAddSongToPlaylist(selectedSongForOptions!!)
                showSongOptionsSheet = false
            },
            onShowSongInfo = {
                onShowSongInfo(selectedSongForOptions!!)
                showSongOptionsSheet = false
            },
            onGoToAlbum = { /* Hidden for album screen */ },
            onGoToArtist = {
                val song = selectedSongForOptions!!
                showSongOptionsSheet = false
                handleArtistTap(song)
            },
            showRemoveFromPlaylist = false, // Always hide for albums
            showGoToAlbum = false,         // Already on the album screen
            haptics = haptics
        )
    }
}

@Composable
private fun CollapsibleAlbumHeader(
    albumName: String,
    artist: String,
    metadata: String,
    artworkUri: Uri?,
    hasCanvas: Boolean,
    canvasArtwork: CanvasArtwork?,
    backgroundColor: Color,
    height: Dp,
    collapseFraction: Float,
    artworkAlpha: Float,
    sortOrder: AlbumSortOrder,
    showSortMenu: Boolean,
    onShowShowSortMenu: (() -> Unit)? = null, // unused but keep for safety or just keep original callbacks
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortOrderSelected: (AlbumSortOrder) -> Unit,
    onBack: () -> Unit,
    onArtistClick: () -> Unit,
    firstSong: Song? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val collapsedStateAlpha = ((collapseFraction - 0.58f) / 0.42f).coerceIn(0f, 1f)
    val titleStartPadding = lerpDp(24.dp, 72.dp, collapseFraction)
    val titleEndPadding = lerpDp(24.dp, 72.dp, collapseFraction)
    val titleBottomPadding = lerpDp(30.dp, 16.dp, collapseFraction)
    val expandedMetadataAlpha = ((0.18f - collapseFraction) / 0.18f).coerceIn(0f, 1f)
    val titleStyle = if (collapseFraction > 0.72f) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineLarge
    }
    val headerChromeContainer = lerp(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
        collapsedStateAlpha
    )
    val headerChromeContent = lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.onSecondaryContainer,
        collapsedStateAlpha
    )
    val titleColor = lerp(
        MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.onSurface,
        collapsedStateAlpha
    )
    val currentKey = when (sortOrder) {
        AlbumSortOrder.TRACK_NUMBER -> "TRACK_NUMBER"
        AlbumSortOrder.TITLE_ASC, AlbumSortOrder.TITLE_DESC -> "TITLE"
        AlbumSortOrder.DURATION_ASC, AlbumSortOrder.DURATION_DESC -> "DURATION"
    }
    val isAscending = when (sortOrder) {
        AlbumSortOrder.TITLE_DESC, AlbumSortOrder.DURATION_DESC -> false
        else -> true
    }
    val sortOptions = remember(context) {
        listOf(
            RhythmSortOption("TRACK_NUMBER", context.getString(R.string.sort_track_number), RhythmIcons.FormatListNumbered),
            RhythmSortOption("TITLE", context.getString(R.string.library_sort_title), RhythmIcons.SortByAlpha),
            RhythmSortOption("DURATION", context.getString(R.string.sort_duration), RhythmIcons.AccessTime)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = artworkAlpha }
        ) {
            // Static Artwork (always rendered first as a base placeholder)
            if (artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(artworkUri, albumName, context.cacheDir, M3PlaceholderType.ALBUM))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
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
                        )
                )
            }

            // Animated Canvas Player (rendered on top of base artwork, fades in once ready)
            if (hasCanvas) {
                CanvasArtworkPlayer(
                    primaryUrl = canvasArtwork?.animated,
                    fallbackUrl = canvasArtwork?.videoUrl,
                    alwaysPlay = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.6f),
                            backgroundColor
                        ),
                        startY = 300f
                    )
                )
                .graphicsLayer { alpha = 1f - collapseFraction }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = collapsedStateAlpha * 0.82f))
        )

        FilledIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = headerChromeContainer,
                contentColor = headerChromeContent
            )
        ) {
            Icon(
                imageVector = RhythmIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
        ) {
            FilledIconButton(
                onClick = onShowSortMenu,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = headerChromeContainer,
                    contentColor = headerChromeContent
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
                onDismissRequest = onDismissSortMenu,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .widthIn(min = 250.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp)
            ) {
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
                        onSortOrderSelected(newOrder)
                    },
                    onDirectionToggled = { asc ->
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        val newOrder = when (currentKey) {
                            "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                            "TITLE" -> if (asc) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                            "DURATION" -> if (asc) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                            else -> AlbumSortOrder.TRACK_NUMBER
                        }
                        onSortOrderSelected(newOrder)
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = titleStartPadding, end = titleEndPadding, bottom = titleBottomPadding)
        ) {
            Text(
                text = albumName,
                style = titleStyle,
                fontWeight = if (collapseFraction > 0.72f) FontWeight.Bold else FontWeight.Black,
                color = titleColor,
                maxLines = if (collapseFraction > 0.72f) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (expandedMetadataAlpha > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = expandedMetadataAlpha },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = onArtistClick)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    firstSong?.let { song ->
                        AudioQualityIcon(
                            song = song,
                            iconSize = 36.dp, // Takes two lines space, making it bigger
                            padding = 0.dp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumDiscFilterChips(
    selectedDisc: Int,
    availableDiscs: List<Int>,
    onDiscSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedDisc == 0,
                onClick = { onDiscSelected(0) },
                label = {
                    Text(
                        text = stringResource(R.string.bottomsheet_all_discs),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = if (selectedDisc == 0) {
                    {
                        Icon(
                            imageVector = RhythmIcons.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }

        items(availableDiscs) { disc ->
            FilterChip(
                selected = selectedDisc == disc,
                onClick = { onDiscSelected(disc) },
                label = {
                    Text(
                        text = stringResource(R.string.bottomsheet_disc_option, disc),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = if (selectedDisc == disc) {
                    {
                        Icon(
                            imageVector = RhythmIcons.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun AlbumListControls(
    shouldShowDiscFilter: Boolean,
    selectedDisc: Int,
    availableDiscs: List<Int>,
    onDiscSelected: (Int) -> Unit,
    sortOrder: AlbumSortOrder,
    showSortMenu: Boolean,
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortOrderSelected: (AlbumSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val currentKey = when (sortOrder) {
        AlbumSortOrder.TRACK_NUMBER -> "TRACK_NUMBER"
        AlbumSortOrder.TITLE_ASC, AlbumSortOrder.TITLE_DESC -> "TITLE"
        AlbumSortOrder.DURATION_ASC, AlbumSortOrder.DURATION_DESC -> "DURATION"
    }
    val isAscending = when (sortOrder) {
        AlbumSortOrder.TITLE_DESC, AlbumSortOrder.DURATION_DESC -> false
        else -> true
    }
    val sortOptions = remember(context) {
        listOf(
            RhythmSortOption("TRACK_NUMBER", context.getString(R.string.sort_track_number), RhythmIcons.FormatListNumbered),
            RhythmSortOption("TITLE", context.getString(R.string.library_sort_title), RhythmIcons.SortByAlpha),
            RhythmSortOption("DURATION", context.getString(R.string.sort_duration), RhythmIcons.AccessTime)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (shouldShowDiscFilter) {
            AlbumDiscFilterChips(
                selectedDisc = selectedDisc,
                availableDiscs = availableDiscs,
                onDiscSelected = { disc ->
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                    onDiscSelected(disc)
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 0.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Box {
            FilledTonalIconButton(
                onClick = onShowSortMenu,
                modifier = Modifier.size(40.dp),
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
                onDismissRequest = onDismissSortMenu,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .widthIn(min = 250.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp)
            ) {
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
                        onSortOrderSelected(newOrder)
                    },
                    onDirectionToggled = { asc ->
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        val newOrder = when (currentKey) {
                            "TRACK_NUMBER" -> AlbumSortOrder.TRACK_NUMBER
                            "TITLE" -> if (asc) AlbumSortOrder.TITLE_ASC else AlbumSortOrder.TITLE_DESC
                            "DURATION" -> if (asc) AlbumSortOrder.DURATION_ASC else AlbumSortOrder.DURATION_DESC
                            else -> AlbumSortOrder.TRACK_NUMBER
                        }
                        onSortOrderSelected(newOrder)
                    }
                )
            }
        }
    }
}

@Composable
private fun AboutAlbumSection(
    description: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "About Album",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = !expanded }
            )
            if (description.length > 150) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (expanded) "Show Less" else "Show More",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlbumSongItem(
    song: Song,
    onClick: () -> Unit,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    useHoursFormat: Boolean = false,
    index: Int = 0,
    totalCount: Int = 0,
    itemShape: RoundedCornerShape? = null,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isCurrentSong = currentSong?.id == song.id

    val containerColor by animateColorAsState(
        targetValue = when {
            isCurrentSong -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(300),
        label = "containerColor"
    )

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = itemShape ?: groupedAlbumItemShape(index, totalCount),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with expressive shape — matches library song items
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = rememberExpressiveShapeFor(
                        ExpressiveShapeTarget.SONG_ART,
                        fallbackShape = MaterialTheme.shapes.large
                    ),
                    border = if (isCurrentSong) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    M3ImageUtils.TrackImage(
                        imageUrl = song.artworkUri,
                        trackName = song.title,
                        modifier = Modifier.fillMaxSize(),
                        applyExpressiveShape = false
                    )
                }
                if (isCurrentSong && isPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .offset(x = 4.dp, y = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PlayingEqIcon(
                                modifier = Modifier.size(width = 10.dp, height = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                isPlaying = isPlaying,
                                bars = 3
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    AudioQualityIcon(
                        song = song,
                        iconSize = 16.dp,
                        padding = 0.dp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (song.duration > 0) {
                Text(
                    text = formatDuration(song.duration, useHoursFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // 3-dot button matching library screen style: FilledIconButton, primaryContainer, 32×44dp, pill shape
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                    onMoreClick()
                },
                modifier = Modifier
                    .width(32.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(50),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = RhythmIcons.More,
                    contentDescription = stringResource(R.string.content_desc_more_options),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun groupedAlbumItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp,
            bottomStart = 6.dp, bottomEnd = 6.dp
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = 6.dp, topEnd = 6.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp
        )
        else -> RoundedCornerShape(6.dp)
    }
}

@Composable
private fun AnimateIn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(durationMillis = 300, delayMillis = 50), label = "alpha")
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.98f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")

    Box(modifier = modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)) {
        content()
    }
}
