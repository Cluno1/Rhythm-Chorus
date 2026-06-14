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


// Custom Colors Dialog for Theme Customization
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentScheme: String,
    selectedColorSource: ColorSource,
    onApply: (Color, Color, Color) -> Unit,
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

        // Parse current custom colors from the scheme name, or use defaults
        val customScheme = parseCustomColorScheme(currentScheme, false)

        var primaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.primary)
            } else {
                mutableStateOf(Color(0xFF5C4AD5)) // Default purple
            }
        }
        var secondaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.secondary)
            } else {
                mutableStateOf(Color(0xFF5D5D6B))
            }
        }
        var tertiaryColor by remember(currentScheme) {
            if (customScheme != null) {
                mutableStateOf(customScheme.tertiary)
            } else {
                mutableStateOf(Color(0xFFFFDDB6))
            }
        }

        var selectedColorType by remember { mutableStateOf(ColorType.PRIMARY) }

        ModalBottomSheet(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Header
                AnimateIn {
                    StandardBottomSheetHeader(
                        title = context.getString(R.string.theme_custom_picker),
                        subtitle = context.getString(R.string.theme_custom_picker_desc),
                        visible = showContent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (selectedColorSource != ColorSource.CUSTOM) {
                        // Unavailable state
                        item {
                            AnimateIn {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
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
                                            text = context.getString(R.string.theme_custom_colors_unavailable),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = context.getString(R.string.theme_custom_colors_switch),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Main Color Customization Card
                        item {
                            AnimateIn {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        // Color Preview Section
                                        Text(
                                            text = stringResource(R.string.customcolorsdialog_color_preview),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Color row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            ColorPreviewItem(
                                                label = context.getString(R.string.theme_color_primary),
                                                color = primaryColor,
                                                isSelected = selectedColorType == ColorType.PRIMARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                    selectedColorType = ColorType.PRIMARY
                                                }
                                            )
                                            ColorPreviewItem(
                                                label = context.getString(R.string.theme_color_secondary),
                                                color = secondaryColor,
                                                isSelected = selectedColorType == ColorType.SECONDARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                    selectedColorType = ColorType.SECONDARY
                                                }
                                            )
                                            ColorPreviewItem(
                                                label = context.getString(R.string.theme_color_tertiary),
                                                color = tertiaryColor,
                                                isSelected = selectedColorType == ColorType.TERTIARY,
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                    selectedColorType = ColorType.TERTIARY
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Color Picker Section
                                        Text(
                                            text = stringResource(R.string.customcolorsdialog_color_picker),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        when (selectedColorType) {
                                            ColorType.PRIMARY -> ColorPickerControls(
                                                color = primaryColor,
                                                onColorChange = { primaryColor = it }
                                            )
                                            ColorType.SECONDARY -> ColorPickerControls(
                                                color = secondaryColor,
                                                onColorChange = { secondaryColor = it }
                                            )
                                            ColorType.TERTIARY -> ColorPickerControls(
                                                color = tertiaryColor,
                                                onColorChange = { tertiaryColor = it }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Color Inspirations Section
                                        Text(
                                            text = stringResource(R.string.customcolorsdialog_color_inspirations),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        // Warm colors
                                        PresetColorRow(
                                            title = stringResource(R.string.customcolorsdialog_warm_cozy),
                                            colors = listOf(
                                                Color(0xFFFF6B35), Color(0xFFFF8F00), Color(0xFFFF6F00),
                                                Color(0xFFFF5722), Color(0xFFE91E63), Color(0xFF9C27B0)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Cool colors
                                        PresetColorRow(
                                            title = stringResource(R.string.customcolorsdialog_cool_fresh),
                                            colors = listOf(
                                                Color(0xFF1E88E5), Color(0xFF0097A7), Color(0xFF00ACC1),
                                                Color(0xFF00BCD4), Color(0xFF4DD0E1), Color(0xFF26A69A)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Nature colors
                                        PresetColorRow(
                                            title = stringResource(R.string.customcolorsdialog_nature_earth),
                                            colors = listOf(
                                                Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFF4CAF50),
                                                Color(0xFF66BB6A), Color(0xFF81C784), Color(0xFFA5D6A7)
                                            ),
                                            onColorSelected = { color ->
                                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                when (selectedColorType) {
                                                    ColorType.PRIMARY -> primaryColor = color
                                                    ColorType.SECONDARY -> secondaryColor = color
                                                    ColorType.TERTIARY -> tertiaryColor = color
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        item {
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ui_cancel))
                                }
                                Button(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                        onApply(primaryColor, secondaryColor, tertiaryColor)
                                        val primaryHex = String.format("%06X", (primaryColor.toArgb() and 0xFFFFFF))
                                        val secondaryHex = String.format("%06X", (secondaryColor.toArgb() and 0xFFFFFF))
                                        val tertiaryHex = String.format("%06X", (tertiaryColor.toArgb() and 0xFFFFFF))
                                        val customScheme = "custom_${primaryHex}_${secondaryHex}_${tertiaryHex}"
                                        appSettings.setCustomColorScheme(customScheme)
                                        Toast.makeText(context, context.getString(R.string.theme_colors_applied), Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.ui_apply))
                                }
                            }
                        }

                        // Bottom padding
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}