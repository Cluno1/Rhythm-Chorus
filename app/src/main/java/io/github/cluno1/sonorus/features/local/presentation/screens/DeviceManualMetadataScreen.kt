/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.features.local.data.device.DeviceArtworkCandidate
import io.github.cluno1.sonorus.features.local.data.device.DeviceLyricsCandidate
import io.github.cluno1.sonorus.features.local.data.device.DeviceManualMetadataKind
import io.github.cluno1.sonorus.features.local.data.device.DeviceMetadataRequest
import io.github.cluno1.sonorus.features.local.data.device.DevicePublicMetadataProvider
import io.github.cluno1.sonorus.features.local.presentation.viewmodel.DeviceManualMetadataError
import io.github.cluno1.sonorus.features.local.presentation.viewmodel.DeviceManualProviderStatus
import io.github.cluno1.sonorus.features.local.presentation.viewmodel.DeviceManualMetadataUiState
import io.github.cluno1.sonorus.shared.data.model.AppSettings
import io.github.cluno1.sonorus.shared.data.model.Song
import io.github.cluno1.sonorus.shared.presentation.components.common.CollapsibleHeaderScreen
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
import io.github.cluno1.sonorus.ui.LocalMiniPlayerPadding
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManualMetadataScreen(
    song: Song?,
    initialKind: DeviceManualMetadataKind,
    state: DeviceManualMetadataUiState,
    appSettings: AppSettings,
    onStart: (String, DeviceManualMetadataKind) -> Unit,
    onSearch: (
        String,
        DeviceManualMetadataKind,
        DeviceMetadataRequest,
        Set<DevicePublicMetadataProvider>,
    ) -> Unit,
    onApplyLyrics: (String, DeviceLyricsCandidate) -> Unit,
    onApplyArtwork: (String, DeviceArtworkCandidate) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val publicMetadataEnabled by appSettings.devicePublicMetadataEnabled.collectAsState()
    var kind by rememberSaveable(song?.id, initialKind) { mutableStateOf(initialKind) }
    var title by rememberSaveable(song?.id) { mutableStateOf(song?.title.orEmpty()) }
    var artist by rememberSaveable(song?.id) { mutableStateOf(song?.artist.orEmpty()) }
    var album by rememberSaveable(song?.id) { mutableStateOf(song?.album.orEmpty()) }
    var duration by rememberSaveable(song?.id) {
        mutableStateOf(song?.duration?.takeIf { it > 0 }?.let(::formatDurationInput).orEmpty())
    }
    var lrclibSelected by rememberSaveable(song?.id) { mutableStateOf(true) }
    var musicBrainzSelected by rememberSaveable(song?.id) { mutableStateOf(true) }
    var deezerSelected by rememberSaveable(song?.id) { mutableStateOf(true) }
    var previewLyrics by remember { mutableStateOf<String?>(null) }
    var selectedLyrics by remember { mutableStateOf<DeviceLyricsCandidate?>(null) }
    var selectedArtwork by remember { mutableStateOf<DeviceArtworkCandidate?>(null) }
    val miniPlayerBottomPadding = LocalMiniPlayerPadding.current.calculateBottomPadding()
    val durationSeconds = parseDurationSeconds(duration)
    val durationValid = duration.isBlank() || durationSeconds != null

    LaunchedEffect(song?.id, kind) {
        song?.let { onStart(it.id, kind) }
    }
    LaunchedEffect(state.applied) {
        if (state.applied) onBack()
    }
    DisposableEffect(Unit) {
        onDispose(onClear)
    }

    CollapsibleHeaderScreen(
        title = stringResource(R.string.device_manual_metadata_title),
        showBackButton = true,
        onBackClick = onBack,
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .then(modifier)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp + miniPlayerBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (song == null || state.error == DeviceManualMetadataError.SONG_UNAVAILABLE) {
                item {
                    MetadataCard {
                        Text(
                            text = stringResource(R.string.device_manual_metadata_song_unavailable),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                MetadataCard {
                    Text(
                        text = stringResource(R.string.device_manual_metadata_current_song),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == DeviceManualMetadataKind.LYRICS,
                        onClick = { kind = DeviceManualMetadataKind.LYRICS },
                        label = { Text(stringResource(R.string.device_manual_metadata_lyrics)) },
                        leadingIcon = {
                            Icon(
                                icon = RhythmIcons.Player.Lyrics,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    FilterChip(
                        selected = kind == DeviceManualMetadataKind.ARTWORK,
                        onClick = { kind = DeviceManualMetadataKind.ARTWORK },
                        label = { Text(stringResource(R.string.device_manual_metadata_artwork)) },
                        leadingIcon = {
                            Icon(
                                icon = RhythmIcons.Album,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            if (!publicMetadataEnabled) {
                item {
                    MetadataCard {
                        Text(
                            text = stringResource(R.string.settings_device_public_metadata),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.settings_device_public_metadata_privacy),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { appSettings.setDevicePublicMetadataEnabled(true) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.device_manual_metadata_enable_public))
                        }
                    }
                }
            }

            item {
                MetadataCard {
                    Text(
                        text = stringResource(R.string.device_manual_metadata_sources),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    when (kind) {
                        DeviceManualMetadataKind.LYRICS -> ProviderChip(
                            selected = lrclibSelected,
                            onClick = { lrclibSelected = !lrclibSelected },
                            title = "LRCLIB",
                            description = stringResource(R.string.device_manual_metadata_lrclib_desc),
                            status = state.providerStatuses[DevicePublicMetadataProvider.LRCLIB],
                            resultCount = state.lyricsCandidates.size,
                        )
                        DeviceManualMetadataKind.ARTWORK -> {
                            ProviderChip(
                                selected = musicBrainzSelected,
                                onClick = { musicBrainzSelected = !musicBrainzSelected },
                                title = "MusicBrainz + Cover Art Archive",
                                description = stringResource(R.string.device_manual_metadata_caa_desc),
                                status = state.providerStatuses[
                                    DevicePublicMetadataProvider.MUSICBRAINZ_CAA
                                ],
                                resultCount = state.artworkCandidates.count {
                                    it.provider == DevicePublicMetadataProvider.MUSICBRAINZ_CAA
                                },
                            )
                            ProviderChip(
                                selected = deezerSelected,
                                onClick = { deezerSelected = !deezerSelected },
                                title = "Deezer",
                                description = stringResource(R.string.device_manual_metadata_deezer_desc),
                                status = state.providerStatuses[DevicePublicMetadataProvider.DEEZER],
                                resultCount = state.artworkCandidates.count {
                                    it.provider == DevicePublicMetadataProvider.DEEZER
                                },
                            )
                        }
                    }
                }
            }

            item {
                MetadataCard {
                    Text(
                        text = stringResource(R.string.device_manual_metadata_query),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.device_manual_metadata_field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text(stringResource(R.string.device_manual_metadata_field_artist)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = album,
                        onValueChange = { album = it },
                        label = { Text(stringResource(R.string.device_manual_metadata_field_album)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(stringResource(R.string.device_manual_metadata_field_duration)) },
                        supportingText = if (!durationValid) {
                            { Text(stringResource(R.string.device_manual_metadata_duration_error)) }
                        } else {
                            null
                        },
                        isError = !durationValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            title = song.title
                            artist = song.artist
                            album = song.album
                            duration = song.duration.takeIf { it > 0 }?.let(::formatDurationInput).orEmpty()
                        },
                        enabled = !state.isSearching && !state.isApplying,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.device_manual_metadata_restore_defaults))
                    }
                }
            }

            item {
                val providers = buildSet {
                    if (kind == DeviceManualMetadataKind.LYRICS && lrclibSelected) {
                        add(DevicePublicMetadataProvider.LRCLIB)
                    }
                    if (kind == DeviceManualMetadataKind.ARTWORK && musicBrainzSelected) {
                        add(DevicePublicMetadataProvider.MUSICBRAINZ_CAA)
                    }
                    if (kind == DeviceManualMetadataKind.ARTWORK && deezerSelected) {
                        add(DevicePublicMetadataProvider.DEEZER)
                    }
                }
                Button(
                    onClick = {
                        onSearch(
                            song.id,
                            kind,
                            DeviceMetadataRequest(
                                title = title,
                                artist = artist,
                                album = album,
                                durationSeconds = durationSeconds,
                            ),
                            providers,
                        )
                    },
                    enabled = publicMetadataEnabled && durationValid &&
                        !state.isSearching && !state.isApplying,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    } else {
                        Icon(
                            icon = RhythmIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.device_manual_metadata_search))
                    }
                }
            }

            state.error?.takeUnless { it == DeviceManualMetadataError.SONG_UNAVAILABLE }?.let { error ->
                item {
                    Text(
                        text = stringResource(error.stringResource()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.hasSearched && !state.isSearching &&
                state.lyricsCandidates.isEmpty() && state.artworkCandidates.isEmpty()
            ) {
                item {
                    MetadataCard {
                        Text(
                            text = stringResource(R.string.device_manual_metadata_no_results),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.device_manual_metadata_no_results_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.lyricsCandidates.isNotEmpty() || state.artworkCandidates.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.device_manual_metadata_results),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            items(state.lyricsCandidates, key = { "lyrics:${it.externalId}" }) { candidate ->
                LyricsCandidateCard(
                    candidate = candidate,
                    queryDurationSeconds = durationSeconds,
                    onClick = {
                        selectedLyrics = candidate
                        previewLyrics = candidate.lyrics.syncedLyrics ?: candidate.lyrics.plainLyrics
                    },
                )
            }
            items(
                state.artworkCandidates,
                key = { "artwork:${it.provider}:${it.externalId}" },
            ) { candidate ->
                ArtworkCandidateCard(
                    candidate = candidate,
                    queryDurationSeconds = durationSeconds,
                    onClick = { selectedArtwork = candidate },
                )
            }
        }
    }

    selectedLyrics?.let { candidate ->
        AlertDialog(
            onDismissRequest = {
                selectedLyrics = null
                previewLyrics = null
            },
            title = { Text(candidate.title) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = previewLyrics.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedLyrics = null
                        previewLyrics = null
                        song?.let { onApplyLyrics(it.id, candidate) }
                    },
                    enabled = !state.isApplying,
                ) {
                    Text(stringResource(R.string.device_manual_metadata_use_lyrics))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedLyrics = null
                        previewLyrics = null
                    },
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    selectedArtwork?.let { candidate ->
        AlertDialog(
            onDismissRequest = { selectedArtwork = null },
            title = { Text(candidate.album.ifBlank { candidate.title }) },
            text = {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(candidate.imageUrl).build(),
                    contentDescription = stringResource(R.string.device_manual_metadata_artwork_preview),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedArtwork = null
                        song?.let { onApplyArtwork(it.id, candidate) }
                    },
                    enabled = !state.isApplying,
                ) {
                    Text(stringResource(R.string.device_manual_metadata_use_artwork))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArtwork = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun MetadataCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ProviderChip(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String,
    status: DeviceManualProviderStatus?,
    resultCount: Int,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                status?.let {
                    Text(
                        text = when (it) {
                            DeviceManualProviderStatus.LOADING -> stringResource(
                                R.string.device_manual_metadata_provider_loading,
                            )
                            DeviceManualProviderStatus.SUCCESS -> stringResource(
                                R.string.device_manual_metadata_provider_success,
                                resultCount,
                            )
                            DeviceManualProviderStatus.EMPTY -> stringResource(
                                R.string.device_manual_metadata_provider_empty,
                            )
                            DeviceManualProviderStatus.FAILED -> stringResource(
                                R.string.device_manual_metadata_provider_failed,
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it == DeviceManualProviderStatus.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LyricsCandidateCard(
    candidate: DeviceLyricsCandidate,
    queryDurationSeconds: Int?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LRCLIB", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(
                        R.string.device_manual_metadata_confidence,
                        (candidate.confidence * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(candidate.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(candidate.artist, candidate.album).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CandidateDuration(
                candidateSeconds = candidate.durationSeconds,
                queryDurationSeconds = queryDurationSeconds,
            )
            Text(
                if (candidate.lyrics.syncedLyrics.isNullOrBlank()) {
                    stringResource(R.string.device_manual_metadata_plain_lyrics)
                } else {
                    stringResource(R.string.device_manual_metadata_synced_lyrics)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            val preview = (candidate.lyrics.syncedLyrics ?: candidate.lyrics.plainLyrics).orEmpty()
                .lineSequence()
                .filter(String::isNotBlank)
                .take(3)
                .joinToString("\n")
            if (preview.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtworkCandidateCard(
    candidate: DeviceArtworkCandidate,
    queryDurationSeconds: Int?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(candidate.imageUrl).build(),
                contentDescription = stringResource(R.string.device_manual_metadata_artwork_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = when (candidate.provider) {
                        DevicePublicMetadataProvider.MUSICBRAINZ_CAA -> "MusicBrainz + CAA"
                        DevicePublicMetadataProvider.DEEZER -> "Deezer"
                        DevicePublicMetadataProvider.LRCLIB -> "LRCLIB"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = candidate.album.ifBlank { candidate.title },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = candidate.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CandidateDuration(
                    candidateSeconds = candidate.durationSeconds,
                    queryDurationSeconds = queryDurationSeconds,
                )
                Text(
                    stringResource(
                        R.string.device_manual_metadata_confidence,
                        (candidate.confidence * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun CandidateDuration(candidateSeconds: Double?, queryDurationSeconds: Int?) {
    val rounded = candidateSeconds?.takeIf { it > 0 }?.roundToInt() ?: return
    val durationText = formatDurationInput(rounded * 1000L)
    val difference = queryDurationSeconds?.let { abs(rounded - it) }
    Text(
        text = if (difference == null) {
            stringResource(R.string.device_manual_metadata_candidate_duration, durationText)
        } else {
            stringResource(
                R.string.device_manual_metadata_candidate_duration_difference,
                durationText,
                difference,
            )
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun parseDurationSeconds(raw: String): Int? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    val seconds = if (':' in value) {
        val parts = value.split(':')
        if (parts.size !in 2..3 || parts.any { it.toIntOrNull() == null }) return null
        val numbers = parts.map(String::toInt)
        if (numbers.drop(1).any { it !in 0..59 }) return null
        if (numbers.size == 2) numbers[0] * 60L + numbers[1]
        else numbers[0] * 3600L + numbers[1] * 60L + numbers[2]
    } else {
        value.toLongOrNull() ?: return null
    }
    return seconds.takeIf { it in 1..86_400 }?.toInt()
}

private fun formatDurationInput(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = totalSeconds % 3600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun DeviceManualMetadataError.stringResource(): Int = when (this) {
    DeviceManualMetadataError.SONG_UNAVAILABLE -> R.string.device_manual_metadata_song_unavailable
    DeviceManualMetadataError.TITLE_REQUIRED -> R.string.device_manual_metadata_title_required
    DeviceManualMetadataError.PROVIDER_REQUIRED -> R.string.device_manual_metadata_provider_required
    DeviceManualMetadataError.REQUEST_FAILED -> R.string.device_manual_metadata_request_failed
    DeviceManualMetadataError.APPLY_FAILED -> R.string.device_manual_metadata_apply_failed
}
