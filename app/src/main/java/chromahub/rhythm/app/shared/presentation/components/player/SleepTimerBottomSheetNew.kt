@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.player
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType


import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel.SleepAction
import chromahub.rhythm.app.shared.presentation.components.common.RhythmWavyProgressLoader
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton

data class SleepTimerOption(
    val minutes: Int,
    val label: String,
    val icon: MaterialSymbolIcon
)

/** The three states the bottom sheet content can be in */
private enum class SheetContentState { Presets, Active, InlinePicker }

@Composable
fun SleepTimerBottomSheetNew(
    onDismiss: () -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // ViewModel state
    val isTimerActive by musicViewModel.sleepTimerActive.collectAsState()
    val remainingSeconds by musicViewModel.sleepTimerRemainingSeconds.collectAsState()
    val timerAction by musicViewModel.sleepTimerAction.collectAsState()
    val serviceConnected by musicViewModel.serviceConnected.collectAsState()

    // Local UI states
    var selectedAction by remember { mutableStateOf(SleepAction.valueOf(timerAction.takeIf { it.isNotBlank() } ?: "FADE_OUT")) }

    // totalTimerDuration: captured once when a timer session starts.
    // Using a Long state keyed on isTimerActive so it resets cleanly each session.
    var totalTimerDuration by remember { mutableLongStateOf(0L) }

    // Capture total duration on timer start; reset on stop.
    // Only update when we don't have a value yet (first broadcast of the session).
    LaunchedEffect(isTimerActive) {
        if (!isTimerActive) {
            totalTimerDuration = 0L
        }
    }
    // Seed duration from the first non-zero remainingSeconds of an active session.
    LaunchedEffect(isTimerActive, remainingSeconds) {
        if (isTimerActive && totalTimerDuration == 0L && remainingSeconds > 0L) {
            totalTimerDuration = remainingSeconds
        }
    }

    // Timer options
    val timerOptions = listOf(
        SleepTimerOption(5, "5 min", MaterialSymbolIcon("coffee", filled = true)),
        SleepTimerOption(15, "15 min", MaterialSymbolIcon("local_cafe", filled = true)),
        SleepTimerOption(30, "30 min", MaterialSymbolIcon("wb_twilight", filled = true)),
        SleepTimerOption(45, "45 min", MaterialSymbolIcon("bedtime", filled = true)),
        SleepTimerOption(60, "1 hour", MaterialSymbolIcon("nightlight_round", filled = true)),
        SleepTimerOption(90, "1.5 hr", RhythmIcons.DarkMode)
    )

    // Action options
    val actionOptions = listOf(
        Triple(SleepAction.FADE_OUT, "Fade Out", RhythmIcons.VolumeDown),
        Triple(SleepAction.PAUSE, "Pause", RhythmIcons.Pause),
        Triple(SleepAction.STOP, "Stop", RhythmIcons.Stop)
    )

    // Helper functions
    fun startTimer(minutes: Int) {
        if (!isPlaying || currentSong == null) {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
            return
        }
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        // Set total duration immediately so progress is accurate from the first tick
        totalTimerDuration = minutes * 60L
        musicViewModel.startSleepTimer(minutes, selectedAction)
    }

    fun stopTimer() {
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        totalTimerDuration = 0L
        musicViewModel.stopSleepTimer()
    }

    fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }

    // Sheet content state — drives AnimatedContent
    var sheetState by remember { mutableStateOf(SheetContentState.Presets) }

    // Sync sheet content state with timer, but never interrupt InlinePicker
    LaunchedEffect(isTimerActive) {
        when {
            isTimerActive && sheetState != SheetContentState.InlinePicker ->
                sheetState = SheetContentState.Active
            !isTimerActive && sheetState == SheetContentState.Active ->
                sheetState = SheetContentState.Presets
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.sleep_timer),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                                text = when {
                                    isTimerActive -> "Active • ${formatTime(remainingSeconds)} remaining"
                                    !isPlaying || !serviceConnected || currentSong == null -> "No music playing"
                                    else -> "Set automatic playback control"
                                },
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = if (!isPlaying || !serviceConnected || currentSong == null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // ── Animated content area ────────────────────────────────────────
            AnimatedContent(
                targetState = sheetState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { if (targetState == SheetContentState.Active) -it / 4 else it / 4 }
                    ) togetherWith fadeOut(animationSpec = tween(200)) + slideOutVertically(
                        animationSpec = tween(200),
                        targetOffsetY = { if (targetState == SheetContentState.Active) it / 4 else -it / 4 }
                    )
                },
                label = "sleep_timer_content"
            ) { state ->
                when (state) {
                    // ── Active timer ───────────────────────────────────────
                    SheetContentState.Active -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Wavy progress card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.size(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val elapsedSeconds = totalTimerDuration - remainingSeconds
                                        val progress = if (totalTimerDuration > 0L) {
                                            (elapsedSeconds.toFloat() / totalTimerDuration).coerceIn(0f, 1f)
                                        } else 0f

                                        RhythmWavyProgressLoader(
                                            progress = progress,
                                            modifier = Modifier.fillMaxSize(),
                                            indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = formatTime(remainingSeconds),
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = context.getString(R.string.bottomsheet_timer_remaining),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Action selection
                            ActionSelectionCard(
                                selectedAction = selectedAction,
                                actionOptions = actionOptions,
                                onActionSelected = { action ->
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    selectedAction = action
                                    musicViewModel.appSettings.setSleepTimerAction(action.name)
                                },
                                context = context
                            )

                            // Bottom button group — Stop | Edit
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 3.dp
                            ) {
                                ExpressiveButtonGroup(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    style = ButtonGroupStyle.Tonal
                                ) {
                                    ExpressiveGroupButton(
                                        onClick = { stopTimer() },
                                        modifier = Modifier.weight(1f),
                                        isStart = true,
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(RhythmIcons.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(context.getString(R.string.bottomsheet_cancel))
                                    }
                                    ExpressiveGroupButton(
                                        onClick = { sheetState = SheetContentState.InlinePicker },
                                        modifier = Modifier.weight(1f),
                                        isEnd = true
                                    ) {
                                        Icon(RhythmIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(context.getString(R.string.bottomsheet_timer_edit))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // ── Inline time picker ─────────────────────────────────
                    SheetContentState.InlinePicker -> {
                        InlineTimePickerContent(
                            onCancel = {
                                sheetState = if (isTimerActive) SheetContentState.Active else SheetContentState.Presets
                            },
                            onTimeSelected = { hours, minutes ->
                                val totalMinutes = hours * 60 + minutes
                                if (totalMinutes > 0) {
                                    startTimer(totalMinutes)
                                }
                                sheetState = SheetContentState.Active
                            }
                        )
                    }

                    // ── Presets ────────────────────────────────────────────
                    SheetContentState.Presets -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Quick preset chips
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbolIcon("timer", filled = true),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = context.getString(R.string.sleep_timer_quick),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        items(timerOptions, key = { "timer_${it.minutes}" }) { option ->
                                            val isTimerAvailable = isPlaying && serviceConnected && currentSong != null
                                            Card(
                                                onClick = { if (isTimerAvailable) startTimer(option.minutes) },
                                                modifier = Modifier.size(width = 80.dp, height = 84.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isTimerAvailable) {
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    }
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = option.icon,
                                                        contentDescription = null,
                                                        tint = if (isTimerAvailable) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        },
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = option.label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isTimerAvailable) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        },
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Custom timer card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = context.getString(R.string.sleep_timer_custom),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = context.getString(R.string.bottomsheet_timer_custom_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val isCustomTimerAvailable = isPlaying && serviceConnected && currentSong != null
                                    FilledTonalButton(
                                        onClick = {
                                            if (isCustomTimerAvailable) {
                                                sheetState = SheetContentState.InlinePicker
                                            }
                                        },
                                        enabled = isCustomTimerAvailable,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(context.getString(R.string.bottomsheet_timer_custom_title))
                                    }
                                }
                            }

                            // Action selection
                            ActionSelectionCard(
                                selectedAction = selectedAction,
                                actionOptions = actionOptions,
                                onActionSelected = { action ->
                                    HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                    selectedAction = action
                                    musicViewModel.appSettings.setSleepTimerAction(action.name)
                                },
                                context = context
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSelectionCard(
    selectedAction: SleepAction,
    actionOptions: List<Triple<SleepAction, String, MaterialSymbolIcon>>,
    onActionSelected: (SleepAction) -> Unit,
    context: android.content.Context
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = RhythmIcons.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.sleep_timer_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = context.getString(R.string.sleep_timer_action_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actionOptions.forEach { (action, label, icon) ->
                    val isSelected = selectedAction == action
                    Card(
                        onClick = { onActionSelected(action) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineTimePickerContent(
    onCancel: () -> Unit,
    onTimeSelected: (hours: Int, minutes: Int) -> Unit
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 30,
        is24Hour = true
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Inline TimePicker — no extra header or back button; sheet header is sufficient
        TimePicker(
            state = timePickerState,
            colors = TimePickerDefaults.colors(
                clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                selectorColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface,
                periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom buttons — Cancel | Set — matching HomeSectionReorderBottomSheet style
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                ExpressiveGroupButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    isStart = true,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(context.getString(R.string.bottomsheet_cancel))
                }
                ExpressiveGroupButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                    },
                    modifier = Modifier.weight(1f),
                    isEnd = true
                ) {
                    Text(context.getString(R.string.bottomsheet_timer_set))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
