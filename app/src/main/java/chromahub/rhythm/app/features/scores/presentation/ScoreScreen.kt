@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package chromahub.rhythm.app.features.scores.presentation

import alphaTab.AlphaTabView
import alphaTab.PlayerMode
import alphaTab.Settings
import alphaTab.collections.List as AlphaTabList
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.model.Score
import alphaTab.model.Track
import alphaTab.model.NoteStyle
import alphaTab.model.NoteSubElement
import alphaTab.model.Color as AlphaTabColor
import alphaTab.synth.PlayerState
import android.graphics.Color as AndroidColor
import android.view.View
import android.widget.RelativeLayout
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.scores.data.BundledScoreLoader
import chromahub.rhythm.app.features.scores.data.BundledScoreVariant
import chromahub.rhythm.app.features.scores.data.LoadedScore
import chromahub.rhythm.app.features.scores.data.ScoreEditSession
import chromahub.rhythm.app.features.scores.data.ScoreNoteRef
import chromahub.rhythm.app.features.scores.data.ScoreSourceMap
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface ScoreUiState {
    data object Loading : ScoreUiState
    data class Ready(
        val scores: Map<BundledScoreVariant, LoadedScore>,
        val soundFont: ByteArray
    ) : ScoreUiState
    data object Error : ScoreUiState
}

private enum class ScoreViewMode {
    OCR,
    MIDI,
    COMPARE
}

private enum class ScorePlaybackStatus {
    PREPARING,
    READY,
    PLAYING,
    PAUSED,
    ERROR
}

private enum class ScorePlaybackIndicatorMode {
    LINE,
    PULSE,
}

private data class MergedDisplayProjection(
    val trackIndexes: Set<Int>,
    val scores: Map<BundledScoreVariant, Score>
)

private class ScorePlaybackController {
    private var view: AlphaTabView? = null
    private var score: Score? = null
    private var playerIsReady = false
    private var mutedTrackIndexes: Set<Int> = emptySet()
    private val completionTracker = ScorePlaybackCompletionTracker()
    private val displayBindings = mutableMapOf<AlphaTabView, ScorePlaybackDisplayBinding>()
    private var currentTick = 0.0
    private var activePositions: List<ScorePlaybackBeatPosition> = emptyList()

    fun attach(view: AlphaTabView, score: Score) {
        this.view = view
        this.score = score
        playerIsReady = false
        completionTracker.reset()
        resetDisplayPosition()
    }

    fun detach(view: AlphaTabView) {
        if (this.view === view) {
            this.view = null
            score = null
            playerIsReady = false
            completionTracker.reset()
        }
    }

    fun onPlayerReady(view: AlphaTabView) {
        if (this.view === view) {
            playerIsReady = true
            applyMutedTracks()
        }
    }

    fun setMutedTrackIndexes(indexes: Set<Int>) {
        mutedTrackIndexes = indexes
        applyMutedTracks()
    }

    fun playPause() {
        val currentView = view ?: return
        if (completionTracker.consumeFinished()) {
            // alphaTab's Android player runs commands on a FIFO worker queue. Queueing stop
            // before playPause guarantees that a naturally completed score returns to tick 0
            // before playback starts again.
            currentView.api.stop()
            currentView.api.playPause()
            Log.i(SCORE_PLAYBACK_TAG, "replay after finish: reset to start")
        } else {
            currentView.api.playPause()
        }
    }

    fun stop() {
        completionTracker.reset()
        view?.api?.stop()
        resetDisplayPosition()
    }

    fun onPlayerFinished(view: AlphaTabView) {
        if (this.view === view) {
            completionTracker.markFinished()
            resetDisplayPosition()
        }
    }

    fun onPlayerStarted(view: AlphaTabView) {
        if (this.view === view) {
            completionTracker.reset()
        }
    }

    fun onPlayerPositionChanged(view: AlphaTabView, tick: Double) {
        if (this.view !== view) return
        currentTick = tick
        displayBindings.forEach { (displayView, binding) ->
            displayView.post {
                if (displayBindings[displayView] === binding) {
                    // Pulse mode keeps alphaTab's cursor transparent, but still uses its
                    // position internally so automatic scrolling follows the active beat.
                    displayView.api.tickPosition = tick
                }
            }
        }
    }

    fun onActiveBeatsChanged(view: AlphaTabView, positions: List<ScorePlaybackBeatPosition>) {
        if (this.view !== view || positions == activePositions) return
        activePositions = positions
        displayBindings.forEach { (displayView, binding) ->
            displayView.post {
                if (displayBindings[displayView] !== binding) return@post
                when (binding.mode) {
                    ScorePlaybackIndicatorMode.LINE -> displayView.api.scrollToCursor()
                    ScorePlaybackIndicatorMode.PULSE -> {
                        binding.onPulsePositions(positions)
                        displayView.api.scrollToCursor()
                    }
                }
            }
        }
    }

    fun attachDisplay(
        view: AlphaTabView,
        mode: ScorePlaybackIndicatorMode,
        onPulsePositions: (List<ScorePlaybackBeatPosition>) -> Unit,
    ) {
        val binding = ScorePlaybackDisplayBinding(mode, onPulsePositions)
        displayBindings[view] = binding
        view.post {
            if (displayBindings[view] !== binding) return@post
            when (mode) {
                ScorePlaybackIndicatorMode.LINE -> view.api.tickPosition = currentTick
                ScorePlaybackIndicatorMode.PULSE -> onPulsePositions(activePositions)
            }
        }
    }

    fun detachDisplay(view: AlphaTabView) {
        displayBindings.remove(view)
    }

    private fun resetDisplayPosition() {
        currentTick = 0.0
        activePositions = emptyList()
        displayBindings.forEach { (displayView, binding) ->
            displayView.post {
                if (displayBindings[displayView] !== binding) return@post
                when (binding.mode) {
                    ScorePlaybackIndicatorMode.LINE -> displayView.api.tickPosition = 0.0
                    ScorePlaybackIndicatorMode.PULSE -> binding.onPulsePositions(emptyList())
                }
            }
        }
    }

