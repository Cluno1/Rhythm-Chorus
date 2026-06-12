@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.player
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType


import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel.SleepAction
import chromahub.rhythm.app.shared.presentation.components.common.RhythmWavyProgressLoader
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class SleepTimerOption(
    val minutes: Int,
    val label: String,
    val icon: MaterialSymbolIcon
)



@Composable
fun SleepTimerBottomSheetNew(
    onDismiss: () -> Unit,
    currentSong: Song?,
    isPlaying: Boolean,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    // Use ViewModel's sleep timer state directly
    val isTimerActive by musicViewModel.sleepTimerActive.collectAsState()
    val remainingSeconds by musicViewModel.sleepTimerRemainingSeconds.collectAsState()
    val timerAction by musicViewModel.sleepTimerAction.collectAsState()
    val serviceConnected by musicViewModel.serviceConnected.collectAsState()
    
    // Local UI states
    var selectedAction by remember { mutableStateOf(SleepAction.valueOf(timerAction.takeIf { it.isNotBlank() } ?: "FADE_OUT")) }
    var totalTimerDuration by remember { mutableLongStateOf(0L) }
    
    // Update total duration when timer starts
    LaunchedEffect(isTimerActive, remainingSeconds) {
        if (isTimerActive && totalTimerDuration == 0L && remainingSeconds > 0) {
            totalTimerDuration = remainingSeconds
        } else if (!isTimerActive) {
            totalTimerDuration = 0L
        }
    }
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "sleep_timer_animation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Timer options
    val timerOptions = listOf(
        SleepTimerOption(5, "5 min", MaterialSymbolIcon("coffee", filled = true)),
        SleepTimerOption(15, "15 min", MaterialSymbolIcon("local_cafe", filled = true)),
        SleepTimerOption(30, "30 min", MaterialSymbolIcon("wb_twilight", filled = true)),
        SleepTimerOption(45, "45 min", MaterialSymbolIcon("bedtime", filled = true)),
        SleepTimerOption(60, "1 hour", MaterialSymbolIcon("nightlight_round", filled = true)),
        SleepTimerOption(90, "1.5 hour", RhythmIcons.DarkMode)
    )
    
    // Action options
    val actionOptions = listOf(
        Triple(SleepAction.FADE_OUT, "Fade Out", RhythmIcons.VolumeDown),
        Triple(SleepAction.PAUSE, "Pause", RhythmIcons.Pause),
        Triple(SleepAction.STOP, "Stop", RhythmIcons.Stop)
    )
    
    // Clean timer functions
    fun startTimer(minutes: Int) {
        if (!isPlaying || currentSong == null) {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
            return
        }
        
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        totalTimerDuration = minutes * 60L
        musicViewModel.startSleepTimer(minutes, selectedAction)
    }
    
    fun stopTimer() {
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        musicViewModel.stopSleepTimer()
    }
    
    // State for Material 3 TimePicker dialog
    var showTimePickerDialog by remember { mutableStateOf(false) }
    
    fun showTimePicker() {
        // Check if music is playing and service is connected before showing picker
        if (!isPlaying || currentSong == null) {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
            // Don't show picker - let the UI show the disabled state
            return
        }
        
        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
        showTimePickerDialog = true
    }
    
    // Format time
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with improved layout
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
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
                                    shape = androidx.compose.foundation.shape.CircleShape
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = if (!isPlaying || !serviceConnected || currentSong == null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    if (isTimerActive) {
                        Icon(
                            imageVector = MaterialSymbolIcon("timer", filled = true),
                            contentDescription = null,
                            
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(rotationAngle)
                                .scale(pulseScale)
                        )
                    }
                }
            }
            
            // Timer Display (when active)
            if (isTimerActive) {
                item {
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
                                modifier = Modifier.size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val elapsedSeconds = totalTimerDuration - remainingSeconds
                                val progress = if (totalTimerDuration > 0) elapsedSeconds.toFloat() / totalTimerDuration else 0f

                                RhythmWavyProgressLoader(
                                    progress = progress,
                                    modifier = Modifier.fillMaxSize(),
                                    indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
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
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { stopTimer() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                        brush = androidx.compose.foundation.BorderStroke(
                                            1.dp, 
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        ).brush
                                    )
                                ) {
                                    Icon(RhythmIcons.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.bottomsheet_cancel))
                                }
                                
                                Button(
                                    onClick = { showTimePicker() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(RhythmIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.bottomsheet_timer_edit))
                                }
                            }
                        }
                    }
                }
            } else {
                // Quick Timer Options
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = MaterialSymbolIcon("timer", filled = true),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                                        modifier = Modifier.size(width = 85.dp, height = 90.dp),
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
                                                .padding(12.dp),
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
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
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
                }
                
                // Custom Timer
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                                onClick = { if (isCustomTimerAvailable) showTimePicker() },
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
                }
            }
            
            // Action Selection
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Play,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
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
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actionOptions.forEach { (action, label, icon) ->
                                val isSelected = selectedAction == action
                                
                                Card(
                                    onClick = { 
                                        HapticUtils.performHapticFeedback(context, haptics, HapticType.HEAVY)
                                        selectedAction = action 
                                        musicViewModel.appSettings.setSleepTimerAction(action.name)
                                    },
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
        }
    }
    
    // Material 3 TimePicker Dialog
    if (showTimePickerDialog) {
        Material3TimePickerDialog(
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { hours, minutes ->
                val totalMinutes = hours * 60 + minutes
                if (totalMinutes > 0) {
                    startTimer(totalMinutes)
                }
                showTimePickerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Material3TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (hours: Int, minutes: Int) -> Unit
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = 0,
        initialMinute = 30,
        is24Hour = true
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = context.getString(R.string.sleep_timer_select_duration),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = context.getString(R.string.bottomsheet_timer_hours_minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // TimePicker
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.bottomsheet_cancel))
                    }
                    
                    Button(
                        onClick = {
                            onTimeSelected(timePickerState.hour, timePickerState.minute)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.bottomsheet_timer_set))
                    }
                }
            }
        }
    }
}
