/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings

import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import androidx.compose.foundation.layout.PaddingValues
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import chromahub.rhythm.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.util.ArtistSeparator
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilterChip
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonSize
import chromahub.rhythm.app.shared.presentation.components.common.RhythmButtonWeighted
import chromahub.rhythm.app.shared.presentation.components.common.RhythmGroupedButton
import chromahub.rhythm.app.ui.utils.LazyListStateSaver
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import kotlinx.coroutines.launch

private enum class DelimiterSheetPage {
    Main,
    AddCustom
}

private data class ArtistDelimiterPreset(
    val nameRes: Int,
    val delimiters: List<String>
)

private data class DelimiterCardItem(
    val token: String,
    val displayName: String,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSeparatorsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)
    val scope = rememberCoroutineScope()

    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()

    var showDelimiterBottomSheet by remember { mutableStateOf(false) }

    val currentDelimitersList = remember(artistSeparatorDelimiters) {
        ArtistSeparator.parseDelimiters(artistSeparatorDelimiters)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.artists_title),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
            onBackClick()
        }
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.artist_multi_parsing),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Artist,
                        context.getString(R.string.artist_enable_separation),
                        context.getString(R.string.artist_enable_separation_desc),
                        toggleState = artistSeparatorEnabled,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            appSettings.setArtistSeparatorEnabled(it)
                        }
                    ),
                    SettingItem(
                        RhythmIcons.Settings,
                        context.getString(R.string.artist_configure_delimiters),
                        context.getString(R.string.artist_current_delimiters, currentDelimitersList.joinToString(", ")),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showDelimiterBottomSheet = true
                        }
                    )
                ),
            )
        )

        val lazyListState = rememberSaveable(
            saver = LazyListStateSaver
        ) {
            androidx.compose.foundation.lazy.LazyListState()
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(
                items = settingGroups,
                key = { "setting_${it.title}" },
                contentType = { "settingGroup" }
            ) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = if (group.title == "Multi-Artist Parsing") {
                    buildList {
                        if (group.items.isNotEmpty()) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[0], hapticFeedback = haptic))
                        }
                        if (artistSeparatorEnabled && group.items.size > 1) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[1], hapticFeedback = haptic))
                        }
                    }
                } else {
                    group.items.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
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
                                imageVector = RhythmIcons.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_about_multi_artist),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = context.getString(R.string.settings_multi_artist_parsing_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDelimiterBottomSheet) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )

        var currentPage by remember { mutableStateOf(DelimiterSheetPage.Main) }

        val presets = remember {
            listOf(
                ArtistDelimiterPreset(R.string.preset_standard, listOf(";", "/")),
                ArtistDelimiterPreset(R.string.preset_minimal, listOf(";")),
                ArtistDelimiterPreset(R.string.preset_featured, listOf(";", "/", "feat.", "ft.", "featuring")),
                ArtistDelimiterPreset(R.string.preset_extended, listOf(";", "/", ",", "+", "&")),
                ArtistDelimiterPreset(R.string.preset_cjk, listOf("、", "／", "・", "•"))
            )
        }

        // Active tokens currently selected/saved
        var activeTokens by remember {
            mutableStateOf(ArtistSeparator.parseDelimiters(artistSeparatorDelimiters))
        }

        // Known built-in delimiters with localized display names
        val baseDelimiters = remember {
            listOf(
                DelimiterCardItem("/", context.getString(R.string.delimiter_slash)),
                DelimiterCardItem(";", context.getString(R.string.delimiter_semicolon)),
                DelimiterCardItem(",", context.getString(R.string.delimiter_comma)),
                DelimiterCardItem("+", context.getString(R.string.delimiter_plus)),
                DelimiterCardItem("&", context.getString(R.string.delimiter_ampersand))
            )
        }

        // Dynamic list of custom delimiters that the user has added
        var customTokens by remember {
            val initialCustom = ArtistSeparator.parseDelimiters(artistSeparatorDelimiters)
                .filterNot { token -> token in listOf("/", ";", ",", "+", "&") }
            mutableStateOf(initialCustom)
        }

        var customInputText by remember { mutableStateOf("") }

        val quickSuggestions = remember {
            listOf("feat.", "ft.", "featuring", "//", " x ", "with", "vs.", "、", "／", "・", "•")
        }

        LaunchedEffect(Unit) {
            sheetState.expand()
        }

        ModalBottomSheet(
            onDismissRequest = { showDelimiterBottomSheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                // Fixed Header Area per page
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        val floatSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                        val offsetSpring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)
                        if (targetState == DelimiterSheetPage.AddCustom) {
                            (slideInHorizontally(animationSpec = offsetSpring, initialOffsetX = { it }) + fadeIn(animationSpec = floatSpring))
                                .togetherWith(slideOutHorizontally(animationSpec = offsetSpring, targetOffsetX = { -it / 2 }) + fadeOut(animationSpec = floatSpring))
                        } else {
                            (slideInHorizontally(animationSpec = offsetSpring, initialOffsetX = { -it }) + fadeIn(animationSpec = floatSpring))
                                .togetherWith(slideOutHorizontally(animationSpec = offsetSpring, targetOffsetX = { it / 2 }) + fadeOut(animationSpec = floatSpring))
                        }
                    },
                    label = "delimiter_sheet_header_transition",
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        DelimiterSheetPage.Main -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = context.getString(R.string.settings_configure_delimiters),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Preset filter chips (Fixed row)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    items(presets, key = { it.nameRes }) { preset ->
                                        val isSelected = activeTokens.toSet() == preset.delimiters.toSet()
                                        ExpressiveFilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                activeTokens = preset.delimiters
                                                val newCustom = preset.delimiters.filterNot { it in listOf("/", ";", ",", "+", "&") }
                                                customTokens = (customTokens + newCustom).distinct()
                                            },
                                            label = { Text(stringResource(preset.nameRes)) },
                                            leadingIcon = if (isSelected) ({
                                                Icon(
                                                    imageVector = RhythmIcons.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }) else null
                                        )
                                    }
                                }
                            }
                        }

                        DelimiterSheetPage.AddCustom -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                        currentPage = DelimiterSheetPage.Main
                                    },
                                    modifier = Modifier.offset(x = (-8).dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Back,
                                            contentDescription = stringResource(R.string.common_back),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.delimiter_add_custom),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable Content Area (Weighted to eliminate remeasurement jitter during dragging)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            val floatSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                            val offsetSpring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow)
                            if (targetState == DelimiterSheetPage.AddCustom) {
                                (slideInHorizontally(animationSpec = offsetSpring, initialOffsetX = { it }) + fadeIn(animationSpec = floatSpring))
                                    .togetherWith(slideOutHorizontally(animationSpec = offsetSpring, targetOffsetX = { -it / 2 }) + fadeOut(animationSpec = floatSpring))
                            } else {
                                (slideInHorizontally(animationSpec = offsetSpring, initialOffsetX = { -it }) + fadeIn(animationSpec = floatSpring))
                                    .togetherWith(slideOutHorizontally(animationSpec = offsetSpring, targetOffsetX = { it / 2 }) + fadeOut(animationSpec = floatSpring))
                            }
                        },
                        label = "delimiter_sheet_body_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            DelimiterSheetPage.Main -> {
                                val mainScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(mainScrollState)
                                        .padding(horizontal = 24.dp)
                                        .padding(bottom = 12.dp)
                                ) {
                                    // 2-column card grid in the previous UI style
                                    val allDisplayCards = buildList {
                                        addAll(baseDelimiters)
                                        customTokens.forEach { token ->
                                            add(DelimiterCardItem(token, token, isCustom = true))
                                        }
                                    }

                                    val totalSlots = allDisplayCards.size + 1
                                    val rows = (totalSlots + 1) / 2

                                    for (rowIndex in 0 until rows) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val leftIndex = rowIndex * 2
                                            val rightIndex = leftIndex + 1

                                            // Left Card
                                            if (leftIndex < allDisplayCards.size) {
                                                val item = allDisplayCards[leftIndex]
                                                val isSelected = activeTokens.contains(item.token)
                                                DelimiterGridCard(
                                                    modifier = Modifier.weight(1f),
                                                    symbol = item.token,
                                                    label = item.displayName,
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                        activeTokens = if (isSelected) {
                                                            activeTokens.filterNot { it == item.token }
                                                        } else {
                                                            activeTokens + item.token
                                                        }
                                                    }
                                                )
                                            } else if (leftIndex == allDisplayCards.size) {
                                                AddCustomGridCard(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                        currentPage = DelimiterSheetPage.AddCustom
                                                    }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }

                                            // Right Card
                                            if (rightIndex < allDisplayCards.size) {
                                                val item = allDisplayCards[rightIndex]
                                                val isSelected = activeTokens.contains(item.token)
                                                DelimiterGridCard(
                                                    modifier = Modifier.weight(1f),
                                                    symbol = item.token,
                                                    label = item.displayName,
                                                    isSelected = isSelected,
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                        activeTokens = if (isSelected) {
                                                            activeTokens.filterNot { it == item.token }
                                                        } else {
                                                            activeTokens + item.token
                                                        }
                                                    }
                                                )
                                            } else if (rightIndex == allDisplayCards.size) {
                                                AddCustomGridCard(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                        currentPage = DelimiterSheetPage.AddCustom
                                                    }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Live preview card in previous UI style
                                    val currentSerialized = remember(activeTokens) {
                                        ArtistSeparator.serializeDelimiters(activeTokens)
                                    }

                                    val liveSamples = remember(currentSerialized) {
                                        listOf(
                                            "Kendrick Lamar; SZA" to ArtistSeparator.splitArtistNames("Kendrick Lamar; SZA", currentSerialized, true).joinToString(", "),
                                            "AC\\/DC" to ArtistSeparator.splitArtistNames("AC\\/DC", currentSerialized, true).joinToString(", "),
                                            "Tyler, The Creator" to ArtistSeparator.splitArtistNames("Tyler, The Creator", currentSerialized, true).joinToString(", "),
                                            "Simon & Garfunkel" to ArtistSeparator.splitArtistNames("Simon & Garfunkel", currentSerialized, true).joinToString(", "),
                                            "Daft Punk feat. Pharrell Williams" to ArtistSeparator.splitArtistNames("Daft Punk feat. Pharrell Williams", currentSerialized, true).joinToString(", "),
                                            "Artist 1 / Artist 2" to ArtistSeparator.splitArtistNames("Artist 1 / Artist 2", currentSerialized, true).joinToString(", ")
                                        )
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = MaterialSymbolIcon("lightbulb"),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = context.getString(R.string.settings_examples),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            liveSamples.forEach { (original, result) ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = original,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = RhythmIcons.Forward,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = result,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DelimiterSheetPage.AddCustom -> {
                                val addScrollState = rememberScrollState()
                                val onAddToken = {
                                    val trimmed = customInputText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                        customTokens = (customTokens + trimmed).distinct()
                                        activeTokens = (activeTokens + trimmed).distinct()
                                        customInputText = ""
                                        currentPage = DelimiterSheetPage.Main
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(addScrollState)
                                        .padding(horizontal = 24.dp)
                                        .padding(bottom = 12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customInputText,
                                        onValueChange = { customInputText = it },
                                        placeholder = {
                                            Text(
                                                text = stringResource(R.string.delimiter_custom_hint),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { onAddToken() }),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Quick Suggestions
                                    Text(
                                        text = stringResource(R.string.delimiter_quick_suggestions),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        quickSuggestions.forEach { suggestion ->
                                            val isSelected = activeTokens.contains(suggestion)
                                            SuggestionChip(
                                                onClick = {
                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                    customTokens = (customTokens + suggestion).distinct()
                                                    activeTokens = (activeTokens + suggestion).distinct()
                                                    currentPage = DelimiterSheetPage.Main
                                                },
                                                label = {
                                                    Text(
                                                        text = suggestion,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    labelColor = if (isSelected)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }

                                    if (customTokens.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.delimiter_active_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        ) {
                                            customTokens.forEach { token ->
                                                InputChip(
                                                    selected = activeTokens.contains(token),
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                        activeTokens = if (activeTokens.contains(token)) {
                                                            activeTokens.filterNot { it == token }
                                                        } else {
                                                            activeTokens + token
                                                        }
                                                    },
                                                    label = { Text(token) },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = RhythmIcons.Close,
                                                            contentDescription = stringResource(R.string.ui_close),
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable {
                                                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                                                    customTokens = customTokens.filterNot { it == token }
                                                                    activeTokens = activeTokens.filterNot { it == token }
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
                }

                // Sticky action buttons at bottom (Fixed)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    RhythmGroupedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        size = RhythmButtonSize.Large
                    ) {
                        if (currentPage == DelimiterSheetPage.Main) {
                            RhythmButtonWeighted(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                                    activeTokens = ArtistSeparator.parseDelimiters(AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS)
                                },
                                weight = 1f,
                                isFirst = true,
                                icon = MaterialSymbolIcon("restart_alt"),
                                text = context.getString(R.string.bottomsheet_reset)
                            )
                            RhythmButtonWeighted(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                    scope.launch {
                                        val serialized = ArtistSeparator.serializeDelimiters(activeTokens)
                                        appSettings.setArtistSeparatorDelimiters(serialized)
                                        showDelimiterBottomSheet = false
                                    }
                                },
                                weight = 1f,
                                isLast = true,
                                icon = RhythmIcons.Check,
                                text = context.getString(R.string.bottomsheet_save),
                                enabled = activeTokens.isNotEmpty()
                            )
                        } else {
                            val onAddToken = {
                                val trimmed = customInputText.trim()
                                if (trimmed.isNotEmpty()) {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                    customTokens = (customTokens + trimmed).distinct()
                                    activeTokens = (activeTokens + trimmed).distinct()
                                    customInputText = ""
                                    currentPage = DelimiterSheetPage.Main
                                }
                            }

                            RhythmButtonWeighted(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                                    currentPage = DelimiterSheetPage.Main
                                },
                                weight = 1f,
                                isFirst = true,
                                icon = RhythmIcons.Back,
                                text = stringResource(R.string.common_back)
                            )
                            RhythmButtonWeighted(
                                onClick = onAddToken,
                                weight = 1f,
                                isLast = true,
                                icon = RhythmIcons.Add,
                                text = stringResource(R.string.delimiter_add_button),
                                enabled = customInputText.isNotBlank()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DelimiterGridCard(
    symbol: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AddCustomGridCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = RhythmIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.delimiter_add_custom),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}