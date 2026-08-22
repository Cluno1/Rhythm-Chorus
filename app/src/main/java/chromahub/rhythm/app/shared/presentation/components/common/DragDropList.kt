/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import kotlinx.coroutines.launch

@Composable
fun <T> DragDropLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    itemKey: (T) -> Any,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (item: T, isDragging: Boolean, index: Int) -> Unit
) {
    val edgeThresholdPx = 84f
    val maxAutoScrollSpeedPxPerFrame = 30f

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    var draggedItem by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }
    var autoScrollSpeedPx by remember { mutableFloatStateOf(0f) }
    
    val animatedOffset = remember { Animatable(0f) }
    
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            autoScrollSpeedPx = 0f
            animatedOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring()
            )
        }
    }

    LaunchedEffect(isDragging) {
        while (isDragging) {
            val speed = autoScrollSpeedPx
            if (speed != 0f) {
                val consumed = lazyListState.scrollBy(speed)
                if (consumed != 0f) {
                    draggedDistance += consumed
                    animatedOffset.snapTo(draggedDistance)
                }
            }
            withFrameNanos { }
        }
    }
    
    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .find { info ->
                            offset.y >= info.offset && offset.y <= info.offset + info.size
                        }
                    itemInfo?.let { info ->
                        draggedItem = info.index
                        isDragging = true
                        HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                    }
                },
                onDrag = { change, dragAmount ->
                    if (isDragging) {
                        change.consume()
                        draggedDistance += dragAmount.y
                        scope.launch {
                            animatedOffset.snapTo(draggedDistance)
                        }

                        val viewportStart = lazyListState.layoutInfo.viewportStartOffset.toFloat()
                        val viewportEnd = lazyListState.layoutInfo.viewportEndOffset.toFloat()
                        val pointerY = change.position.y

                        autoScrollSpeedPx = when {
                            pointerY < viewportStart + edgeThresholdPx -> {
                                val intensity = ((viewportStart + edgeThresholdPx - pointerY) / edgeThresholdPx)
                                    .coerceIn(0f, 1f)
                                -maxAutoScrollSpeedPxPerFrame * intensity
                            }

                            pointerY > viewportEnd - edgeThresholdPx -> {
                                val intensity = ((pointerY - (viewportEnd - edgeThresholdPx)) / edgeThresholdPx)
                                    .coerceIn(0f, 1f)
                                maxAutoScrollSpeedPxPerFrame * intensity
                            }

                            else -> 0f
                        }
                        
                        val itemSize = lazyListState.layoutInfo.visibleItemsInfo
                            .firstOrNull()?.size ?: 0
                        
                        if (itemSize > 0) {
                            val targetIndex = draggedItem + (draggedDistance / itemSize).toInt()
                            val clampedIndex = targetIndex.coerceIn(0, items.size - 1)
                            
                            if (clampedIndex != draggedItem && clampedIndex in items.indices) {
                                onMove(draggedItem, clampedIndex)
                                draggedItem = clampedIndex
                                draggedDistance = 0f
                                scope.launch {
                                    animatedOffset.snapTo(0f)
                                }
                            }
                        }
                    }
                },
                onDragEnd = {
                    isDragging = false
                    autoScrollSpeedPx = 0f
                    draggedItem = -1
                    draggedDistance = 0f
                }
            )
        },
        contentPadding = contentPadding,
        state = lazyListState
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> itemKey(item) }
        ) { index, item ->
            val isCurrentlyDragged = isDragging && index == draggedItem
            
            Box(
                modifier = Modifier
                    .zIndex(if (isCurrentlyDragged) 1f else 0f)
                    .graphicsLayer {
                        if (isCurrentlyDragged) {
                            translationY = animatedOffset.value
                            shadowElevation = 0f
                            alpha = 0.9f
                        }
                    }
            ) {
                itemContent(item, isCurrentlyDragged, index)
            }
        }
    }
}
