package io.github.cluno1.sonorus.features.catalog.presentation

import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.CatalogScoreOption
import io.github.cluno1.sonorus.shared.data.model.ScoreSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class ScoreLibraryUiModelTest {
    @Test
    fun dualLabelWorkRemainsSingleItemForEachMatchingFilter() {
        val work = scoreWork(
            id = "work-a",
            title = "Alpha",
            options = listOf(
                scoreOption("rough", "ocr", scoreLabel = "粗谱"),
                scoreOption("edited", "midi_transcription", scoreLabel = "精校谱"),
            ),
        )

        assertEquals(1, prepareCatalogScoreWorks(listOf(work), "粗谱", ScoreSortOrder.TITLE_ASC).size)
        assertEquals(1, prepareCatalogScoreWorks(listOf(work), "精校谱", ScoreSortOrder.TITLE_ASC).size)
    }

    @Test
    fun filteredNavigationSelectsAnOptionWithTheActiveBackendLabel() {
        val rough = scoreOption("rough", "ocr", scoreLabel = "粗谱", preferred = true)
        val edited = scoreOption("edited", "midi_transcription", scoreLabel = "精校谱")
        val work = scoreWork(
            id = "work-a",
            title = "Alpha",
            defaultScoreId = rough.scoreId,
            options = listOf(rough, edited),
        )

        assertEquals(edited.scoreId, work.initialOptionFor("精校谱")?.scoreId)
        assertEquals(rough.scoreId, work.initialOptionFor("粗谱")?.scoreId)
    }

    @Test
    fun overviewNavigationUsesBackendCurrentScoreInsteadOfGuessingByPublishedTime() {
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
        assertEquals(olderPreferred.scoreId, work.currentScoreOption()?.scoreId)
        assertEquals(olderPreferred.scoreId, work.initialOptionFor(null)?.scoreId)
        assertEquals(olderPreferred.scoreId, work.resolveInitialScoreId(null))
        assertEquals(olderPreferred.scoreId, work.resolveInitialScoreId("missing"))
        assertEquals(olderPreferred.scoreId, work.resolveInitialScoreId(olderPreferred.scoreId))
    }

    @Test
    fun sortingUsesStableSecondaryTitleOrder() {
        val alpha = scoreWork("a", "Alpha", latestPublishedAt = "2026-01-01", scoreCount = 1)
        val beta = scoreWork("b", "Beta", latestPublishedAt = "2026-02-01", scoreCount = 3)

        assertEquals(
            listOf("a", "b"),
            prepareCatalogScoreWorks(listOf(beta, alpha), null, ScoreSortOrder.TITLE_ASC).map { it.workId },
        )
        assertEquals(
            listOf("b", "a"),
            prepareCatalogScoreWorks(listOf(alpha, beta), null, ScoreSortOrder.PUBLISHED_DESC).map { it.workId },
        )
        assertEquals(
            listOf("b", "a"),
            prepareCatalogScoreWorks(listOf(alpha, beta), null, ScoreSortOrder.SCORE_COUNT_DESC).map { it.workId },
        )
    }

    @Test
    fun availableFiltersUseDistinctBackendLabelsInResponseOrder() {
        val first = scoreWork(
            id = "a",
            title = "Alpha",
            options = listOf(
                scoreOption("rough-a", "ocr", scoreLabel = "粗谱"),
                scoreOption("edited-a", "midi_transcription", scoreLabel = "精校谱"),
            ),
        )
        val second = scoreWork(
            id = "b",
            title = "Beta",
            options = listOf(scoreOption("rough-b", "unexpected_origin", scoreLabel = "粗谱")),
        )

        assertEquals(listOf("粗谱", "精校谱"), availableScoreLabels(listOf(first, second)))
    }

    @Test
    fun publishedTimeWithOffsetUsesDeviceTimeZone() {
        assertEquals(
            "2026-09-09 16:12",
            formatScoreRevisionTime(
                "2026-09-09T08:12:21.937599+00:00",
                ZoneId.of("Asia/Shanghai"),
            ),
        )
    }

    @Test
    fun publishedTimeWithoutOffsetKeepsItsWallClockTime() {
        assertEquals(
            "2026-09-04 19:18",
            formatScoreRevisionTime(
                "2026-09-04 19:18:41.718752",
                ZoneId.of("Asia/Shanghai"),
            ),
        )
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
        scoreLabel: String = "Score $id",
        preferred: Boolean = false,
        publishedAt: String = "2026-01-01",
    ) = CatalogScoreOption(
        arrangementId = "arrangement-$id",
        arrangementName = "Arrangement $id",
        scoreId = "score-$id",
        revisionId = "revision-$id",
        scoreLabel = scoreLabel,
        origin = origin,
        partCount = 1,
        revisionNo = 1,
        publishedAt = publishedAt,
        preferred = preferred,
    )
}
