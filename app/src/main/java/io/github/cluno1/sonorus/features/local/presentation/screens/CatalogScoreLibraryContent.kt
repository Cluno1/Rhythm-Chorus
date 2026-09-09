package io.github.cluno1.sonorus.features.local.presentation.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.CatalogScoreOption
import io.github.cluno1.sonorus.features.catalog.presentation.components.ScoreWorkGridCard
import io.github.cluno1.sonorus.features.catalog.presentation.components.ScoreWorkListItem
import io.github.cluno1.sonorus.features.catalog.presentation.availableScoreLabels
import io.github.cluno1.sonorus.features.catalog.presentation.initialOptionFor
import io.github.cluno1.sonorus.features.catalog.presentation.prepareCatalogScoreWorks
import io.github.cluno1.sonorus.shared.data.model.ScoreSortOrder
import io.github.cluno1.sonorus.shared.data.model.ScoreViewType
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveScrollBar
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
import io.github.cluno1.sonorus.util.HapticType
import io.github.cluno1.sonorus.util.HapticUtils

@Composable
internal fun CatalogScoreLibraryContent(
    scoreWorks: List<CatalogLibraryScoreWork>,
    trustedServerUrl: String?,
    viewType: ScoreViewType,
    sortOrder: ScoreSortOrder,
    scoreLabelFilter: String?,
    onScoreLabelFilterChange: (String?) -> Unit,
    onScoreWorkClick: (CatalogLibraryScoreWork, CatalogScoreOption?) -> Unit,
    listState: LazyListState,
    gridState: LazyGridState,
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scoreLabels = remember(scoreWorks) { availableScoreLabels(scoreWorks) }
    val activeScoreLabel = scoreLabelFilter?.takeIf(scoreLabels::contains)
    val scoreLabelFilters = remember(scoreLabels) { listOf<String?>(null) + scoreLabels }
    val preparedScoreWorks = remember(scoreWorks, activeScoreLabel, sortOrder) {
        prepareCatalogScoreWorks(scoreWorks, activeScoreLabel, sortOrder)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (scoreWorks.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = scoreLabelFilters, key = { it?.let { label -> "label:$label" } ?: "all_scores" }) { filter ->
                    val selected = filter == activeScoreLabel
                    FilterChip(
                        selected = selected,
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                            onScoreLabelFilterChange(filter)
                        },
                        label = {
                            Text(
                                text = filter ?: stringResource(R.string.catalog_score_filter_all),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = RhythmIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else null,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (preparedScoreWorks.isEmpty()) {
                ScoreLibraryEmptyState()
                return@Box
            }

            val canScroll by remember(listState, gridState, viewType) {
                derivedStateOf {
                    if (viewType == ScoreViewType.GRID) gridState.canScrollForward || gridState.canScrollBackward
                    else listState.canScrollForward || listState.canScrollBackward
                }
            }
            val animatedEndPadding by animateDpAsState(
                targetValue = if (canScroll) 36.dp else 16.dp,
                animationSpec = tween(durationMillis = 200),
                label = "scoreLibraryEndPadding",
            )
            val fastScrollLabelProvider = remember(preparedScoreWorks, sortOrder) {
                { index: Int ->
                    preparedScoreWorks.getOrNull(index)?.let { work ->
                        when (sortOrder) {
                            ScoreSortOrder.TITLE_ASC, ScoreSortOrder.TITLE_DESC -> work.title.firstOrNull()?.uppercase()
                            ScoreSortOrder.PUBLISHED_ASC, ScoreSortOrder.PUBLISHED_DESC -> work.latestPublishedAt.take(10)
                            ScoreSortOrder.SCORE_COUNT_ASC, ScoreSortOrder.SCORE_COUNT_DESC -> work.scoreCount.toString()
                        }
                    }
                }
            }

            if (viewType == ScoreViewType.GRID) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = animatedEndPadding,
                        top = 8.dp,
                        bottom = bottomPadding + 80.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(
                        items = preparedScoreWorks,
                        key = { _, work -> work.workId },
                        contentType = { _, _ -> "score_work" },
                    ) { _, work ->
                        ScoreWorkGridCard(
                            work = work,
                            trustedServerUrl = trustedServerUrl,
                            onClick = { onScoreWorkClick(work, work.initialOptionFor(activeScoreLabel)) },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                        )
                    }
                }
                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
                    gridState = gridState,
                    visible = canScroll,
                    dragLabelProvider = fastScrollLabelProvider,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = animatedEndPadding,
                        top = 8.dp,
                        bottom = bottomPadding + 80.dp,
                    ),
                ) {
                    itemsIndexed(
                        items = preparedScoreWorks,
                        key = { _, work -> work.workId },
                        contentType = { _, _ -> "score_work" },
                    ) { index, work ->
                        ScoreWorkListItem(
                            work = work,
                            trustedServerUrl = trustedServerUrl,
                            onClick = { onScoreWorkClick(work, work.initialOptionFor(activeScoreLabel)) },
                            modifier = Modifier.animateItem(),
                            shape = groupedLibraryItemShape(index, preparedScoreWorks.size),
                        )
                    }
                }
                ExpressiveScrollBar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
                    listState = listState,
                    visible = canScroll,
                    dragLabelProvider = fastScrollLabelProvider,
                )
            }
        }
    }
}

@Composable
private fun ScoreLibraryEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = RhythmIcons.Score,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.catalog_scores_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
