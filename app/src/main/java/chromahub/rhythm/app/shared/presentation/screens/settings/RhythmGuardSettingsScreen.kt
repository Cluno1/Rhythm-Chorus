@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings


import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import chromahub.rhythm.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.BuildConfig
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.data.repository.StatsTimeRange
import chromahub.rhythm.app.util.GsonUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.system.exitProcess
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveScrollBar
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.common.StyledProgressBar
import chromahub.rhythm.app.shared.presentation.components.common.ProgressStyle
import chromahub.rhythm.app.shared.presentation.components.common.ThumbStyle
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.LicensesBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.UpdateBottomSheet
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeProvider
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.common.buildSplashBackdropShapes
import chromahub.rhythm.app.shared.presentation.components.common.SplashBackgroundOrbs
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppVersion
import chromahub.rhythm.app.ui.theme.getFontPreviewStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import chromahub.rhythm.app.utils.FontLoader
import chromahub.rhythm.app.ui.theme.parseCustomColorScheme
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import chromahub.rhythm.app.shared.presentation.components.common.M3FourColorCircularLoader
import chromahub.rhythm.app.shared.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.BulkPlaylistExportDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistImportDialog
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShape
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationProgressDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.PlaylistOperationResultDialog
import chromahub.rhythm.app.shared.presentation.components.dialogs.AppRestartDialog
import chromahub.rhythm.app.shared.presentation.components.player.PlayerChipOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.settings.LibraryTabOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem

import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingRow
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerAnimatedSwitch
import chromahub.rhythm.app.shared.presentation.screens.settings.TunerSettingCard
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingItem
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingGroup


