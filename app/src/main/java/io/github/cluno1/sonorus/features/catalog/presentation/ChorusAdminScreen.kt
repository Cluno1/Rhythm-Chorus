package io.github.cluno1.sonorus.features.catalog.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.features.catalog.domain.CatalogAdminDevice
import io.github.cluno1.sonorus.features.catalog.domain.ChorusModerationItem
import io.github.cluno1.sonorus.shared.presentation.components.Material3SettingsGroup
import io.github.cluno1.sonorus.shared.presentation.components.Material3SettingsItem
import io.github.cluno1.sonorus.shared.presentation.components.common.CollapsibleHeaderScreen
import io.github.cluno1.sonorus.shared.presentation.components.common.TabAnimation
import io.github.cluno1.sonorus.shared.presentation.components.icons.MaterialSymbolIcon
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.screens.settings.TunerAnimatedSwitch
import io.github.cluno1.sonorus.ui.LocalMiniPlayerPadding
import kotlinx.coroutines.launch

@Composable
fun ChorusAdminScreen(
    state: CatalogUiState,
    viewModel: CatalogViewModel,
    onOpenDeviceRegistration: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }
    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.deviceRegistered) {
        if (state.deviceRegistered) viewModel.refreshAdministrator()
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && player.playbackState == Player.STATE_ENDED) playingTrackId = null
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun preview(item: ChorusModerationItem) {
        if (playingTrackId == item.track.id && player.isPlaying) {
            player.pause()
            playingTrackId = null
            return
        }
        scope.launch {
            playbackError = null
            viewModel.playback(item.track.renditionId).fold(
                onSuccess = { descriptor ->
                    player.setMediaItem(MediaItem.fromUri(descriptor.relativeUrl))
                    player.prepare()
                    player.play()
                    playingTrackId = item.track.id
                },
                onFailure = { playbackError = it.message ?: context.getString(R.string.chorus_admin_preview_error) },
            )
        }
    }

    CollapsibleHeaderScreen(
        title = stringResource(R.string.chorus_admin_title),
        showBackButton = true,
        onBackClick = onBack,
    ) { modifier ->
        when {
            !state.deviceRegistered -> AdministratorDeviceRequired(
                onOpenDeviceRegistration = onOpenDeviceRegistration,
                modifier = modifier,
            )
            state.administratorDashboard != null -> AdministratorDashboard(
                state = state,
                playingTrackId = playingTrackId,
                playbackError = playbackError,
                onPreview = ::preview,
                onAutomaticApprovalChange = viewModel::setChorusAutomaticApproval,
                onModerate = viewModel::moderateChorusTrack,
                onIssueInvite = viewModel::issueInviteAsAdministrator,
                onSetAdministrator = viewModel::setDeviceAdministrator,
                onRefresh = viewModel::refreshAdministrator,
                modifier = modifier,
            )
            else -> AdministratorLogin(
                state = state,
                onLogin = viewModel::authenticateAdministrator,
                onSetAdministrator = viewModel::setDeviceAdministrator,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AdministratorDeviceRequired(
    onOpenDeviceRegistration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(MaterialSymbolIcon("admin_panel_settings", filled = true), null, Modifier.size(44.dp))
                Text(stringResource(R.string.chorus_admin_device_required), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.chorus_admin_device_required_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenDeviceRegistration) {
                    Text(stringResource(R.string.chorus_admin_open_registration))
                }
            }
        }
    }
}

