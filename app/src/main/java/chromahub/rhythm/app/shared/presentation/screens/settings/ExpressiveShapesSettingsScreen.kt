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


// ============================================================================
// EXPRESSIVE SHAPES SETTINGS SCREEN
// ============================================================================

/**
 * Data class for shape option display
 */
data class ShapeOption(
    val id: String,
    val displayName: String,
    val description: String,
    val category: String
)

/**
 * Data class for shape preset display
 */
data class PresetOption(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: MaterialSymbolIcon
)

/**
 * Settings screen for configuring Material 3 Expressive Shapes
 * Allows users to customize organic shapes for different UI components
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveShapesSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current
    
    // Collect settings states
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
    val currentPreset by appSettings.expressiveShapePreset.collectAsState()
    val shapeAlbumArt by appSettings.expressiveShapeAlbumArt.collectAsState()
    val shapePlayerArt by appSettings.expressiveShapePlayerArt.collectAsState()
    val shapeSongArt by appSettings.expressiveShapeSongArt.collectAsState()
    val shapePlaylistArt by appSettings.expressiveShapePlaylistArt.collectAsState()
    val shapeArtistArt by appSettings.expressiveShapeArtistArt.collectAsState()
    val shapePlayerControls by appSettings.expressiveShapePlayerControls.collectAsState()
    val shapeMiniPlayer by appSettings.expressiveShapeMiniPlayer.collectAsState()
    
    // Dialog states
    var showPresetDialog by remember { mutableStateOf(false) }
    var showShapePickerDialog by remember { mutableStateOf<String?>(null) } // Target ID
    
    // Define presets
    val presets = remember {
        listOf(
            PresetOption("DEFAULT", "Default", "Gentle expressive shapes for all ages", RhythmIcons.RadioButtonUnchecked),
            PresetOption("FRIENDLY", "Friendly", "Warm and approachable shapes", RhythmIcons.FavoriteFilled),
            PresetOption("CHEERFUL", "Cheerful", "Bright and expressive shapes", MaterialSymbolIcon("wb_sunny")),
            PresetOption("MODERN", "Modern", "Contemporary expressive design", MaterialSymbolIcon("star")),
            PresetOption("PLAYFUL", "Playful", "Fun and expressive shapes", MaterialSymbolIcon("celebration")),
            PresetOption("ORGANIC", "Organic", "Nature-inspired shapes", MaterialSymbolIcon("park")),
            PresetOption("GEOMETRIC", "Geometric", "Clean and modern shapes", RhythmIcons.Category),
            PresetOption("RETRO", "Retro", "Pixelated nostalgic shapes", MaterialSymbolIcon("gamepad")),
            PresetOption("CUSTOM", "Custom", "Your personalized selection", RhythmIcons.Tune)
        )
    }
    
    // Define available shapes grouped by category
    val allShapes = remember {
        listOf(
            // Basic Shapes
            ShapeOption("CIRCLE", "Circle", "A perfect circle", "Basic"),
            ShapeOption("SQUARE", "Square", "Rounded square", "Basic"),
            ShapeOption("OVAL", "Oval", "Elongated oval", "Basic"),
            ShapeOption("PILL", "Pill", "Capsule shape", "Basic"),
            ShapeOption("DIAMOND", "Diamond", "Diamond shape", "Basic"),
            ShapeOption("TRIANGLE", "Triangle", "Rounded triangle", "Basic"),
            ShapeOption("PENTAGON", "Pentagon", "Five-sided polygon", "Basic"),
            // Organic Shapes
            ShapeOption("FLOWER", "Flower", "Flower with petals", "Organic"),
            ShapeOption("CLOVER_4_LEAF", "4-Leaf Clover", "Four-leaf clover", "Organic"),
            ShapeOption("CLOVER_8_LEAF", "8-Leaf Clover", "Eight-leaf clover", "Organic"),
            ShapeOption("HEART", "Heart", "Heart shape", "Organic"),
            ShapeOption("BUN", "Bun", "Bun/bread shape", "Organic"),
            // Playful Shapes
            ShapeOption("BOOM", "Boom", "Explosion shape", "Playful"),
            ShapeOption("SOFT_BOOM", "Soft Boom", "Softer explosion", "Playful"),
            ShapeOption("BURST", "Burst", "Starburst shape", "Playful"),
            ShapeOption("SOFT_BURST", "Soft Burst", "Softer starburst", "Playful"),
            ShapeOption("SUNNY", "Sunny", "Sun with rays", "Playful"),
            ShapeOption("VERY_SUNNY", "Very Sunny", "Sun with more rays", "Playful"),
            // Cookie Shapes
            ShapeOption("COOKIE_4", "Cookie 4", "4-sided cookie", "Cookie"),
            ShapeOption("COOKIE_6", "Cookie 6", "6-sided cookie", "Cookie"),
            ShapeOption("COOKIE_7", "Cookie 7", "7-sided cookie", "Cookie"),
            ShapeOption("COOKIE_9", "Cookie 9", "9-sided cookie", "Cookie"),
            ShapeOption("COOKIE_12", "Cookie 12", "12-sided cookie", "Cookie"),
            // Whimsical Shapes
            ShapeOption("GHOSTISH", "Ghostish", "Ghost-like shape", "Whimsical"),
            ShapeOption("PUFFY", "Puffy", "Cloud-like shape", "Whimsical"),
            ShapeOption("PUFFY_DIAMOND", "Puffy Diamond", "Puffy diamond", "Whimsical"),
            ShapeOption("BUN", "Bun", "Bun/bread shape", "Whimsical"),
            ShapeOption("FAN", "Fan", "Fan shape", "Whimsical"),
            ShapeOption("ARROW", "Arrow", "Arrow pointer", "Whimsical"),
            // Special Shapes
            ShapeOption("ARCH", "Arch", "Arch shape", "Special"),
            ShapeOption("CLAM_SHELL", "Clam Shell", "Shell shape", "Special"),
            ShapeOption("GEM", "Gem", "Gemstone shape", "Special"),
            ShapeOption("SEMI_CIRCLE", "Semi Circle", "Half circle", "Special"),
            ShapeOption("SLANTED", "Slanted", "Slanted square", "Special"),
            // Pixel Shapes
            ShapeOption("PIXEL_CIRCLE", "Pixel Circle", "Pixelated circle", "Pixel"),
            ShapeOption("PIXEL_TRIANGLE", "Pixel Triangle", "Pixelated triangle", "Pixel")
        )
    }

    // Migrate legacy unsupported shape IDs to currently available equivalents.
    val availableShapeIds = remember(allShapes) { allShapes.map { it.id }.toSet() }
    val legacyShapeReplacements = remember {
        mapOf(
            "STAR" to "BURST",
            "BUTTERFLY" to "FLOWER"
        )
    }

    LaunchedEffect(
        shapeAlbumArt,
        shapePlayerArt,
        shapeSongArt,
        shapePlaylistArt,
        shapeArtistArt,
        shapePlayerControls,
        shapeMiniPlayer,
        availableShapeIds
    ) {
        fun sanitizeShapeId(value: String, fallback: String): String {
            val mapped = legacyShapeReplacements[value] ?: value
            return if (mapped in availableShapeIds) mapped else fallback
        }

        val sanitizedAlbumArt = sanitizeShapeId(shapeAlbumArt, "GHOSTISH")
        if (sanitizedAlbumArt != shapeAlbumArt) appSettings.setExpressiveShapeAlbumArt(sanitizedAlbumArt)

        val sanitizedPlayerArt = sanitizeShapeId(shapePlayerArt, "BUN")
        if (sanitizedPlayerArt != shapePlayerArt) appSettings.setExpressiveShapePlayerArt(sanitizedPlayerArt)

        val sanitizedSongArt = sanitizeShapeId(shapeSongArt, "CLOVER_8_LEAF")
        if (sanitizedSongArt != shapeSongArt) appSettings.setExpressiveShapeSongArt(sanitizedSongArt)

        val sanitizedPlaylistArt = sanitizeShapeId(shapePlaylistArt, "CLOVER_4_LEAF")
        if (sanitizedPlaylistArt != shapePlaylistArt) appSettings.setExpressiveShapePlaylistArt(sanitizedPlaylistArt)

        val sanitizedArtistArt = sanitizeShapeId(shapeArtistArt, "PIXEL_CIRCLE")
        if (sanitizedArtistArt != shapeArtistArt) appSettings.setExpressiveShapeArtistArt(sanitizedArtistArt)

        val sanitizedPlayerControls = sanitizeShapeId(shapePlayerControls, "COOKIE_12")
        if (sanitizedPlayerControls != shapePlayerControls) appSettings.setExpressiveShapePlayerControls(sanitizedPlayerControls)

        val sanitizedMiniPlayer = sanitizeShapeId(shapeMiniPlayer, "COOKIE_4")
        if (sanitizedMiniPlayer != shapeMiniPlayer) appSettings.setExpressiveShapeMiniPlayer(sanitizedMiniPlayer)
    }
    
    // Define shape targets with current values
    val shapeTargets = remember(shapeAlbumArt, shapePlayerArt, shapeSongArt, shapePlaylistArt, shapeArtistArt, shapePlayerControls, shapeMiniPlayer) {
        listOf(
            Triple("ALBUM_ART", "Album Artwork" to "Shape for album artwork", shapeAlbumArt),
            Triple("PLAYER_ART", "Player Artwork" to "Shape for player screen artwork", shapePlayerArt),
            Triple("SONG_ART", "Song Artwork" to "Shape for song artwork in lists", shapeSongArt),
            Triple("PLAYLIST_ART", "Playlist Artwork" to "Shape for playlist covers", shapePlaylistArt),
            Triple("ARTIST_ART", "Artist Artwork" to "Shape for artist images", shapeArtistArt),
            Triple("PLAYER_CONTROLS", "Player Controls" to "Shape for player control buttons", shapePlayerControls),
            Triple("MINI_PLAYER", "Mini Player" to "Shape for mini player artwork", shapeMiniPlayer)
        )
    }
    
    CollapsibleHeaderScreen(
        title = context.getString(R.string.settings_expressive_shapes),
        showBackButton = true,
        onBackClick = onBackClick,
        headerContent = {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (expressiveShapesEnabled)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("interests"),
                        contentDescription = null,
                        tint = if (expressiveShapesEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(35.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (expressiveShapesEnabled) "Active" else "Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TunerAnimatedSwitch(
                        checked = expressiveShapesEnabled,
                        onCheckedChange = { enabled ->
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                            appSettings.setExpressiveShapesEnabled(enabled)
                        }
                    )
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
            // Preset Selection with animation
            item(key = "preset_section") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.theme_quick_presets),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Material3SettingsGroup(
                            items = listOf(
                                toMaterial3SettingsItem(
                                    context = context,
                                    hapticFeedback = haptic,
                                    item = SettingItem(
                                        MaterialSymbolIcon("style"),
                                        stringResource(R.string.settings_shape_preset),
                                        getLocalizedPresetName(currentPreset),
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                            showPresetDialog = true
                                        }
                                    )
                                )
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }
            
            // Preset Preview Row with animation
            item(key = "preset_preview") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(presets.filter { it.id != "CUSTOM" }) { preset ->
                            val isSelected = preset.id == currentPreset
                            Card(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    appSettings.applyExpressiveShapePreset(preset.id)
                                },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = getLocalizedPresetName(preset.id),
                                        modifier = Modifier.size(28.dp),
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = getLocalizedPresetName(preset.id),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Randomize Button
            item(key = "randomize_shapes") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                            appSettings.randomizeExpressiveShapes()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Shuffle,
                                contentDescription = stringResource(R.string.expressiveshapessettingsscreen_randomize_shapes),
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.expressiveshapessettingsscreen_randomize_all_shapes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            
            // Individual Shape Customization with animation
            item(key = "individual_shapes_header") {
                AnimatedVisibility(
                    visible = expressiveShapesEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.expressiveshapessettingsscreen_individual_shape_settings),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    Material3SettingsGroup(
                        items = shapeTargets.map { (targetId, _, currentShape) ->
                            val targetName = getLocalizedTargetName(targetId)
                            val currentShapeName = getLocalizedShapeName(currentShape)
                            toMaterial3SettingsItem(
                                context = context,
                                hapticFeedback = haptic,
                                item = SettingItem(
                                    icon = when (targetId) {
                                        "ALBUM_ART" -> RhythmIcons.Album
                                        "PLAYER_ART" -> RhythmIcons.MusicNote
                                        "SONG_ART" -> MaterialSymbolIcon("audio_file")
                                        "PLAYLIST_ART" -> RhythmIcons.Queue
                                        "ARTIST_ART" -> RhythmIcons.Artist
                                        "PLAYER_CONTROLS" -> MaterialSymbolIcon("play_circle")
                                        "MINI_PLAYER" -> RhythmIcons.MusicNote
                                        else -> RhythmIcons.Category
                                    },
                                    title = targetName,
                                    description = currentShapeName,
                                    onClick = { showShapePickerDialog = targetId }
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                    } // Column
                } // AnimatedVisibility
            }
            
            // Info/Tip Card about M3 Expressive
            item(key = "expressive_info_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.expressiveshapessettingsscreen_about_expressive_shapes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.expressiveshapessettingsscreen_material_3_expressive_introduces),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                        )
                    }
                }
            }

            // Bottom spacer
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    
    // Preset Selection Bottom Sheet
    if (showPresetDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showPresetContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            showPresetContent = true
        }

        LaunchedEffect(sheetState) {
            sheetState.expand()
        }

        ModalBottomSheet(
            onDismissRequest = { showPresetDialog = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Header with animation
                StandardBottomSheetHeader(
                    title = stringResource(R.string.expressiveshapessettingsscreen_choose_a_preset),
                    subtitle = stringResource(R.string.expressiveshapessettingsscreen_select_a_theme_for),
                    visible = showPresetContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = preset.id == currentPreset

                        // Master animation states
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "preset_scale"
                        )

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "preset_container_color"
                        )

                        Card(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                isPressed = true
                                appSettings.applyExpressiveShapePreset(preset.id)
                                showPresetDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            ),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = preset.displayName,
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = getLocalizedPresetName(preset.id),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = getLocalizedPresetDesc(preset.id),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Reset press state
                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                delay(150)
                                isPressed = false
                            }
                        }
                    }
                }
            }
        }
    }

    // Individual Shape Picker Bottom Sheet
    showShapePickerDialog?.let { targetId ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val targetName = shapeTargets.find { it.first == targetId }?.second?.first ?: targetId
        var showShapeContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            showShapeContent = true
        }

        LaunchedEffect(sheetState) {
            sheetState.expand()
        }

        val currentShapeForTarget = when (targetId) {
            "ALBUM_ART" -> shapeAlbumArt
            "PLAYER_ART" -> shapePlayerArt
            "SONG_ART" -> shapeSongArt
            "PLAYLIST_ART" -> shapePlaylistArt
            "ARTIST_ART" -> shapeArtistArt
            "PLAYER_CONTROLS" -> shapePlayerControls
            "MINI_PLAYER" -> shapeMiniPlayer
            else -> "CIRCLE"
        }

        // Group shapes by category
        val groupedShapes = allShapes.groupBy { it.category }

        ModalBottomSheet(
            onDismissRequest = { showShapePickerDialog = null },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 24.dp)
            ) {
                // Header with animation
                StandardBottomSheetHeader(
                                    title = stringResource(R.string.settings_shape_for, getLocalizedTargetName(targetId)),
                                    subtitle = stringResource(R.string.expressiveshapessettingsscreen_choose_an_expressive_shape),
                                    visible = showShapeContent
                                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shape options in a grid
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    groupedShapes.forEach { (category, shapes) ->
                        item(key = "category_$category", span = { GridItemSpan(2) }) {
                            Text(
                                text = getLocalizedShapeCategory(category),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = shapes,
                            key = { "shape_${it.id}" }
                        ) { shape ->
                            val isSelected = shape.id == currentShapeForTarget

                            // Master animation states
                            var isPressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.96f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "shape_scale"
                            )

                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "shape_container_color"
                            )

                            Card(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    isPressed = true
                                    when (targetId) {
                                        "ALBUM_ART" -> appSettings.setExpressiveShapeAlbumArt(shape.id)
                                        "PLAYER_ART" -> appSettings.setExpressiveShapePlayerArt(shape.id)
                                        "SONG_ART" -> appSettings.setExpressiveShapeSongArt(shape.id)
                                        "PLAYLIST_ART" -> appSettings.setExpressiveShapePlaylistArt(shape.id)
                                        "ARTIST_ART" -> appSettings.setExpressiveShapeArtistArt(shape.id)
                                        "PLAYER_CONTROLS" -> appSettings.setExpressiveShapePlayerControls(shape.id)
                                        "MINI_PLAYER" -> appSettings.setExpressiveShapeMiniPlayer(shape.id)
                                    }
                                    showShapePickerDialog = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = containerColor
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Shape Preview
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = rememberExpressiveShape(shape.id, CircleShape),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize())
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = getLocalizedShapeName(shape.id),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = getLocalizedShapeDesc(shape.id),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.1f
                                    )
                                }
                            }

                            // Reset press state
                            LaunchedEffect(isPressed) {
                                if (isPressed) {
                                    delay(150)
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ColorPreviewItem(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color,
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = RhythmIcons.Check,
                        contentDescription = stringResource(R.string.streaming_selected),
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun getLocalizedShapeName(id: String): String {
    val resId = when (id) {
        "CIRCLE" -> R.string.shape_option_circle
        "SQUARE" -> R.string.shape_option_square
        "OVAL" -> R.string.shape_option_oval
        "PILL" -> R.string.shape_option_pill
        "DIAMOND" -> R.string.shape_option_diamond
        "TRIANGLE" -> R.string.shape_option_triangle
        "PENTAGON" -> R.string.shape_option_pentagon
        "FLOWER" -> R.string.shape_option_flower
        "CLOVER_4_LEAF" -> R.string.shape_option_clover_4_leaf
        "CLOVER_8_LEAF" -> R.string.shape_option_clover_8_leaf
        "HEART" -> R.string.shape_option_heart
        "BUN" -> R.string.shape_option_bun
        "BOOM" -> R.string.shape_option_boom
        "SOFT_BOOM" -> R.string.shape_option_soft_boom
        "BURST" -> R.string.shape_option_burst
        "SOFT_BURST" -> R.string.shape_option_soft_burst
        "SUNNY" -> R.string.shape_option_sunny
        "VERY_SUNNY" -> R.string.shape_option_very_sunny
        "COOKIE_4" -> R.string.shape_option_cookie4
        "COOKIE_6" -> R.string.shape_option_cookie6
        "COOKIE_7" -> R.string.shape_option_cookie7
        "COOKIE_9" -> R.string.shape_option_cookie9
        "COOKIE_12" -> R.string.shape_option_cookie12
        "GHOSTISH" -> R.string.shape_option_ghostish
        "PUFFY" -> R.string.shape_option_puffy
        "PUFFY_DIAMOND" -> R.string.shape_option_puffy_diamond
        "FAN" -> R.string.shape_option_fan
        "ARROW" -> R.string.shape_option_arrow
        "ARCH" -> R.string.shape_option_arch
        "CLAM_SHELL" -> R.string.shape_option_clam_shell
        "GEM" -> R.string.shape_option_gem
        "SEMI_CIRCLE" -> R.string.shape_option_semi_circle
        "SLANTED" -> R.string.shape_option_slanted
        "PIXEL_CIRCLE" -> R.string.shape_option_pixel_circle
        "PIXEL_TRIANGLE" -> R.string.shape_option_pixel_triangle
        else -> null
    }
    return if (resId != null) stringResource(resId) else id
}

@Composable
private fun getLocalizedShapeDesc(id: String): String {
    val resId = when (id) {
        "CIRCLE" -> R.string.shape_option_circle_desc
        "SQUARE" -> R.string.shape_option_square_desc
        "OVAL" -> R.string.shape_option_oval_desc
        "PILL" -> R.string.shape_option_pill_desc
        "DIAMOND" -> R.string.shape_option_diamond_desc
        "TRIANGLE" -> R.string.shape_option_triangle_desc
        "PENTAGON" -> R.string.shape_option_pentagon_desc
        "FLOWER" -> R.string.shape_option_flower_desc
        "CLOVER_4_LEAF" -> R.string.shape_option_clover_4_leaf_desc
        "CLOVER_8_LEAF" -> R.string.shape_option_clover_8_leaf_desc
        "HEART" -> R.string.shape_option_heart_desc
        "BUN" -> R.string.shape_option_bun_desc
        "BOOM" -> R.string.shape_option_boom_desc
        "SOFT_BOOM" -> R.string.shape_option_soft_boom_desc
        "BURST" -> R.string.shape_option_burst_desc
        "SOFT_BURST" -> R.string.shape_option_soft_burst_desc
        "SUNNY" -> R.string.shape_option_sunny_desc
        "VERY_SUNNY" -> R.string.shape_option_very_sunny_desc
        "COOKIE_4" -> R.string.shape_option_cookie4_desc
        "COOKIE_6" -> R.string.shape_option_cookie6_desc
        "COOKIE_7" -> R.string.shape_option_cookie7_desc
        "COOKIE_9" -> R.string.shape_option_cookie9_desc
        "COOKIE_12" -> R.string.shape_option_cookie12_desc
        "GHOSTISH" -> R.string.shape_option_ghostish_desc
        "PUFFY" -> R.string.shape_option_puffy_desc
        "PUFFY_DIAMOND" -> R.string.shape_option_puffy_diamond_desc
        "FAN" -> R.string.shape_option_fan_desc
        "ARROW" -> R.string.shape_option_arrow_desc
        "ARCH" -> R.string.shape_option_arch_desc
        "CLAM_SHELL" -> R.string.shape_option_clam_shell_desc
        "GEM" -> R.string.shape_option_gem_desc
        "SEMI_CIRCLE" -> R.string.shape_option_semi_circle_desc
        "SLANTED" -> R.string.shape_option_slanted_desc
        "PIXEL_CIRCLE" -> R.string.shape_option_pixel_circle_desc
        "PIXEL_TRIANGLE" -> R.string.shape_option_pixel_triangle_desc
        else -> null
    }
    return if (resId != null) stringResource(resId) else ""
}

@Composable
private fun getLocalizedPresetName(id: String): String {
    val resId = when (id) {
        "DEFAULT" -> R.string.shape_preset_default
        "FRIENDLY" -> R.string.shape_preset_friendly
        "CHEERFUL" -> R.string.shape_preset_cheerful
        "MODERN" -> R.string.shape_preset_modern
        "PLAYFUL" -> R.string.shape_preset_playful
        "ORGANIC" -> R.string.shape_preset_organic
        "GEOMETRIC" -> R.string.shape_preset_geometric
        "RETRO" -> R.string.shape_preset_retro
        "CUSTOM" -> R.string.shape_preset_custom
        else -> null
    }
    return if (resId != null) stringResource(resId) else id
}

@Composable
private fun getLocalizedPresetDesc(id: String): String {
    val resId = when (id) {
        "DEFAULT" -> R.string.shape_preset_default_desc
        "FRIENDLY" -> R.string.shape_preset_friendly_desc
        "CHEERFUL" -> R.string.shape_preset_cheerful_desc
        "MODERN" -> R.string.shape_preset_modern_desc
        "PLAYFUL" -> R.string.shape_preset_playful_desc
        "ORGANIC" -> R.string.shape_preset_organic_desc
        "GEOMETRIC" -> R.string.shape_preset_geometric_desc
        "RETRO" -> R.string.shape_preset_retro_desc
        "CUSTOM" -> R.string.shape_preset_custom_desc
        else -> null
    }
    return if (resId != null) stringResource(resId) else ""
}

@Composable
private fun getLocalizedShapeCategory(category: String): String {
    val resId = when (category) {
        "Basic" -> R.string.shape_group_basic
        "Organic" -> R.string.shape_group_organic
        "Playful" -> R.string.shape_group_playful
        "Cookie" -> R.string.shape_group_cookie
        "Whimsical" -> R.string.shape_group_whimsical
        "Special" -> R.string.shape_group_special
        "Pixel" -> R.string.shape_group_pixel
        else -> null
    }
    return if (resId != null) stringResource(resId) else category
}

@Composable
private fun getLocalizedTargetName(targetId: String): String {
    val resId = when (targetId) {
        "ALBUM_ART" -> R.string.shape_target_album_art
        "PLAYER_ART" -> R.string.shape_target_player_art
        "SONG_ART" -> R.string.shape_target_song_art
        "PLAYLIST_ART" -> R.string.shape_target_playlist_art
        "ARTIST_ART" -> R.string.shape_target_artist_art
        "PLAYER_CONTROLS" -> R.string.shape_utils_player_controls
        "MINI_PLAYER" -> R.string.shape_utils_mini_player
        else -> null
    }
    return if (resId != null) stringResource(resId) else targetId
}