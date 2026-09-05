/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.cluno1.sonorus.shared.presentation.components.dialogs

import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer
import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.SheetAdaptiveType

import io.github.cluno1.sonorus.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
import io.github.cluno1.sonorus.shared.presentation.components.icons.MaterialSymbolIcon
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.cluno1.sonorus.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

private data class BetaFeature(
    val icon: MaterialSymbolIcon,
    val title: String,
    val description: String
)

@Composable
fun BetaProgramPopup(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        val haptic = LocalHapticFeedback.current
        val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))

        val betaFeatures = listOf(
            BetaFeature(
                icon = MaterialSymbolIcon("flight_takeoff", filled = true),
                title = stringResource(R.string.betaprogrampopup_early_access),
                description = stringResource(R.string.betaprogrampopup_early_access_desc)
            ),
            BetaFeature(
                icon = MaterialSymbolIcon("edit_note", filled = true),
                title = stringResource(R.string.betaprogrampopup_shape_the_future),
                description = stringResource(R.string.betaprogrampopup_shape_the_future_desc)
            ),
            BetaFeature(
                icon = MaterialSymbolIcon("message", filled = true),
                title = stringResource(R.string.betaprogrampopup_direct_feedback),
                description = stringResource(R.string.betaprogrampopup_direct_feedback_desc)
            ),
        )

        RhythmAdaptiveModalSheet(
            adaptiveType = SheetAdaptiveType.AUTO_DIALOG,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
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
            StandardBottomSheetHeader(
                title = stringResource(R.string.beta_program),
                subtitle = stringResource(R.string.betaprogrampopup_youre_part_of_an),
                visible = true
            )

            val scrollState = rememberScrollState()

            AdaptiveSheetScrollContainer(
                scrollState = scrollState,
                modifier = Modifier.fillMaxWidth()
            ) { endPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(start = 24.dp, end = 24.dp + endPadding, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        betaFeatures.forEach { feature ->
                            BetaFeatureCard(feature = feature)
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Button (Pinned at bottom)
            Box(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.betaprogrampopup_start_exploring),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BetaFeatureCard(feature: BetaFeature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = feature.title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
