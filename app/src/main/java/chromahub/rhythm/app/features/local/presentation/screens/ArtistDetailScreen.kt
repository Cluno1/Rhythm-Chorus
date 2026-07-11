@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package chromahub.rhythm.app.features.local.presentation.screens

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.ArtistCollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.common.RhythmSongMenuContent
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

private data class ArtistDetailContent(
    val songs: List<Song>,
    val albums: List<Album>
)

@Composable
fun ArtistDetailScreen(
    artistName: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToQueueAll: (List<Song>) -> Unit = {},
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onPlayerClick: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit = {},
    favoriteSongs: Set<String> = emptySet(),
    onShowSongInfo: (Song) -> Unit = {},
    showPlayNextAction: Boolean = true,
    showAddToQueueAction: Boolean = true,
    showToggleFavoriteAction: Boolean = true,
    showAddToPlaylistAction: Boolean = true,
    showSongInfoAction: Boolean = true,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    artistOverride: Artist? = null,
    songsOverride: List<Song>? = null,
    albumsOverride: List<Album>? = null,
    isContentLoadingOverride: Boolean? = null,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val groupByAlbumArtist by appSettings.groupByAlbumArtist.collectAsState()
    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
    
    // Get songs and albums from viewModel
    val allSongs by viewModel.filteredSongs.collectAsState()
    val allAlbums by viewModel.albums.collectAsState()
    val allArtists by viewModel.artists.collectAsState()
    
    // Find the artist
    val artist = remember(allArtists, artistName, artistOverride) {
        artistOverride ?: allArtists.find { it.name == artistName }
    }

    val artistContent by produceState<ArtistDetailContent?>(
        initialValue = if (songsOverride != null && albumsOverride != null) {
            ArtistDetailContent(songs = songsOverride, albums = albumsOverride)
        } else {
            null
        },
        allSongs,
        allAlbums,
        artistName,
        groupByAlbumArtist,
        artistSeparatorEnabled,
        artistSeparatorDelimiters,
        songsOverride,
        albumsOverride
    ) {
        if (songsOverride != null && albumsOverride != null) {
            value = ArtistDetailContent(songs = songsOverride, albums = albumsOverride)
            return@produceState
        }

        value = withContext(Dispatchers.Default) {
            fun splitArtistNames(artistNameStr: String): List<String> {
                return chromahub.rhythm.app.util.ArtistSeparator.splitArtistNames(
                    artistName = artistNameStr,
                    delimiters = artistSeparatorDelimiters,
                    enabled = artistSeparatorEnabled
                )
            }

            fun songMatchesArtist(song: Song): Boolean {
                val artistField = if (groupByAlbumArtist) {
                    val explicitAlbumArtist = song.albumArtist?.trim().orEmpty()
                    if (explicitAlbumArtist.isNotBlank() && !explicitAlbumArtist.equals("<unknown>", ignoreCase = true)) {
                        explicitAlbumArtist
                    } else {
                        song.artist
                    }
                } else {
                    song.artist
                }

                return splitArtistNames(artistField).any { it.equals(artistName, ignoreCase = true) }
            }

            val songs = allSongs.filter(::songMatchesArtist)
            val albums = allAlbums.filter {album -> album.songs.any {song -> songMatchesArtist(song) } }
            ArtistDetailContent(songs = songs, albums = albums)
        }
    }

    val artistSongs = songsOverride ?: artistContent?.songs.orEmpty()
    val artistAlbums = albumsOverride ?: artistContent?.albums.orEmpty()
    val isArtistContentLoading = isContentLoadingOverride ?: (
        if (songsOverride != null && albumsOverride != null) {
            false
        } else {
            artistContent == null
        }
        )

    val imageRefreshRequestedArtistIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(artist?.id, artist?.artworkUri, artistOverride) {
        if (artistOverride != null) {
            return@LaunchedEffect
        }
        val currentArtist = artist ?: return@LaunchedEffect
        if (currentArtist.artworkUri == null && !imageRefreshRequestedArtistIds.contains(currentArtist.id)) {
            imageRefreshRequestedArtistIds.add(currentArtist.id)
            viewModel.refreshArtistImage(currentArtist.id)
        }
    }
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Info and Artwork (40%)
                Surface(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Back Button
                        IconButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                onBack()
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Navigation.Back,
                                contentDescription = "Back"
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                modifier = Modifier.size(260.dp),
                                shape = RoundedCornerShape(32.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .apply(
                                            ImageUtils.buildImageRequest(
                                                artist?.artworkUri,
                                                artistName,
                                                context.cacheDir,
                                                M3PlaceholderType.ARTIST
                                            )
                                        )
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val songsCount = artistSongs.size.takeIf { it > 0 }
                                    ?: artist?.numberOfTracks
                                    ?: 0
                                val albumsCount = artistAlbums.size.takeIf { it > 0 }
                                    ?: artist?.numberOfAlbums
                                    ?: 0

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$songsCount songs",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "$albumsCount albums",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Panel: Scrollable actions and content (60%)
                Surface(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    color = Color.Transparent
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 24.dp, bottom = (LocalMiniPlayerPadding.current.calculateBottomPadding() + 24.dp).coerceAtLeast(100.dp), start = 16.dp, end = 24.dp)
                    ) {
                        item {
                            if (isArtistContentLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                ArtistActionButtons(
                                    artistSongs = artistSongs,
                                    onPlayAll = {
                                        if (artistSongs.isNotEmpty()) {
                                            onPlayAll(artistSongs)
                                            onPlayerClick()
                                        }
                                    },
                                    onShufflePlay = {
                                        if (artistSongs.isNotEmpty()) {
                                            onShufflePlay(artistSongs)
                                            onPlayerClick()
                                        }
                                    },
                                    onAddToQueueAll = {
                                        if (artistSongs.isNotEmpty()) {
                                            onAddToQueueAll(artistSongs)
                                        }
                                    },
                                    haptics = haptics
                                )
                            }
                        }

                        if (!isArtistContentLoading && artistAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                ArtistAlbumsSection(
                                    artistAlbums = artistAlbums,
                                    viewModel = viewModel,
                                    onAlbumClick = onAlbumClick,
                                    haptics = haptics
                                )
                            }
                        }

                        if (!isArtistContentLoading && artistSongs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = context.getString(R.string.bottomsheet_songs),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = RhythmIcons.Music.Song,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = "${artistSongs.size}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        artistSongs.forEach { song ->
                                            ArtistSongItem(
                                                song = song,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onSongClick(song)
                                                    onPlayerClick()
                                                },
                                                onAddToQueue = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onAddToQueue(song)
                                                },
                                                onAddToPlaylist = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onAddSongToPlaylist(song)
                                                },
                                                onPlayNext = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onPlayNext(song)
                                                },
                                                onToggleFavorite = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onToggleFavorite(song)
                                                },
                                                isFavorite = favoriteSongs.contains(song.id),
                                                onShowSongInfo = {
                                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                                    onShowSongInfo(song)
                                                },
                                                showPlayNextAction = showPlayNextAction,
                                                showAddToQueueAction = showAddToQueueAction,
                                                showToggleFavoriteAction = showToggleFavoriteAction,
                                                showAddToPlaylistAction = showAddToPlaylistAction,
                                                showSongInfoAction = showSongInfoAction,
                                                currentSong = currentSong,
                                                isPlaying = isPlaying,
                                                useHoursFormat = useHoursFormat,
                                                haptics = haptics
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
    } else {
        ArtistCollapsibleHeaderScreen(
            title = artistName,
            artist = artist,
            artworkUri = artist?.artworkUri?.toString(),
            artistName = artistName,
            artistSongsCount = artistSongs.size.takeIf { it > 0 }
                ?: artist?.numberOfTracks
                ?: 0,
            artistAlbumsCount = artistAlbums.size.takeIf { it > 0 }
                ?: artist?.numberOfAlbums
                ?: 0,
            showBackButton = true,
            onBackClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                onBack()
            }
        ) { modifier ->
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(bottom = (LocalMiniPlayerPadding.current.calculateBottomPadding() + 24.dp).coerceAtLeast(100.dp))
            ) {
                // Action Buttons Section
                item {
                    Spacer(modifier = Modifier.height(38.dp))
                    if (isArtistContentLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ArtistActionButtons(
                            artistSongs = artistSongs,
                            onPlayAll = {
                                if (artistSongs.isNotEmpty()) {
                                    onPlayAll(artistSongs)
                                    onPlayerClick()
                                }
                            },
                            onShufflePlay = {
                                if (artistSongs.isNotEmpty()) {
                                    onShufflePlay(artistSongs)
                                    onPlayerClick()
                                }
                            },
                            onAddToQueueAll = {
                                if (artistSongs.isNotEmpty()) {
                                    onAddToQueueAll(artistSongs)
                                }
                            },
                            haptics = haptics
                        )
                    }
                }

                // Albums Section
                if (!isArtistContentLoading && artistAlbums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ArtistAlbumsSection(
                            artistAlbums = artistAlbums,
                            viewModel = viewModel,
                            onAlbumClick = onAlbumClick,
                            haptics = haptics
                        )
                    }
                }

                // Songs Section
                if (!isArtistContentLoading && artistSongs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Songs Section Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = context.getString(R.string.bottomsheet_songs),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = RhythmIcons.Music.Song,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${artistSongs.size}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                // Songs List
                                artistSongs.forEach { song ->
                                    ArtistSongItem(
                                        song = song,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onSongClick(song)
                                            onPlayerClick()
                                        },
                                        onAddToQueue = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onAddToQueue(song)
                                        },
                                        onAddToPlaylist = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onAddSongToPlaylist(song)
                                        },
                                        onPlayNext = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onPlayNext(song)
                                        },
                                        onToggleFavorite = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onToggleFavorite(song)
                                        },
                                        isFavorite = favoriteSongs.contains(song.id),
                                        onShowSongInfo = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                                            onShowSongInfo(song)
                                        },
                                        showPlayNextAction = showPlayNextAction,
                                        showAddToQueueAction = showAddToQueueAction,
                                        showToggleFavoriteAction = showToggleFavoriteAction,
                                        showAddToPlaylistAction = showAddToPlaylistAction,
                                        showSongInfoAction = showSongInfoAction,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        useHoursFormat = useHoursFormat,
                                        haptics = haptics
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

