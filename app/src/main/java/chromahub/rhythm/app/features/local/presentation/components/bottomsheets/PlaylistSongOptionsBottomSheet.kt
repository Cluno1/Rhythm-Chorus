package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowSongInfo: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    showRemoveFromPlaylist: Boolean = true,
    haptics: HapticFeedback
) {
    val context = LocalContext.current
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
        targetValue = if (showContent) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentTranslation"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }
    
    ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        onDismissRequest = onDismiss,
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
                .verticalScroll(rememberScrollState())
        ) {
            // Header with song info
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.playlistsongoptionsbottomsheet_song_options),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Song title and artist
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions section with grid layout
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer

                    val gridItems = remember {
                        buildList {
                            add(
                                OptionItem(
                                    icon = RhythmIcons.SkipNext,
                                    text = context.getString(R.string.action_play_next),
                                    containerColor = primaryContainer,
                                    iconColor = onPrimaryContainer,
                                    onClick = onPlayNext
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.Queue,
                                    text = context.getString(R.string.action_add_to_queue),
                                    containerColor = primaryContainer,
                                    iconColor = onPrimaryContainer,
                                    onClick = onAddToQueue
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.AddToPlaylist,
                                    text = context.getString(R.string.content_desc_add_to_playlist),
                                    containerColor = primaryContainer,
                                    iconColor = onPrimaryContainer,
                                    onClick = onAddToPlaylist
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.Album,
                                    text = context.getString(R.string.multiselectionbottomsheet_go_to_album),
                                    containerColor = secondaryContainer,
                                    iconColor = onSecondaryContainer,
                                    onClick = onGoToAlbum
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.Artist,
                                    text = context.getString(R.string.multiselectionbottomsheet_go_to_artist),
                                    containerColor = secondaryContainer,
                                    iconColor = onSecondaryContainer,
                                    onClick = onGoToArtist
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.Info,
                                    text = context.getString(R.string.action_song_info),
                                    containerColor = secondaryContainer,
                                    iconColor = onSecondaryContainer,
                                    onClick = onShowSongInfo
                                )
                            )
                            add(
                                OptionItem(
                                    icon = RhythmIcons.Share,
                                    text = context.getString(R.string.action_share),
                                    containerColor = secondaryContainer,
                                    iconColor = onSecondaryContainer,
                                    onClick = onShare
                                )
                            )
                        }
                    }

                    val chunks = remember(gridItems) { gridItems.chunked(2) }

                    chunks.forEach { chunk ->
                        if (chunk.size == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = chunk[0].icon,
                                        text = chunk[0].text,
                                        containerColor = chunk[0].containerColor,
                                        iconColor = chunk[0].iconColor,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                            chunk[0].onClick()
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = chunk[1].icon,
                                        text = chunk[1].text,
                                        containerColor = chunk[1].containerColor,
                                        iconColor = chunk[1].iconColor,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                            chunk[1].onClick()
                                        }
                                    )
                                }
                            }
                        } else {
                            SongOptionGridItem(
                                icon = chunk[0].icon,
                                text = chunk[0].text,
                                containerColor = chunk[0].containerColor,
                                iconColor = chunk[0].iconColor,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    chunk[0].onClick()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (showRemoveFromPlaylist) {
                        SongOptionGridItem(
                            icon = RhythmIcons.Remove,
                            text = stringResource(R.string.cd_remove_from_playlist),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                onRemoveFromPlaylist()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private data class OptionItem(
    val icon: MaterialSymbolIcon,
    val text: String,
    val containerColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun SongOptionGridItem(
    icon: MaterialSymbolIcon,
    text: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with colored background
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = containerColor.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    containerColor.copy(alpha = 0.15f),
                                    containerColor.copy(alpha = 0.05f)
                                ),
                                radius = 22f
                            )
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
