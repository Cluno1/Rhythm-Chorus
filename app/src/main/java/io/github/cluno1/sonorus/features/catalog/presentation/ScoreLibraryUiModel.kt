package io.github.cluno1.sonorus.features.catalog.presentation

import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.CatalogScoreOption
import io.github.cluno1.sonorus.shared.data.model.ScoreSortOrder

fun availableScoreLabels(scoreWorks: List<CatalogLibraryScoreWork>): List<String> =
    scoreWorks.flatMap { work -> work.scoreOptions.map(CatalogScoreOption::scoreLabel) }.distinct()

fun CatalogScoreOption.matchesScoreLabel(scoreLabel: String?): Boolean =
    scoreLabel == null || this.scoreLabel == scoreLabel

fun CatalogLibraryScoreWork.matchesScoreLabel(scoreLabel: String?): Boolean =
    scoreLabel == null || scoreOptions.any { it.matchesScoreLabel(scoreLabel) }

fun CatalogLibraryScoreWork.latestPublishedOption(): CatalogScoreOption? =
    scoreOptions.maxWithOrNull(
        compareBy<CatalogScoreOption> { it.publishedAt }
            .thenBy { it.revisionNo }
            .thenBy { it.scoreId },
    )

fun CatalogLibraryScoreWork.initialOptionFor(scoreLabel: String?): CatalogScoreOption? {
    val matching = scoreOptions.filter { it.matchesScoreLabel(scoreLabel) }
    return matching.maxWithOrNull(
        compareBy<CatalogScoreOption> { it.publishedAt }
            .thenBy { it.revisionNo }
            .thenBy { it.scoreId },
    )
}

fun prepareCatalogScoreWorks(
    scoreWorks: List<CatalogLibraryScoreWork>,
    scoreLabelFilter: String?,
    sortOrder: ScoreSortOrder,
): List<CatalogLibraryScoreWork> {
    val filtered = scoreWorks.filter { it.matchesScoreLabel(scoreLabelFilter) }
    return when (sortOrder) {
        ScoreSortOrder.TITLE_ASC -> filtered.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title)
                .thenBy(CatalogLibraryScoreWork::workId),
        )
        ScoreSortOrder.TITLE_DESC -> filtered.sortedWith(
            compareByDescending(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title)
                .thenBy(CatalogLibraryScoreWork::workId),
        )
        ScoreSortOrder.PUBLISHED_ASC -> filtered.sortedWith(
            compareBy<CatalogLibraryScoreWork> { it.latestPublishedAt }
                .thenBy(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title),
        )
        ScoreSortOrder.PUBLISHED_DESC -> filtered.sortedWith(
            compareByDescending<CatalogLibraryScoreWork> { it.latestPublishedAt }
                .thenBy(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title),
        )
        ScoreSortOrder.SCORE_COUNT_ASC -> filtered.sortedWith(
            compareBy<CatalogLibraryScoreWork> { it.scoreCount }
                .thenBy(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title),
        )
        ScoreSortOrder.SCORE_COUNT_DESC -> filtered.sortedWith(
            compareByDescending<CatalogLibraryScoreWork> { it.scoreCount }
                .thenBy(String.CASE_INSENSITIVE_ORDER, CatalogLibraryScoreWork::title),
        )
    }
}