@Composable
private fun AdministratorLogin(
    state: CatalogUiState,
    onLogin: (String, String) -> Unit,
    onSetAdministrator: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.administratorSessionActive) {
        if (state.administratorSessionActive) password = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.administratorSessionActive) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.chorus_admin_login_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.chorus_admin_login_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.serverUrl.startsWith("http://")) {
                        Text(
                            stringResource(R.string.chorus_admin_http_warning),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.catalog_admin_username)) },
                        enabled = !state.administratorLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.catalog_admin_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !state.administratorLoading,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onLogin(username, password) },
                        enabled = username.isNotBlank() && password.isNotEmpty() && !state.administratorLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.chorus_admin_login_action))
                    }
                }
            }
        } else {
            Text(stringResource(R.string.chorus_admin_choose_device), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.chorus_admin_choose_device_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeviceList(
                devices = state.administratorDevices,
                currentDeviceId = state.currentDeviceId,
                busy = state.administratorLoading,
                onSetAdministrator = onSetAdministrator,
            )
        }
        state.adminError?.let { ErrorMessage(it) }
        if (state.administratorLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun AdministratorDashboard(
    state: CatalogUiState,
    playingTrackId: String?,
    playbackError: String?,
    onPreview: (ChorusModerationItem) -> Unit,
    onAutomaticApprovalChange: (Boolean) -> Unit,
    onModerate: (String, Int, Boolean, String?) -> Unit,
    onIssueInvite: (String, String, Boolean) -> Unit,
    onSetAdministrator: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboard = requireNotNull(state.administratorDashboard)
    var inviteUserId by rememberSaveable { mutableStateOf("") }
    var inviteDisplayName by rememberSaveable { mutableStateOf("") }
    var replaceDevice by rememberSaveable { mutableStateOf(false) }
    var rejecting by remember { mutableStateOf<ChorusModerationItem?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(ChorusAdminSection.REVIEW) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp,
            top = 16.dp,
            end = 24.dp,
            bottom = 32.dp + LocalMiniPlayerPadding.current.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Material3SettingsGroup(
                title = stringResource(R.string.chorus_admin_policy_title),
                items = listOf(
                    Material3SettingsItem(
                        icon = MaterialSymbolIcon("verified_user", filled = true),
                        title = { Text(stringResource(R.string.chorus_admin_device_active)) },
                        description = { Text(stringResource(R.string.chorus_admin_device_active_desc)) },
                        trailingContent = {
                            IconButton(onClick = onRefresh, enabled = !state.administratorLoading) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("refresh"),
                                    contentDescription = stringResource(R.string.chorus_admin_refresh),
                                )
                            }
                        },
                        onClick = onRefresh,
                    ),
                    Material3SettingsItem(
                        icon = MaterialSymbolIcon("fact_check", filled = true),
                        title = { Text(stringResource(R.string.chorus_admin_auto_review)) },
                        description = {
                            Text(
                                stringResource(
                                    if (dashboard.settings.automaticApproval) {
                                        R.string.chorus_admin_auto_review_on_desc
                                    } else {
                                        R.string.chorus_admin_auto_review_off_desc
                                    },
                                ),
                            )
                        },
                        trailingContent = {
                            TunerAnimatedSwitch(
                                checked = dashboard.settings.automaticApproval,
                                onCheckedChange = onAutomaticApprovalChange,
                                enabled = !state.administratorLoading,
                            )
                        },
                        onClick = {
                            if (!state.administratorLoading) {
                                onAutomaticApprovalChange(!dashboard.settings.automaticApproval)
                            }
                        },
                    ),
                ),
            )
        }
        item {
            ChorusAdminSectionTabs(
                selectedSection = selectedSection,
                pendingCount = dashboard.pendingTracks.size,
                onSectionSelected = { selectedSection = it },
            )
        }

        playbackError?.let { item { ErrorMessage(it) } }
        state.adminError?.let { item { ErrorMessage(it) } }

        when (selectedSection) {
            ChorusAdminSection.REVIEW -> {
                if (dashboard.pendingTracks.isEmpty()) {
                    item {
                        Material3SettingsGroup(
                            items = listOf(
                                Material3SettingsItem(
                                    icon = MaterialSymbolIcon("task_alt", filled = true),
                                    title = { Text(stringResource(R.string.chorus_admin_pending_empty)) },
                                ),
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                } else {
                    items(dashboard.pendingTracks, key = { it.track.id }) { item ->
                        ModerationCard(
                            item = item,
                            playing = playingTrackId == item.track.id,
                            busy = state.administratorLoading,
                            onPreview = { onPreview(item) },
                            onPublish = { onModerate(item.track.id, item.track.revision, true, null) },
                            onReject = { rejecting = item },
                        )
                    }
                }
            }

            ChorusAdminSection.INVITE -> item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            stringResource(R.string.chorus_admin_invite_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedTextField(
                            value = inviteUserId,
                            onValueChange = { inviteUserId = it },
                            label = { Text(stringResource(R.string.catalog_user_id)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = inviteDisplayName,
                            onValueChange = { inviteDisplayName = it },
                            label = { Text(stringResource(R.string.catalog_user_display_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.catalog_replace_existing_device))
                                Text(
                                    stringResource(R.string.catalog_replace_existing_device_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TunerAnimatedSwitch(
                                checked = replaceDevice,
                                onCheckedChange = { replaceDevice = it },
                            )
                        }
                        Button(
                            onClick = { onIssueInvite(inviteUserId, inviteDisplayName, replaceDevice) },
                            enabled = inviteUserId.isNotBlank() && !state.administratorLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.chorus_admin_invite_action))
                        }
                        state.issuedInvite?.let { invite ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.chorus_admin_invite_created),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(invite.inviteCode, style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        stringResource(R.string.chorus_admin_invite_expiry, invite.expiresAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ChorusAdminSection.DEVICES -> item {
                DeviceList(
                    devices = dashboard.devices,
                    currentDeviceId = state.currentDeviceId,
                    busy = state.administratorLoading,
                    onSetAdministrator = onSetAdministrator,
                )
            }
        }
        if (state.administratorLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    rejecting?.let { item ->
        RejectDialog(
            trackName = item.track.displayLabel,
            busy = state.administratorLoading,
            onDismiss = { rejecting = null },
            onConfirm = { reason ->
                onModerate(item.track.id, item.track.revision, false, reason)
                rejecting = null
            },
        )
    }
}

private enum class ChorusAdminSection {
    REVIEW,
    INVITE,
    DEVICES,
}

@Composable
private fun ChorusAdminSectionTabs(
    selectedSection: ChorusAdminSection,
    pendingCount: Int,
    onSectionSelected: (ChorusAdminSection) -> Unit,
) {
    val sections = ChorusAdminSection.entries
    val titles = listOf(
        stringResource(R.string.chorus_admin_pending_title, pendingCount),
        stringResource(R.string.chorus_admin_workspace_invite),
        stringResource(R.string.chorus_admin_workspace_devices),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.chorus_admin_workspace_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(sections) { index, section ->
                val title = titles[index]
                TabAnimation(
                    index = index,
                    selectedIndex = sections.indexOf(selectedSection),
                    title = title,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedColor = MaterialTheme.colorScheme.surfaceContainer,
                    onUnselectedColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onSectionSelected(section) },
                    modifier = Modifier.padding(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon(
                                when (section) {
                                    ChorusAdminSection.REVIEW -> "fact_check"
                                    ChorusAdminSection.INVITE -> "key"
                                    ChorusAdminSection.DEVICES -> "devices"
                                },
                                filled = section == selectedSection,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (section == selectedSection) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationCard(
    item: ChorusModerationItem,
    playing: Boolean,
    busy: Boolean,
    onPreview: () -> Unit,
    onPublish: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(item.track.displayLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${item.projectTitle} · ${item.track.uploaderDisplayName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Waveform(item.track.waveformPeaks)
            Text(
                stringResource(
                    R.string.chorus_admin_track_meta,
                    item.track.contributionKind,
                    formatDuration(item.track.durationMs),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPreview, enabled = !busy) {
                    Icon(MaterialSymbolIcon(if (playing) "pause" else "play_arrow", filled = true), null)
                    Text(stringResource(if (playing) R.string.chorus_admin_pause else R.string.chorus_admin_preview))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReject, enabled = !busy) {
                    Text(stringResource(R.string.chorus_admin_reject))
                }
                Button(onClick = onPublish, enabled = !busy) {
                    Text(stringResource(R.string.chorus_admin_publish))
                }
            }
        }
    }
}

@Composable
private fun Waveform(peaks: List<Float>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val values = peaks.ifEmpty { List(32) { 0.12f } }
        val step = size.width / values.size.coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val half = size.height * value.coerceIn(0.05f, 1f) / 2f
            val x = step * index + step / 2f
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f - half),
                end = Offset(x, size.height / 2f + half),
                strokeWidth = (step * 0.45f).coerceAtLeast(2f),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<CatalogAdminDevice>,
    currentDeviceId: String?,
    busy: Boolean,
    onSetAdministrator: (String, Boolean) -> Unit,
) {
    val currentDeviceLabel = stringResource(R.string.chorus_admin_current_device)
    Material3SettingsGroup(
        items = devices.map { device ->
            val current = device.deviceId == currentDeviceId
            Material3SettingsItem(
                icon = MaterialSymbolIcon("smartphone", filled = current),
                title = {
                    Text(
                        device.displayName ?: device.userId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                description = {
                    Text(
                        buildString {
                            append(device.userId)
                            if (current) append(" · ").append(currentDeviceLabel)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    TextButton(
                        onClick = { onSetAdministrator(device.deviceId, !device.isAdministrator) },
                        enabled = device.status == "active" && !busy,
                    ) {
                        Text(
                            stringResource(
                                if (device.isAdministrator) {
                                    R.string.chorus_admin_device_revoke
                                } else {
                                    R.string.chorus_admin_device_grant
                                },
                            ),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
private fun RejectDialog(
    trackName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chorus_admin_reject_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(trackName)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.chorus_admin_reject_reason)) },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank() && !busy) {
                Text(stringResource(R.string.chorus_admin_reject_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ErrorMessage(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        )
    }
}

private fun formatDuration(durationMs: Long?): String {
    val seconds = ((durationMs ?: 0L) / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
