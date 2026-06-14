@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings



import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import androidx.compose.foundation.layout.PaddingValues
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


// Data classes and enums for theme customization
data class ColorSchemeOption(
    val name: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
)

data class FontOption(
    val name: String,
    val displayName: String,
    val description: String
)

enum class ColorSource(val displayName: String, val description: String, val icon: MaterialSymbolIcon) {
    ALBUM_ART("Album Art", "Extract colors from currently playing album artwork", RhythmIcons.Image),
    MONET("System Colors", "Use Material You colors from your wallpaper", MaterialSymbolIcon("color_lens", filled = true)),
    CUSTOM("Custom Scheme", "Choose from predefined color schemes", RhythmIcons.Palette)
}

fun ColorSource.getDisplayName(context: Context): String {
    return when (this) {
        ColorSource.ALBUM_ART -> context.getString(R.string.color_source_album_art)
        ColorSource.MONET -> context.getString(R.string.color_source_system_colors)
        ColorSource.CUSTOM -> context.getString(R.string.color_source_custom_scheme)
    }
}

fun ColorSource.getDescription(context: Context): String {
    return when (this) {
        ColorSource.ALBUM_ART -> context.getString(R.string.color_source_album_art_desc)
        ColorSource.MONET -> context.getString(R.string.color_source_system_colors_desc)
        ColorSource.CUSTOM -> context.getString(R.string.color_source_custom_scheme_desc)
    }
}

enum class FontSource(val displayName: String, val description: String, val icon: MaterialSymbolIcon) {
    SYSTEM("System Font", "Use the device's default font", MaterialSymbolIcon("phone_android", filled = true)),
    CUSTOM("Custom Font", "Import and use a custom font file", MaterialSymbolIcon("text_fields", filled = true))
}

fun FontSource.getDisplayName(context: Context): String {
    return when (this) {
        FontSource.SYSTEM -> context.getString(R.string.font_source_system_font)
        FontSource.CUSTOM -> context.getString(R.string.font_source_custom_font)
    }
}

fun FontSource.getDescription(context: Context): String {
    return when (this) {
        FontSource.SYSTEM -> context.getString(R.string.font_source_system_font_desc)
        FontSource.CUSTOM -> context.getString(R.string.font_source_custom_font_desc)
    }
}



// HSL Color conversion utilities
data class HSLColor(val hue: Float, val saturation: Float, val lightness: Float)

fun Color.toHSL(): HSLColor {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val diff = max - min

    val lightness = (max + min) / 2f

    val saturation = if (diff == 0f) 0f else diff / (1f - kotlin.math.abs(2f * lightness - 1f))

    val hue = when (max) {
        min -> 0f
        r -> ((g - b) / diff) % 6
        g -> (b - r) / diff + 2
        b -> (r - g) / diff + 4
        else -> 0f
    } * 60f

    return HSLColor(
        hue = if (hue < 0) hue + 360f else hue,
        saturation = saturation,
        lightness = lightness
    )
}



fun HSLColor.toColor(): Color {
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}