@Composable
fun RhythmGuardSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()

    val auraMode by appSettings.rhythmGuardMode.collectAsState()
    val auraAge by appSettings.rhythmGuardAge.collectAsState()
    val manualWarningsEnabled by appSettings.rhythmGuardManualWarningsEnabled.collectAsState()
    val manualVolumeThreshold by appSettings.rhythmGuardManualVolumeThreshold.collectAsState()
    val alertThresholdMinutes by appSettings.rhythmGuardAlertThresholdMinutes.collectAsState()
    val warningTimeoutMinutes by appSettings.rhythmGuardWarningTimeoutMinutes.collectAsState()
    val postTimeoutCooldownMinutes by appSettings.rhythmGuardPostTimeoutCooldownMinutes.collectAsState()
    val breakResumeMinutes by appSettings.rhythmGuardBreakResumeMinutes.collectAsState()
    val timeoutUntilMs by appSettings.rhythmGuardTimeoutUntilMs.collectAsState()
    val timeoutStartedAtMs by appSettings.rhythmGuardTimeoutStartedAtMs.collectAsState()
    val timeoutCooldownUntilMs by appSettings.rhythmGuardTimeoutCooldownUntilMs.collectAsState()

    val dailyListeningStats by appSettings.dailyListeningStats.collectAsState()

    val stopPlaybackOnZeroVolume by appSettings.stopPlaybackOnZeroVolume.collectAsState()
    val rhythmGuardApplyVolumeLimitOnSpeaker by appSettings.rhythmGuardApplyVolumeLimitOnSpeaker.collectAsState()

    val currentSystemVolume = rememberSystemMusicVolumeFraction(context)
    val playbackStatsRepository = remember(context) { PlaybackStatsRepository.getInstance(context) }

    var todayExposureMs by remember { mutableLongStateOf(0L) }
    var weeklyAverageSessions by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(dailyListeningStats) {
        val todaySummary = runCatching {
            playbackStatsRepository.loadSummary(StatsTimeRange.TODAY)
        }.getOrNull()
        val weekSummary = runCatching {
            playbackStatsRepository.loadSummary(StatsTimeRange.WEEK)
        }.getOrNull()

        todayExposureMs = todaySummary?.totalDurationMs ?: 0L
        weeklyAverageSessions = weekSummary?.averageSessionsPerDay ?: 0f
    }

    val nowEpochMs = System.currentTimeMillis()
    val isRhythmGuardEnabled = auraMode != AppSettings.RHYTHM_GUARD_MODE_OFF
    val todayExposureMinutes = (todayExposureMs / 60000L).toInt().coerceAtLeast(0)

    val activePolicy = remember(auraAge) { appSettings.getRhythmGuardPolicy(auraAge) }
    val policyTable = remember { appSettings.getRhythmGuardPolicyBands() }
    val recommendedVolumeThreshold = activePolicy.maxVolumeThreshold
    val recommendedDailyMinutes = activePolicy.recommendedDailyMinutes

    val effectiveExposureLimitMinutes = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedDailyMinutes
    } else if (alertThresholdMinutes > 0) {
        alertThresholdMinutes
    } else {
        recommendedDailyMinutes
    }
    val totalExposureMinutes = (todayExposureMs / 60000L).toInt().coerceAtLeast(0)

    val currentVolumePercent = (currentSystemVolume * 100f).toInt().coerceIn(0, 100)
    val manualThresholdPercent = (manualVolumeThreshold * 100f).toInt().coerceIn(0, 100)
    val recommendedThresholdPercent = (recommendedVolumeThreshold * 100f).toInt().coerceIn(0, 100)

    val formattedTotalExposure = remember(todayExposureMs, useHoursFormat) {
        rhythmGuardFormatDurationFromMillis(todayExposureMs, useHoursFormat)
    }
    val formattedDailyTarget = remember(effectiveExposureLimitMinutes, useHoursFormat) {
        rhythmGuardFormatDurationFromMinutes(effectiveExposureLimitMinutes, useHoursFormat)
    }
    val formattedTimeout = remember(warningTimeoutMinutes, useHoursFormat) {
        rhythmGuardFormatDurationFromMinutes(warningTimeoutMinutes, useHoursFormat)
    }
    val formattedPostTimeoutCooldown = remember(postTimeoutCooldownMinutes, useHoursFormat) {
        rhythmGuardFormatDurationFromMinutes(postTimeoutCooldownMinutes, useHoursFormat)
    }
    val formattedResumeInterval = remember(breakResumeMinutes, useHoursFormat) {
        rhythmGuardFormatDurationFromMinutes(breakResumeMinutes, useHoursFormat)
    }
    val activeVolumeThreshold = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedVolumeThreshold
    } else {
        manualVolumeThreshold
    }
    val activeThresholdPercent = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
        recommendedThresholdPercent
    } else {
        manualThresholdPercent
    }
    val isTimeoutActive = isRhythmGuardEnabled && timeoutUntilMs > nowEpochMs
    val isCooldownActive = isRhythmGuardEnabled && !isTimeoutActive && timeoutCooldownUntilMs > nowEpochMs
    val timeoutRemainingSeconds = ((timeoutUntilMs - nowEpochMs) / 1000L).coerceAtLeast(0L)
    val cooldownRemainingSeconds = ((timeoutCooldownUntilMs - nowEpochMs) / 1000L).coerceAtLeast(0L)

    val timeoutStartFallbackMs = timeoutUntilMs - breakResumeMinutes.coerceIn(1, 180).toLong() * 60_000L
    val resolvedTimeoutStartMs = timeoutStartedAtMs
        .takeIf { it > 0L && it < timeoutUntilMs }
        ?: timeoutStartFallbackMs
    val timeoutTotalMs = (timeoutUntilMs - resolvedTimeoutStartMs).coerceAtLeast(1_000L)
    val timeoutElapsedMs = (timeoutTotalMs - (timeoutUntilMs - nowEpochMs).coerceAtLeast(0L))
        .coerceIn(0L, timeoutTotalMs)
    val timeoutProgress = (timeoutElapsedMs.toFloat() / timeoutTotalMs.toFloat()).coerceIn(0f, 1f)

    val cooldownTotalMs = postTimeoutCooldownMinutes.coerceIn(1, 60).toLong() * 60_000L
    val cooldownElapsedMs = (cooldownTotalMs - (timeoutCooldownUntilMs - nowEpochMs).coerceAtLeast(0L))
        .coerceIn(0L, cooldownTotalMs)
    val cooldownProgress = (cooldownElapsedMs.toFloat() / cooldownTotalMs.toFloat()).coerceIn(0f, 1f)

    val showVolumeWarning = isRhythmGuardEnabled &&
        auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL &&
        manualWarningsEnabled &&
        currentSystemVolume > manualVolumeThreshold
    val showExposureWarning = isRhythmGuardEnabled &&
        auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL &&
        manualWarningsEnabled &&
        totalExposureMinutes > effectiveExposureLimitMinutes
    val safetySnapshot = remember(
        isRhythmGuardEnabled,
        auraMode,
        manualWarningsEnabled,
        currentSystemVolume,
        activeVolumeThreshold,
        totalExposureMinutes,
        effectiveExposureLimitMinutes,
        weeklyAverageSessions,
        isTimeoutActive,
        isCooldownActive,
        cooldownProgress
    ) {
        rhythmGuardCalculateSafetySnapshot(
            isEnabled = isRhythmGuardEnabled,
            isManualMode = auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL,
            manualWarningsEnabled = manualWarningsEnabled,
            currentVolumeFraction = currentSystemVolume,
            safeVolumeThresholdFraction = activeVolumeThreshold,
            exposureMinutes = totalExposureMinutes,
            exposureLimitMinutes = effectiveExposureLimitMinutes,
            weeklyAverageSessions = weeklyAverageSessions,
            timeoutActive = isTimeoutActive,
            cooldownActive = isCooldownActive,
            cooldownProgress = cooldownProgress
        )
    }
    val healthRiskScore = safetySnapshot.riskScore
    val safetyScorePercent = (safetySnapshot.safetyProgress * 100f).toInt().coerceIn(0, 100)
    val overallHealthLevel = when {
        !isRhythmGuardEnabled -> RhythmGuardOverallHealthLevel.OFF
        isTimeoutActive -> RhythmGuardOverallHealthLevel.TIMEOUT
        isCooldownActive -> RhythmGuardOverallHealthLevel.COOLDOWN
        healthRiskScore < 0.40f -> RhythmGuardOverallHealthLevel.GOOD
        healthRiskScore < 0.72f -> RhythmGuardOverallHealthLevel.FAIR
        else -> RhythmGuardOverallHealthLevel.RISK
    }
    val overallHealthProgress = when {
        !isRhythmGuardEnabled -> 0f
        isTimeoutActive -> timeoutProgress
        isCooldownActive -> cooldownProgress
        else -> safetySnapshot.safetyProgress
    }
    val guardStatusText = when {
        !isRhythmGuardEnabled -> context.getString(R.string.settings_rhythm_guard_state_inactive)
        isTimeoutActive -> context.getString(R.string.settings_rhythm_guard_state_timeout_active)
        isCooldownActive -> context.getString(R.string.settings_rhythm_guard_state_cooldown_active)
        else -> context.getString(R.string.settings_rhythm_guard_state_active)
    }
    val guardStatusDetail = when {
        isTimeoutActive -> context.getString(
            R.string.settings_rhythm_guard_state_timeout_remaining,
            rhythmGuardFormatCountdownFromSeconds(timeoutRemainingSeconds, useHoursFormat)
        )
        isCooldownActive -> context.getString(
            R.string.settings_rhythm_guard_state_cooldown_remaining,
            rhythmGuardFormatCountdownFromSeconds(cooldownRemainingSeconds, useHoursFormat)
        )
        isRhythmGuardEnabled -> context.getString(
            R.string.settings_rhythm_guard_state_safety_score,
            safetyScorePercent
        )
        else -> null
    }
    val guardStatusAccentColor = when {
        isCooldownActive -> Color(0xFF1565C0)
        isTimeoutActive -> MaterialTheme.colorScheme.error
        isRhythmGuardEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val activeManualPreset = remember(
        manualVolumeThreshold,
        alertThresholdMinutes,
        warningTimeoutMinutes,
        postTimeoutCooldownMinutes,
        breakResumeMinutes,
        manualWarningsEnabled
    ) {
        rhythmGuardResolveProtectionPreset(
            manualVolumeThreshold = manualVolumeThreshold,
            alertThresholdMinutes = alertThresholdMinutes,
            warningTimeoutMinutes = warningTimeoutMinutes,
            postTimeoutCooldownMinutes = postTimeoutCooldownMinutes,
            breakResumeMinutes = breakResumeMinutes,
            manualWarningsEnabled = manualWarningsEnabled
        )
    }
    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_rhythm_guard),
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRhythmGuardEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Security,
                            contentDescription = null,
                            tint = if (isRhythmGuardEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(35.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRhythmGuardEnabled) "Active" else "Disabled",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRhythmGuardEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TunerAnimatedSwitch(
                            checked = isRhythmGuardEnabled,
                            onCheckedChange = { enabled ->
                                HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                if (enabled) {
                                    val restoredMode = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                                        AppSettings.RHYTHM_GUARD_MODE_MANUAL
                                    } else {
                                        AppSettings.RHYTHM_GUARD_MODE_AUTO
                                    }
                                    appSettings.setRhythmGuardMode(restoredMode)
                                } else {
                                    appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_OFF)
                                }
                            }
                        )
                    }

                    // Smoothly expand the mode selector when active
                    AnimatedVisibility(
                        visible = isRhythmGuardEnabled,
                        enter = fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut() + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            ExpressiveButtonGroup(
                                items = listOf(
                                    context.getString(R.string.settings_rhythm_guard_mode_auto),
                                    context.getString(R.string.settings_rhythm_guard_mode_manual)
                                ),
                                selectedIndex = if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) 1 else 0,
                                onItemClick = { index ->
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    when (index) {
                                        0 -> appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_AUTO)
                                        else -> appSettings.setRhythmGuardMode(AppSettings.RHYTHM_GUARD_MODE_MANUAL)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                RhythmGuardExpressiveDashboard(
                    level = overallHealthLevel,
                    overallProgress = overallHealthProgress,
                    statusText = guardStatusText,
                    statusDetail = guardStatusDetail,
                    // Exposure bindings
                    exposureValue = formattedTotalExposure,
                    exposureSubtitle = formattedDailyTarget,
                    exposureProgress = (totalExposureMinutes / maxOf(effectiveExposureLimitMinutes, 1).toFloat()).coerceIn(0f, 1f),
                    exposureWarning = showExposureWarning,
                    // Volume bindings
                    volumeValue = "$currentVolumePercent%",
                    volumeSubtitle = "${activeThresholdPercent}%",
                    volumeProgress = (currentSystemVolume / maxOf(activeVolumeThreshold, 0.01f)).coerceIn(0f, 1f),
                    volumeWarning = showVolumeWarning,

                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isRhythmGuardEnabled) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_age_label,
                                            auraAge
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_age_desc,
                                            recommendedThresholdPercent,
                                            recommendedDailyMinutes
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Slider(
                                value = auraAge.toFloat(),
                                onValueChange = { appSettings.setRhythmGuardAge(it.toInt()) },
                                valueRange = 8f..80f,
                                steps = 71
                            )
                        }
                    }
                }

                item {
                    val outputSettingItems = listOf(
                        SettingItem(
                            RhythmIcons.Speaker,
                            context.getString(R.string.settings_rhythm_guard_device_controls_speaker_limit_title),
                            context.getString(R.string.settings_rhythm_guard_device_controls_speaker_limit_desc),
                            toggleState = rhythmGuardApplyVolumeLimitOnSpeaker,
                            onToggleChange = {
                                appSettings.setRhythmGuardApplyVolumeLimitOnSpeaker(
                                    it
                                )
                            }
                        )
                    )

                    val materialItems = outputSettingItems.map { item ->
                        toMaterial3SettingsItem(
                            context = context,
                            item = item,
                            hapticFeedback = haptic
                        )
                    }

                    Material3SettingsGroup(
                        title = context.getString(R.string.settings_rhythm_guard_device_controls_title),
                        items = materialItems,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }

                if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_alert_controls_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Threshold control
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_alert_threshold_title,
                                            if (alertThresholdMinutes > 0) {
                                                rhythmGuardFormatDurationFromMinutes(
                                                    alertThresholdMinutes,
                                                    useHoursFormat
                                                )
                                            } else {
                                                context.getString(R.string.settings_rhythm_guard_alert_threshold_policy_default)
                                            }
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(-1, 60, 90, 120).forEach { option ->
                                            val isSelected = alertThresholdMinutes == option
                                            val cornerRadius by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 12.dp,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "alertThresholdCornerRadius"
                                            )
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    appSettings.setRhythmGuardAlertThresholdMinutes(
                                                        option
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = RhythmIcons.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(cornerRadius),
                                                label = {
                                                    Text(
                                                        if (option > 0) {
                                                            rhythmGuardFormatDurationFromMinutes(
                                                                option,
                                                                useHoursFormat
                                                            )
                                                        } else {
                                                            context.getString(R.string.settings_rhythm_guard_alert_threshold_policy_default)
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Slider(
                                        value = maxOf(alertThresholdMinutes, 15).toFloat(),
                                        onValueChange = {
                                            appSettings.setRhythmGuardAlertThresholdMinutes(
                                                it.toInt()
                                            )
                                        },
                                        valueRange = 15f..360f,
                                        steps = 344
                                    )
                                }

                                // Timeout control
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_alert_timeout_title,
                                            formattedTimeout
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(2, 5, 10, 15).forEach { option ->
                                            val isSelected = warningTimeoutMinutes == option
                                            val cornerRadius by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 12.dp,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "warningTimeoutCornerRadius"
                                            )
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    appSettings.setRhythmGuardWarningTimeoutMinutes(
                                                        option
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = RhythmIcons.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(cornerRadius),
                                                label = {
                                                    Text(
                                                        rhythmGuardFormatDurationFromMinutes(
                                                            option,
                                                            useHoursFormat
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Slider(
                                        value = warningTimeoutMinutes.toFloat(),
                                        onValueChange = {
                                            appSettings.setRhythmGuardWarningTimeoutMinutes(
                                                it.toInt()
                                            )
                                        },
                                        valueRange = 1f..30f,
                                        steps = 28
                                    )
                                }

                                // Post-timeout cooldown control
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_post_timeout_cooldown_title,
                                            formattedPostTimeoutCooldown
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(3, 5, 10, 15).forEach { option ->
                                            val isSelected = postTimeoutCooldownMinutes == option
                                            val cornerRadius by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 12.dp,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "postTimeoutCooldownCornerRadius"
                                            )
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    appSettings.setRhythmGuardPostTimeoutCooldownMinutes(
                                                        option
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = RhythmIcons.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(cornerRadius),
                                                label = {
                                                    Text(
                                                        rhythmGuardFormatDurationFromMinutes(
                                                            option,
                                                            useHoursFormat
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Slider(
                                        value = postTimeoutCooldownMinutes.toFloat(),
                                        onValueChange = {
                                            appSettings.setRhythmGuardPostTimeoutCooldownMinutes(
                                                it.toInt()
                                            )
                                        },
                                        valueRange = 1f..30f,
                                        steps = 28
                                    )
                                }

                                // Break interval control
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_break_resume_default_title,
                                            formattedResumeInterval
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(10, 15, 30, 60).forEach { option ->
                                            val isSelected = breakResumeMinutes == option
                                            val cornerRadius by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 12.dp,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "breakResumeCornerRadius"
                                            )
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    appSettings.setRhythmGuardBreakResumeMinutes(
                                                        option
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = RhythmIcons.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(cornerRadius),
                                                label = {
                                                    Text(
                                                        rhythmGuardFormatDurationFromMinutes(
                                                            option,
                                                            useHoursFormat
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    Slider(
                                        value = breakResumeMinutes.toFloat(),
                                        onValueChange = {
                                            appSettings.setRhythmGuardBreakResumeMinutes(
                                                it.toInt()
                                            )
                                        },
                                        valueRange = 1f..120f,
                                        steps = 118
                                    )
                                }

                                // Manual protection presets (quick multi-setting tunes)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = context.getString(R.string.settings_rhythm_guard_protection_presets_title),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = context.getString(R.string.settings_rhythm_guard_protection_presets_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            RhythmGuardProtectionPreset.GENTLE,
                                            RhythmGuardProtectionPreset.BALANCED,
                                            RhythmGuardProtectionPreset.STRICT
                                        ).forEach { preset ->
                                            val isSelected = activeManualPreset == preset
                                            val cornerRadius by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 12.dp,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "presetCornerRadius"
                                            )
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val values = rhythmGuardPresetValues(preset)
                                                    appSettings.setRhythmGuardManualWarningsEnabled(
                                                        true
                                                    )
                                                    appSettings.setRhythmGuardManualVolumeThreshold(
                                                        values.volumeThreshold
                                                    )
                                                    appSettings.setRhythmGuardAlertThresholdMinutes(
                                                        values.alertThresholdMinutes
                                                    )
                                                    appSettings.setRhythmGuardWarningTimeoutMinutes(
                                                        values.warningTimeoutMinutes
                                                    )
                                                    appSettings.setRhythmGuardPostTimeoutCooldownMinutes(
                                                        values.postTimeoutCooldownMinutes
                                                    )
                                                    appSettings.setRhythmGuardBreakResumeMinutes(
                                                        values.breakResumeMinutes
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = RhythmIcons.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(cornerRadius),
                                                label = {
                                                    Text(
                                                        text = when (preset) {
                                                            RhythmGuardProtectionPreset.GENTLE -> context.getString(
                                                                R.string.settings_rhythm_guard_protection_preset_gentle
                                                            )
 
                                                            RhythmGuardProtectionPreset.BALANCED -> context.getString(
                                                                R.string.settings_rhythm_guard_protection_preset_balanced
                                                            )
 
                                                            RhythmGuardProtectionPreset.STRICT -> context.getString(
                                                                R.string.settings_rhythm_guard_protection_preset_strict
                                                            )
 
                                                            RhythmGuardProtectionPreset.CUSTOM -> context.getString(
                                                                R.string.settings_rhythm_guard_protection_preset_custom
                                                            )
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (auraMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_auto_policy_table_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                policyTable.forEachIndexed { index, band ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(
                                                R.string.settings_rhythm_guard_auto_policy_band,
                                                band.minAge,
                                                band.maxAge
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = context.getString(
                                                R.string.settings_rhythm_guard_auto_policy_value,
                                                (band.maxVolumeThreshold * 100f).toInt(),
                                                band.recommendedDailyMinutes
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (auraAge in band.minAge..band.maxAge) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = if (auraAge in band.minAge..band.maxAge) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    }
                                    if (index < policyTable.lastIndex) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.08f
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (auraMode == AppSettings.RHYTHM_GUARD_MODE_MANUAL) {
                    item {
                        val manualSettingItems = listOf(
                            SettingItem(
                                RhythmIcons.Warning,
                                context.getString(R.string.settings_rhythm_guard_manual_warning_toggle),
                                context.getString(R.string.settings_rhythm_guard_manual_warning_toggle_desc),
                                toggleState = manualWarningsEnabled,
                                onToggleChange = {
                                    appSettings.setRhythmGuardManualWarningsEnabled(
                                        it
                                    )
                                }
                            ),
                            SettingItem(
                                RhythmIcons.Stop,
                                context.getString(R.string.settings_stop_playback_on_zero_volume),
                                context.getString(R.string.settings_stop_playback_on_zero_volume_desc),
                                toggleState = stopPlaybackOnZeroVolume,
                                onToggleChange = { appSettings.setStopPlaybackOnZeroVolume(it) }
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = context.getString(
                                            R.string.settings_rhythm_guard_manual_threshold_title,
                                            manualThresholdPercent
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = context.getString(R.string.settings_rhythm_guard_manual_threshold_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Slider(
                                        value = manualVolumeThreshold,
                                        onValueChange = {
                                            appSettings.setRhythmGuardManualVolumeThreshold(
                                                it
                                            )
                                        },
                                        valueRange = 0.40f..0.95f
                                    )
                                }
                            }

                            val materialItems = manualSettingItems.map { item ->
                                toMaterial3SettingsItem(
                                    context = context,
                                    item = item,
                                    hapticFeedback = haptic
                                )
                            }

                            Material3SettingsGroup(
                                title = context.getString(R.string.settings_rhythm_guard_manual_controls_title),
                                items = materialItems,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    }
                }

                if (showVolumeWarning || showExposureWarning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = context.getString(R.string.settings_rhythm_guard_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when {
                                        showExposureWarning -> context.getString(
                                            R.string.settings_rhythm_guard_warning_daily_exposure,
                                            formattedTotalExposure,
                                            formattedDailyTarget
                                        )

                                        else -> context.getString(
                                            R.string.settings_rhythm_guard_warning_high_volume,
                                            activeThresholdPercent
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}



fun rhythmGuardWeeklyAverageSessions(stats: Map<String, Long>): Float {
    if (stats.isEmpty()) return 0f
    val recentDays = stats.toList()
        .sortedByDescending { it.first }
        .take(7)
        .map { it.second }

    if (recentDays.isEmpty()) return 0f
    return recentDays.average().toFloat()
}



internal data class RhythmGuardSafetySnapshot(
    val riskScore: Float,
    val safetyProgress: Float
)

internal fun rhythmGuardCalculateSafetySnapshot(
    isEnabled: Boolean,
    isManualMode: Boolean,
    manualWarningsEnabled: Boolean,
    currentVolumeFraction: Float,
    safeVolumeThresholdFraction: Float,
    exposureMinutes: Int,
    exposureLimitMinutes: Int,
    weeklyAverageSessions: Float,
    timeoutActive: Boolean,
    cooldownActive: Boolean,
    cooldownProgress: Float
): RhythmGuardSafetySnapshot {
    if (!isEnabled) {
        return RhythmGuardSafetySnapshot(riskScore = 0f, safetyProgress = 0f)
    }

    if (timeoutActive) {
        return RhythmGuardSafetySnapshot(riskScore = 1f, safetyProgress = 0f)
    }

    val safeVolumeThreshold = rhythmGuardSanitizeFloat(safeVolumeThresholdFraction, fallback = 0.01f)
        .coerceIn(0.01f, 1f)
    val safeCurrentVolume = rhythmGuardSanitizeFloat(currentVolumeFraction)
    val safeExposureMinutes = exposureMinutes.coerceAtLeast(0)
    val safeExposureLimit = exposureLimitMinutes.coerceAtLeast(1)
    val safeWeeklySessions = rhythmGuardSanitizeFloat(weeklyAverageSessions)

    val volumeRatio = safeCurrentVolume / safeVolumeThreshold
    val exposureRatio = safeExposureMinutes.toFloat() / safeExposureLimit.toFloat()
    val sessionRatio = safeWeeklySessions / 8f

    val volumePressure = rhythmGuardNormalizePressure(volumeRatio)
    val exposurePressure = rhythmGuardNormalizePressure(exposureRatio)
    val sessionPressure = rhythmGuardNormalizePressure(sessionRatio)

    var riskScore = (
        (volumePressure * 0.40f) +
            (exposurePressure * 0.42f) +
            (sessionPressure * 0.18f)
        ).coerceIn(0f, 1f)

    if (isManualMode && !manualWarningsEnabled) {
        riskScore = (riskScore + 0.07f).coerceIn(0f, 1f)
    }

    if (cooldownActive) {
        val recoveryProgress = rhythmGuardSanitizeFloat(cooldownProgress).coerceIn(0f, 1f)
        val decay = 0.82f - (recoveryProgress * 0.30f)
        riskScore = (riskScore * decay).coerceIn(0.18f, 0.70f)
    }

    val safetyProgress = (1f - riskScore).coerceIn(0f, 1f)
    return RhythmGuardSafetySnapshot(riskScore = riskScore, safetyProgress = safetyProgress)
}



fun rhythmGuardSanitizeFloat(value: Float, fallback: Float = 0f): Float {
    return if (value.isFinite()) value.coerceAtLeast(0f) else fallback
}



fun rhythmGuardNormalizePressure(ratio: Float): Float {
    val safeRatio = rhythmGuardSanitizeFloat(ratio)
    return when {
        safeRatio <= 0f -> 0f
        safeRatio <= 0.6f -> (safeRatio / 0.6f) * 0.32f
        safeRatio <= 1f -> 0.32f + ((safeRatio - 0.6f) / 0.4f) * 0.24f
        safeRatio <= 1.5f -> 0.56f + ((safeRatio - 1f) / 0.5f) * 0.29f
        safeRatio <= 2f -> 0.85f + ((safeRatio - 1.5f) / 0.5f) * 0.10f
        else -> 0.95f + ((safeRatio - 2f) / 2f) * 0.05f
    }.coerceIn(0f, 1f)
}



@Composable
fun rememberSystemMusicVolumeFraction(context: Context): Float {
    var systemVolume by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

        fun refreshVolume() {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            systemVolume = if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f
        }

        refreshVolume()

        val observer = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper())
        ) {
            override fun onChange(selfChange: Boolean) {
                refreshVolume()
            }
        }

        context.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            observer
        )

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    return systemVolume
}



fun rhythmGuardFormatDurationFromMinutes(minutes: Int, useHoursFormat: Boolean = true): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    if (!useHoursFormat) {
        return "${safeMinutes}m"
    }
    val days = safeMinutes / (24 * 60)
    val hours = (safeMinutes % (24 * 60)) / 60
    val mins = safeMinutes % 60

    return when {
        days > 0 && hours > 0 && mins > 0 -> "${days}d ${hours}h ${mins}m"
        days > 0 && hours > 0 -> "${days}d ${hours}h"
        days > 0 && mins > 0 -> "${days}d ${mins}m"
        days > 0 -> "${days}d"
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}



fun rhythmGuardFormatDurationFromMillis(durationMs: Long, useHoursFormat: Boolean = true): String {
    return rhythmGuardFormatDurationFromMinutes((durationMs / 60000L).toInt(), useHoursFormat)
}



fun rhythmGuardFormatCountdownFromSeconds(seconds: Long, useHoursFormat: Boolean = true): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val totalMinutes = safeSeconds / 60L
    val secs = safeSeconds % 60L

    return if (useHoursFormat && totalMinutes >= 60L) {
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", totalMinutes, secs)
    }
}



@Composable
fun RhythmGuardOverviewGauge(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    progress: Float,
    isWarning: Boolean,
    icon: MaterialSymbolIcon
) {
    val progressValue = progress.coerceIn(0f, 1f)
    val progressPercent = (progressValue * 100f).toInt()
    val containerColor = if (isWarning) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isWarning) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = contentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isWarning) "Risk" else "Safe",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            StyledProgressBar(
                progress = progressValue,
                style = ProgressStyle.WAVY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progressColor = if (isWarning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.16f),
                isPlaying = true,
                showThumb = false,
                waveAmplitudeWhenPlaying = 2.5.dp,
                waveLength = 80.dp,
                height = 4.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isWarning) {
                        stringResource(R.string.settings_rhythm_guard_snapshot_widget_above_limit)
                    } else {
                        stringResource(R.string.settings_rhythm_guard_snapshot_widget_within_limit)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
        }
    }
}



@Composable
fun RhythmGuardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    progress: Float,
    containerColor: Color,
    contentColor: Color
) {
    val progressValue = progress.coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${(progressValue * 100f).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            StyledProgressBar(
                progress = progressValue,
                style = ProgressStyle.WAVY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progressColor = contentColor.copy(alpha = 0.78f),
                trackColor = contentColor.copy(alpha = 0.2f),
                isPlaying = true,
                showThumb = false,
                waveAmplitudeWhenPlaying = 2.5.dp,
                waveLength = 80.dp,
                height = 4.dp
            )

            Text(
                text = stringResource(R.string.settings_rhythm_guard_snapshot_widget_load_label),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
fun RhythmGuardHeroCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    icon: MaterialSymbolIcon,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    val progressValue = progress.coerceIn(0f, 1.5f)
    val visualLevel = when {
        isWarning || progressValue >= 1f -> RhythmGuardWidgetVisualLevel.CRITICAL
        progressValue >= 0.82f -> RhythmGuardWidgetVisualLevel.ELEVATED
        progressValue >= 0.56f -> RhythmGuardWidgetVisualLevel.WATCH
        else -> RhythmGuardWidgetVisualLevel.STABLE
    }
    val iconColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.primary
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.tertiary
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.secondary
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.error
    }
    val containerColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.primaryContainer
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.tertiaryContainer
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.secondaryContainer
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (visualLevel) {
        RhythmGuardWidgetVisualLevel.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        RhythmGuardWidgetVisualLevel.ELEVATED -> MaterialTheme.colorScheme.onSecondaryContainer
        RhythmGuardWidgetVisualLevel.WATCH -> MaterialTheme.colorScheme.onTertiaryContainer
        RhythmGuardWidgetVisualLevel.STABLE -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val dynamicValueFontSize = rememberRhythmGuardHeroValueFontSize(value)

    Column(
        modifier = modifier
            .height(184.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = dynamicValueFontSize,
                lineHeight = (dynamicValueFontSize.value + 3f).sp
            ),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        StyledProgressBar(
            progress = progress.coerceIn(0f, 1f),
            style = ProgressStyle.WAVY,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            progressColor = iconColor,
            trackColor = contentColor.copy(alpha = 0.2f),
            isPlaying = true,
            showThumb = false,
            waveAmplitudeWhenPlaying = 2.5.dp,
            waveLength = 80.dp,
            height = 3.dp
        )
    }
}



@Composable
fun rememberRhythmGuardHeroValueFontSize(value: String): TextUnit {
    val targetSize = when {
        value.length <= 5 -> 34.sp
        value.length <= 8 -> 30.sp
        value.length <= 11 -> 26.sp
        value.length <= 15 -> 22.sp
        else -> 20.sp
    }
    val animatedSize by animateFloatAsState(
        targetValue = targetSize.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rhythm_guard_hero_value_font_size"
    )
    return animatedSize.sp
}



enum class RhythmGuardOverallHealthLevel {
    OFF,
    GOOD,
    FAIR,
    RISK,
    COOLDOWN,
    TIMEOUT
}



enum class RhythmGuardProtectionPreset {
    GENTLE,
    BALANCED,
    STRICT,
    CUSTOM
}



data class RhythmGuardProtectionPresetValues(
    val volumeThreshold: Float,
    val alertThresholdMinutes: Int,
    val warningTimeoutMinutes: Int,
    val postTimeoutCooldownMinutes: Int,
    val breakResumeMinutes: Int
)

fun rhythmGuardPresetValues(preset: RhythmGuardProtectionPreset): RhythmGuardProtectionPresetValues {
    return when (preset) {
        RhythmGuardProtectionPreset.GENTLE -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.80f,
            alertThresholdMinutes = 120,
            warningTimeoutMinutes = 10,
            postTimeoutCooldownMinutes = 5,
            breakResumeMinutes = 10
        )
        RhythmGuardProtectionPreset.BALANCED -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.68f,
            alertThresholdMinutes = 90,
            warningTimeoutMinutes = 5,
            postTimeoutCooldownMinutes = 10,
            breakResumeMinutes = 15
        )
        RhythmGuardProtectionPreset.STRICT -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.58f,
            alertThresholdMinutes = 60,
            warningTimeoutMinutes = 3,
            postTimeoutCooldownMinutes = 15,
            breakResumeMinutes = 20
        )
        RhythmGuardProtectionPreset.CUSTOM -> RhythmGuardProtectionPresetValues(
            volumeThreshold = 0.68f,
            alertThresholdMinutes = 90,
            warningTimeoutMinutes = 5,
            postTimeoutCooldownMinutes = 10,
            breakResumeMinutes = 15
        )
    }
}



fun rhythmGuardResolveProtectionPreset(
    manualVolumeThreshold: Float,
    alertThresholdMinutes: Int,
    warningTimeoutMinutes: Int,
    postTimeoutCooldownMinutes: Int,
    breakResumeMinutes: Int,
    manualWarningsEnabled: Boolean
): RhythmGuardProtectionPreset {
    if (!manualWarningsEnabled) {
        return RhythmGuardProtectionPreset.CUSTOM
    }

    return when {
        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.GENTLE)
        ) -> RhythmGuardProtectionPreset.GENTLE

        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.BALANCED)
        ) -> RhythmGuardProtectionPreset.BALANCED

        rhythmGuardMatchesPreset(
            manualVolumeThreshold,
            alertThresholdMinutes,
            warningTimeoutMinutes,
            postTimeoutCooldownMinutes,
            breakResumeMinutes,
            rhythmGuardPresetValues(RhythmGuardProtectionPreset.STRICT)
        ) -> RhythmGuardProtectionPreset.STRICT

        else -> RhythmGuardProtectionPreset.CUSTOM
    }
}



fun rhythmGuardMatchesPreset(
    manualVolumeThreshold: Float,
    alertThresholdMinutes: Int,
    warningTimeoutMinutes: Int,
    postTimeoutCooldownMinutes: Int,
    breakResumeMinutes: Int,
    preset: RhythmGuardProtectionPresetValues
): Boolean {
    return kotlin.math.abs(manualVolumeThreshold - preset.volumeThreshold) <= 0.015f &&
        alertThresholdMinutes == preset.alertThresholdMinutes &&
        warningTimeoutMinutes == preset.warningTimeoutMinutes &&
        postTimeoutCooldownMinutes == preset.postTimeoutCooldownMinutes &&
        breakResumeMinutes == preset.breakResumeMinutes
}



enum class RhythmGuardWidgetVisualLevel {
    STABLE,
    WATCH,
    ELEVATED,
    CRITICAL
}


@Composable
fun RhythmGuardOverallHealthCard(
    level: RhythmGuardOverallHealthLevel,
    progress: Float,
    statusText: String,
    statusDetail: String?,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val statusGood = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val statusFair = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val statusCooldown = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    val contentColor = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> statusGood
        RhythmGuardOverallHealthLevel.FAIR -> statusFair
        RhythmGuardOverallHealthLevel.RISK, RhythmGuardOverallHealthLevel.TIMEOUT -> MaterialTheme.colorScheme.error
        RhythmGuardOverallHealthLevel.COOLDOWN -> statusCooldown
        RhythmGuardOverallHealthLevel.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Dynamic Comic Content
    val headerText = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> "All Good"
        RhythmGuardOverallHealthLevel.FAIR -> "Careful"
        RhythmGuardOverallHealthLevel.RISK -> "Too Loud"
        RhythmGuardOverallHealthLevel.COOLDOWN -> "Grace Period"
        RhythmGuardOverallHealthLevel.TIMEOUT -> "Locked"
        RhythmGuardOverallHealthLevel.OFF -> "Disabled"
    }

    val comicSubtitle = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> "Your ears are throwing a perfectly-volumed party. Keep vibing responsibly! 🎉"
        RhythmGuardOverallHealthLevel.FAIR -> "Getting a little spicy in there. Maybe turn it down a notch before the guard steps in? 🌶️"
        RhythmGuardOverallHealthLevel.RISK -> "Whoa rockstar, you are in the eardrum danger zone! Dial it back before the locks trigger! 🎸🔥"
        RhythmGuardOverallHealthLevel.COOLDOWN -> "Safe zone active! Rhythm Guard is off-duty on coffee break, so jam freely without timeouts! 🎫☕"
        RhythmGuardOverallHealthLevel.TIMEOUT -> "Acoustic time-out active. A mandatory ear nap is in progress to protect your hearing! 😴🛑"
        RhythmGuardOverallHealthLevel.OFF -> "Rhythm Guard is off duty. You are flying completely solo on volume safety. Godspeed! 🏍️💨"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.displayMedium,
                color = contentColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = comicSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (!statusDetail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = contentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusDetail,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

data class BackupRestoreResultState(
    val title: String,
    val message: String,
    val isError: Boolean,
    val requiresRestart: Boolean
)

@Composable
fun BackupRestoreSectionPickerBottomSheet(
    title: String,
    subtitle: String,
    confirmLabel: String,
    confirmIcon: MaterialSymbolIcon,
    sections: AppSettings.BackupRestoreSections,
    isProcessing: Boolean,
    onSectionsChange: (AppSettings.BackupRestoreSections) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AppSettings.BackupRestoreSections) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "backup_restore_picker_alpha"
    )

    LaunchedEffect(Unit) {
        delay(80)
        showContent = true
    }

    val selectedSectionCount = listOf(
        sections.includeGeneralSettings,
        sections.includeLibraryData,
        sections.includeStatsAndRhythmGuard
    ).count { it }

    ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .graphicsLayer(alpha = contentAlpha)
        ) {
            StandardBottomSheetHeader(
                title = title,
                subtitle = subtitle,
                visible = showContent,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = RhythmIcons.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.rhythmguardsettingsscreen_choose_sections),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$selectedSectionCount of 3 enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                onSectionsChange(
                                    sections.copy(
                                        includeGeneralSettings = true,
                                        includeLibraryData = true,
                                        includeStatsAndRhythmGuard = true
                                    )
                                )
                            },
                            enabled = !isProcessing,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.autoeqprofileselector_all), style = MaterialTheme.typography.labelLarge)
                        }
                        FilledTonalButton(
                            onClick = {
                                onSectionsChange(
                                    sections.copy(
                                        includeGeneralSettings = false,
                                        includeLibraryData = false,
                                        includeStatsAndRhythmGuard = false
                                    )
                                )
                            },
                            enabled = !isProcessing,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(stringResource(R.string.rhythmguardsettingsscreen_none), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val pickerItems = listOf(
                Material3SettingsItem(
                    icon = RhythmIcons.Settings,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.rhythmguardsettingsscreen_general_settings),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Core",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    description = {
                        Text("Theme, player, UI, API, and app preferences.")
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            TunerAnimatedSwitch(
                                checked = sections.includeGeneralSettings,
                                onCheckedChange = { onSectionsChange(sections.copy(includeGeneralSettings = it)) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (sections.includeGeneralSettings) "Included" else "Excluded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSectionsChange(sections.copy(includeGeneralSettings = !sections.includeGeneralSettings))
                    }
                ),
                Material3SettingsItem(
                    icon = RhythmIcons.Library,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.rhythmguardsettingsscreen_library_data),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Collection",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    description = {
                        Text("Playlists, favorites, blacklist/whitelist, pinned folders.")
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            TunerAnimatedSwitch(
                                checked = sections.includeLibraryData,
                                onCheckedChange = { onSectionsChange(sections.copy(includeLibraryData = it)) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (sections.includeLibraryData) "Included" else "Excluded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSectionsChange(sections.copy(includeLibraryData = !sections.includeLibraryData))
                    }
                ),
                Material3SettingsItem(
                    icon = MaterialSymbolIcon("auto_graph"),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.rhythmguardsettingsscreen_stats_rhythm_guard),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Insight",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    description = {
                        Text("Play counts, daily stats, genres, and Rhythm Guard configuration.")
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            TunerAnimatedSwitch(
                                checked = sections.includeStatsAndRhythmGuard,
                                onCheckedChange = { onSectionsChange(sections.copy(includeStatsAndRhythmGuard = it)) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (sections.includeStatsAndRhythmGuard) "Included" else "Excluded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSectionsChange(sections.copy(includeStatsAndRhythmGuard = !sections.includeStatsAndRhythmGuard))
                    }
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Material3SettingsGroup(
                    items = pickerItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            ExpressiveButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                style = ButtonGroupStyle.Tonal
            ) {
                ExpressiveGroupButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    isStart = true
                ) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(context.getString(R.string.ui_cancel))
                }

                ExpressiveGroupButton(
                    onClick = { onConfirm(sections) },
                    modifier = Modifier.weight(1f),
                    enabled = sections.hasAtLeastOneSectionSelected && !isProcessing,
                    isEnd = true
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = confirmIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhythmGuardExpressiveDashboard(
    level: RhythmGuardOverallHealthLevel,
    overallProgress: Float,
    statusText: String,
    statusDetail: String?,
    exposureValue: String,
    exposureSubtitle: String,
    exposureProgress: Float,
    exposureWarning: Boolean,
    volumeValue: String,
    volumeSubtitle: String,
    volumeProgress: Float,
    volumeWarning: Boolean,
    modifier: Modifier = Modifier
) {
    // Since we aren't using a background card, these colors will be applied directly 
    // to the text and elements against the screen's default surface.
    val heroColor = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> MaterialTheme.colorScheme.primary
        RhythmGuardOverallHealthLevel.FAIR -> MaterialTheme.colorScheme.tertiary
        RhythmGuardOverallHealthLevel.RISK, RhythmGuardOverallHealthLevel.TIMEOUT -> MaterialTheme.colorScheme.error
        RhythmGuardOverallHealthLevel.COOLDOWN -> MaterialTheme.colorScheme.secondary
        RhythmGuardOverallHealthLevel.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Dynamic Comic Content
    val headerText = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> "All Good"
        RhythmGuardOverallHealthLevel.FAIR -> "Careful"
        RhythmGuardOverallHealthLevel.RISK -> "Too Loud"
        RhythmGuardOverallHealthLevel.COOLDOWN -> "Grace Period"
        RhythmGuardOverallHealthLevel.TIMEOUT -> "Locked"
        RhythmGuardOverallHealthLevel.OFF -> "Disabled"
    }

    val comicSubtitle = when (level) {
        RhythmGuardOverallHealthLevel.GOOD -> "Your ears are throwing a perfectly-volumed party. Keep vibing responsibly! 🎉"
        RhythmGuardOverallHealthLevel.FAIR -> "Getting a little spicy in there. Maybe turn it down a notch before the guard steps in? 🌶️"
        RhythmGuardOverallHealthLevel.RISK -> "Whoa rockstar, you're in the eardrum danger zone! Dial it back before the locks trigger! 🎸🔥"
        RhythmGuardOverallHealthLevel.COOLDOWN -> "Safe zone active! Rhythm Guard is on coffee break, so jam freely without timeouts! 🎫☕"
        RhythmGuardOverallHealthLevel.TIMEOUT -> "Acoustic time-out active. A mandatory ear nap is in progress to protect your hearing! 😴🛑"
        RhythmGuardOverallHealthLevel.OFF -> "Rhythm Guard is off duty. You are flying completely solo on volume safety. Godspeed! 🏍️💨"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expressive API loves floating pills for status badges
        if (!statusDetail.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = heroColor.copy(alpha = 0.12f),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = statusDetail.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = heroColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Massive, unconstrained typography
        Text(
            text = headerText,
            style = MaterialTheme.typography.displayLarge,
            color = heroColor,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 52.sp,
            letterSpacing = (-1.5).sp 
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = comicSubtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Chunky Wavy Progress Indicator sitting directly on the surface
        StyledProgressBar(
            progress = overallProgress.coerceIn(0f, 1f),
            style = ProgressStyle.WAVY,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(CircleShape),
            progressColor = heroColor,
            trackColor = heroColor.copy(alpha = 0.15f),
            isPlaying = level != RhythmGuardOverallHealthLevel.OFF,
            showThumb = false,
            waveAmplitudeWhenPlaying = 5.dp,
            waveLength = 60.dp,
            height = 12.dp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Integrated Metrics Row (These retain the card/pill shape to ground the UI)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardMetricPill(
                title = stringResource(R.string.settings_rhythm_guard_snapshot_exposure_title),
                value = exposureValue,
                subtitle = "/$exposureSubtitle",
                progress = exposureProgress,
                isWarning = exposureWarning,
                icon = RhythmIcons.AccessTime,
                baseColor = heroColor,
                modifier = Modifier.weight(1f)
            )

            DashboardMetricPill(
                title = stringResource(R.string.settings_rhythm_guard_snapshot_volume_title),
                value = volumeValue,
                subtitle = "of $volumeSubtitle",
                progress = volumeProgress,
                isWarning = volumeWarning,
                icon = MaterialSymbolIcon("graphic_eq"),
                baseColor = heroColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DashboardMetricPill(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    isWarning: Boolean,
    icon: Any,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    val dynamicValueFontSize = rememberRhythmGuardHeroValueFontSize(value)

    // Using tonal containers for the metrics to make them pop against the uncarded background
    val metricContainerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val metricContentColor = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    val progressTint = if (isWarning) MaterialTheme.colorScheme.error else baseColor
    
    // Hyper-rounded pill aesthetic
    Surface(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(36.dp),
        color = metricContainerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section: Icon and Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = progressTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (icon is ImageVector) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = progressTint)
                        } else if (icon is MaterialSymbolIcon) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = progressTint)
                        }
                    }
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = metricContentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.5.sp
                )
            }

            // Middle section: Giant Values
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = dynamicValueFontSize,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = metricContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = metricContentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom section: Thick mini progress
            StyledProgressBar(
                progress = progress.coerceIn(0f, 1f),
                style = ProgressStyle.WAVY,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(8.dp)
                    .clip(CircleShape),
                progressColor = progressTint,
                trackColor = progressTint.copy(alpha = 0.2f),
                isPlaying = true,
                showThumb = false,
                waveAmplitudeWhenPlaying = 2.dp,
                waveLength = 35.dp,
                height = 5.dp
            )
        }
    }
}