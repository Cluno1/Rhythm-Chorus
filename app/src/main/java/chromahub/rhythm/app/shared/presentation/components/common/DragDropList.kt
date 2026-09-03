/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.HapticType
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun <T> DragDropLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    itemKey: (T) -> Any,
    isReorderableItem: (T) -> Boolean = { true },
    isStickyHeader: (T) -> Boolean = { false },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemSpacing: Dp = 0.dp,
    animateItemPlacement: Boolean = false,
    dragTopInset: Dp = 0.dp,
    dragBottomInset: Dp = 0.dp,
    itemContent: @Composable (item: T, isDragging: Boolean, index: Int) -> Unit
) {
    val edgeThresholdPx = 84f
    val maxAutoScrollSpeedPxPerFrame = 30f

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragTopInsetPx = dragTopInset.value * density.density
    val dragBottomInsetPx = dragBottomInset.value * density.density

    val currentItems = rememberUpdatedState(items)
    val currentOnMove = rememberUpdatedState(onMove)
    val currentReorderable = rememberUpdatedState(isReorderableItem)

    var isDragging by remember { mutableStateOf(false) }
    var isDropping by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var draggedItem by remember { mutableStateOf<T?>(null) }
    var dragRowHeightPx by remember { mutableFloatStateOf(0f) }
    var grabOffsetPx by remember { mutableFloatStateOf(0f) }
    var lastPointerY by remember { mutableFloatStateOf(0f) }
    var boxHeightPx by remember { mutableFloatStateOf(0f) }
    var reorderMoved by remember { mutableStateOf(false) }

    val overlayTop = remember { Animatable(0f) }

    fun isReorderableAt(index: Int): Boolean {
        val list = currentItems.value
        return index in list.indices && currentReorderable.value(list[index])
    }

    fun visibleInfoAtY(y: Float): LazyListItemInfo? =
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            y >= info.offset && y <= info.offset + info.size
        }

    fun visibleInfoOfIndex(index: Int): LazyListItemInfo? =
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    fun reorderableRange(): IntRange? {
        val list = currentItems.value
        var first = -1
        var last = -1
        for (i in list.indices) {
            if (isReorderableAt(i)) {
                if (first == -1) first = i
                last = i
            }
        }
        return if (first == -1) null else first..last
    }

    fun isDragLayoutSynced(): Boolean {
        val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggedKey }
        return info == null || info.index == draggedIndex
    }

    fun clampEdges(
        visible: List<LazyListItemInfo>,
        range: IntRange,
        containerBottomLimit: Float
    ): Pair<Float, Float> {
        val top = visible.minOfOrNull { it.offset }
            ?.let { maxOf(dragTopInsetPx, it.toFloat()) }
            ?: dragTopInsetPx
        val bottom = when {
            visible.isEmpty() -> containerBottomLimit
            visible.maxOf { it.index } >= range.last -> {
                val lastBottom = visible.maxOf { it.offset + it.size }
                (lastBottom - dragRowHeightPx).coerceIn(dragTopInsetPx, containerBottomLimit)
            }
            else -> containerBottomLimit
        }
        return top to bottom
    }

    fun finishDrag() {
        isDragging = false
        isDropping = false
        draggedIndex = -1
        draggedKey = null
        draggedItem = null
        if (reorderMoved) {
            HapticUtils.performHapticFeedback(context, haptic, HapticType.MEDIUM)
            reorderMoved = false
        }
    }

    suspend fun advanceDragFrame() {
        val range = reorderableRange() ?: return
        if (draggedIndex !in range) return

        val layout = lazyListState.layoutInfo
        val visible = layout.visibleItemsInfo.filter { isReorderableAt(it.index) }
        val topVisibleIndex = visible.minOfOrNull { it.index }
        val bottomVisibleIndex = visible.maxOfOrNull { it.index }
        val containerBottomLimit =
            (boxHeightPx - dragBottomInsetPx - dragRowHeightPx).coerceAtLeast(0f)

        val (topEdge, bottomEdge) = clampEdges(visible, range, containerBottomLimit)
        val desiredTop = lastPointerY - grabOffsetPx

        val pushUp = (topEdge - desiredTop).coerceAtLeast(0f)
        val pushDown = (desiredTop - bottomEdge).coerceAtLeast(0f)
        val canScrollUp =
            topVisibleIndex != null && topVisibleIndex > range.first && draggedIndex > range.first
        val canScrollDown =
            bottomVisibleIndex != null && bottomVisibleIndex < range.last && draggedIndex < range.last
        val scrollSpeed = when {
            pushUp > 0f && canScrollUp ->
                -maxAutoScrollSpeedPxPerFrame * (pushUp / edgeThresholdPx).coerceIn(0f, 1f)

            pushDown > 0f && canScrollDown ->
                maxAutoScrollSpeedPxPerFrame * (pushDown / edgeThresholdPx).coerceIn(0f, 1f)

            else -> 0f
        }

        if (scrollSpeed != 0f) {
            lazyListState.scrollBy(scrollSpeed)
        }

        val freshVisible = lazyListState.layoutInfo.visibleItemsInfo.filter { isReorderableAt(it.index) }
        val (finalTopEdge, finalBottomEdge) =
            if (scrollSpeed != 0f) clampEdges(freshVisible, range, containerBottomLimit)
            else topEdge to bottomEdge
        val cardTop = desiredTop.coerceIn(finalTopEdge, finalBottomEdge)
        overlayTop.snapTo(cardTop)

        if (isDragLayoutSynced()) {
            val hovered = visibleInfoAtY(cardTop + dragRowHeightPx / 2f)
            if (hovered != null && hovered.index != draggedIndex && isReorderableAt(hovered.index)) {
                reorderMoved = true
                HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                val fromIndex = draggedIndex
                currentOnMove.value(fromIndex, hovered.index)
                draggedIndex = hovered.index
            }
        }
    }

    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        while (isDragging && !isDropping) {
            advanceDragFrame()
            withFrameNanos { }
        }
    }

    fun settleDrop() {
        if (!isDragging || isDropping) return
        isDropping = true
        scope.launch {
            for (i in 0 until 10) {
                withFrameNanos { }
                if (!lazyListState.isScrollInProgress && isDragLayoutSynced()) break
            }
            val slotTop = visibleInfoOfIndex(draggedIndex)?.offset?.toFloat()
            if (slotTop != null && abs(slotTop - overlayTop.value) > 1f) {
                overlayTop.animateTo(slotTop, animationSpec = tween(durationMillis = 140))
            }
            finishDrag()
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { boxHeightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (isDragging || isDropping) return@detectDragGesturesAfterLongPress
                        val info = visibleInfoAtY(offset.y) ?: return@detectDragGesturesAfterLongPress
                        if (!isReorderableAt(info.index)) return@detectDragGesturesAfterLongPress
                        draggedIndex = info.index
                        draggedKey = itemKey(currentItems.value[info.index])
                        draggedItem = currentItems.value[info.index]
                        dragRowHeightPx = info.size.toFloat()
                        grabOffsetPx = (offset.y - info.offset).coerceIn(0f, info.size.toFloat())
                        lastPointerY = offset.y
                        reorderMoved = false
                        scope.launch { overlayTop.snapTo(info.offset.toFloat()) }
                        HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                        isDragging = true
                    },
                    onDrag = { change, _ ->
                        if (isDragging && !isDropping) {
                            change.consume()
                            lastPointerY = change.position.y
                        }
                    },
                    onDragEnd = {
                        settleDrop()
                    },
                    onDragCancel = {
                        settleDrop()
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            state = lazyListState
        ) {
            items.forEachIndexed { index, item ->
                val key = itemKey(item)
                if (isStickyHeader(item)) {
                    stickyHeader(key = key) {
                        itemContent(item, false, index)
                    }
                } else {
                    item(key = key) {
                        val isHiddenInList = isDragging && index == draggedIndex
                        val isCurrentlyDragged = isDragging && index == draggedIndex

                        val placementModifier = if (animateItemPlacement) {
                            Modifier.animateItem(
                                fadeInSpec = tween(durationMillis = 0),
                                placementSpec = if (isDragging) {
                                    tween(durationMillis = 0)
                                } else {
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                },
                                fadeOutSpec = tween(durationMillis = 0)
                            )
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .zIndex(if (isCurrentlyDragged) 1f else 0f)
                                .then(placementModifier)
                                .graphicsLayer {
                                    alpha = if (isHiddenInList) 0f else 1f
                                }
                        ) {
                            itemContent(item, isCurrentlyDragged, index)
                        }
                    }
                }
            }
        }

        val overlayItem = draggedItem
        if (isDragging && overlayItem != null) {                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(6f)
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = overlayTop.value
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                ) {
                    itemContent(overlayItem, true, draggedIndex)
                }
            }
        }
    }
}
