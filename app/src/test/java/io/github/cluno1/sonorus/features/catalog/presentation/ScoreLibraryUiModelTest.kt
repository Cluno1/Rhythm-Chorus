package io.github.cluno1.sonorus.features.catalog.presentation

import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.CatalogScoreOption
import io.github.cluno1.sonorus.shared.data.model.ScoreOriginFilter
import io.github.cluno1.sonorus.shared.data.model.ScoreSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreLibraryUiModelTest {
    @Test
    fun dualSourceWorkRemainsSingleItemForEachMatchingFilter() {
        val work = scoreWork(
            id = "work-a",
            title = "Alpha",
            options = listOf(
                scoreOption("edited", "musicxml_import"),
                scoreOption("midi", "midi_transcription"),
            ),
        )

        assertEquals(1, prepareCatalogScoreWorks(listOf(work), ScoreOriginFilter.EDITED, ScoreSortOrder.TITLE_ASC).size)
        assertEquals(1, prepareCatalogScoreWorks(listOf(work), ScoreOriginFilter.MIDI, ScoreSortOrder.TITLE_ASC).size)
    }

    @Test
    fun filteredNavigationSelectsAnOptionFromTheActiveOrigin() {
        val edited = scoreOption("edited", "musicxml_import", preferred = true)
        val midi = scoreOption("midi", "midi_transcription")
        val work = scoreWork(
            id = "work-a",
            title = "Alpha",
            defaultScoreId = edited.scoreId,
            options = listOf(edited, midi),
        )

        assertEquals(midi.scoreId, work.initialOptionFor(ScoreOriginFilter.MIDI)?.scoreId)
        assertEquals(edited.scoreId, work.initialOptionFor(ScoreOriginFilter.EDITED)?.scoreId)
    }

    @Test
    fun overviewNavigationSelectsLatestPublishedOptionInsteadOfPreferredDefault() {
        val olderPreferred = scoreOption(
            id = "older",
            origin = "musicxml_import",
            preferred = true,
            publishedAt = "2026-09-03T10:00:00",
        )
        val latest = scoreOption(
            id = "latest",
            origin = "musicxml_import",
            publishedAt = "2026-09-04T19:18:41.718752",
        )
        val work = scoreWork(
            id = "work-a",
            title = "Alpha",
            defaultScoreId = olderPreferred.scoreId,
            options = listOf(olderPreferred, latest),
        )

        assertEquals(latest.scoreId, work.latestPublishedOption()?.scoreId)
        assertEquals(latest.scoreId, work.initialOptionFor(ScoreOriginFilter.ALL)?.scoreId)
    }

    @Test
    fun sortingUsesStableSecondaryTitleOrder() {
        val alpha = scoreWork("a", "Alpha", latestPublishedAt = "2026-01-01", scoreCount = 1)
        val beta = scoreWork("b", "Beta", latestPublishedAt = "2026-02-01", scoreCount = 3)

        assertEquals(
            listOf("a", "b"),
            prepareCatalogScoreWorks(listOf(beta, alpha), ScoreOriginFilter.ALL, ScoreSortOrder.TITLE_ASC).map { it.workId },
        )
        assertEquals(
            listOf("b", "a"),
            prepareCatalogScoreWorks(listOf(alpha, beta), ScoreOriginFilter.ALL, ScoreSortOrder.PUBLISHED_DESC).map { it.workId },
        )
        assertEquals(
            listOf("b", "a"),
            prepareCatalogScoreWorks(listOf(alpha, beta), ScoreOriginFilter.ALL, ScoreSortOrder.SCORE_COUNT_DESC).map { it.workId },
        )
    }

    @Test
    fun editedFilterTreatsOnlyMidiTranscriptionAsMidi() {
        val imported = scoreOption("imported", "musicxml_import")
        assertTrue(imported.matchesScoreOrigin(ScoreOriginFilter.EDITED))
    }

    private fun scoreWork(
        id: String,
        title: String,
        latestPublishedAt: String = "2026-01-01",
        scoreCount: Int = 1,
        defaultScoreId: String = "score-$id",
        options: List<CatalogScoreOption> = listOf(scoreOption(id, "musicxml_import")),
    ) = CatalogLibraryScoreWork(
        workId = id,
        title = title,
        artist = "Artist",
        coverAssetId = null,
        coverUrl = null,
        defaultScoreId = defaultScoreId,
        latestPublishedAt = latestPublishedAt,
        scoreCount = scoreCount,
        origins = options.map(CatalogScoreOption::origin).distinct(),
        scoreOptions = options,
    )

    private fun scoreOption(
        id: String,
        origin: String,
        preferred: Boolean = false,
        publishedAt: String = "2026-01-01",
    ) = CatalogScoreOption(
        arrangementId = "arrangement-$id",
        arrangementName = "Arrangement $id",
        scoreId = "score-$id",
        revisionId = "revision-$id",
        scoreLabel = "Score $id",
        origin = origin,
        partCount = 1,
        revisionNo = 1,
        publishedAt = publishedAt,
        preferred = preferred,
    )
}
