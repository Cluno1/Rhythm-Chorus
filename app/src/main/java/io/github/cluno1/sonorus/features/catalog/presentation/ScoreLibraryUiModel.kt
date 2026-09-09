package io.github.cluno1.sonorus.features.catalog.presentation

import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.CatalogScoreOption
import io.github.cluno1.sonorus.shared.data.model.ScoreOriginFilter
import io.github.cluno1.sonorus.shared.data.model.ScoreSortOrder

private const val MIDI_TRANSCRIPTION_ORIGIN = "midi_transcription"

fun CatalogScoreOption.matchesScoreOrigin(filter: ScoreOriginFilter): Boolean = when (filter) {
    ScoreOriginFilter.ALL -> true
    ScoreOriginFilter.MIDI -> origin.equals(MIDI_TRANSCRIPTION_ORIGIN, ignoreCase = true)
    ScoreOriginFilter.EDITED -> !origin.equals(MIDI_TRANSCRIPTION_ORIGIN, ignoreCase = true)
}

fun CatalogLibraryScoreWork.matchesScoreOrigin(filter: ScoreOriginFilter): Boolean =
    filter == ScoreOriginFilter.ALL || scoreOptions.any { it.matchesScoreOrigin(filter) }

fun CatalogLibraryScoreWork.latestPublishedOption(): CatalogScoreOption? =
    scoreOptions.maxWithOrNull(
        compareBy<CatalogScoreOption> { it.publishedAt }
            .thenBy { it.revisionNo }
            .thenBy { it.scoreId },
    )

fun CatalogLibraryScoreWork.initialOptionFor(filter: ScoreOriginFilter): CatalogScoreOption? {
    val matching = scoreOptions.filter { it.matchesScoreOrigin(filter) }
    return matching.maxWithOrNull(
        compareBy<CatalogScoreOption> { it.publishedAt }
            .thenBy { it.revisionNo }
            .thenBy { it.scoreId },
    )
}

fun prepareCatalogScoreWorks(
    scoreWorks: List<CatalogLibraryScoreWork>,
    originFilter: ScoreOriginFilter,
    sortOrder: ScoreSortOrder,
): List<CatalogLibraryScoreWork> {
    val filtered = scoreWorks.filter { it.matchesScoreOrigin(originFilter) }
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