@Composable
private fun ArtistHeroHeader(
    artist: Artist?,
    artistName: String,
    artistSongsCount: Int,
    artistAlbumsCount: Int,
    showContent: Boolean
) {
    val context = LocalContext.current
    val headerAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(600),
        label = "headerAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .graphicsLayer { alpha = headerAlpha }
    ) {
        // Background Image with Gradient
        AsyncImage(
            model = ImageRequest.Builder(context)
                .apply(
                    ImageUtils.buildImageRequest(
                        artist?.artworkUri,
                        artistName,
                        context.cacheDir,
                        M3PlaceholderType.ARTIST
                    )
                )
                .build(),
            contentDescription = stringResource(R.string.artist_artwork_description, artistName),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Multi-layer gradient overlay for better readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
        
        // Artist Info - Bottom aligned
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Album,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$artistAlbumsCount albums",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Music.Song,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "$artistSongsCount songs",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistActionButtons(
    artistSongs: List<Song>,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddToQueueAll: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play All Button
            Button(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                    playAllPressed = true
                    onPlayAll()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = playAllScale
                        scaleY = playAllScale
                    },
                shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = artistSongs.isNotEmpty()
            ) {
                Icon(
                    imageVector = RhythmIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.action_play_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Shuffle Button
            FilledTonalButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                    shufflePressed = true
                    onShufflePlay()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = shuffleScale
                        scaleY = shuffleScale
                    },
                shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                enabled = artistSongs.isNotEmpty()
            ) {
                Icon(
                    imageVector = RhythmIcons.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.cd_shuffle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Add to Queue Button
        FilledTonalButton(
            onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                addToQueuePressed = true
                onAddToQueueAll()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    scaleX = addToQueueScale
                    scaleY = addToQueueScale
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            enabled = artistSongs.isNotEmpty()
        ) {
            Icon(
                imageVector = RhythmIcons.Queue,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.action_add_to_queue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ArtistAlbumsSection(
    artistAlbums: List<Album>,
    viewModel: MusicViewModel,
    onAlbumClick: (Album) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.bottomsheet_albums),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = artistAlbums,
                key = { "artistalbum_${it.id}" }
            ) { album ->
                ArtistAlbumCard(
                    album = album,
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                        onAlbumClick(album)
                    },
                    onPlay = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        viewModel.playAlbum(album)
                    },
                    haptics = haptics
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    val albumArtShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.ALBUM_ART,
        fallbackShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    )
    
    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
            onClick()
        },
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
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
                        .clip(albumArtShape)
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                            onPlay()
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Play,
                            contentDescription = stringResource(R.string.content_desc_play_album),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${album.numberOfSongs} ${if (album.numberOfSongs == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArtistSongItem(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean,
    onShowSongInfo: () -> Unit,
    showPlayNextAction: Boolean = true,
    showAddToQueueAction: Boolean = true,
    showToggleFavoriteAction: Boolean = true,
    showAddToPlaylistAction: Boolean = true,
    showSongInfoAction: Boolean = true,
    currentSong: Song?,
    isPlaying: Boolean,
    useHoursFormat: Boolean,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    var showDropdown by remember { mutableStateOf(false) }
    
    val isCurrentSong = currentSong?.id == song.id
    
    val titleColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "titleColor"
    )
    
    val albumColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "albumColor"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(300),
        label = "containerColor"
    )

    val hasOverflowActions =
        showPlayNextAction ||
            showAddToQueueAction ||
            showToggleFavoriteAction ||
            showAddToPlaylistAction ||
            showSongInfoAction
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        ListItem(
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = albumColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (song.duration > 0) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = albumColor
                        )
                        
                        val durationText = formatDuration(song.duration, useHoursFormat)
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = albumColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            leadingContent = {
                val songArtShape = rememberExpressiveShapeFor(
                    ExpressiveShapeTarget.SONG_ART,
                    fallbackShape = RoundedCornerShape(12.dp)
                )
                Box {
                    Surface(
                        shape = songArtShape,
                        modifier = Modifier.size(48.dp),
                        tonalElevation = 2.dp
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .apply(
                                    ImageUtils.buildImageRequest(
                                        song.artworkUri,
                                        song.title,
                                        context.cacheDir,
                                        M3PlaceholderType.TRACK
                                    )
                                )
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    if (isCurrentSong && isPlaying) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .offset(x = 4.dp, y = 4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayingEqIcon(
                                    modifier = Modifier.size(width = 13.dp, height = 11.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    isPlaying = isPlaying,
                                    bars = 3
                                )
                            }
                        }
                    }
                }
            },
            trailingContent = {
                if (hasOverflowActions) {
                FilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                        showDropdown = true
                    },
                    modifier = Modifier.size(width = 42.dp, height = 38.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                        .padding(4.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    RhythmSongMenuContent(
                        song = song,
                        onPlayNext = if (showPlayNextAction) {
                            {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                showDropdown = false
                                onPlayNext()
                            }
                        } else null,
                        onAddToQueue = if (showAddToQueueAction) {
                            {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                showDropdown = false
                                onAddToQueue()
                            }
                        } else null,
                        isFavorite = isFavorite,
                        onToggleFavorite = if (showToggleFavoriteAction) {
                            {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                showDropdown = false
                                onToggleFavorite()
                            }
                        } else null,
                        onAddToPlaylist = if (showAddToPlaylistAction) {
                            {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                showDropdown = false
                                onAddToPlaylist()
                            }
                        } else null,
                        onShowSongInfo = if (showSongInfoAction) {
                            {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                showDropdown = false
                                onShowSongInfo()
                            }
                        } else null
                    )
                }
                }
            },
            modifier = Modifier.clickable(onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                onClick()
            }),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )
        }
    }
}
