package chromahub.rhythm.app.shared.presentation.screens.settings


import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import androidx.compose.foundation.layout.PaddingValues
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater
import chromahub.rhythm.app.infrastructure.widget.glance.RhythmWidgetReceiver
import chromahub.rhythm.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onBackClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = AppSettings.getInstance(context)
    
    // Collect widget settings
    val showAlbumArt by appSettings.widgetShowAlbumArt.collectAsState()
    val showArtist by appSettings.widgetShowArtist.collectAsState()
    val showAlbum by appSettings.widgetShowAlbum.collectAsState()
    val showFavoriteButton by appSettings.widgetShowFavoriteButton.collectAsState()
    val cornerRadius by appSettings.widgetCornerRadius.collectAsState()
    val autoUpdate by appSettings.widgetAutoUpdate.collectAsState()
    val widgetTheme by appSettings.widgetTheme.collectAsState()
    
    var showCornerRadiusSheet by remember { mutableStateOf(false) }
    var showWidgetThemeSheet by remember { mutableStateOf(false) }

    fun buildToggleSettingsItem(
        icon: MaterialSymbolIcon,
        title: String,
        description: String,
        checked: Boolean,
        onToggle: (Boolean) -> Unit
    ): Material3SettingsItem {
        return Material3SettingsItem(
            icon = icon,
            title = { Text(title) },
            description = { Text(description) },
            trailingContent = {
                TunerAnimatedSwitch(
                    checked = checked,
                    onCheckedChange = {
                        HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                        onToggle(it)
                    }
                )
            },
            onClick = {
                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                onToggle(!checked)
            }
        )
    }
    
    CollapsibleHeaderScreen(
        title = stringResource(R.string.widgetsettingsscreen_widget_settings),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.HEAVY)
            onBackClick()
        }
    ) { modifier ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {

            
            // Display Options
            item {
                Spacer(modifier = Modifier.height(24.dp))
                val displayItems = listOf(
                    buildToggleSettingsItem(
                        icon = RhythmIcons.Image,
                        title = stringResource(R.string.onboarding_widget_album_art),
                        description = stringResource(R.string.widget_show_album_art_desc),
                        checked = showAlbumArt,
                        onToggle = {
                            appSettings.setWidgetShowAlbumArt(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = RhythmIcons.Artist,
                        title = stringResource(R.string.onboarding_widget_artist),
                        description = stringResource(R.string.widget_show_artist_desc),
                        checked = showArtist,
                        onToggle = {
                            appSettings.setWidgetShowArtist(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = RhythmIcons.Album,
                        title = stringResource(R.string.onboarding_widget_album),
                        description = stringResource(R.string.widget_show_album_desc),
                        checked = showAlbum,
                        onToggle = {
                            appSettings.setWidgetShowAlbum(it)
                            updateAllWidgets(context)
                        }
                    ),
                    buildToggleSettingsItem(
                        icon = RhythmIcons.FavoriteFilled,
                        title = stringResource(R.string.widgetsettingsscreen_show_favorite_button),
                        description = stringResource(R.string.widget_show_favorite_button_desc),
                        checked = showFavoriteButton,
                        onToggle = {
                            appSettings.setWidgetShowFavoriteButton(it)
                            updateAllWidgets(context)
                        }
                    )
                )

                Material3SettingsGroup(
                    title = stringResource(R.string.settings_display_options),
                    items = displayItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
            
            // Appearance Options
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Material3SettingsGroup(
                    title = stringResource(R.string.widget_appearance),
                    items = listOf(
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("rounded_corner"),
                            title = { Text(stringResource(R.string.settings_miniplayer_corner_radius)) },
                            description = { Text(stringResource(R.string.widget_settings_radius_desc, cornerRadius)) },
                            trailingContent = {
                                Icon(
                                    imageVector = RhythmIcons.Forward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                                showCornerRadiusSheet = true
                            }
                        ),
                        Material3SettingsItem(
                            icon = MaterialSymbolIcon("palette"),
                            title = { Text(stringResource(R.string.widgetsettingsscreen_widget_theme)) },
                            description = {
                                val themeName = when (widgetTheme) {
                                    1 -> stringResource(R.string.widget_theme_solid_dark)
                                    2 -> stringResource(R.string.widget_theme_translucent_dark)
                                    3 -> stringResource(R.string.widget_theme_solid_purple)
                                    else -> stringResource(R.string.widget_theme_dynamic)
                                }
                                Text(stringResource(R.string.widget_theme_glance_suffix, themeName))
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = RhythmIcons.Forward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                                showWidgetThemeSheet = true
                            }
                        )
                    ),
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
                                text = stringResource(R.string.onboarding_widgets_tips_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        WidgetTipItem(
                            icon = MaterialSymbolIcon("apps"),
                            text = stringResource(R.string.widgetsettingsscreen_rhythm_uses_material_3)
                        )
                        WidgetTipItem(
                            icon = MaterialSymbolIcon("widgets"),
                            text = stringResource(R.string.widgetsettingsscreen_widget_adapts_from_1x1)
                        )
                        WidgetTipItem(
                            icon = MaterialSymbolIcon("touch_app"),
                            text = stringResource(R.string.widget_tip_controls)
                        )
                        WidgetTipItem(
                            icon = MaterialSymbolIcon("grid_on"),
                            text = stringResource(R.string.widgetsettingsscreen_longpress_and_resize_for)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Corner Radius Slider Sheet
        if (showCornerRadiusSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var tempRadius by remember { mutableIntStateOf(cornerRadius) }
            
            ModalBottomSheet(
                onDismissRequest = { showCornerRadiusSheet = false },
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
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_miniplayer_corner_radius),
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
                                    text = stringResource(R.string.unit_dp, tempRadius),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Slider(
                        value = tempRadius.toFloat(),
                        onValueChange = { tempRadius = it.toInt() },
                        onValueChangeFinished = {
                            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.HEAVY)
                            appSettings.setWidgetCornerRadius(tempRadius)
                            updateAllWidgets(context)
                        },
                        valueRange = 0f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Info card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.widgetsettingsscreen_applies_to_glance_widgets),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Widget Theme Selection Sheet
        if (showWidgetThemeSheet) {
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

            val themes = listOf(
                Triple(0, stringResource(R.string.widget_theme_dynamic_system), stringResource(R.string.widget_theme_dynamic_desc)),
                Triple(1, stringResource(R.string.widget_theme_solid_dark), stringResource(R.string.widget_theme_solid_dark_desc)),
                Triple(2, stringResource(R.string.widget_theme_translucent_dark), stringResource(R.string.widget_theme_translucent_dark_desc)),
                Triple(3, stringResource(R.string.widget_theme_solid_purple_signature), stringResource(R.string.widget_theme_solid_purple_desc))
            )
            
            ModalBottomSheet(
                onDismissRequest = { showWidgetThemeSheet = false },
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
                                text = stringResource(R.string.widgetsettingsscreen_widget_theme),
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
                                    text = stringResource(R.string.widgetsettingsscreen_personalize_home_screen_widgets),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        themes.forEach { (value, name, desc) ->
                            val isSelected = widgetTheme == value
                            val icon = when (value) {
                                1 -> MaterialSymbolIcon("dark_mode", filled = true)
                                2 -> MaterialSymbolIcon("opacity")
                                3 -> MaterialSymbolIcon("palette")
                                else -> MaterialSymbolIcon("auto_awesome")
                            }
                            
                            Card(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
                                    appSettings.setWidgetTheme(value)
                                    updateAllWidgets(context)
                                    showWidgetThemeSheet = false
                                },
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
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = desc,
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
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun WidgetTipItem(
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

fun updateAllWidgets(context: Context) {
    // Update Glance widgets via broadcast
    val glanceIntent = android.content.Intent(context, RhythmWidgetReceiver::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val glanceIds = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, RhythmWidgetReceiver::class.java))
    glanceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, glanceIds)
    context.sendBroadcast(glanceIntent)
    GlanceWidgetUpdater.forceUpdateAll(context)
}
