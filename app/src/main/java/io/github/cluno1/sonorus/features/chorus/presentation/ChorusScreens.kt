package io.github.cluno1.sonorus.features.chorus.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import io.github.cluno1.sonorus.features.catalog.presentation.CatalogViewModel
import io.github.cluno1.sonorus.features.chorus.data.ChorusAudioRecorder
import io.github.cluno1.sonorus.features.chorus.data.ChorusRecordingResult
import io.github.cluno1.sonorus.features.scores.presentation.RemoteScoreScreen
import io.github.cluno1.sonorus.features.scores.presentation.ScorePlaybackCommand
import io.github.cluno1.sonorus.features.scores.presentation.ScorePlaybackCommandAction
import io.github.cluno1.sonorus.shared.presentation.components.common.M3CircularLoader
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
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
    var projects by remember(workId) { mutableStateOf<List<ChorusProject>>(emptyList()) }
    var activeProjectId by remember(workId) { mutableStateOf<String?>(null) }
    var selectedIds by remember(workId) { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mix by remember { mutableStateOf<ChorusMix?>(null) }
    var pendingUpload by remember { mutableStateOf<PendingAudio?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val project = projects.firstOrNull { it.id == activeProjectId }
    val playableTracks = project?.tracks.orEmpty().filter { it.status == "published" }
    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }
    var playing by remember { mutableStateOf(false) }

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
        bottomBar = {
            if (project != null) {
                Surface(shadowElevation = 6.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { picker.launch("audio/*") },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(RhythmIcons.CloudUpload, null)
                            Text(" 上传音频")
                        }
                        Button(
                            onClick = {
                                onRecord(project.id, project.alignmentScoreRevisionId)
                            },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(RhythmIcons.MusicNote, null)
                            Text(" 录制声部")
                        }
                    }
                }
            }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                            onWithdraw = if (track.ownedByRequester && track.status != "withdrawn") ({
                                scope.launch {
                                    busy = true
                                    viewModel.withdrawChorusTrack(track.id).fold(
                                        onSuccess = { refreshKey++ },
                                        onFailure = { error = it.message ?: "撤回失败" },
                                    )
                                    busy = false
                                }
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
    var scoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var recorder by remember { mutableStateOf<ChorusAudioRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ChorusRecordingResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLyrics by remember { mutableStateOf(false) }
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
            error = "需要麦克风权限才能录制合唱音轨"
        }
    }

    fun requestStart() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(projectId, revisionId) {
        val loadedProject = viewModel.chorusProject(projectId).getOrElse {
            error = it.message ?: "合唱项目加载失败"
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
        val revision = viewModel.scoreRevision(revisionId).getOrElse {
            error = it.message ?: "谱面修订加载失败"
            return@LaunchedEffect
        }
        scoreBytes = viewModel.scoreBytes(revision).mapCatching(MusicXmlRuntimeSanitizer::forAlphaTab)
            .getOrElse { error = it.message ?: "谱面加载失败"; null }
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
                    error = "音频设备已变化，录音已安全暂停；确认耳机后再继续"
                }
            }
        }
        if (recording) audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { if (recording) audioManager.unregisterAudioDeviceCallback(callback) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("录制合唱", fontWeight = FontWeight.Bold)
                        Text(formatDuration(duration), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp)) {
                        Icon(RhythmIcons.Back, "返回")
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = {
                            if (recording) scope.launch { recorder?.pause(); recording = false }
                            if (recording) sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                            else requestStart()
                        },
                        enabled = project != null && result == null && countdown == 0,
                    ) { Icon(if (recording) RhythmIcons.Pause else RhythmIcons.Play, "开始或暂停") }
                    FilledTonalIconButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                runCatching { recorder?.finish() ?: error("还没有录音") }.fold(
                                    onSuccess = {
                                        val endAnchor = ChorusSyncAnchor(
                                            anchorOrder = recordingAnchors.size,
                                            scoreTick = scoreTick,
                                            mediaMs = it.durationMs,
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
                                        result = it
                                        recording = false
                                        sendPlaybackCommand(ScorePlaybackCommandAction.PAUSE)
                                    },
                                    onFailure = { error = it.message ?: "录音终止失败" },
                                )
                                busy = false
                            }
                        },
                        enabled = currentRecorder?.hasAudio == true && result == null && countdown == 0 && !busy,
                    ) { Icon(RhythmIcons.Stop, "终止录音") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (countdown > 0) Text("预备 $countdown", style = MaterialTheme.typography.headlineMedium)
                    Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                        drawLine(
                            color = if (peak > .88f) Color.Red else Color(0xff43a047),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width * peak, size.height / 2),
                            strokeWidth = size.height / 2,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showLyrics, onClick = { showLyrics = false }, label = { Text("乐谱") })
                        FilterChip(selected = showLyrics, onClick = { showLyrics = true }, label = { Text("歌词") })
                        AssistChip(onClick = {}, label = { Text("节拍器可在谱面设置中开启") })
                    }
                    Text(
                        "建议使用有线或 USB 耳机；蓝牙延迟可能变化。切到后台或音频中断时会自动安全暂停并保留草稿。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            if (result == null) {
                Box(Modifier.fillMaxSize()) {
                    val bytes = scoreBytes
                    if (bytes == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { M3CircularLoader() }
                    else RemoteScoreScreen(
                        title = title,
                        canonicalMusicXml = bytes,
                        onBackClick = onBack,
                        expectedPartCount = project?.parts?.size,
                        onPlaybackPositionChanged = { tick, timeMs ->
                            scoreTick = tick
                            scoreTimeMs = timeMs
                        },
                        playbackCommand = playbackCommand,
                        modifier = Modifier.fillMaxSize().alpha(if (showLyrics) 0f else 1f),
                    )
                    if (showLyrics) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            if (lyrics.isNullOrBlank()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("这份作品暂无可显示的歌词；录音仍会按谱面时间轴对齐。")
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
                }
            } else {
                RecordingReview(
                    result = result!!,
                    parts = project?.parts.orEmpty(),
                    partId = partId,
                    kind = kind,
                    label = label,
                    offsetMs = offsetMs,
                    rightsConfirmed = rightsConfirmed,
                    busy = busy,
                    onPart = { partId = it },
                    onKind = { kind = it },
                    onLabel = { label = it },
                    onOffset = { offsetMs = it.coerceIn(-60_000, 60_000) },
                    onRights = { rightsConfirmed = it },
                    onDiscard = {
                        scope.launch { recorder?.discard(); recorder = null; result = null }
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
                                displayLabel = label.ifBlank { "我的声部" },
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
                                onSuccess = { recorder?.discard(); onUploaded() },
                                onFailure = { error = it.message ?: "上传录音失败" },
                            )
                            busy = false
                        }
                    },
                )
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
    onWithdraw: (() -> Unit)?,
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
            onWithdraw?.let { TextButton(onClick = it) { Text("撤回") } }
        }
    }
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
                    Text("我拥有此录音的上传权，并同意审核通过后向已登记用户公开。")
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
            ) { Text(if (busy) "上传中" else "确认上传") }
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