@Composable
fun ThemeCustomizationSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    val haptic = LocalHapticFeedback.current

    // Theme states
    val useSystemTheme by appSettings.useSystemTheme.collectAsState()
    val darkMode by appSettings.darkMode.collectAsState()
    val amoledTheme by appSettings.amoledTheme.collectAsState()
    val useDynamicColors by appSettings.useDynamicColors.collectAsState()
    val customColorScheme by appSettings.customColorScheme.collectAsState()
    val colorSource by appSettings.colorSource.collectAsState()
    val extractedAlbumColors by appSettings.extractedAlbumColors.collectAsState()

    // Font states
    val fontSource by appSettings.fontSource.collectAsState()
    val customFontPath by appSettings.customFontPath.collectAsState()
    val customFontFamily by appSettings.customFontFamily.collectAsState()

    // Color source state - initialize based on saved setting
    var selectedColorSource by remember(colorSource) {
        mutableStateOf(
            when (colorSource) {
                "ALBUM_ART" -> ColorSource.ALBUM_ART
                "MONET" -> ColorSource.MONET
                "CUSTOM" -> ColorSource.CUSTOM
                else -> ColorSource.CUSTOM
            }
        )
    }

    // Font source state - initialize based on saved setting
    var selectedFontSource by remember(fontSource) {
        mutableStateOf(
            when (fontSource) {
                "CUSTOM" -> FontSource.CUSTOM
                "SYSTEM" -> FontSource.SYSTEM
                else -> FontSource.SYSTEM
            }
        )
    }

    // Font file picker launcher
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy font to internal storage
            val fontPath = FontLoader.copyFontToInternalStorage(context, it)
            if (fontPath != null) {
                // Validate that the font can be loaded
                val testFont = FontLoader.loadCustomFont(context, fontPath)
                if (testFont != null) {
                    // Save to settings
                    appSettings.setCustomFontPath(fontPath)
                    appSettings.setFontSource("CUSTOM")

                    // Extract and save font name
                    val fontName = FontLoader.getFontFileName(fontPath) ?: "Custom Font"
                    appSettings.setCustomFontFamily(fontName)

                    // Show success feedback
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    Toast.makeText(context, context.getString(R.string.theme_font_imported), Toast.LENGTH_SHORT).show()
                } else {
                    // Font file copied but can't be loaded
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.Reject)
                    Toast.makeText(context, context.getString(R.string.theme_font_invalid), Toast.LENGTH_SHORT).show()
                }
            } else {
                // Failed to copy font file
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.Reject)
                Toast.makeText(context, context.getString(R.string.theme_font_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Color schemes - expanded list matching bottomsheet
    val colorSchemes = remember(context) {
        listOf(
            ColorSchemeOption(
                name = "Default",
                displayName = context.getString(R.string.color_scheme_default_title),
                description = context.getString(R.string.color_scheme_default_desc),
                primaryColor = Color(0xFF6750A4),
                secondaryColor = Color(0xFF625B71),
                tertiaryColor = Color(0xFF7D5260)
            ),
            ColorSchemeOption(
                name = "Warm",
                displayName = context.getString(R.string.color_scheme_warm_title),
                description = context.getString(R.string.color_scheme_warm_desc),
                primaryColor = Color(0xFFFF6B35),
                secondaryColor = Color(0xFFF7931E),
                tertiaryColor = Color(0xFFFFC857)
            ),
            ColorSchemeOption(
                name = "Cool",
                displayName = context.getString(R.string.color_scheme_cool_title),
                description = context.getString(R.string.color_scheme_cool_desc),
                primaryColor = Color(0xFF1E88E5),
                secondaryColor = Color(0xFF00897B),
                tertiaryColor = Color(0xFF80DEEA)
            ),
            ColorSchemeOption(
                name = "Forest",
                displayName = context.getString(R.string.color_scheme_forest_title),
                description = context.getString(R.string.color_scheme_forest_desc),
                primaryColor = Color(0xFF2E7D32),
                secondaryColor = Color(0xFF558B2F),
                tertiaryColor = Color(0xFF9CCC65)
            ),
            ColorSchemeOption(
                name = "Rose",
                displayName = context.getString(R.string.color_scheme_rose_title),
                description = context.getString(R.string.color_scheme_rose_desc),
                primaryColor = Color(0xFFE91E63),
                secondaryColor = Color(0xFFC2185B),
                tertiaryColor = Color(0xFFF8BBD0)
            ),
            ColorSchemeOption(
                name = "Monochrome",
                displayName = context.getString(R.string.color_scheme_monochrome_title),
                description = context.getString(R.string.color_scheme_monochrome_desc),
                primaryColor = Color(0xFF424242),
                secondaryColor = Color(0xFF616161),
                tertiaryColor = Color(0xFF9E9E9E)
            ),
            ColorSchemeOption(
                name = "Lavender",
                displayName = context.getString(R.string.color_scheme_lavender_title),
                description = context.getString(R.string.color_scheme_lavender_desc),
                primaryColor = Color(0xFF7C4DFF),
                secondaryColor = Color(0xFF9575CD),
                tertiaryColor = Color(0xFFBA68C8)
            ),
            ColorSchemeOption(
                name = "Ocean",
                displayName = context.getString(R.string.color_scheme_ocean_title),
                description = context.getString(R.string.color_scheme_ocean_desc),
                primaryColor = Color(0xFF006064),
                secondaryColor = Color(0xFF00838F),
                tertiaryColor = Color(0xFF00ACC1)
            ),
            ColorSchemeOption(
                name = "Aurora",
                displayName = context.getString(R.string.color_scheme_aurora_title),
                description = context.getString(R.string.color_scheme_aurora_desc),
                primaryColor = Color(0xFF00C853),
                secondaryColor = Color(0xFF00E676),
                tertiaryColor = Color(0xFF69F0AE)
            ),
            ColorSchemeOption(
                name = "Amber",
                displayName = context.getString(R.string.color_scheme_amber_title),
                description = context.getString(R.string.color_scheme_amber_desc),
                primaryColor = Color(0xFFFF6F00),
                secondaryColor = Color(0xFFFF8F00),
                tertiaryColor = Color(0xFFFFC107)
            ),
            ColorSchemeOption(
                name = "Crimson",
                displayName = context.getString(R.string.color_scheme_crimson_title),
                description = context.getString(R.string.color_scheme_crimson_desc),
                primaryColor = Color(0xFFB71C1C),
                secondaryColor = Color(0xFFC62828),
                tertiaryColor = Color(0xFFD32F2F)
            ),
            ColorSchemeOption(
                name = "Emerald",
                displayName = context.getString(R.string.color_scheme_emerald_title),
                description = context.getString(R.string.color_scheme_emerald_desc),
                primaryColor = Color(0xFF2E7D32),
                secondaryColor = Color(0xFF388E3C),
                tertiaryColor = Color(0xFF4CAF50)
            ),
            ColorSchemeOption(
                name = "Mint",
                displayName = context.getString(R.string.color_scheme_mint_title),
                description = context.getString(R.string.color_scheme_mint_desc),
                primaryColor = Color(0xFF0097A7),
                secondaryColor = Color(0xFF00ACC1),
                tertiaryColor = Color(0xFF00BCD4)
            )
        )
    }

    // Font options - matching bottomsheet
    val fontOptions = remember(context) {
        listOf(
            FontOption(
                name = "Geom",
                displayName = "Geom",
                description = context.getString(R.string.font_geom_desc)
            ),
            FontOption(
                name = "System",
                displayName = context.getString(R.string.font_system_title),
                description = context.getString(R.string.font_system_desc)
            ),
            FontOption(
                name = "Slate",
                displayName = "Slate",
                description = context.getString(R.string.font_slate_desc)
            ),
            FontOption(
                name = "Inter",
                displayName = "Inter",
                description = context.getString(R.string.font_inter_desc)
            ),
            FontOption(
                name = "JetBrains",
                displayName = "JetBrains Mono",
                description = context.getString(R.string.font_jetbrains_desc)
            ),
            FontOption(
                name = "Quicksand",
                displayName = "Quicksand",
                description = context.getString(R.string.font_quicksand_desc)
            )
        )
    }
    val currentFont by appSettings.customFont.collectAsState()

    // Dialog states
    var showColorSourceDialog by remember { mutableStateOf(false) }
    var showFontSourceDialog by remember { mutableStateOf(false) }
    var showColorSchemesDialog by remember { mutableStateOf(false) }
    var showCustomColorsDialog by remember { mutableStateOf(false) }
    var showFontSelectionDialog by remember { mutableStateOf(false) }
    var navigateToExpressiveShapes by remember { mutableStateOf(false) }
    
    // Restart dialog states
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartDialogMessage by remember { mutableStateOf("") }
    
    // Festive theme states
    val festiveThemeEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveThemeAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()
    val festiveThemeType by appSettings.festiveThemeType.collectAsState()
    val festiveThemeIntensity by appSettings.festiveThemeIntensity.collectAsState()
    val festiveSnowflakeSize by appSettings.festiveSnowflakeSize.collectAsState()
    val festiveSnowflakeArea by appSettings.festiveSnowflakeArea.collectAsState()
    val festiveShowTopLights by appSettings.festiveShowTopLights.collectAsState()
    val festiveShowSideGarland by appSettings.festiveShowSideGarland.collectAsState()
    val festiveShowBottomSnow by appSettings.festiveShowBottomSnow.collectAsState()
    val festiveShowSnowfall by appSettings.festiveShowSnowfall.collectAsState()
    var showFestivalSelectionDialog by remember { mutableStateOf(false) }
    
    // Handle navigation to Expressive Shapes screen with proper animation
    AnimatedContent(
        targetState = navigateToExpressiveShapes,
        transitionSpec = {
            if (targetState) {
                // Slide in from right when navigating to Expressive Shapes
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = 50
                    )
                ) + scaleIn(
                    initialScale = 0.92f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 250)
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                )
            } else {
                // Slide in from left when going back to Theme
                slideInHorizontally(
                    initialOffsetX = { -it / 4 },
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = 50
                    )
                ) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 250)
                ) + scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = EaseInCubic
                    )
                )
            }
        },
        label = "theme_to_shapes_navigation",
        contentKey = { it }
    ) { isNavigatingToShapes ->
        if (isNavigatingToShapes) {
            ExpressiveShapesSettingsScreen(onBackClick = { navigateToExpressiveShapes = false })
        } else {
            CollapsibleHeaderScreen(
                title = context.getString(R.string.settings_theme),
                showBackButton = true,
                onBackClick = onBackClick
            ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.settings_display_mode),
                items = listOf(
                    // Display Mode Button Group
                    SettingItem(
                        RhythmIcons.Settings,
                        context.getString(R.string.settings_theme_mode),
                        context.getString(R.string.settings_theme_mode_desc),
                        onClick = {
                            // This will be replaced with button group below
                        }
                    ),
                    // AMOLED Theme - always in list, rendered conditionally via AnimatedVisibility
                    SettingItem(
                        RhythmIcons.DarkMode,
                        context.getString(R.string.settings_amoled_theme),
                        context.getString(R.string.settings_amoled_theme_desc),
                        toggleState = amoledTheme,
                        onToggleChange = { appSettings.setAmoledTheme(it) }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_color_customization),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Palette,
                        context.getString(R.string.settings_color_source),
                        when (selectedColorSource) {
                            ColorSource.ALBUM_ART -> context.getString(R.string.settings_color_source_album)
                            ColorSource.MONET -> context.getString(R.string.settings_color_source_monet)
                            ColorSource.CUSTOM -> context.getString(R.string.settings_color_source_custom, customColorScheme)
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showColorSourceDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("color_lens"),
                        context.getString(R.string.settings_color_schemes),
                        if (selectedColorSource == ColorSource.CUSTOM)
                            context.getString(R.string.settings_color_schemes_desc)
                        else
                            context.getString(R.string.settings_custom_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showColorSchemesDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("brush"),
                        context.getString(R.string.settings_custom_colors),
                        if (selectedColorSource == ColorSource.CUSTOM)
                            context.getString(R.string.settings_custom_colors_desc)
                        else
                            context.getString(R.string.settings_custom_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showCustomColorsDialog = true
                        }
                    )
                )
            ),
            SettingGroup(
                title = context.getString(R.string.settings_font_customization),
                items = listOf(
                    SettingItem(
                        MaterialSymbolIcon("text_fields"),
                        context.getString(R.string.settings_font_source),
                        when (selectedFontSource) {
                            FontSource.SYSTEM -> context.getString(R.string.settings_font_source_system, currentFont)
                            FontSource.CUSTOM -> context.getString(
                                R.string.settings_font_source_custom,
                                customFontFamily
                            )
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showFontSourceDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("text_fields"),
                        context.getString(R.string.settings_font_selection),
                        if (selectedFontSource == FontSource.SYSTEM)
                            context.getString(R.string.settings_font_selection_desc)
                        else
                            context.getString(R.string.settings_system_font_only),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showFontSelectionDialog = true
                        }
                    ),
                    SettingItem(
                        MaterialSymbolIcon("file_upload"),
                        context.getString(R.string.settings_import_custom_font),
                        if (customFontPath != null)
                            context.getString(R.string.settings_font_imported_name, customFontFamily)
                        else
                            context.getString(R.string.settings_import_font_desc),
                        onClick = {
                            HapticUtils.performHapticFeedback(
                                context,
                                haptic,
                                HapticType.HEAVY
                            )
                            fontPickerLauncher.launch("font/*")
                        }
                    )
                )
            ),

            SettingGroup(
                title = context.getString(R.string.settings_festive_themes),
                items = buildList {
                    add(
                        SettingItem(
                            MaterialSymbolIcon("celebration"),
                            context.getString(R.string.settings_enable_festive),
                            context.getString(R.string.settings_enable_festive_desc),
                            toggleState = festiveThemeEnabled,
                            onToggleChange = { appSettings.setFestiveThemeEnabled(it) }
                        )
                    )
                    if (festiveThemeEnabled) {
                        add(
                            SettingItem(
                                MaterialSymbolIcon("event_available"),
                                context.getString(R.string.settings_auto_detect_holidays),
                                context.getString(R.string.settings_auto_detect_holidays_desc),
                                toggleState = festiveThemeAutoDetect,
                                onToggleChange = { appSettings.setFestiveThemeAutoDetect(it) }
                            )
                        )
                        if (!festiveThemeAutoDetect) {
                            add(
                                SettingItem(
                                    RhythmIcons.AutoAwesome,
                                    context.getString(R.string.settings_select_festival),
                                    getFestivalDisplayName(festiveThemeType),
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                        showFestivalSelectionDialog = true
                                    }
                                )
                            )
                        }
                    }
                }
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(settingGroups, key = { "setting_${it.title}_${settingGroups.indexOf(it)}" }) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = when (group.title) {
                    context.getString(R.string.settings_display_mode) -> {
                        buildList {
                            add(
                                Material3SettingsItem(
                                    icon = RhythmIcons.Settings,
                                    title = { Text(context.getString(R.string.settings_theme_mode)) },
                                    description = {
                                        Column {
                                            Text(context.getString(R.string.settings_theme_mode_desc))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            ExpressiveButtonGroup(
                                                items = listOf(
                                                    context.getString(R.string.settings_theme_system),
                                                    context.getString(R.string.settings_theme_light),
                                                    context.getString(R.string.settings_theme_dark)
                                                ),
                                                selectedIndex = when {
                                                    useSystemTheme -> 0
                                                    !darkMode -> 1
                                                    else -> 2
                                                },
                                                onItemClick = { index ->
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                    when (index) {
                                                        0 -> {
                                                            appSettings.setUseSystemTheme(true)
                                                        }

                                                        1 -> {
                                                            appSettings.setUseSystemTheme(false)
                                                            appSettings.setDarkMode(false)
                                                        }

                                                        2 -> {
                                                            appSettings.setUseSystemTheme(false)
                                                            appSettings.setDarkMode(true)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                )
                            )

                            if (!useSystemTheme && darkMode && group.items.size > 1) {
                                add(
                                    toMaterial3SettingsItem(
                                        context = context,
                                        item = group.items[1],
                                        hapticFeedback = haptic
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        group.items.map { item ->
                            toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                        }
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            // Tips Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
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
                                text = context.getString(R.string.theme_good_to_know),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        ThemeTipItem(
                            icon = RhythmIcons.Palette,
                            text = context.getString(R.string.theme_tip_album_art)
                        )
                        ThemeTipItem(
                            icon = MaterialSymbolIcon("wallpaper", filled = true),
                            text = context.getString(R.string.theme_tip_material_you)
                        )
                        ThemeTipItem(
                            icon = MaterialSymbolIcon("font_download", filled = true),
                            text = context.getString(R.string.theme_tip_custom_fonts)
                        )
                    }
                }
            }
        }
        }  // End of CollapsibleHeaderScreen else block
        }  // End of AnimatedContent
    }

    // App Restart Dialog for theme changes
    if (showRestartDialog) {
        AppRestartDialog(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                showRestartDialog = false
                chromahub.rhythm.app.util.AppRestarter.restartApp(context)
            },
            onContinue = {
                showRestartDialog = false
                // Continue without restart
            },
            message = restartDialogMessage
        )
    }

    // Dialogs
    ColorSourceDialog(
        showDialog = showColorSourceDialog,
        onDismiss = { showColorSourceDialog = false },
        selectedColorSource = selectedColorSource,
        onColorSourceSelected = { selectedColorSource = it },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    FontSourceDialog(
        showDialog = showFontSourceDialog,
        onDismiss = { showFontSourceDialog = false },
        selectedFontSource = selectedFontSource,
        onFontSourceSelected = { selectedFontSource = it },
        appSettings = appSettings,
        customFontPath = customFontPath,
        context = context,
        haptic = haptic,
        onShowRestartDialog = { message ->
            showRestartDialog = true
            restartDialogMessage = message
        }
    )

    ColorSchemesDialog(
        showDialog = showColorSchemesDialog,
        onDismiss = { showColorSchemesDialog = false },
        colorSchemes = colorSchemes,
        currentScheme = customColorScheme,
        selectedColorSource = selectedColorSource,
        onSchemeSelected = { scheme ->
            appSettings.setCustomColorScheme(scheme)
            showColorSchemesDialog = false
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    CustomColorsDialog(
        showDialog = showCustomColorsDialog,
        onDismiss = { showCustomColorsDialog = false },
        currentScheme = customColorScheme,
        selectedColorSource = selectedColorSource,
        onApply = { primary, secondary, tertiary ->
            val primaryHex = String.format("%06X", (primary.toArgb() and 0xFFFFFF))
            val secondaryHex = String.format("%06X", (secondary.toArgb() and 0xFFFFFF))
            val tertiaryHex = String.format("%06X", (tertiary.toArgb() and 0xFFFFFF))
            val customScheme = "custom_${primaryHex}_${secondaryHex}_${tertiaryHex}"
            appSettings.setCustomColorScheme(customScheme)
            showCustomColorsDialog = false
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    FontSelectionDialog(
        showDialog = showFontSelectionDialog,
        onDismiss = { showFontSelectionDialog = false },
        fontOptions = fontOptions,
        currentFont = currentFont,
        selectedFontSource = selectedFontSource,
        onFontSelected = { selectedFont ->
            appSettings.setCustomFont(selectedFont)
            showFontSelectionDialog = false
            showRestartDialog = true
            restartDialogMessage = "Font changed. Restart the app to apply the new font."
        },
        appSettings = appSettings,
        context = context,
        haptic = haptic
    )

    // Festival Selection Dialog with Intensity Controls
    if (showFestivalSelectionDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            onDismissRequest = { showFestivalSelectionDialog = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            val festiveContentPadding = 24.dp

            LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                item {
                    StandardBottomSheetHeader(
                        title = context.getString(R.string.theme_festive_settings),
                        subtitle = context.getString(R.string.settings_choose_festive_theme),
                        visible = true,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = festiveContentPadding)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = context.getString(R.string.settings_select_festival),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val festivals = listOf(
                            "CHRISTMAS" to context.getString(R.string.settings_festival_christmas),
                            "NEW_YEAR" to context.getString(R.string.settings_festival_new_year)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            festivals.forEach { (id, name) ->
                                val isSelected = id == festiveThemeType
                                Card(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                        appSettings.setFestiveThemeType(id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = RhythmIcons.CheckCircle,
                                                contentDescription = context.getString(R.string.ui_selected),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = context.getString(R.string.settings_decoration_intensity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_intensity),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(festiveThemeIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = festiveThemeIntensity,
                            onValueChange = { appSettings.setFestiveThemeIntensity(it) },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.settings_snowflake_size),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(festiveSnowflakeSize * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = festiveSnowflakeSize,
                            onValueChange = { appSettings.setFestiveSnowflakeSize(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = context.getString(R.string.settings_snowflake_display_area),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = festiveSnowflakeArea == "FULL_SCREEN",
                                onClick = { appSettings.setFestiveSnowflakeArea("FULL_SCREEN") },
                                label = { Text(context.getString(R.string.settings_area_full)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = festiveSnowflakeArea == "LEFT_RIGHT_ONLY",
                                onClick = { appSettings.setFestiveSnowflakeArea("LEFT_RIGHT_ONLY") },
                                label = { Text(context.getString(R.string.settings_area_sides)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = festiveSnowflakeArea == "TOP_ONE_THIRD",
                                onClick = { appSettings.setFestiveSnowflakeArea("TOP_ONE_THIRD") },
                                label = { Text(context.getString(R.string.settings_area_top_third)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = context.getString(R.string.settings_decoration_elements),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_snowfall),
                                description = context.getString(R.string.settings_snowfall_desc),
                                icon = MaterialSymbolIcon("ac_unit", filled = true),
                                isEnabled = festiveShowSnowfall,
                                onToggle = { appSettings.setFestiveShowSnowfall(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_top_lights),
                                description = context.getString(R.string.settings_top_lights_desc),
                                icon = MaterialSymbolIcon("lightbulb", filled = true),
                                isEnabled = festiveShowTopLights,
                                onToggle = { appSettings.setFestiveShowTopLights(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_side_garland),
                                description = context.getString(R.string.settings_side_garland_desc),
                                icon = MaterialSymbolIcon("park", filled = true),
                                isEnabled = festiveShowSideGarland,
                                onToggle = { appSettings.setFestiveShowSideGarland(it) }
                            )
                            DecorationToggleCard(
                                title = context.getString(R.string.settings_snow_pile),
                                description = context.getString(R.string.settings_snow_pile_desc),
                                icon = MaterialSymbolIcon("terrain", filled = true),
                                isEnabled = festiveShowBottomSnow,
                                onToggle = { appSettings.setFestiveShowBottomSnow(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}


//                                    modifier = Modifier.size(40.dp)
//                                ) {
//                                    Box(
//                                        contentAlignment = Alignment.Center,
//                                        modifier = Modifier.fillMaxSize()
//                                    ) {
//                                        Icon(
//                                            imageVector = source.icon,
//                                            contentDescription = null,
//                                            tint = if (isSelected)
//                                                MaterialTheme.colorScheme.onPrimary
//                                            else
//                                                MaterialTheme.colorScheme.onSurfaceVariant,
//                                            modifier = Modifier.size(20.dp)
//                                        )
//                                    }
//                                }
//
//                                Spacer(modifier = Modifier.width(16.dp))
//
//                                Column(modifier = Modifier.weight(1f)) {
//                                    Text(
//                                        text = source.displayName,
//                                        style = MaterialTheme.typography.titleMedium,
//                                        fontWeight = FontWeight.SemiBold,
//                                        color = if (isSelected)
//                                            MaterialTheme.colorScheme.onPrimaryContainer
//                                        else
//                                            MaterialTheme.colorScheme.onSurface
//                                    )
//                                    Spacer(modifier = Modifier.height(4.dp))
//                                    Text(
//                                        text = source.description,
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = if (isSelected)
//                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
//                                        else
//                                            MaterialTheme.colorScheme.onSurfaceVariant
//                                    )
//                                }
//
//                                if (isSelected) {
//                                    Icon(
//                                        imageVector = RhythmIcons.CheckCircle,
//                                        contentDescription = "Selected",
//                                        
//                                        modifier = Modifier.size(24.dp)
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

// Color Schemes Dialog for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemesDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    colorSchemes: List<ColorSchemeOption>,
    currentScheme: String,
    selectedColorSource: ColorSource,
    onSchemeSelected: (String) -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        // Animation states
        var showContent by remember { mutableStateOf(false) }

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "contentAlpha"
        )

        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = context.getString(R.string.theme_color_schemes),
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
                                text = context.getString(R.string.theme_color_schemes_desc),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedColorSource != ColorSource.CUSTOM) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = context.getString(R.string.theme_color_schemes_unavailable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.theme_color_schemes_switch),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Featured schemes
                        item {
                            Text(
                                text = context.getString(R.string.theme_featured_schemes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val featuredSchemes = colorSchemes.filter {
                            it.name in listOf("Default", "Warm", "Cool", "Forest", "Rose", "Monochrome")
                        }

                        items(featuredSchemes, key = { "featured_${it.name}" }) { option ->
                            ColorSchemeCard(
                                option = option,
                                isSelected = currentScheme == option.name,
                                onSelect = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    onSchemeSelected(option.name)
                                }
                            )
                        }

                        // More schemes
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.theme_more_schemes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val otherSchemes = colorSchemes.filter {
                            it.name !in listOf("Default", "Warm", "Cool", "Forest", "Rose", "Monochrome")
                        }

                        items(otherSchemes, key = { "other_${it.name}" }) { option ->
                            ColorSchemeCard(
                                option = option,
                                isSelected = currentScheme == option.name,
                                onSelect = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    onSchemeSelected(option.name)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ColorSchemeCard(
    option: ColorSchemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced color preview with better visibility
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = option.primaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = option.secondaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = option.tertiaryColor,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                ) {}
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = RhythmIcons.CheckCircle,
                    contentDescription = stringResource(R.string.streaming_selected),
                    
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}



@Composable
fun AnimateIn(
    delay: Int = 50,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 0),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "translationY"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale,
            translationY = translationY
        )
    ) {
        content()
    }
}



@Composable
fun ExpressiveColorPickerControls(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val hsl = remember(color) { color.toHSL() }

    var hue by remember(color) { mutableStateOf(hsl.hue) }
    var saturation by remember(color) { mutableStateOf(hsl.saturation) }
    var lightness by remember(color) { mutableStateOf(hsl.lightness) }

    var showAdvanced by remember { mutableStateOf(false) }

    // Update color when HSL values change
    LaunchedEffect(hue, saturation, lightness) {
        val newColor = HSLColor(hue, saturation, lightness).toColor()
        onColorChange(newColor)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Enhanced Current color display with gradient background
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle pattern overlay
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Palette,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("#%06X", (color.toArgb() and 0xFFFFFF)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.9f) else Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Color properties
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.alpha(0.8f)
                        ) {
                            Text(
                                text = "H:${hue.toInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "S:${(saturation * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "L:${(lightness * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced Hue Slider with gradient preview
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_hue),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${hue.toInt()}°",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hue slider with enhanced gradient
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = (0..360 step 10).map { h ->
                                    HSLColor(h.toFloat(), 1f, 0.5f).toColor()
                                }
                            )
                        )
                ) {
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enhanced Saturation Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_saturation),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${(saturation * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.LightGray,
                                    HSLColor(hue, 1f, lightness).toColor()
                                )
                            )
                        )
                ) {
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enhanced Lightness Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_lightness),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "${(lightness * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black,
                                    HSLColor(hue, saturation, 0.5f).toColor(),
                                    Color.White
                                )
                            )
                        )
                ) {
                    Slider(
                        value = lightness,
                        onValueChange = { lightness = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxSize(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced RGB controls toggle with enhanced design
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = RhythmIcons.Tune,
                        contentDescription = null,
                        
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = context.getString(R.string.theme_advanced_rgb),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TunerAnimatedSwitch(
                    checked = showAdvanced,
                    onCheckedChange = { showAdvanced = it }
                )
            }
        }

        // Advanced RGB controls with enhanced design
        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val red = (color.red * 255).toInt()
                    val green = (color.green * 255).toInt()
                    val blue = (color.blue * 255).toInt()

                    var redValue by remember(color) { mutableStateOf(red.toFloat()) }
                    var greenValue by remember(color) { mutableStateOf(green.toFloat()) }
                    var blueValue by remember(color) { mutableStateOf(blue.toFloat()) }

                    // Update HSL when RGB changes
                    LaunchedEffect(redValue, greenValue, blueValue) {
                        val rgbColor = Color(
                            red = redValue / 255f,
                            green = greenValue / 255f,
                            blue = blueValue / 255f
                        )
                        val newHsl = rgbColor.toHSL()
                        hue = newHsl.hue
                        saturation = newHsl.saturation
                        lightness = newHsl.lightness
                    }

                    Text(
                        text = stringResource(R.string.themecustomizationsettingsscreen_rgb_values),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ExpressiveColorSlider(
                        label = stringResource(R.string.theme_color_red),
                        value = redValue,
                        onValueChange = { redValue = it },
                        color = Color.Red,
                        valueRange = 0f..255f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpressiveColorSlider(
                        label = stringResource(R.string.theme_color_green),
                        value = greenValue,
                        onValueChange = { greenValue = it },
                        color = Color.Green,
                        valueRange = 0f..255f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpressiveColorSlider(
                        label = stringResource(R.string.theme_color_blue),
                        value = blueValue,
                        onValueChange = { blueValue = it },
                        color = Color.Blue,
                        valueRange = 0f..255f
                    )
                }
            }
        }
    }
}



@Composable
fun PresetColorRow(
    title: String,
    colors: List<Color>,
    onColorSelected: (Color) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onColorSelected(color) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    // Empty content
                }
            }
        }
    }
}



@Composable
fun ExpressiveColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.3f),
                    activeTickColor = color,
                    inactiveTickColor = color.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}



// Helper function to get color temperature (approximate)
fun getColorTemperature(color: Color): Int {
    // Simplified color temperature calculation
    val r = color.red
    val g = color.green
    val b = color.blue

    // Rough approximation using RGB ratios
    val temperature = if (r > g && r > b) {
        2000 + (r - b) * 3000 // Warm
    } else if (b > r && b > g) {
        6000 + (b - r) * 4000 // Cool
    } else {
        4000 + (g - r) * 2000 // Neutral
    }

    return temperature.toInt().coerceIn(2000, 10000)
}



@Composable
fun ColorPickerControls(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val hsl = remember(color) { color.toHSL() }

    var hue by remember(color) { mutableStateOf(hsl.hue) }
    var saturation by remember(color) { mutableStateOf(hsl.saturation) }
    var lightness by remember(color) { mutableStateOf(hsl.lightness) }

    var showAdvanced by remember { mutableStateOf(false) }

    // Update color when HSL values change
    LaunchedEffect(hue, saturation, lightness) {
        val newColor = HSLColor(hue, saturation, lightness).toColor()
        onColorChange(newColor)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Current color display with hex code
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Palette,
                            contentDescription = null,
                            tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("#%06X", (color.toArgb() and 0xFFFFFF)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.9f) else Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hue Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_hue),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${hue.toInt()}°",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hue slider with color gradient
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = (0..360 step 20).map { h ->
                                HSLColor(h.toFloat(), 1f, 0.5f).toColor()
                            }
                        )
                    )
            ) {
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saturation Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_saturation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.LightGray,
                                HSLColor(hue, 1f, lightness).toColor()
                            )
                        )
                    )
            ) {
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Lightness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_lightness),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "${(lightness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black,
                                HSLColor(hue, saturation, 0.5f).toColor(),
                                Color.White
                            )
                        )
                    )
            ) {
                Slider(
                    value = lightness,
                    onValueChange = { lightness = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxSize(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Advanced RGB controls toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = context.getString(R.string.theme_advanced_rgb),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TunerAnimatedSwitch(
                checked = showAdvanced,
                onCheckedChange = { showAdvanced = it }
            )
        }

        // Advanced RGB controls
        AnimatedVisibility(
            visible = showAdvanced,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                val red = (color.red * 255).toInt()
                val green = (color.green * 255).toInt()
                val blue = (color.blue * 255).toInt()

                var redValue by remember(color) { mutableStateOf(red.toFloat()) }
                var greenValue by remember(color) { mutableStateOf(green.toFloat()) }
                var blueValue by remember(color) { mutableStateOf(blue.toFloat()) }

                // Update HSL when RGB changes
                LaunchedEffect(redValue, greenValue, blueValue) {
                    val rgbColor = Color(
                        red = redValue / 255f,
                        green = greenValue / 255f,
                        blue = blueValue / 255f
                    )
                    val newHsl = rgbColor.toHSL()
                    hue = newHsl.hue
                    saturation = newHsl.saturation
                    lightness = newHsl.lightness
                }

                ColorSlider(
                    label = "Red",
                    value = redValue,
                    onValueChange = { redValue = it },
                    color = Color.Red,
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "Green",
                    value = greenValue,
                    onValueChange = { greenValue = it },
                    color = Color.Green,
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "Blue",
                    value = blueValue,
                    onValueChange = { blueValue = it },
                    color = Color.Blue,
                    valueRange = 0f..255f
                )
            }
        }
    }
}



@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f),
                activeTickColor = color,
                inactiveTickColor = color.copy(alpha = 0.3f)
            )
        )
    }
}



enum class ColorType {
    PRIMARY, SECONDARY, TERTIARY
}



@Composable
fun FontCard(
    option: FontOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = RhythmIcons.CheckCircle,
                        contentDescription = stringResource(R.string.streaming_selected),
                        
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font preview text
            Surface(
                color = if (isSelected)
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_quick_brown_fox),
                    style = getFontPreviewStyle(option.name),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}



@Composable
fun ThemeTipItem(
    icon: MaterialSymbolIcon,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}