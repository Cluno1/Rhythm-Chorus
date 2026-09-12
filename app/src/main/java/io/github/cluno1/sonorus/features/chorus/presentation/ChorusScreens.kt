package io.github.cluno1.sonorus.features.chorus.presentation

import android.Manifest
import android.media.MediaMetadataRetriever
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import io.github.cluno1.sonorus.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.cluno1.sonorus.features.catalog.domain.ChorusMix
import io.github.cluno1.sonorus.features.catalog.domain.ChorusPart
import io.github.cluno1.sonorus.features.catalog.domain.ChorusProject
import io.github.cluno1.sonorus.features.catalog.domain.ChorusSyncAnchor
import io.github.cluno1.sonorus.features.catalog.domain.ChorusTrack
import io.github.cluno1.sonorus.features.catalog.domain.ChorusTrackUpload
import io.github.cluno1.sonorus.features.catalog.domain.MusicXmlRuntimeSanitizer
import io.github.cluno1.sonorus.features.catalog.domain.ScoreRevision
import io.github.cluno1.sonorus.features.catalog.presentation.CatalogViewModel
import io.github.cluno1.sonorus.features.catalog.presentation.formatScoreRevisionTime
import io.github.cluno1.sonorus.features.chorus.data.ChorusAudioRecorder
import io.github.cluno1.sonorus.features.chorus.data.ChorusRecordingResult
import io.github.cluno1.sonorus.features.scores.presentation.RemoteScoreScreen
import io.github.cluno1.sonorus.features.scores.presentation.ScorePlaybackCommand
import io.github.cluno1.sonorus.features.scores.presentation.ScorePlaybackCommandAction
import io.github.cluno1.sonorus.shared.data.repository.PlaybackMediaKind
import io.github.cluno1.sonorus.shared.data.repository.PlaybackSubject
import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.SheetAdaptiveType
import io.github.cluno1.sonorus.shared.presentation.components.common.ButtonGroupStyle
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveButtonGroup
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveGroupButton
import io.github.cluno1.sonorus.shared.presentation.components.common.M3CircularLoader
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.MaterialSymbolIcon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
import io.github.cluno1.sonorus.ui.LocalMiniPlayerPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChorusScreen(
    workId: String,
    title: String,
    viewModel: CatalogViewModel,
    onBack: () -> Unit,
    onRecord: (projectId: String, revisionId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var projects by remember(workId) { mutableStateOf<List<ChorusProject>>(emptyList()) }
    var activeProjectId by remember(workId) { mutableStateOf<String?>(null) }
    var selectedIds by remember(workId) { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mix by remember { mutableStateOf<ChorusMix?>(null) }
    var pendingUpload by remember { mutableStateOf<PendingAudio?>(null) }
    var pendingDelete by remember { mutableStateOf<ChorusTrack?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val project = projects.firstOrNull { it.id == activeProjectId }
    val playableTracks = project?.tracks.orEmpty().filter { it.status == "published" }
    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun playReadyMix(value: ChorusMix) {
        val url = value.playback?.url ?: return
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            error = null
            runCatching { copyPickedAudio(context, uri) }.fold(
                onSuccess = { pendingUpload = it },
                onFailure = { error = it.message ?: "无法读取音频文件" },
            )
            busy = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(playbackError: PlaybackException) {
                error = "合唱播放失败，可以刷新后重试"
            }
        }
        player.addListener(listener)
        onDispose { player.release() }
    }

    LaunchedEffect(workId, refreshKey) {
        loading = true
        viewModel.chorus(workId).fold(
            onSuccess = { catalog ->
                projects = catalog.projects
                val chosen = catalog.projects.firstOrNull { it.id == activeProjectId }
                    ?: catalog.projects.firstOrNull()
                activeProjectId = chosen?.id
                selectedIds = chosen?.tracks.orEmpty().filter { it.status == "published" }
                    .mapTo(linkedSetOf(), ChorusTrack::id)
                error = null
            },
            onFailure = { error = it.message ?: "合唱项目加载失败" },
        )
        loading = false
    }

    LaunchedEffect(selectedIds) {
        if (mix != null && mix?.selectedTrackIds?.toSet() != selectedIds) {
            player.stop()
            mix = null
        }
    }

    LaunchedEffect(mix?.id, mix?.state) {
        val initial = mix ?: return@LaunchedEffect
        if (initial.state !in setOf("queued", "processing")) return@LaunchedEffect
        repeat(40) {
            delay(1_000)
            val updated = viewModel.chorusMix(initial.id).getOrElse {
                error = it.message ?: "合唱混音加载失败"
                return@LaunchedEffect
            }
            mix = updated
            if (updated.state == "ready") {
                playReadyMix(updated)
                return@LaunchedEffect
            }
            if (updated.state == "failed") {
                error = updated.errorSummary ?: "合唱混音失败"
                return@LaunchedEffect
            }
        }
        error = "混音仍在处理中，请稍后重试"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("在线合唱", fontWeight = FontWeight.Bold)
                        Text(title, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) { Icon(RhythmIcons.Back, contentDescription = "返回") }
                },
                actions = {
                    FilledTonalIconButton(onClick = { refreshKey++ }) {
                        Icon(RhythmIcons.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                M3CircularLoader()
            }
            projects.isEmpty() -> EmptyChorus(
                message = error ?: "管理员尚未为这份乐谱开放合唱项目",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            project == null -> EmptyChorus(
                message = "当前合唱项目已经不可用，请刷新后重试",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ExpressiveButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonGroupStyle.Tonal,
                    ) {
                        ExpressiveGroupButton(
                            onClick = { picker.launch("audio/*") },
                            enabled = !busy,
                            isStart = true,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                        ) {
                            Icon(RhythmIcons.CloudUpload, null)
                            Text(" 上传音频", modifier = Modifier.padding(start = 8.dp))
                        }
                        ExpressiveGroupButton(
                            onClick = { onRecord(project.id, project.alignmentScoreRevisionId) },
                            enabled = !busy,
                            isEnd = true,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Icon(RhythmIcons.MusicNote, null)
                            Text(" 录制声部", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                error?.let { message ->
                    item { ErrorCard(message) }
                }
                if (projects.size > 1) {
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            projects.forEach { item ->
                                FilterChip(
                                    selected = item.id == project.id,
                                    onClick = {
                                        activeProjectId = item.id
                                        selectedIds = item.tracks.filter { it.status == "published" }
                                            .mapTo(linkedSetOf(), ChorusTrack::id)
                                        mix = null
                                        player.stop()
                                    },
                                    label = { Text(item.title) },
                                )
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(project.title, style = MaterialTheme.typography.titleLarge)
                            Text("公开音轨默认全部勾选。播放的是服务器按当前选择生成的一条同步混音。")
                            Text(
                                "已选 ${selectedIds.size}/${playableTracks.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { selectedIds = playableTracks.mapTo(linkedSetOf(), ChorusTrack::id) },
                                    label = { Text("全选") },
                                    leadingIcon = { Icon(RhythmIcons.SelectAll, null) },
                                )
                                AssistChip(onClick = { selectedIds = emptySet() }, label = { Text("清空") })
                                Button(
                                    onClick = {
                                        val current = project
                                        if (playing) {
                                            player.pause()
                                            return@Button
                                        }
                                        val ready = mix?.takeIf {
                                            it.state == "ready" && it.selectedTrackIds.toSet() == selectedIds
                                        }
                                        if (ready != null) {
                                            scope.launch {
                                                busy = true
                                                viewModel.chorusMix(ready.id).fold(
                                                    onSuccess = { fresh -> mix = fresh; playReadyMix(fresh) },
                                                    onFailure = { error = it.message ?: "无法刷新播放地址" },
                                                )
                                                busy = false
                                            }
                                            return@Button
                                        }
                                        scope.launch {
                                            busy = true
                                            error = null
                                            viewModel.resolveChorusMix(current.id, selectedIds.toList()).fold(
                                                onSuccess = {
                                                    mix = it
                                                    if (it.state == "ready") playReadyMix(it)
                                                },
                                                onFailure = { error = it.message ?: "无法生成合唱混音" },
                                            )
                                            busy = false
                                        }
                                    },
                                    enabled = selectedIds.isNotEmpty() && !busy,
                                ) {
                                    Icon(if (playing) RhythmIcons.Pause else RhythmIcons.Play, null)
                                    Text(if (mix?.state in setOf("queued", "processing")) " 混音中" else " 播放")
                                }
                            }
                            if (mix?.state in setOf("queued", "processing")) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
                if (project.tracks.isEmpty()) {
                    item { Text("还没有人上传音轨。你可以成为第一个。") }
                } else {
                    items(project.tracks, key = ChorusTrack::id) { track ->
                        TrackCard(
                            track = track,
                            part = project.parts.firstOrNull { it.id == track.partId },
                            checked = track.id in selectedIds,
                            onChecked = { checked ->
                                if (track.status == "published") {
                                    selectedIds = if (checked) selectedIds + track.id else selectedIds - track.id
                                }
                            },
                            onDelete = if (track.ownedByRequester && track.status != "withdrawn") ({
                                pendingDelete = track
                            }) else null,
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    pendingUpload?.let { pending ->
        UploadAudioDialog(
            audio = pending,
            parts = project?.parts.orEmpty(),
            busy = busy,
            onDismiss = { if (!busy) { pending.file.delete(); pendingUpload = null } },
            onUpload = { partId, kind, label, offsetMs ->
                val current = project ?: return@UploadAudioDialog
                scope.launch {
                    busy = true
                    error = null
                    val upload = ChorusTrackUpload(
                        file = pending.file,
                        mediaType = pending.mediaType,
                        sha256 = pending.sha256,
                        durationMs = pending.durationMs,
                        partId = partId,
                        contributionKind = kind,
                        displayLabel = label,
                        initialAnchors = listOf(ChorusSyncAnchor(0, 0, 0)),
                    )
                    uploadAndSubmit(viewModel, current.id, upload, offsetMs).fold(
                        onSuccess = { pending.file.delete(); refreshKey++; pendingUpload = null },
                        onFailure = { error = it.message ?: "上传音轨失败" },
                    )
                    busy = false
                }
            },
        )
    }

    pendingDelete?.let { track ->
        DeleteTrackDialog(
            trackName = track.displayLabel,
            busy = busy,
            onDismiss = { if (!busy) pendingDelete = null },
            onConfirm = {
                scope.launch {
                    busy = true
                    error = null
                    viewModel.withdrawChorusTrack(track.id).fold(
                        onSuccess = {
                            selectedIds = selectedIds - track.id
                            pendingDelete = null
                            refreshKey++
                        },
                        onFailure = {
                            error = it.message ?: context.getString(R.string.chorus_track_delete_error)
                        },
                    )
                    busy = false
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChorusRecordingScreen(
    projectId: String,
    revisionId: String,
    title: String,
    viewModel: CatalogViewModel,
    onBack: () -> Unit,
    onUploaded: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalogState by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val rootView = LocalView.current
    var project by remember { mutableStateOf<ChorusProject?>(null) }
    var scoreRevision by remember { mutableStateOf<ScoreRevision?>(null) }
    var scoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var recorder by remember { mutableStateOf<ChorusAudioRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ChorusRecordingResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLyrics by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var rightsConfirmed by remember { mutableStateOf(false) }
    var offsetMs by remember { mutableLongStateOf(0L) }
    var scoreTick by remember { mutableLongStateOf(0L) }
    var scoreTimeMs by remember { mutableLongStateOf(0L) }
    var transportOffsetMs by remember { mutableLongStateOf(0L) }
    var commandSequence by remember { mutableLongStateOf(0L) }
    var playbackCommand by remember { mutableStateOf<ScorePlaybackCommand?>(null) }
    var recordingAnchors by remember { mutableStateOf<List<ChorusSyncAnchor>>(emptyList()) }
    var resultAnchors by remember { mutableStateOf<List<ChorusSyncAnchor>>(emptyList()) }
    var label by remember { mutableStateOf("我的声部") }
    var kind by remember { mutableStateOf("vocal_part") }
    var partId by remember { mutableStateOf<String?>(null) }
    val currentRecorder = recorder
    val emptyDuration = remember { MutableStateFlow(0L) }
    val emptyPeak = remember { MutableStateFlow(0f) }
    val duration by (currentRecorder?.durationMs ?: emptyDuration).collectAsState()
    val peak by (currentRecorder?.peak ?: emptyPeak).collectAsState()
    val lyrics = catalogState.songs.firstOrNull { it.workId == project?.workId }?.lyrics
    val scoreWork = catalogState.scoreWorks.firstOrNull { it.workId == project?.workId }
    val scoreOption = scoreWork?.scoreOptions?.firstOrNull { it.revisionId == scoreRevision?.id }
    val scoreDisplayLabel = scoreOption?.scoreLabel
        ?: stringResource(R.string.chorus_recording_alignment_score)
    val playbackSubject = scoreRevision?.let { revision ->
        PlaybackSubject(
            subjectId = "rhythm-score:score:${revision.scoreId}",
            mediaKind = PlaybackMediaKind.SCORE,
            title = scoreWork?.title ?: title,
            artist = scoreWork?.artist,
            collection = scoreOption?.scoreLabel,
            artworkUri = scoreWork?.coverUrl,
            workId = project?.workId,
            scoreId = revision.scoreId,
            revisionId = revision.id,
        )
    }

    fun sendPlaybackCommand(action: ScorePlaybackCommandAction) {
        commandSequence++
        playbackCommand = ScorePlaybackCommand(commandSequence, action)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch {
                sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                countdown = 4
                val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()
                try {
                    repeat(4) {
                        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                        delay(500)
                        countdown--
                    }
                } finally {
                    tone?.release()
                }
                runCatching {
                    val next = recorder ?: ChorusAudioRecorder(context, projectId).also { recorder = it }
                    val nextTransportOffset = scoreTimeMs - next.durationMs.value
                    if (recordingAnchors.isEmpty()) {
                        transportOffsetMs = nextTransportOffset
                        next.saveTransportOffset(nextTransportOffset)
                    }
                    val nextAnchors = recordingAnchors + ChorusSyncAnchor(
                        anchorOrder = recordingAnchors.size,
                        scoreTick = scoreTick,
                        mediaMs = next.durationMs.value,
                    )
                    recordingAnchors = nextAnchors
                    next.saveTimelineAnchors(nextAnchors.map { anchor -> anchor.scoreTick to anchor.mediaMs })
                    next.startSegment()
                    recording = true
                    sendPlaybackCommand(ScorePlaybackCommandAction.PLAY)
                }.onFailure { error = it.message ?: "无法开始录音" }
            }
        } else {
            error = context.getString(R.string.chorus_recording_microphone_required)
        }
    }

    fun requestStart() {
        permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(projectId, revisionId) {
        val loadedProject = viewModel.chorusProject(projectId).getOrElse {
            error = it.message ?: context.getString(R.string.chorus_recording_project_load_error)
            return@LaunchedEffect
        }
        project = loadedProject
        partId = loadedProject.parts.firstOrNull()?.id
        ChorusAudioRecorder.recover(context, projectId)?.let { recovered ->
            recorder = recovered
            transportOffsetMs = recovered.recoveredTransportOffset()
            if (recovered.hasAudio) {
                val savedAnchors = recovered.recoveredTimelineAnchors().mapIndexed { index, pair ->
                    ChorusSyncAnchor(index, pair.first, pair.second)
                }
                recordingAnchors = savedAnchors.ifEmpty { listOf(ChorusSyncAnchor(0, 0, 0)) }
                recovered.existingResult()?.let { restored ->
                    result = restored
                    resultAnchors = savedAnchors.ifEmpty {
                        listOf(
                            ChorusSyncAnchor(0, 0, 0),
                            ChorusSyncAnchor(1, 0, restored.durationMs),
                        )
                    }
                }
            }
        }
        val revision = viewModel.scoreRevision(loadedProject.alignmentScoreRevisionId).getOrElse {
            error = it.message ?: context.getString(R.string.chorus_recording_revision_load_error)
            return@LaunchedEffect
        }
        scoreRevision = revision
        scoreBytes = viewModel.scoreBytes(revision).mapCatching(MusicXmlRuntimeSanitizer::forAlphaTab)
            .getOrElse {
                error = it.message ?: context.getString(R.string.chorus_recording_score_load_error)
                null
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                recorder?.pauseSafely()
                sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                recording = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recorder?.pauseSafely()
        }
    }

    DisposableEffect(rootView, recording) {
        rootView.keepScreenOn = recording
        onDispose { rootView.keepScreenOn = false }
    }

    DisposableEffect(recording) {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                if (recording && removedDevices.isNotEmpty()) {
                    recorder?.pauseSafely()
                    sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                    recording = false
                    error = context.getString(R.string.chorus_recording_audio_route_changed)
                }
            }
        }
        if (recording) audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { if (recording) audioManager.unregisterAudioDeviceCallback(callback) }
    }

    val recordedResult = result
    val bytes = scoreBytes
    when {
        recordedResult != null -> RecordingReview(
            title = title,
            result = recordedResult,
            parts = project?.parts.orEmpty(),
            partId = partId,
            kind = kind,
            label = label,
            offsetMs = offsetMs,
            rightsConfirmed = rightsConfirmed,
            busy = busy,
            error = error,
            onBack = onBack,
            onHelp = { showHelp = true },
            onPart = { partId = it },
            onKind = { kind = it },
            onLabel = { label = it },
            onOffset = { offsetMs = it.coerceIn(-60_000, 60_000) },
            onRights = { rightsConfirmed = it },
            onDiscard = {
                scope.launch {
                    recorder?.discard()
                    recorder = null
                    result = null
                    resultAnchors = emptyList()
                    recordingAnchors = emptyList()
                    error = null
                }
            },
            onUpload = {
                val activeProject = project ?: return@RecordingReview
                val recorded = result ?: return@RecordingReview
                scope.launch {
                    busy = true
                    error = null
                    val sha = sha256(recorded.uploadFile)
                    val upload = ChorusTrackUpload(
                        file = recorded.uploadFile,
                        mediaType = recorded.mediaType,
                        sha256 = sha,
                        durationMs = recorded.durationMs,
                        partId = partId,
                        contributionKind = kind,
                        displayLabel = label.ifBlank {
                            context.getString(R.string.chorus_recording_default_track_name)
                        },
                        initialAnchors = resultAnchors.ifEmpty {
                            listOf(ChorusSyncAnchor(0, scoreTick, scoreTimeMs.coerceAtLeast(0)))
                        },
                    )
                    uploadAndSubmit(
                        viewModel,
                        activeProject.id,
                        upload,
                        (offsetMs + transportOffsetMs).coerceIn(-15 * 60_000L, 15 * 60_000L),
                    ).fold(
                        onSuccess = {
                            recorder?.discard()
                            onUploaded()
                        },
                        onFailure = {
                            error = it.message
                                ?: context.getString(R.string.chorus_recording_upload_error)
                        },
                    )
                    busy = false
                }
            },
        )

        bytes != null && project != null -> RemoteScoreScreen(
            title = title,
            canonicalMusicXml = bytes,
            onBackClick = onBack,
            onHelpClick = { showHelp = true },
            expectedPartCount = project?.parts?.size,
            onPlaybackPositionChanged = { tick, timeMs ->
                scoreTick = tick
                scoreTimeMs = timeMs
            },
            playbackCommand = playbackCommand,
            playbackInteractionEnabled = !recording && countdown == 0 && !finishing,
            scoreLabel = stringResource(R.string.chorus_recording_header_subtitle, scoreDisplayLabel),
            revisionLabel = scoreRevision?.let {
                stringResource(R.string.score_revision_label, it.revisionNo)
            },
            revisionTimeLabel = scoreRevision?.let { formatScoreRevisionTime(it.createdAt) },
            revisionLockedMessage = stringResource(R.string.chorus_recording_revision_locked),
            playbackSubject = playbackSubject,
            topContent = {
                ChorusRecordingPanel(
                    durationMs = duration,
                    peak = peak,
                    recording = recording,
                    countdown = countdown,
                    finishing = finishing,
                    hasAudio = currentRecorder?.hasAudio == true,
                    showLyrics = showLyrics,
                    error = error,
                    onShowScore = { showLyrics = false },
                    onShowLyrics = { showLyrics = true },
                    onToggleRecording = {
                        if (recording) {
                            scope.launch {
                                recorder?.pause()
                                sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                                recording = false
                            }
                        } else {
                            requestStart()
                        }
                    },
                    onFinish = {
                        scope.launch {
                            finishing = true
                            error = null
                            runCatching {
                                recorder?.finish()
                                    ?: error(context.getString(R.string.chorus_recording_no_audio))
                            }.fold(
                                onSuccess = { finished ->
                                    val endAnchor = ChorusSyncAnchor(
                                        anchorOrder = recordingAnchors.size,
                                        scoreTick = scoreTick,
                                        mediaMs = finished.durationMs,
                                    )
                                    val anchors = (recordingAnchors + endAnchor)
                                        .filterIndexed { index, anchor ->
                                            index == 0 || anchor.scoreTick >=
                                                (recordingAnchors.getOrNull(index - 1)?.scoreTick ?: 0L)
                                        }
                                        .mapIndexed { index, anchor -> anchor.copy(anchorOrder = index) }
                                    resultAnchors = anchors
                                    recorder?.saveTimelineAnchors(
                                        anchors.map { anchor -> anchor.scoreTick to anchor.mediaMs },
                                    )
                                    result = finished
                                    recording = false
                                    sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                                },
                                onFailure = {
                                    error = it.message
                                        ?: context.getString(R.string.chorus_recording_finish_error)
                                },
                            )
                            finishing = false
                        }
                    },
                )
            },
            overlayContent = {
                if (showLyrics) {
                    ChorusRecordingLyrics(lyrics)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        else -> ChorusRecordingLoadState(
            error = error,
            onBack = onBack,
            onHelp = { showHelp = true },
        )
    }

    if (showHelp) {
        ChorusRecordingHelpSheet(onDismiss = { showHelp = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChorusRecordingLoadState(
    error: String?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chorus_recording_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(RhythmIcons.Back, stringResource(R.string.score_back))
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onHelp) {
                        Icon(
                            MaterialSymbolIcon("help", filled = true),
                            stringResource(R.string.chorus_recording_help_open),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (error == null) M3CircularLoader()
            else ErrorCard(error)
        }
    }
}

@Composable
private fun ChorusRecordingPanel(
    durationMs: Long,
    peak: Float,
    recording: Boolean,
    countdown: Int,
    finishing: Boolean,
    hasAudio: Boolean,
    showLyrics: Boolean,
    error: String?,
    onShowScore: () -> Unit,
    onShowLyrics: () -> Unit,
    onToggleRecording: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        countdown > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                        recording -> MaterialTheme.colorScheme.errorContainer
                        hasAudio -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            MaterialSymbolIcon(
                                if (recording) "mic" else if (hasAudio) "pause" else "music_note",
                                filled = true,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            when {
                                countdown > 0 -> stringResource(
                                    R.string.chorus_recording_countdown,
                                    countdown,
                                )
                                recording -> stringResource(R.string.chorus_recording_status_recording)
                                hasAudio -> stringResource(R.string.chorus_recording_status_paused)
                                else -> stringResource(R.string.chorus_recording_status_ready)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    formatDuration(durationMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.chorus_recording_input_level),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        if (peak > .88f) stringResource(R.string.chorus_recording_input_too_loud)
                        else stringResource(R.string.chorus_recording_input_good),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (peak > .88f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { peak.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (peak > .88f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !showLyrics,
                    onClick = onShowScore,
                    label = { Text(stringResource(R.string.chorus_recording_tab_score)) },
                    leadingIcon = { Icon(RhythmIcons.Score, null, modifier = Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = showLyrics,
                    onClick = onShowLyrics,
                    label = { Text(stringResource(R.string.chorus_recording_tab_lyrics)) },
                    leadingIcon = {
                        Icon(MaterialSymbolIcon("lyrics", filled = true), null, modifier = Modifier.size(18.dp))
                    },
                )
                Text(
                    stringResource(R.string.chorus_recording_metronome_hint),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onToggleRecording,
                    enabled = countdown == 0 && !finishing,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (recording) RhythmIcons.Pause else MaterialSymbolIcon("mic", filled = true),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            if (recording) R.string.chorus_recording_pause
                            else if (hasAudio) R.string.chorus_recording_continue
                            else R.string.chorus_recording_start,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = onFinish,
                    enabled = hasAudio && countdown == 0 && !finishing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (finishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(MaterialSymbolIcon("preview", filled = true), contentDescription = null)
                    }
                    Text(
                        text = stringResource(
                            if (finishing) R.string.chorus_recording_preparing_preview
                            else R.string.chorus_recording_finish_preview,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Text(
                stringResource(R.string.chorus_recording_headphone_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { ErrorCard(it) }
        }
    }
}

@Composable
private fun ChorusRecordingLyrics(lyrics: String?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (lyrics.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.chorus_recording_no_lyrics),
                    modifier = Modifier.padding(28.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                lyrics,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChorusRecordingHelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    RhythmAdaptiveModalSheet(
        onDismissRequest = onDismiss,
        adaptiveType = SheetAdaptiveType.AUTO_DIALOG,
        tabletMaxWidth = 680.dp,
        showCloseButton = false,
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 760.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            MaterialSymbolIcon("help", filled = true),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.chorus_recording_help_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.chorus_recording_help_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalIconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        MaterialSymbolIcon("close", filled = true),
                        stringResource(R.string.chorus_recording_help_close),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            stringResource(R.string.chorus_recording_help_overview),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    Text(
                        stringResource(R.string.chorus_recording_help_steps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    ChorusRecordingHelpStep(
                        1,
                        stringResource(R.string.chorus_recording_help_step_prepare_title),
                        stringResource(R.string.chorus_recording_help_step_prepare_body),
                    )
                }
                item {
                    ChorusRecordingHelpStep(
                        2,
                        stringResource(R.string.chorus_recording_help_step_record_title),
                        stringResource(R.string.chorus_recording_help_step_record_body),
                    )
                }
                item {
                    ChorusRecordingHelpStep(
                        3,
                        stringResource(R.string.chorus_recording_help_step_preview_title),
                        stringResource(R.string.chorus_recording_help_step_preview_body),
                    )
                }
                item {
                    ChorusRecordingHelpStep(
                        4,
                        stringResource(R.string.chorus_recording_help_step_publish_title),
                        stringResource(R.string.chorus_recording_help_step_publish_body),
                    )
                }
                item {
                    ChorusRecordingHelpNote(
                        "lock",
                        stringResource(R.string.chorus_recording_help_version_title),
                        stringResource(R.string.chorus_recording_help_version_body),
                    )
                }
                item {
                    ChorusRecordingHelpNote(
                        "headphones",
                        stringResource(R.string.chorus_recording_help_latency_title),
                        stringResource(R.string.chorus_recording_help_latency_body),
                    )
                }
                item {
                    ChorusRecordingHelpNote(
                        "save",
                        stringResource(R.string.chorus_recording_help_draft_title),
                        stringResource(R.string.chorus_recording_help_draft_body),
                    )
                }
                item {
                    ChorusRecordingHelpNote(
                        "delete",
                        stringResource(R.string.chorus_recording_help_delete_title),
                        stringResource(R.string.chorus_recording_help_delete_body),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChorusRecordingHelpStep(number: Int, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number.toString(), fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChorusRecordingHelpNote(iconName: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                MaterialSymbolIcon(iconName, filled = true),
                contentDescription = null,
                modifier = Modifier.size(21.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: ChorusTrack,
    part: ChorusPart?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onChecked, enabled = track.status == "published")
            Column(Modifier.weight(1f)) {
                Text(track.displayLabel, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(part?.name, kindLabel(track.contributionKind), track.uploaderDisplayName).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${statusLabel(track.status)} · ${alignmentLabel(track.alignmentState)}" +
                        (track.durationMs?.let { " · ${formatDuration(it)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            onDelete?.let {
                TextButton(onClick = it) {
                    Icon(MaterialSymbolIcon("delete", filled = true), contentDescription = null)
                    Text(stringResource(R.string.chorus_track_delete))
                }
            }
        }
    }
}

@Composable
private fun DeleteTrackDialog(
    trackName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(MaterialSymbolIcon("delete", filled = true), contentDescription = null) },
        title = { Text(stringResource(R.string.chorus_track_delete_title)) },
        text = {
            Text(stringResource(R.string.chorus_track_delete_body, trackName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(
                    stringResource(
                        if (busy) R.string.chorus_track_deleting else R.string.chorus_track_delete_confirm,
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.chorus_track_delete_cancel))
            }
        },
    )
}

@Composable
private fun EmptyChorus(message: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(RhythmIcons.MusicNote, null, modifier = Modifier.size(48.dp))
            Text(message)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun UploadAudioDialog(
    audio: PendingAudio,
    parts: List<ChorusPart>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onUpload: (String?, String, String, Long) -> Unit,
) {
    var selectedPart by remember(parts) { mutableStateOf(parts.firstOrNull()?.id) }
    var kind by remember { mutableStateOf(if (parts.isEmpty()) "other" else "vocal_part") }
    var label by remember { mutableStateOf(audio.file.nameWithoutExtension.take(100)) }
    var offsetMs by remember { mutableLongStateOf(0L) }
    var rightsConfirmed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("上传已有音频") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${formatDuration(audio.durationMs)} · ${audio.mediaType}")
                AudioPreviewButton(audio.file)
                KindAndPartFields(parts, selectedPart, kind, { selectedPart = it }, { kind = it })
                OutlinedTextField(label, { label = it.take(300) }, label = { Text("音轨名称") })
                Text("起点微调：$offsetMs ms", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-50L, -10L, 10L, 50L).forEach { delta ->
                        AssistChip(
                            onClick = { offsetMs = (offsetMs + delta).coerceIn(-60_000, 60_000) },
                            label = { Text(if (delta > 0) "+$delta" else "$delta") },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(rightsConfirmed, { rightsConfirmed = it })
                    Text(stringResource(R.string.chorus_recording_rights_direct))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpload(
                        if (kind == "vocal_part") selectedPart else null,
                        kind,
                        label.ifBlank { "我的音轨" },
                        offsetMs,
                    )
                },
                enabled = !busy && rightsConfirmed && label.isNotBlank() &&
                    (kind != "vocal_part" || selectedPart != null),
            ) {
                Text(
                    stringResource(
                        if (busy) R.string.chorus_recording_uploading
                        else R.string.chorus_recording_upload_publish,
                    )
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}

@Composable
private fun AudioPreviewButton(file: File) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
    }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose { player.release() }
    }
    FilledTonalButton(onClick = { if (playing) player.pause() else player.play() }) {
        Icon(if (playing) RhythmIcons.Pause else RhythmIcons.Play, null)
        Text(if (playing) " 暂停预览" else " 播放预览")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingReview(
    title: String,
    result: ChorusRecordingResult,
    parts: List<ChorusPart>,
    partId: String?,
    kind: String,
    label: String,
    offsetMs: Long,
    rightsConfirmed: Boolean,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onPart: (String?) -> Unit,
    onKind: (String) -> Unit,
    onLabel: (String) -> Unit,
    onOffset: (Long) -> Unit,
    onRights: (Boolean) -> Unit,
    onDiscard: () -> Unit,
    onUpload: () -> Unit,
) {
    val context = LocalContext.current
    val miniPlayerBottomPadding = LocalMiniPlayerPadding.current.calculateBottomPadding()
    val previewPlayer = remember(result.wavFile) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(result.wavFile)))
            prepare()
        }
    }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewPositionMs by remember { mutableLongStateOf(0L) }
    val previewDurationMs = result.durationMs.coerceAtLeast(1L)
    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { previewPlaying = isPlaying }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    previewPlayer.seekTo(0)
                    previewPositionMs = 0
                }
            }
        }
        previewPlayer.addListener(listener)
        onDispose { previewPlayer.release() }
    }
    LaunchedEffect(previewPlaying) {
        while (previewPlaying) {
            previewPositionMs = previewPlayer.currentPosition.coerceIn(0L, previewDurationMs)
            delay(150)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.chorus_recording_preview_title),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Icon(RhythmIcons.Back, stringResource(R.string.score_back))
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onHelp) {
                        Icon(
                            MaterialSymbolIcon("help", filled = true),
                            stringResource(R.string.chorus_recording_help_open),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 24.dp + miniPlayerBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ExpressiveButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonGroupStyle.Tonal,
                ) {
                    ExpressiveGroupButton(
                        onClick = onDiscard,
                        enabled = !busy,
                        isStart = true,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    ) {
                        Icon(MaterialSymbolIcon("restart_alt", filled = true), null)
                        Text(
                            stringResource(R.string.chorus_recording_record_again),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    ExpressiveGroupButton(
                        onClick = onUpload,
                        enabled = !busy && rightsConfirmed && label.isNotBlank() &&
                            (kind != "vocal_part" || partId != null),
                        isEnd = true,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(RhythmIcons.CloudUpload, null)
                        }
                        Text(
                            stringResource(
                                if (busy) R.string.chorus_recording_uploading
                                else R.string.chorus_recording_upload_publish,
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    MaterialSymbolIcon("check", filled = true),
                                    contentDescription = null,
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.chorus_recording_preview_ready),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(R.string.chorus_recording_preview_local),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.chorus_recording_preview_listen),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Waveform(result.peakSamples)
                        Slider(
                            value = previewPositionMs.toFloat(),
                            onValueChange = {
                                previewPositionMs = it.toLong()
                                previewPlayer.seekTo(previewPositionMs)
                            },
                            valueRange = 0f..previewDurationMs.toFloat(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(formatDuration(previewPositionMs), style = MaterialTheme.typography.labelMedium)
                            Text(formatDuration(previewDurationMs), style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = {
                                if (previewPlaying) previewPlayer.pause()
                                else previewPlayer.play()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(if (previewPlaying) RhythmIcons.Pause else RhythmIcons.Play, null)
                            Text(
                                stringResource(
                                    if (previewPlaying) R.string.chorus_recording_preview_pause
                                    else R.string.chorus_recording_preview_play,
                                ),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Text(
                            stringResource(
                                R.string.chorus_recording_preview_format,
                                formatDuration(result.durationMs),
                                result.mediaType,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.chorus_recording_track_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        KindAndPartFields(parts, partId, kind, onPart, onKind)
                        OutlinedTextField(
                            label,
                            onLabel,
                            label = { Text(stringResource(R.string.chorus_recording_track_name)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.chorus_recording_alignment_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.chorus_recording_alignment_value, offsetMs),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(-50L, -10L, 10L, 50L).forEach { delta ->
                                AssistChip(
                                    onClick = { onOffset(offsetMs + delta) },
                                    label = { Text(if (delta > 0) "+$delta ms" else "$delta ms") },
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.chorus_recording_alignment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(rightsConfirmed, onRights)
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.chorus_recording_publish_direct_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.chorus_recording_rights_direct),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            error?.let { message -> item { ErrorCard(message) } }
        }
    }
}

@Composable
private fun KindAndPartFields(
    parts: List<ChorusPart>,
    partId: String?,
    kind: String,
    onPart: (String?) -> Unit,
    onKind: (String) -> Unit,
) {
    Text(stringResource(R.string.chorus_recording_type), fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("vocal_part", "harmony", "guitar", "piano", "percussion", "other").forEach { value ->
            FilterChip(selected = value == kind, onClick = { onKind(value) }, label = { Text(kindLabel(value)) })
        }
    }
    if (kind == "vocal_part") {
        Text(stringResource(R.string.chorus_recording_part), fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parts.forEach { part ->
                FilterChip(selected = part.id == partId, onClick = { onPart(part.id) }, label = { Text(part.name) })
            }
        }
    }
}

@Composable
private fun Waveform(peaks: List<Float>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        if (peaks.isEmpty()) return@Canvas
        val step = size.width / peaks.size
        peaks.forEachIndexed { index, value ->
            val half = value.coerceIn(0f, 1f) * size.height / 2
            drawLine(color, Offset(index * step, size.height / 2 - half), Offset(index * step, size.height / 2 + half), max(1f, step * .65f))
        }
    }
}

private data class PendingAudio(val file: File, val mediaType: String, val durationMs: Long, val sha256: String)

private suspend fun copyPickedAudio(context: android.content.Context, uri: Uri): PendingAudio =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val directory = File(context.filesDir, "chorus-imports").apply { mkdirs() }
        val staging = File(directory, ".${System.currentTimeMillis()}.part")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法打开所选音频" }
            staging.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var size = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    size += read
                    require(size <= 100L * 1024 * 1024) { "音频不能超过 100 MiB" }
                    output.write(buffer, 0, read)
                }
            }
        }
        val normalizedType = detectAudioMediaType(staging)
            ?: run { staging.delete(); error("文件内容不是支持的 WAV、M4A、MP3、FLAC 或 OGG 音频") }
        val extension = when (normalizedType) {
            "audio/wav" -> "wav"
            "audio/mp4" -> "m4a"
            "audio/mpeg" -> "mp3"
            "audio/flac" -> "flac"
            else -> "ogg"
        }
        val target = File(directory, "${System.currentTimeMillis()}.$extension")
        require(staging.renameTo(target)) { "无法保存导入的音频" }
        val retriever = MediaMetadataRetriever()
        val duration = try {
            retriever.setDataSource(target.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally {
            retriever.release()
        }
        require(duration != null && duration in 1..15 * 60 * 1000L) { "音频时长无效或超过 15 分钟" }
        PendingAudio(target, normalizedType, duration, sha256(target))
    }

private fun detectAudioMediaType(file: File): String? {
    val header = file.inputStream().use { input -> ByteArray(32).also { input.read(it) } }
    return when {
        header.copyOfRange(0, 4).contentEquals("fLaC".toByteArray()) -> "audio/flac"
        header.copyOfRange(0, 4).contentEquals("OggS".toByteArray()) -> "audio/ogg"
        header.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
            header.copyOfRange(8, 12).contentEquals("WAVE".toByteArray()) -> "audio/wav"
        header.copyOfRange(0, 3).contentEquals("ID3".toByteArray()) ||
            (header[0].toInt() and 0xff) == 0xff && (header[1].toInt() and 0xe0) == 0xe0 -> "audio/mpeg"
        header.copyOfRange(4, 8).contentEquals("ftyp".toByteArray()) -> "audio/mp4"
        else -> null
    }
}

private suspend fun uploadAndSubmit(
    viewModel: CatalogViewModel,
    projectId: String,
    upload: ChorusTrackUpload,
    offsetMs: Long = 0,
): Result<ChorusTrack> = runCatching {
    var track = viewModel.uploadChorusTrack(projectId, upload).getOrThrow()
    var attempts = 0
    while (track.status in setOf("processing", "draft") && attempts < 45) {
        delay(1_000)
        track = viewModel.chorusProject(projectId).getOrThrow().tracks.first { item -> item.id == track.id }
        attempts++
    }
    check(track.status == "pending_review") { "服务器尚未完成音频处理，请稍后在合唱页刷新" }
    val anchors = upload.initialAnchors.ifEmpty { listOf(ChorusSyncAnchor(0, 0, max(0, offsetMs))) }
    track = viewModel.alignChorusTrack(track.id, track.revision, offsetMs, anchors).getOrThrow()
    track = viewModel.submitChorusTrack(track.id).getOrThrow()
    check(track.status in setOf("published", "pending_review")) {
        "服务器未确认音轨提交状态，请刷新后重试"
    }
    track
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun kindLabel(kind: String): String = when (kind) {
    "vocal_part" -> "声部"
    "harmony" -> "和声"
    "guitar" -> "吉他"
    "piano" -> "钢琴"
    "percussion" -> "打击乐"
    else -> "其他"
}

private fun statusLabel(status: String): String = when (status) {
    "published" -> "已公开"
    "pending_review" -> "等待发布确认"
    "processing" -> "处理中"
    "rejected" -> "已下架"
    "withdrawn" -> "已删除"
    "failed" -> "处理失败"
    else -> "草稿"
}

private fun alignmentLabel(state: String): String = when (state) {
    "verified" -> "已核对对齐"
    "manual" -> "已手动对齐"
    "automatic" -> "自动对齐"
    "failed" -> "对齐失败"
    else -> "待校正"
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0) / 1_000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