@Composable
private fun RecordingReview(
    result: ChorusRecordingResult,
    parts: List<ChorusPart>,
    partId: String?,
    kind: String,
    label: String,
    offsetMs: Long,
    rightsConfirmed: Boolean,
    busy: Boolean,
    onPart: (String?) -> Unit,
    onKind: (String) -> Unit,
    onLabel: (String) -> Unit,
    onOffset: (Long) -> Unit,
    onRights: (Boolean) -> Unit,
    onDiscard: () -> Unit,
    onUpload: () -> Unit,
) {
    val context = LocalContext.current
    val previewPlayer = remember(result.wavFile) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setMediaItem(MediaItem.fromUri(Uri.fromFile(result.wavFile)))
            prepare()
        }
    }
    var previewPlaying by remember { mutableStateOf(false) }
    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { previewPlaying = isPlaying }
        }
        previewPlayer.addListener(listener)
        onDispose { previewPlayer.release() }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("录音预览", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Waveform(result.peakSamples)
        Text("时长 ${formatDuration(result.durationMs)} · ${result.mediaType}")
        FilledTonalButton(
            onClick = { if (previewPlaying) previewPlayer.pause() else previewPlayer.play() },
        ) {
            Icon(if (previewPlaying) RhythmIcons.Pause else RhythmIcons.Play, null)
            Text(if (previewPlaying) " 暂停预览" else " 播放预览")
        }
        KindAndPartFields(parts, partId, kind, onPart, onKind)
        OutlinedTextField(label, onLabel, label = { Text("音轨名称") }, modifier = Modifier.fillMaxWidth())
        Text("时间轴微调：${offsetMs} ms")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-50L, -10L, 10L, 50L).forEach { delta ->
                AssistChip(onClick = { onOffset(offsetMs + delta) }, label = { Text(if (delta > 0) "+$delta" else "$delta") })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(rightsConfirmed, onRights)
            Text("我拥有此录音的上传权，并同意审核通过后向已登记用户公开")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDiscard, enabled = !busy, modifier = Modifier.weight(1f)) { Text("放弃") }
            Button(
                onClick = onUpload,
                enabled = !busy && rightsConfirmed && label.isNotBlank() && (kind != "vocal_part" || partId != null),
                modifier = Modifier.weight(1f),
            ) { Text(if (busy) "上传中" else "上传并投稿") }
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
    Text("类型", fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("vocal_part", "harmony", "guitar", "piano", "percussion", "other").forEach { value ->
            FilterChip(selected = value == kind, onClick = { onKind(value) }, label = { Text(kindLabel(value)) })
        }
    }
    if (kind == "vocal_part") {
        Text("声部", fontWeight = FontWeight.SemiBold)
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
    viewModel.submitChorusTrack(track.id).getOrThrow()
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
    "pending_review" -> "等待审核"
    "processing" -> "处理中"
    "rejected" -> "未通过审核"
    "withdrawn" -> "已撤回"
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