    private fun applyMutedTracks() {
        val currentView = view ?: return
        val currentScore = score ?: return
        if (!playerIsReady) return

        val mutedTracks = AlphaTabList<Track>()
        val audibleTracks = AlphaTabList<Track>()
        currentScore.tracks.forEach { track ->
            if (track.index.toInt() in mutedTrackIndexes) {
                mutedTracks.push(track)
            } else {
                audibleTracks.push(track)
            }
        }
        currentView.api.changeTrackMute(mutedTracks, true)
        currentView.api.changeTrackMute(audibleTracks, false)
        Log.i(SCORE_PLAYBACK_TAG, "muted tracks=${mutedTrackIndexes.sorted()}")
    }
}

private data class ScorePlaybackDisplayBinding(
    val mode: ScorePlaybackIndicatorMode,
    val onPulsePositions: (List<ScorePlaybackBeatPosition>) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { BundledScoreLoader(context) }
    val miniPlayerPadding = LocalMiniPlayerPadding.current
    var retryKey by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ScoreUiState>(ScoreUiState.Loading) }
    var viewMode by rememberSaveable { mutableStateOf(ScoreViewMode.OCR) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(loader, retryKey) {
        state = ScoreUiState.Loading
        state = runCatching {
            ScoreUiState.Ready(
                scores = loader.loadAll(),
                soundFont = loader.loadSoundFont()
            )
        }
            .fold(
                onSuccess = { it },
                onFailure = { ScoreUiState.Error }
            )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.score_viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = RhythmIcons.Back,
                            contentDescription = stringResource(R.string.score_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1200.dp)
                    .padding(miniPlayerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoreMetadata(
                    viewMode = viewMode,
                    enabled = !isEditing,
                    onViewModeChange = { viewMode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )

                when (val currentState = state) {
                    ScoreUiState.Loading -> ScoreLoading(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    ScoreUiState.Error -> ScoreError(
                        onRetry = { retryKey++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    is ScoreUiState.Ready -> ScoreReadyContent(
                        loader = loader,
                        scores = currentState.scores,
                        soundFont = currentState.soundFont,
                        viewMode = viewMode,
                        onEditingChange = { isEditing = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

/** Read-only projection of an immutable server ScoreRevision. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScoreScreen(
    title: String,
    canonicalMusicXml: ByteArray,
    onBackClick: () -> Unit,
    revisionLabel: String? = null,
    canOpenNewerRevision: Boolean = false,
    canOpenOlderRevision: Boolean = false,
    onOpenNewerRevision: () -> Unit = {},
    onOpenOlderRevision: () -> Unit = {},
    expectedPartCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = remember(context) { BundledScoreLoader(context) }
    val miniPlayerPadding = LocalMiniPlayerPadding.current
    var loaded by remember(canonicalMusicXml) { mutableStateOf<LoadedScore?>(null) }
    var soundFont by remember(canonicalMusicXml) { mutableStateOf<ByteArray?>(null) }
    var failed by remember(canonicalMusicXml) { mutableStateOf(false) }
    var retryKey by remember(canonicalMusicXml) { mutableIntStateOf(0) }

    LaunchedEffect(loader, canonicalMusicXml, retryKey) {
        runCatching {
            loader.loadVariant(BundledScoreVariant.OCR, canonicalMusicXml) to loader.loadSoundFont()
        }.fold(
            onSuccess = {
                loaded = it.first
                soundFont = it.second
                failed = false
            },
            onFailure = { failed = true },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (revisionLabel == null) title else "$title · $revisionLabel") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(RhythmIcons.Back, contentDescription = stringResource(R.string.score_back))
                    }
                },
                actions = {
                    if (revisionLabel != null) {
                        TextButton(onClick = onOpenNewerRevision, enabled = canOpenNewerRevision) { Text("较新") }
                        TextButton(onClick = onOpenOlderRevision, enabled = canOpenOlderRevision) { Text("较旧") }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).padding(miniPlayerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                failed -> ScoreError(onRetry = {
                    loaded = null
                    soundFont = null
                    failed = false
                    retryKey++
                })
                loaded == null || soundFont == null -> ScoreLoading(Modifier.fillMaxSize())
                else -> Column(Modifier.fillMaxSize()) {
                    val actualTrackCount = checkNotNull(loaded).displayScore.tracks.length.toInt()
                    if (expectedPartCount != null && expectedPartCount != actualTrackCount) {
                        Text(
                            "提示：编配定义 $expectedPartCount 个声部，此修订包含 $actualTrackCount 条谱轨；将按谱面原始结构显示。",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    ScoreReadyContent(
                        loader = loader,
                        scores = mapOf(
                            BundledScoreVariant.OCR to checkNotNull(loaded),
                            BundledScoreVariant.MIDI to checkNotNull(loaded),
                        ),
                        soundFont = checkNotNull(soundFont),
                        viewMode = ScoreViewMode.OCR,
                        onEditingChange = {},
                        allowEditing = false,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreMetadata(
    viewMode: ScoreViewMode,
    enabled: Boolean,
    onViewModeChange: (ScoreViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.score_sample_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(
                R.string.score_composer_format,
                stringResource(R.string.score_sample_composer)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = stringResource(R.string.score_compare_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScoreModeChip(
                selected = viewMode == ScoreViewMode.OCR,
                enabled = enabled,
                onClick = { onViewModeChange(ScoreViewMode.OCR) },
                label = stringResource(R.string.score_source_ocr)
            )
            ScoreModeChip(
                selected = viewMode == ScoreViewMode.MIDI,
                enabled = enabled,
                onClick = { onViewModeChange(ScoreViewMode.MIDI) },
                label = stringResource(R.string.score_source_midi)
            )
            ScoreModeChip(
                selected = viewMode == ScoreViewMode.COMPARE,
                enabled = enabled,
                onClick = { onViewModeChange(ScoreViewMode.COMPARE) },
                label = stringResource(R.string.score_source_compare)
            )
        }
        Text(
            text = stringResource(
                when (viewMode) {
                    ScoreViewMode.OCR -> R.string.score_source_ocr_description
                    ScoreViewMode.MIDI -> R.string.score_source_midi_description
                    ScoreViewMode.COMPARE -> R.string.score_source_compare_description
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScoreModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ScoreReadyContent(
    loader: BundledScoreLoader,
    scores: Map<BundledScoreVariant, LoadedScore>,
    soundFont: ByteArray,
    viewMode: ScoreViewMode,
    onEditingChange: (Boolean) -> Unit,
    allowEditing: Boolean = true,
    modifier: Modifier = Modifier
) {
    var activeScores by remember(scores) { mutableStateOf(scores) }
    val ocrScore = checkNotNull(activeScores[BundledScoreVariant.OCR])
    val midiScore = checkNotNull(activeScores[BundledScoreVariant.MIDI])
    var playbackVariant by rememberSaveable { mutableStateOf(BundledScoreVariant.OCR) }
    var playbackStatus by remember { mutableStateOf(ScorePlaybackStatus.PREPARING) }
    var playbackIndicatorMode by rememberSaveable {
        mutableStateOf(ScorePlaybackIndicatorMode.LINE)
    }
    // View-local on purpose: the previous release saved a single track index in this slot,
    // which is not compatible with the new multi-select bit mask after an app upgrade.
    var staffMode by remember { mutableStateOf(ScoreStaffMode.ALL_STAVES) }
    var notationLayout by remember { mutableStateOf(ScoreNotationLayout.SEPARATE_PARTS) }
    var partColorMode by remember { mutableStateOf(ScorePartColorMode.DEFAULT) }
    var selectedTrackMask by remember { mutableIntStateOf(1) }
    var trackControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var mutedTracksByVariant by remember {
        mutableStateOf<Map<BundledScoreVariant, Set<Int>>>(emptyMap())
    }
    val playbackController = remember { ScorePlaybackController() }
    val coroutineScope = rememberCoroutineScope()
    var editSession by remember { mutableStateOf<ScoreEditSession?>(null) }
    var editVariant by remember { mutableStateOf<BundledScoreVariant?>(null) }
    var editBaseScore by remember { mutableStateOf<LoadedScore?>(null) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var editRevision by remember { mutableIntStateOf(0) }
    var editBusy by remember { mutableStateOf(false) }
    var editMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewMode) {
        when (viewMode) {
            ScoreViewMode.OCR -> playbackVariant = BundledScoreVariant.OCR
            ScoreViewMode.MIDI -> playbackVariant = BundledScoreVariant.MIDI
            ScoreViewMode.COMPARE -> Unit
        }
    }
    LaunchedEffect(playbackVariant) {
        playbackStatus = ScorePlaybackStatus.PREPARING
    }

    val playbackScore = checkNotNull(activeScores[playbackVariant])
    val trackOptions = remember(playbackScore.displayScore) {
        buildScoreTrackOptions(
            playbackScore.displayScore.tracks.toList().map { it.name }
        )
    }
    val effectiveSelectedTrackMask = trackOptions.takeIf { it.isNotEmpty() }?.let {
        normalizeScoreTrackSelectionMask(selectedTrackMask, it.size)
    } ?: selectedTrackMask
    val selectedTrackIndexes = trackOptions
        .mapNotNullTo(mutableSetOf()) { option ->
            option.index.takeIf { index ->
                effectiveSelectedTrackMask and (1 shl index) != 0
            }
        }
        .ifEmpty { trackOptions.firstOrNull()?.let { setOf(it.index) }.orEmpty() }
    val visibleTrackIndexes = when (staffMode) {
        ScoreStaffMode.ALL_STAVES -> trackOptions.mapTo(mutableSetOf()) { it.index }
        ScoreStaffMode.SELECTED_PARTS -> selectedTrackIndexes
    }
    val mutedTrackIndexes = mutedTracksByVariant[playbackVariant].orEmpty()
    var mergedDisplayProjection by remember { mutableStateOf<MergedDisplayProjection?>(null) }

    LaunchedEffect(notationLayout, visibleTrackIndexes, activeScores) {
        if (notationLayout == ScoreNotationLayout.SEPARATE_PARTS) {
            mergedDisplayProjection = null
            return@LaunchedEffect
        }
        mergedDisplayProjection = null
        val projectedScores = runCatching {
            mutableMapOf<BundledScoreVariant, Score>().apply {
                activeScores.forEach { (variant, score) ->
                    this[variant] = score.loadMergedDisplayScore(visibleTrackIndexes)
                }
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            Log.e(SCORE_DISPLAY_TAG, "merged score projection failed; using separate tracks", error)
            activeScores.mapValues { it.value.displayScore }
        }
        mergedDisplayProjection = MergedDisplayProjection(
            trackIndexes = visibleTrackIndexes,
            scores = projectedScores
        )
    }

    val currentMergedScores = mergedDisplayProjection
        ?.takeIf { it.trackIndexes == visibleTrackIndexes }
        ?.scores

    LaunchedEffect(playbackVariant, mutedTrackIndexes) {
        playbackController.setMutedTrackIndexes(mutedTrackIndexes)
    }

    val editSaveSuccess = stringResource(R.string.score_edit_save_success)
    val editApplyError = stringResource(R.string.score_edit_apply_error)
    val editSaveError = stringResource(R.string.score_edit_save_error)
    val editMapError = stringResource(R.string.score_edit_map_error)
    val selectedNote = remember(editSession, editRevision, selectedNoteId) {
        selectedNoteId?.let { editSession?.note(it) }
    }

    fun rebuildEditedScore() {
        val session = editSession ?: return
        val variant = editVariant ?: return
        editBusy = true
        editMessage = null
        coroutineScope.launch {
            runCatching { loader.loadVariant(variant, session.toByteArray()) }
                .onSuccess { loaded ->
                    activeScores = activeScores + (variant to loaded)
                    editRevision++
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(SCORE_EDIT_TAG, "edited score projection failed", error)
                    editMessage = editApplyError
                }
            editBusy = false
        }
    }

    fun runEditCommand(command: ScoreEditSession.() -> Unit) {
        val session = editSession ?: return
        runCatching { session.command() }
            .onSuccess { rebuildEditedScore() }
            .onFailure { error ->
                Log.e(SCORE_EDIT_TAG, "score edit command failed", error)
                editMessage = editApplyError
            }
    }

    fun closeEditSession() {
        editSession = null
        editVariant = null
        editBaseScore = null
        selectedNoteId = null
        editMessage = null
        editBusy = false
        onEditingChange(false)
    }

    fun cancelEditing() {
        val variant = editVariant
        val base = editBaseScore
        if (variant != null && base != null) activeScores = activeScores + (variant to base)
        closeEditSession()
    }

    fun startEditing() {
        val variant = when (viewMode) {
            ScoreViewMode.OCR -> BundledScoreVariant.OCR
            ScoreViewMode.MIDI -> BundledScoreVariant.MIDI
            ScoreViewMode.COMPARE -> return
        }
        val base = checkNotNull(activeScores[variant])
        runCatching { ScoreEditSession.create(base.canonicalMusicXml) }
            .onSuccess { session ->
                playbackController.stop()
                playbackStatus = ScorePlaybackStatus.READY
                playbackVariant = variant
                editSession = session
                editVariant = variant
                editBaseScore = base
                selectedNoteId = null
                editMessage = null
                val trackIndex = selectedTrackIndexes.minOrNull() ?: 0
                selectedTrackMask = 1 shl trackIndex
                staffMode = ScoreStaffMode.SELECTED_PARTS
                notationLayout = ScoreNotationLayout.SEPARATE_PARTS
                onEditingChange(true)
            }
            .onFailure { error ->
                Log.e(SCORE_EDIT_TAG, "could not start score edit session", error)
                editMessage = editApplyError
            }
    }

    fun saveEditing() {
        val session = editSession ?: return
        val variant = editVariant ?: return
        editBusy = true
        coroutineScope.launch {
            runCatching { loader.saveWorkingCopy(variant, session.toByteArray()) }
                .onSuccess {
                    closeEditSession()
                    Log.i(SCORE_EDIT_TAG, editSaveSuccess)
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(SCORE_EDIT_TAG, "could not save working MusicXML revision", error)
                    editMessage = editSaveError
                    editBusy = false
                }
        }
    }

    Column(modifier = modifier) {
        ScorePlaybackControls(
            viewMode = viewMode,
            playbackVariant = playbackVariant,
            status = playbackStatus,
            indicatorMode = playbackIndicatorMode,
            onPlaybackVariantChange = { playbackVariant = it },
            onIndicatorModeChange = { playbackIndicatorMode = it },
            onPlayPause = { playbackController.playPause() },
            onStop = {
                playbackController.stop()
                playbackStatus = ScorePlaybackStatus.READY
            },
            interactionEnabled = editSession == null,
            modifier = Modifier.fillMaxWidth()
        )
        if (allowEditing) {
            ScoreEditControls(
                editing = editSession != null,
                canStart = viewMode != ScoreViewMode.COMPARE,
                canUndo = editSession?.canUndo == true,
                canRedo = editSession?.canRedo == true,
                isDirty = editSession?.isDirty == true,
                busy = editBusy,
                message = editMessage,
                onStart = ::startEditing,
                onUndo = {
                    if (editSession?.undo() == true) rebuildEditedScore()
                },
                onRedo = {
                    if (editSession?.redo() == true) rebuildEditedScore()
                },
                onSave = ::saveEditing,
                onCancel = ::cancelEditing,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (editSession == null) {
            ScoreTrackControls(
                trackOptions = trackOptions,
                expanded = trackControlsExpanded,
                staffMode = staffMode,
                notationLayout = notationLayout,
                partColorMode = partColorMode,
                selectedTrackIndexes = selectedTrackIndexes,
                mutedTrackIndexes = mutedTrackIndexes,
                onExpandedChange = { trackControlsExpanded = it },
                onStaffModeChange = { staffMode = it },
                onNotationLayoutChange = { notationLayout = it },
                onPartColorModeChange = { partColorMode = it },
                onTrackVisibilityToggle = { trackIndex ->
                    selectedTrackMask = toggleScoreTrackSelectionMask(
                        selectedMask = effectiveSelectedTrackMask,
                        trackIndex = trackIndex,
                        trackCount = trackOptions.size
                    )
                    staffMode = ScoreStaffMode.SELECTED_PARTS
                },
                onTrackSoundToggle = { trackIndex ->
                    val nextMutedTracks = mutedTrackIndexes.toMutableSet().apply {
                        if (!add(trackIndex)) remove(trackIndex)
                    }
                    mutedTracksByVariant = mutedTracksByVariant +
                        (playbackVariant to nextMutedTracks)
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ScoreEditStaffControls(
                trackOptions = trackOptions,
                staffMode = staffMode,
                selectedTrackIndexes = selectedTrackIndexes,
                onStaffModeChange = { mode ->
                    staffMode = mode
                    notationLayout = ScoreNotationLayout.SEPARATE_PARTS
                    selectedNoteId = null
                },
                onTrackChange = { trackIndex ->
                    selectedTrackMask = 1 shl trackIndex
                    staffMode = ScoreStaffMode.SELECTED_PARTS
                    notationLayout = ScoreNotationLayout.SEPARATE_PARTS
                    selectedNoteId = null
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (viewMode) {
                ScoreViewMode.OCR -> key(
                    ocrScore.displayScore,
                    staffMode,
                    notationLayout,
                    partColorMode,
                    currentMergedScores?.get(BundledScoreVariant.OCR),
                    selectedTrackIndexes,
                    editVariant == BundledScoreVariant.OCR,
                    selectedNoteId
                ) {
                    AlphaTabScore(
                        loadedScore = ocrScore,
                        staffMode = staffMode,
                        notationLayout = notationLayout,
                        partColorMode = partColorMode,
                        playbackIndicatorMode = playbackIndicatorMode,
                        playbackController = playbackController,
                        mergedDisplayScore = currentMergedScores?.get(BundledScoreVariant.OCR),
                        selectedTrackIndexes = selectedTrackIndexes,
                        editMode = editVariant == BundledScoreVariant.OCR,
                        canonicalNotes = editSession?.notes.orEmpty(),
                        selectedNoteId = selectedNoteId,
                        onNoteSelected = { noteId ->
                            if (noteId == null) editMessage = editMapError
                            else {
                                selectedNoteId = noteId
                                editMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScoreViewMode.MIDI -> key(
                    midiScore.displayScore,
                    staffMode,
                    notationLayout,
                    partColorMode,
                    currentMergedScores?.get(BundledScoreVariant.MIDI),
                    selectedTrackIndexes,
                    editVariant == BundledScoreVariant.MIDI,
                    selectedNoteId
                ) {
                    AlphaTabScore(
                        loadedScore = midiScore,
                        staffMode = staffMode,
                        notationLayout = notationLayout,
                        partColorMode = partColorMode,
                        playbackIndicatorMode = playbackIndicatorMode,
                        playbackController = playbackController,
                        mergedDisplayScore = currentMergedScores?.get(BundledScoreVariant.MIDI),
                        selectedTrackIndexes = selectedTrackIndexes,
                        editMode = editVariant == BundledScoreVariant.MIDI,
                        canonicalNotes = editSession?.notes.orEmpty(),
                        selectedNoteId = selectedNoteId,
                        onNoteSelected = { noteId ->
                            if (noteId == null) editMessage = editMapError
                            else {
                                selectedNoteId = noteId
                                editMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScoreViewMode.COMPARE -> Column(modifier = Modifier.fillMaxSize()) {
                    ScoreComparePane(
                        label = stringResource(R.string.score_source_ocr),
                        loadedScore = ocrScore,
                        staffMode = staffMode,
                        notationLayout = notationLayout,
                        partColorMode = partColorMode,
                        playbackIndicatorMode = playbackIndicatorMode,
                        playbackController = playbackController,
                        mergedDisplayScore = currentMergedScores?.get(BundledScoreVariant.OCR),
                        selectedTrackIndexes = selectedTrackIndexes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    ScoreComparePane(
                        label = stringResource(R.string.score_source_midi),
                        loadedScore = midiScore,
                        staffMode = staffMode,
                        notationLayout = notationLayout,
                        partColorMode = partColorMode,
                        playbackIndicatorMode = playbackIndicatorMode,
                        playbackController = playbackController,
                        mergedDisplayScore = currentMergedScores?.get(BundledScoreVariant.MIDI),
                        selectedTrackIndexes = selectedTrackIndexes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            key(playbackVariant, playbackScore.playbackScore) {
                ScorePlaybackEngine(
                    loadedScore = playbackScore,
                    soundFont = soundFont,
                    controller = playbackController,
                    onStatusChange = { playbackStatus = it },
                    modifier = Modifier.size(1.dp)
                )
            }

        }
        if (editSession != null) {
            ScoreNoteEditor(
                note = selectedNote,
                enabled = !editBusy,
                onLowerPitch = {
                    selectedNoteId?.let { noteId ->
                        runEditCommand { changePitch(noteId, -1) }
                    }
                },
                onRaisePitch = {
                    selectedNoteId?.let { noteId ->
                        runEditCommand { changePitch(noteId, 1) }
                    }
                },
                onApplyLyric = { lyric ->
                    selectedNoteId?.let { noteId ->
                        runEditCommand { setLyrics(noteId, lyric) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ScoreEditControls(
    editing: Boolean,
    canStart: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isDirty: Boolean,
    busy: Boolean,
    message: String?,
    onStart: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!editing) {
                    Button(onClick = onStart, enabled = canStart) {
                        Text(stringResource(R.string.score_edit))
                    }
                    Text(
                        text = stringResource(
                            if (canStart) R.string.score_edit_start_hint
                            else R.string.score_edit_compare_hint
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.score_edit_voice_hint),
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedButton(onClick = onUndo, enabled = canUndo && !busy) {
                        Text(stringResource(R.string.score_edit_undo))
                    }
                    OutlinedButton(onClick = onRedo, enabled = canRedo && !busy) {
                        Text(stringResource(R.string.score_edit_redo))
                    }
                    Button(onClick = onSave, enabled = isDirty && !busy) {
                        Text(stringResource(R.string.score_edit_save))
                    }
                    OutlinedButton(onClick = onCancel, enabled = !busy) {
                        Text(stringResource(R.string.score_edit_cancel))
                    }
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreNoteEditor(
    note: ScoreNoteRef?,
    enabled: Boolean,
    onLowerPitch: () -> Unit,
    onRaisePitch: () -> Unit,
    onApplyLyric: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (note == null) {
            Text(
                text = stringResource(R.string.score_edit_select_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            return@Surface
        }

        var lyric by remember(note.id, note.lyric) { mutableStateOf(note.lyric.orEmpty()) }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.score_edit_selected_note,
                    scorePitchName(note.midiPitch),
                    note.measureIndex + 1
                ),
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onLowerPitch, enabled = enabled) {
                    Text(stringResource(R.string.score_edit_pitch_down))
                }
                OutlinedButton(onClick = onRaisePitch, enabled = enabled) {
                    Text(stringResource(R.string.score_edit_pitch_up))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = lyric,
                    onValueChange = { lyric = it },
                    enabled = enabled,
                    singleLine = true,
                    label = { Text(stringResource(R.string.score_edit_lyric)) },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { onApplyLyric(lyric) }, enabled = enabled) {
                    Text(stringResource(R.string.score_edit_apply_lyric))
                }
            }
        }
    }
}

@Composable
private fun ScoreEditStaffControls(
    trackOptions: List<ScoreTrackOption>,
    staffMode: ScoreStaffMode,
    selectedTrackIndexes: Set<Int>,
    onStaffModeChange: (ScoreStaffMode) -> Unit,
    onTrackChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trackOptions.size <= 1) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_edit_staff_display),
                    style = MaterialTheme.typography.labelLarge
                )
                ScoreModeChip(
                    selected = staffMode == ScoreStaffMode.SELECTED_PARTS,
                    onClick = { onStaffModeChange(ScoreStaffMode.SELECTED_PARTS) },
                    label = stringResource(R.string.score_layout_separate)
                )
                ScoreModeChip(
                    selected = staffMode == ScoreStaffMode.ALL_STAVES,
                    onClick = { onStaffModeChange(ScoreStaffMode.ALL_STAVES) },
                    label = stringResource(R.string.score_staff_all)
                )
            }
            if (staffMode == ScoreStaffMode.SELECTED_PARTS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.score_edit_part_select),
                        style = MaterialTheme.typography.labelLarge
                    )
                    trackOptions.forEach { option ->
                        ScoreModeChip(
                            selected = option.index in selectedTrackIndexes,
                            onClick = { onTrackChange(option.index) },
                            label = option.label
                        )
                    }
                }
            }
        }
    }
}

private fun scorePitchName(midiPitch: Int): String {
    val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    return names[Math.floorMod(midiPitch, 12)] + (Math.floorDiv(midiPitch, 12) - 1)
}

@Composable
private fun ScoreTrackControls(
    trackOptions: List<ScoreTrackOption>,
    expanded: Boolean,
    staffMode: ScoreStaffMode,
    notationLayout: ScoreNotationLayout,
    partColorMode: ScorePartColorMode,
    selectedTrackIndexes: Set<Int>,
    mutedTrackIndexes: Set<Int>,
    onExpandedChange: (Boolean) -> Unit,
    onStaffModeChange: (ScoreStaffMode) -> Unit,
    onNotationLayoutChange: (ScoreNotationLayout) -> Unit,
    onPartColorModeChange: (ScorePartColorMode) -> Unit,
    onTrackVisibilityToggle: (Int) -> Unit,
    onTrackSoundToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trackOptions.size <= 1) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_part_settings),
                    style = MaterialTheme.typography.labelLarge
                )
                OutlinedButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) RhythmIcons.ExpandLess else RhythmIcons.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (expanded) R.string.score_controls_collapse
                            else R.string.score_controls_expand
                        ),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            if (!expanded) return@Column
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_notation_layout),
                    style = MaterialTheme.typography.labelLarge
                )
                ScoreModeChip(
                    selected = notationLayout == ScoreNotationLayout.SEPARATE_PARTS,
                    onClick = { onNotationLayoutChange(ScoreNotationLayout.SEPARATE_PARTS) },
                    label = stringResource(R.string.score_layout_separate)
                )
                ScoreModeChip(
                    selected = notationLayout == ScoreNotationLayout.MERGED_STAVES,
                    onClick = { onNotationLayoutChange(ScoreNotationLayout.MERGED_STAVES) },
                    label = stringResource(R.string.score_layout_merged)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_part_color),
                    style = MaterialTheme.typography.labelLarge
                )
                ScoreModeChip(
                    selected = partColorMode == ScorePartColorMode.DEFAULT,
                    onClick = { onPartColorModeChange(ScorePartColorMode.DEFAULT) },
                    label = stringResource(R.string.score_part_color_default)
                )
                ScoreModeChip(
                    selected = partColorMode == ScorePartColorMode.ENHANCED,
                    onClick = { onPartColorModeChange(ScorePartColorMode.ENHANCED) },
                    label = stringResource(R.string.score_part_color_enhanced)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_staff_display),
                    style = MaterialTheme.typography.labelLarge
                )
                ScoreModeChip(
                    selected = staffMode == ScoreStaffMode.ALL_STAVES,
                    onClick = { onStaffModeChange(ScoreStaffMode.ALL_STAVES) },
                    label = stringResource(R.string.score_staff_all)
                )
                ScoreModeChip(
                    selected = staffMode == ScoreStaffMode.SELECTED_PARTS,
                    onClick = { onStaffModeChange(ScoreStaffMode.SELECTED_PARTS) },
                    label = stringResource(R.string.score_staff_current)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_part_select),
                    style = MaterialTheme.typography.labelLarge
                )
                trackOptions.forEach { option ->
                    ScoreModeChip(
                        selected = option.index in selectedTrackIndexes,
                        onClick = { onTrackVisibilityToggle(option.index) },
                        label = option.label
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_part_sound),
                    style = MaterialTheme.typography.labelLarge
                )
                trackOptions.forEach { option ->
                    val isAudible = option.index !in mutedTrackIndexes
                    FilterChip(
                        selected = isAudible,
                        onClick = { onTrackSoundToggle(option.index) },
                        label = {
                            Text(
                                stringResource(
                                    if (isAudible) {
                                        R.string.score_part_sound_on
                                    } else {
                                        R.string.score_part_sound_muted
                                    },
                                    option.label
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScorePlaybackControls(
    viewMode: ScoreViewMode,
    playbackVariant: BundledScoreVariant,
    status: ScorePlaybackStatus,
    indicatorMode: ScorePlaybackIndicatorMode,
    onPlaybackVariantChange: (BundledScoreVariant) -> Unit,
    onIndicatorModeChange: (ScorePlaybackIndicatorMode) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (viewMode == ScoreViewMode.COMPARE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.score_playback_source),
                        style = MaterialTheme.typography.labelLarge
                    )
                    ScoreModeChip(
                        selected = playbackVariant == BundledScoreVariant.OCR,
                        enabled = interactionEnabled,
                        onClick = { onPlaybackVariantChange(BundledScoreVariant.OCR) },
                        label = stringResource(R.string.score_source_ocr)
                    )
                    ScoreModeChip(
                        selected = playbackVariant == BundledScoreVariant.MIDI,
                        enabled = interactionEnabled,
                        onClick = { onPlaybackVariantChange(BundledScoreVariant.MIDI) },
                        label = stringResource(R.string.score_source_midi)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.score_playback_indicator),
                    style = MaterialTheme.typography.labelLarge
                )
                ScoreModeChip(
                    selected = indicatorMode == ScorePlaybackIndicatorMode.LINE,
                    enabled = interactionEnabled,
                    onClick = { onIndicatorModeChange(ScorePlaybackIndicatorMode.LINE) },
                    label = stringResource(R.string.score_playback_indicator_default)
                )
                ScoreModeChip(
                    selected = indicatorMode == ScorePlaybackIndicatorMode.PULSE,
                    enabled = interactionEnabled,
                    onClick = { onIndicatorModeChange(ScorePlaybackIndicatorMode.PULSE) },
                    label = stringResource(R.string.score_playback_indicator_pulse)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayPause,
                    enabled = interactionEnabled && (status == ScorePlaybackStatus.READY ||
                        status == ScorePlaybackStatus.PLAYING ||
                        status == ScorePlaybackStatus.PAUSED)
                ) {
                    Icon(
                        imageVector = if (status == ScorePlaybackStatus.PLAYING) {
                            RhythmIcons.Pause
                        } else {
                            RhythmIcons.Play
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (status == ScorePlaybackStatus.PLAYING) {
                                R.string.score_pause
                            } else {
                                R.string.score_play
                            }
                        ),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = interactionEnabled && (status == ScorePlaybackStatus.PLAYING ||
                        status == ScorePlaybackStatus.PAUSED)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.score_stop),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = stringResource(
                        when (status) {
                            ScorePlaybackStatus.PREPARING -> R.string.score_playback_preparing
                            ScorePlaybackStatus.READY -> R.string.score_playback_ready
                            ScorePlaybackStatus.PLAYING -> R.string.score_playback_playing
                            ScorePlaybackStatus.PAUSED -> R.string.score_playback_paused
                            ScorePlaybackStatus.ERROR -> R.string.score_playback_error
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == ScorePlaybackStatus.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreComparePane(
    label: String,
    loadedScore: LoadedScore,
    staffMode: ScoreStaffMode,
    notationLayout: ScoreNotationLayout,
    partColorMode: ScorePartColorMode,
    playbackIndicatorMode: ScorePlaybackIndicatorMode,
    playbackController: ScorePlaybackController,
    mergedDisplayScore: Score?,
    selectedTrackIndexes: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        key(
            loadedScore.displayScore,
            staffMode,
            notationLayout,
            partColorMode,
            mergedDisplayScore,
            selectedTrackIndexes
        ) {
            AlphaTabScore(
                loadedScore = loadedScore,
                staffMode = staffMode,
                notationLayout = notationLayout,
                partColorMode = partColorMode,
                playbackIndicatorMode = playbackIndicatorMode,
                playbackController = playbackController,
                mergedDisplayScore = mergedDisplayScore,
                selectedTrackIndexes = selectedTrackIndexes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Text(
            text = stringResource(R.string.score_loading),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ScoreError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PaddingValues(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.score_load_error_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.score_load_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.score_retry))
        }
    }
}

@Composable
private fun AlphaTabScore(
    loadedScore: LoadedScore,
    staffMode: ScoreStaffMode,
    notationLayout: ScoreNotationLayout,
    partColorMode: ScorePartColorMode,
    playbackIndicatorMode: ScorePlaybackIndicatorMode,
    playbackController: ScorePlaybackController,
    mergedDisplayScore: Score?,
    selectedTrackIndexes: Set<Int>,
    editMode: Boolean = false,
    canonicalNotes: List<ScoreNoteRef> = emptyList(),
    selectedNoteId: String? = null,
    onNoteSelected: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (notationLayout == ScoreNotationLayout.MERGED_STAVES && mergedDisplayScore == null) {
        ScoreLoading(modifier)
        return
    }
    val displayScore = mergedDisplayScore ?: loadedScore.displayScore
    val allTracks = displayScore.tracks.toList()
    val tracksToRender = when (notationLayout) {
        ScoreNotationLayout.MERGED_STAVES -> allTracks
        ScoreNotationLayout.SEPARATE_PARTS -> when (staffMode) {
            ScoreStaffMode.ALL_STAVES -> allTracks
            ScoreStaffMode.SELECTED_PARTS -> allTracks
                .filter { it.index.toInt() in selectedTrackIndexes }
                .ifEmpty { allTracks.take(1) }
        }
    }
    applyScorePartColors(
        tracks = tracksToRender,
        notationLayout = notationLayout,
        colorMode = partColorMode
    )
    if (editMode && selectedNoteId != null) {
        val selectedColor = checkNotNull(AlphaTabColor.fromJson("#4f6bff"))
        tracksToRender.forEach { track ->
            track.staves.forEach { staff ->
                staff.bars.forEach { bar ->
                    bar.voices.forEach { voice ->
                        voice.beats.forEach { beat ->
                            beat.notes.forEach { note ->
                                if (ScoreSourceMap.findNoteId(note, canonicalNotes) == selectedNoteId) {
                                    note.style = (note.style ?: NoteStyle()).apply {
                                        NoteSubElement.values().forEach { element ->
                                            colors.set(element, selectedColor)
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
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                AlphaTabView(context, null).apply {
                    val displayView = this
                    val playbackOverlay = ScorePlaybackOverlayView(context)
                    tag = playbackOverlay
                    api.settings.player.playerMode = if (editMode) {
                        PlayerMode.Disabled
                    } else {
                        PlayerMode.EnabledExternalMedia
                    }
                    api.settings.player.enableCursor =
                        !editMode
                    api.settings.player.enableAnimatedBeatCursor = false
                    api.settings.player.enableElementHighlighting = false
                    api.settings.player.enableUserInteraction = editMode
                    // Playback pulse markers and edit hit-testing both need note-head bounds.
                    api.settings.core.includeNoteBounds = true
                    barCursorFillColor = AndroidColor.TRANSPARENT
                    beatCursorFillColor = AndroidColor.rgb(225, 29, 72)
                    // alphaTab renders secondary voices with 100/255 alpha by default.
                    // Explicit per-voice styles carry enhanced colors; this fallback keeps
                    // any unstyled secondary glyph black instead of gray.
                    api.settings.display.resources.secondaryGlyphColor =
                        AlphaTabColor(0.0, 0.0, 0.0, 255.0)
                    if (editMode) {
                        // On touch screens alphaTab only emits noteMouseDown when the finger
                        // lands inside the small note-head bounds. Treat the wider beat hitbox
                        // as a fallback so a normal finger tap can still select its note. For a
                        // chord, the exact noteMouseDown callback below runs afterwards and wins.
                        api.beatMouseDown.on { beat ->
                            val noteId = beat.notes.toList().firstNotNullOfOrNull { note ->
                                ScoreSourceMap.findNoteId(note, canonicalNotes)
                            }
                            Log.i(
                                SCORE_EDIT_TAG,
                                "beat hit track=${beat.voice.bar.staff.track.index.toInt()} " +
                                    "bar=${beat.voice.bar.index.toInt()} " +
                                    "voice=${beat.voice.index.toInt()} beat=${beat.index.toInt()} " +
                                    "canonical=$noteId"
                            )
                            if (noteId != null) post { onNoteSelected(noteId) }
                        }
                        api.noteMouseDown.on { note ->
                            val noteId = ScoreSourceMap.findNoteId(note, canonicalNotes)
                            Log.i(
                                SCORE_EDIT_TAG,
                                "note hit track=${note.beat.voice.bar.staff.track.index.toInt()} " +
                                    "bar=${note.beat.voice.bar.index.toInt()} " +
                                    "voice=${note.beat.voice.index.toInt()} " +
                                    "beat=${note.beat.index.toInt()} note=${note.index.toInt()} " +
                                    "canonical=$noteId"
                            )
                            post { onNoteSelected(noteId) }
                        }
                    }
                    api.updateSettings()
                    doOnLayout {
                        if (!editMode) {
                            val renderWrapper = findViewById<RelativeLayout>(net.alphatab.R.id.renderWrapper)
                            val renderSurface = findViewById<View>(net.alphatab.R.id.renderSurface)
                            api.postRenderFinished.on {
                                // Adding a sibling while alphaTab creates its first render
                                // surface can invalidate the lazy bitmap placeholders. Wait
                                // until that surface is complete before installing the overlay.
                                renderSurface.post {
                                    val overlayLayout = RelativeLayout.LayoutParams(
                                        renderSurface.width,
                                        renderSurface.height,
                                    )
                                    if (playbackOverlay.parent == null) {
                                        renderWrapper.addView(playbackOverlay, overlayLayout)
                                    } else {
                                        playbackOverlay.layoutParams = overlayLayout
                                    }
                                    playbackOverlay.refresh(displayView.api.renderer.boundsLookup)
                                }
                            }
                        }
                        tracks = tracksToRender
                    }
                }
            },
            update = { displayView ->
                if (!editMode) {
                    val cursorColor = if (playbackIndicatorMode == ScorePlaybackIndicatorMode.LINE) {
                        AndroidColor.rgb(225, 29, 72)
                    } else {
                        AndroidColor.TRANSPARENT
                    }
                    if (displayView.beatCursorFillColor != cursorColor) {
                        displayView.beatCursorFillColor = cursorColor
                        displayView.api.updateSettings()
                    }
                    val playbackOverlay = displayView.tag as ScorePlaybackOverlayView
                    if (playbackIndicatorMode == ScorePlaybackIndicatorMode.LINE) {
                        playbackOverlay.showBeats(
                            emptyList(),
                            displayView.api.renderer.boundsLookup,
                        )
                    }
                    playbackController.attachDisplay(
                        view = displayView,
                        mode = playbackIndicatorMode,
                        onPulsePositions = { positions ->
                            val pulseBeats = findScorePlaybackHighlightBeats(
                                tracks = tracksToRender,
                                notationLayout = notationLayout,
                                activePositions = positions,
                            )
                            playbackOverlay.showBeats(
                                pulseBeats,
                                displayView.api.renderer.boundsLookup,
                            )
                        }
                    )
                }
            },
            onRelease = { view ->
                playbackController.detachDisplay(view)
                view.api.destroy()
            }
        )
    }
}

@Composable
private fun ScorePlaybackEngine(
    loadedScore: LoadedScore,
    soundFont: ByteArray,
    controller: ScorePlaybackController,
    onStatusChange: (ScorePlaybackStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AlphaTabView(context, null).apply {
                val playerView = this
                var playerIsReady = false
                controller.attach(playerView, loadedScore.playbackScore)
                val tempoAutomationCount = loadedScore.playbackScore.masterBars
                    .toList()
                    .sumOf { it.tempoAutomations.length.toInt() }
                Log.i(
                    SCORE_PLAYBACK_TAG,
                    "playback score loaded: tempoAutomations=$tempoAutomationCount"
                )
                val playbackSettings = Settings().apply {
                    player.playerMode = PlayerMode.EnabledAutomatic
                    player.enableCursor = false
                    player.enableUserInteraction = false
                }
                settings = playbackSettings
                api.playerReady.on {
                    playerIsReady = true
                    controller.onPlayerReady(playerView)
                    Log.i(SCORE_PLAYBACK_TAG, "player ready")
                    post { onStatusChange(ScorePlaybackStatus.READY) }
                }
                api.soundFontLoaded.on {
                    Log.i(SCORE_PLAYBACK_TAG, "soundfont loaded (${soundFont.size} bytes)")
                }
                api.playerStateChanged.on { event ->
                    val nextStatus = when (event.state) {
                        PlayerState.Playing -> {
                            controller.onPlayerStarted(playerView)
                            ScorePlaybackStatus.PLAYING
                        }
                        PlayerState.Paused -> if (playerIsReady) {
                            ScorePlaybackStatus.PAUSED
                        } else {
                            ScorePlaybackStatus.PREPARING
                        }
                    }
                    Log.i(SCORE_PLAYBACK_TAG, "state=${event.state}")
                    post { onStatusChange(nextStatus) }
                }
                api.playerPositionChanged.on { event ->
                    controller.onPlayerPositionChanged(playerView, event.currentTick)
                }
                api.activeBeatsChanged.on { event ->
                    controller.onActiveBeatsChanged(
                        playerView,
                        activeScorePlaybackPositions(event.activeBeats.toList())
                    )
                }
                api.playerFinished.on {
                    controller.onPlayerFinished(playerView)
                    Log.i(
                        SCORE_PLAYBACK_TAG,
                        "player finished: tick=${api.tickPosition}, time=${api.timePosition}"
                    )
                    post { onStatusChange(ScorePlaybackStatus.READY) }
                }
                api.updateSettings()
                post {
                    tracks = loadedScore.playbackScore.tracks.toList()
                    loadSoundFontWhenPlayerExists(
                        soundFont = soundFont,
                        onFailure = {
                            Log.e(SCORE_PLAYBACK_TAG, "soundfont player initialization timed out")
                            onStatusChange(ScorePlaybackStatus.ERROR)
                        }
                    )
                }
            }
        },
        onRelease = { view ->
            controller.detach(view)
            runCatching { view.api.stop() }
            view.api.destroy()
        }
    )
}

private fun AlphaTabView.loadSoundFontWhenPlayerExists(
    soundFont: ByteArray,
    attempt: Int = 0,
    onFailure: () -> Unit
) {
    val player = api.player
    if (player != null) {
        runCatching {
            player.loadSoundFont(
                Uint8Array(soundFont.asUByteArray()),
                false
            )
        }.onFailure {
            Log.e(SCORE_PLAYBACK_TAG, "soundfont load failed", it)
            onFailure()
        }
        return
    }
    if (attempt < SOUND_FONT_PLAYER_MAX_ATTEMPTS) {
        postDelayed(
            {
                loadSoundFontWhenPlayerExists(
                    soundFont = soundFont,
                    attempt = attempt + 1,
                    onFailure = onFailure
                )
            },
            SOUND_FONT_PLAYER_RETRY_MS
        )
    } else {
        onFailure()
    }
}

private const val SCORE_PLAYBACK_TAG = "ScorePlayback"
private const val SCORE_DISPLAY_TAG = "ScoreDisplay"
private const val SCORE_EDIT_TAG = "ScoreEdit"
private const val SOUND_FONT_PLAYER_MAX_ATTEMPTS = 60
private const val SOUND_FONT_PLAYER_RETRY_MS = 500L
