package chromahub.rhythm.app.shared.presentation.components.bottomsheets

import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

private data class ControlAction(
    val icon: MaterialSymbolIcon,
    val label: String,
    val containerColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraControlBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    hiddenChips: Set<String>,
    equalizerEnabled: Boolean,
    sleepTimerActive: Boolean,
    sleepTimerRemainingSeconds: Long,
    lyrics: LyricsData?,
    isFavorite: Boolean = false,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onPlaybackSpeed: () -> Unit,
    onPlaybackPitch: () -> Unit = {},
    onEqualizer: () -> Unit,
    onSleepTimer: () -> Unit,
    onLyricsEditor: () -> Unit,
    onAlbum: () -> Unit = {},
    onArtist: () -> Unit = {},
    onSongInfo: () -> Unit,
    onShareFile: () -> Unit = {},
    haptic: HapticFeedback,
    isExtraSmallWidth: Boolean = false,
    isCompactWidth: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    fun dismissAndDo(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    val primary = MaterialTheme.colorScheme.primaryContainer
    val onPrimary = MaterialTheme.colorScheme.onPrimaryContainer
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val onSecondary = MaterialTheme.colorScheme.onSecondaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiary = MaterialTheme.colorScheme.onTertiaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val error = MaterialTheme.colorScheme.error

    val actions = buildList {
        // Add to Playlist (always shown)
        add(ControlAction(
            icon = RhythmIcons.AddToPlaylist,
            label = "Add to Playlist",
            containerColor = primary,
            iconColor = onPrimary,
            onClick = {
                HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                dismissAndDo { onAddToPlaylist() }
            }
        ))

        if ("FAVORITE" !in hiddenChips) {
            add(ControlAction(
                icon = if (isFavorite) RhythmIcons.FavoriteFilled else RhythmIcons.Favorite,
                label = if (isFavorite) "Unfavorite" else "Favorite",
                containerColor = if (isFavorite) errorContainer else primary,
                iconColor = if (isFavorite) error else onPrimary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onToggleFavorite() }
                }
            ))
        }

        if ("SPEED" !in hiddenChips) {
            add(ControlAction(
                icon = MaterialSymbolIcon("speed", filled = true),
                label = "Speed",
                containerColor = secondary,
                iconColor = onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onPlaybackSpeed() }
                }
            ))
        }

        if ("PITCH" !in hiddenChips) {
            add(ControlAction(
                icon = MaterialSymbolIcon("graphic_eq", filled = true),
                label = "Pitch",
                containerColor = secondary,
                iconColor = onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onPlaybackPitch() }
                }
            ))
        }

        if ("EQUALIZER" !in hiddenChips) {
            add(ControlAction(
                icon = MaterialSymbolIcon("graphic_eq", filled = true),
                label = if (equalizerEnabled) "EQ (ON)" else "Equalizer",
                containerColor = if (equalizerEnabled) tertiary else secondary,
                iconColor = if (equalizerEnabled) onTertiary else onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onEqualizer() }
                }
            ))
        }

        if ("SLEEP_TIMER" !in hiddenChips) {
            val sleepLabel = if (sleepTimerActive) {
                val m = sleepTimerRemainingSeconds / 60
                val s = sleepTimerRemainingSeconds % 60
                "${m}:${s.toString().padStart(2, '0')}"
            } else "Sleep Timer"
            add(ControlAction(
                icon = if (sleepTimerActive) RhythmIcons.AccessTime else RhythmIcons.AccessTime,
                label = sleepLabel,
                containerColor = if (sleepTimerActive) tertiary else secondary,
                iconColor = if (sleepTimerActive) onTertiary else onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onSleepTimer() }
                }
            ))
        }

        if ("LYRICS" !in hiddenChips) {
            val hasLyrics = lyrics?.getBestLyrics()?.isNotEmpty() == true
            add(ControlAction(
                icon = if (hasLyrics) RhythmIcons.Edit else MaterialSymbolIcon("lyrics", filled = true),
                label = if (hasLyrics) "Edit Lyrics" else "Add Lyrics",
                containerColor = secondary,
                iconColor = onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onLyricsEditor() }
                }
            ))
        }

        if ("ALBUM" !in hiddenChips) {
            add(ControlAction(
                icon = RhythmIcons.AlbumFilled,
                label = "Go to Album",
                containerColor = secondary,
                iconColor = onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onAlbum() }
                }
            ))
        }

        if ("ARTIST" !in hiddenChips) {
            add(ControlAction(
                icon = RhythmIcons.ArtistFilled,
                label = "Go to Artist",
                containerColor = secondary,
                iconColor = onSecondary,
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    dismissAndDo { onArtist() }
                }
            ))
        }

        // Song Info (always shown)
        add(ControlAction(
            icon = RhythmIcons.Info,
            label = "Song Info",
            containerColor = secondary,
            iconColor = onSecondary,
            onClick = {
                HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                dismissAndDo { onSongInfo() }
            }
        ))

        // Share File (always shown)
        add(ControlAction(
            icon = RhythmIcons.Share,
            label = "Share File",
            containerColor = secondary,
            iconColor = onSecondary,
            onClick = {
                HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                dismissAndDo { onShareFile() }
            }
        ))
    }

    ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
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
            // Header — matches SongOptionsBottomSheet style
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
                        text = stringResource(R.string.settings_shapes_player_controls),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            text = stringResource(R.string.libraryscreen_more_actions),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2-column action grid
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
                    actions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowActions.forEach { action ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ControlGridItem(
                                        icon = action.icon,
                                        text = action.label,
                                        containerColor = action.containerColor,
                                        iconColor = action.iconColor,
                                        onClick = action.onClick
                                    )
                                }
                            }
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ControlGridItem(
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
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = containerColor.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
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
