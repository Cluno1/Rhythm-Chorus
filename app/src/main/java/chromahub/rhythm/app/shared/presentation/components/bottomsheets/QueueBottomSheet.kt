package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.shared.presentation.components.common.DragDropLazyColumn
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveClickableSurface
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledTonalIconButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.util.ImageUtils
import androidx.compose.ui.res.stringResource

private fun groupedQueueItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    if (totalCount <= 1) return RoundedCornerShape(24.dp)

    return when (index) {
        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        totalCount - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(6.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    currentSong: Song?,
    queue: List<Song>,
    currentQueueIndex: Int = 0,
    isShuffleEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onSongClick: (Song) -> Unit,
    onSongClickAtIndex: (Int) -> Unit = { _ -> }, // New parameter for index-based clicking
    onDismiss: () -> Unit,
    onRemoveSongAtIndex: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onAddSongsClick: () -> Unit = {},
    onClearQueue: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val hidePlayedQueueSongs by appSettings.hidePlayedQueueSongs.collectAsState()
    val showAlreadyPlayedSongsInQueue = !hidePlayedQueueSongs
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )
    
    val contentTranslation by animateFloatAsState(
        targetValue = if (showContent) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentTranslation"
    )

    LaunchedEffect(Unit) {
        delay(50) // Reduced delay for faster appearance
        showContent = true
    }

    // Use the queue directly for display, create mutable version only for reordering operations
    val displayQueue = queue
    val mutableQueue = remember { mutableStateListOf<Song>() }
    
    // Update mutableQueue when displayQueue changes
    LaunchedEffect(displayQueue) {
        mutableQueue.clear()
        mutableQueue.addAll(displayQueue)
        Log.d("QueueBottomSheet", "Updated displayQueue with ${displayQueue.size} songs")
        Log.d("QueueBottomSheet", "First 5 songs in displayQueue:")
        displayQueue.take(5).forEachIndexed { idx, song ->
            Log.d("QueueBottomSheet", "  $idx: ${song.title} by ${song.artist}")
        }
    }

    ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with title and actions
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                QueueHeader(
                    queueSize = displayQueue.size,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    onAddSongsClick = onAddSongsClick,
                    onClearQueue = if (displayQueue.isNotEmpty()) onClearQueue else null,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Queue settings info and warnings
            if (displayQueue.isNotEmpty()) {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    QueueSettingsInfo(
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        hidePlayedSongs = hidePlayedQueueSongs,
                        queueSize = displayQueue.size
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (displayQueue.isEmpty()) {
                // Empty queue state
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    EmptyQueueContent(
                        onAddSongsClick = onAddSongsClick
                    )
                }
            } else {
                // Now Playing section - show current song separately
                currentSong?.let { song ->
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        NowPlayingCard(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                    }
                    
                    // Add spacing between Now Playing and the rest
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val currentSongIndexInQueue = currentSong
                    ?.let { song -> displayQueue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } }
                    ?.coerceIn(0, displayQueue.lastIndex.coerceAtLeast(0))
                    ?: currentQueueIndex.coerceIn(0, displayQueue.lastIndex.coerceAtLeast(0))
                val isRepeatAll = repeatMode == Player.REPEAT_MODE_ALL
                val shouldHidePlayedSongs = !showAlreadyPlayedSongsInQueue && !isShuffleEnabled
                // Build visible queue according to current playback behavior.
                val visibleQueue = if (isShuffleEnabled) {
                    val upcomingInCurrentCycle =
                        ((currentSongIndexInQueue + 1)..displayQueue.lastIndex).map { index ->
                            index to displayQueue[index]
                        }
                    val wrappedForRepeatAll =
                        if (isRepeatAll && currentSongIndexInQueue > 0) {
                            (0 until currentSongIndexInQueue).map { index ->
                                index to displayQueue[index]
                            }
                        } else {
                            emptyList()
                        }
                    upcomingInCurrentCycle + wrappedForRepeatAll
                } else {
                    displayQueue.mapIndexedNotNull { index, song ->
                        if (shouldHidePlayedSongs && index < currentSongIndexInQueue) return@mapIndexedNotNull null
                        if (index == currentSongIndexInQueue) null else index to song
                    }
                }

                if (visibleQueue.isNotEmpty()) {
                    // Queue header for visible queue songs
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.bottomsheet_up_next),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Text(
                                text = "${visibleQueue.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Queue list with reordering using custom drag and drop (disabled when shuffle is enabled)
                    val lazyListState = rememberLazyListState()
                    
                    if (isShuffleEnabled) {
                        // When shuffle is enabled, show queue but disable reordering
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            itemsIndexed(
                                items = visibleQueue,
                                key = { _, queueItem -> "${queueItem.first}_${queueItem.second.id}" }
                            ) { index, queueItem ->
                                val actualQueuePosition = queueItem.first
                                val song = queueItem.second
                                val isPlayed = !isShuffleEnabled && actualQueuePosition < currentSongIndexInQueue
                                
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    AnimateIn {
                                        QueueItem(
                                            song = song,
                                            index = actualQueuePosition,
                                            itemShape = groupedQueueItemShape(index, visibleQueue.size),
                                            isPlayed = isPlayed,
                                            isDragging = false, // Never dragging when shuffle is enabled
                                            onSongClick = { 
                                                // Use index-based click to handle duplicate songs correctly
                                                onSongClickAtIndex(actualQueuePosition)
                                            },
                                            onRemove = { 
                                                try {
                                                    val indexToRemove = mutableQueue.indexOf(song)
                                                    if (indexToRemove >= 0 && indexToRemove < mutableQueue.size) {
                                                        mutableQueue.removeAt(indexToRemove)
                                                    }
                                                    onRemoveSongAtIndex(actualQueuePosition)
                                                } catch (e: Exception) {
                                                    // Handle error silently
                                                }
                                            },
                                            showDragHandle = false // Hide drag handle when shuffle is enabled
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Normal drag and drop when shuffle is disabled
                        DragDropLazyColumn(
                            items = visibleQueue,
                            modifier = Modifier.fillMaxWidth(),
                            lazyListState = lazyListState,
                            onMove = { fromIndex, toIndex ->
                                val actualFromIndex = visibleQueue[fromIndex].first
                                val actualToIndex = visibleQueue[toIndex].first
                                onMoveQueueItem(actualFromIndex, actualToIndex)
                            },
                            itemKey = { queueItem -> "${queueItem.first}_${queueItem.second.id}" }
                        ) { queueItem, isDragging, visibleIndex ->
                            val actualQueuePosition = queueItem.first
                            val song = queueItem.second
                            val isPlayed = !isShuffleEnabled && actualQueuePosition < currentSongIndexInQueue
                            
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                AnimateIn {
                                    QueueItem(
                                        song = song,
                                        index = actualQueuePosition,
                                        itemShape = groupedQueueItemShape(visibleIndex, visibleQueue.size),
                                        isPlayed = isPlayed,
                                        isDragging = isDragging,
                                        onSongClick = { 
                                            // Use index-based click to handle duplicate songs correctly
                                            onSongClickAtIndex(actualQueuePosition)
                                        },
                                        onRemove = { 
                                            try {
                                                val indexToRemove = mutableQueue.indexOf(song)
                                                if (indexToRemove >= 0 && indexToRemove < mutableQueue.size) {
                                                    mutableQueue.removeAt(indexToRemove)
                                                }
                                                onRemoveSongAtIndex(actualQueuePosition)
                                            } catch (e: Exception) {
                                                // Handle error silently
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (currentSong != null) {
                    // Show empty up next content when only current song is in queue
                    // Add more spacing before empty state
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    EmptyUpNextContent(
                        onAddSongsClick = onAddSongsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    queueSize: Int,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onAddSongsClick: () -> Unit,
    onClearQueue: (() -> Unit)? = null,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = context.getString(R.string.bottomsheet_queue),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (queueSize > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        text = if (queueSize == 1) "1 song" else "$queueSize songs",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shuffle toggle button
            FilledTonalIconButton(
                onClick = onToggleShuffle,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isShuffleEnabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isShuffleEnabled) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Shuffle,
                    contentDescription = if (isShuffleEnabled) "Disable shuffle" else "Enable shuffle",
                    modifier = Modifier.size(20.dp)
                )
            }

            val repeatEnabled = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RhythmIcons.RepeatOne
                else -> RhythmIcons.Repeat
            }

            FilledTonalIconButton(
                onClick = onToggleRepeat,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (repeatEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (repeatEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = if (repeatEnabled) "Disable repeat" else "Enable repeat",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            
            // Clear queue button (only show if queue is not empty)
            onClearQueue?.let { clearAction ->
                FilledTonalIconButton(
                    onClick = clearAction,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                    imageVector = RhythmIcons.Delete,
                        contentDescription = stringResource(R.string.content_desc_clear_queue),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Subtle pulsing animation for the Now Playing indicator
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .apply(ImageUtils.buildImageRequest(
                            song.artworkUri,
                            song.title,
                            LocalContext.current.cacheDir,
                            M3PlaceholderType.TRACK
                        ))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = context.getString(R.string.now_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Playing indicator with pulse animation
            Icon(
                imageVector = RhythmIcons.MusicNote,
                contentDescription = stringResource(R.string.content_desc_now_playing),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = pulseAlpha),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    index: Int,
    itemShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    isPlayed: Boolean,
    isDragging: Boolean,
    onSongClick: () -> Unit,
    onRemove: () -> Unit,
    showDragHandle: Boolean = true
) {
    val context = LocalContext.current

    val dragCardScale by animateFloatAsState(
        targetValue = if (isDragging) 1.015f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragCardScale"
    )

    val dragCardOffsetY by animateFloatAsState(
        targetValue = if (isDragging) -4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragCardOffsetY"
    )

    val cardColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.secondaryContainer
            isPlayed -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 180),
        label = "queueItemCardColor"
    )

    val titleColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.onSecondaryContainer
            isPlayed -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 180),
        label = "queueItemTitleColor"
    )

    val subtitleColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            isPlayed -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "queueItemSubtitleColor"
    )
    
    val songArtShape = rememberExpressiveShapeFor(
        ExpressiveShapeTarget.SONG_ART,
        fallbackShape = RoundedCornerShape(8.dp)
    )

    ExpressiveClickableSurface(
        onClick = onSongClick,
        color = cardColor,
        tonalElevation = if (isDragging) 0.dp else 1.dp,
        shadowElevation = 0.dp,
        shape = itemShape,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = dragCardScale
                scaleY = dragCardScale
                translationY = dragCardOffsetY
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number indicator with updated style
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDragging) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Album art
            Surface(
                modifier = Modifier.size(48.dp),
                shape = songArtShape,
                tonalElevation = 2.dp
            ) {
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
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Drag handle with improved visual feedback (only show if enabled)
            if (showDragHandle) {
                val handleScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.3f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "handleScale"
                )
                
                Icon(
                    imageVector = RhythmIcons.DragHandle,
                    contentDescription = stringResource(R.string.content_desc_drag_reorder),
                    tint = if (isDragging)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else if (isPlayed)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = handleScale
                            scaleY = handleScale
                        }
                )
                        
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Remove button with hover effect
            var isPressed by remember { mutableStateOf(false) }
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "buttonScale"
            )
            
            ExpressiveFilledTonalIconButton(
                onClick = {
                    isPressed = true
                    onRemove()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                ) {
                Icon(
                    imageVector = MaterialSymbolIcon("clear"),
                    contentDescription = stringResource(R.string.content_desc_remove_from_queue),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QueueSettingsInfo(
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    hidePlayedSongs: Boolean,
    queueSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shuffle warning/info
        if (isShuffleEnabled) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (repeatMode == Player.REPEAT_MODE_ALL) {
                            "Shuffle is enabled - up next wraps to the start when the current cycle ends"
                        } else {
                            "Shuffle is enabled - showing next songs in current shuffle order"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Hide played songs info
        if (hidePlayedSongs && queueSize > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.queuebottomsheet_played_songs_are_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimateIn(
    delay: Int = 50,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 0),
        label = "alpha"
    )

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val translationY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationY"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale,
            translationY = translationY
        )
    ) {
        content()
    }
}

@Composable
private fun EmptyQueueContent(
    onAddSongsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated empty state with better design
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = RhythmIcons.Queue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = context.getString(R.string.queue_empty),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = context.getString(R.string.queue_add_songs),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Better styled button
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable { onAddSongsClick() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.browse_library),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyUpNextContent(
    onAddSongsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp), // Increased height for better spacing
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Enhanced empty state design
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(72.dp) // Increased size
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = RhythmIcons.Queue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp) // Increased size
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Enhanced text styling
            Text(
                text = context.getString(R.string.queue_no_more),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = context.getString(R.string.queue_add_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Enhanced button styling
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.clickable { onAddSongsClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Add,
                        contentDescription = null,
                        
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.add_more_songs),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
