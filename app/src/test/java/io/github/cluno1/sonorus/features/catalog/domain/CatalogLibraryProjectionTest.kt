package io.github.cluno1.sonorus.features.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLibraryProjectionTest {
    @Test
    fun clientScoreIgnoresAnUnpublishedPreferredDraft() {
        val draft = score(id = "draft", head = "head-2", published = null)
        val published = score(id = "published", head = "head-3", published = "published-2")
        val arrangement = arrangement(preferredScoreId = draft.id, scores = listOf(draft, published))

        assertEquals(published, arrangement.clientScore())
        assertEquals("published-2", arrangement.clientScore()?.clientRevisionId)
    }

    @Test
    fun clientScoreUsesThePublishedRevisionInsteadOfANewerHead() {
        val score = score(id = "preferred", head = "head-3", published = "published-2")
        val arrangement = arrangement(preferredScoreId = score.id, scores = listOf(score))

        assertEquals("published-2", arrangement.clientScore()?.clientRevisionId)
    }

    @Test
    fun clientScoreIsUnavailableWhenAllScoresAreDrafts() {
        val draft = score(id = "draft", head = "head-1", published = null)

        assertEquals(null, arrangement(scores = listOf(draft)).clientScore())
    }

    @Test
    fun mixedLegacyQueueKeepsCatalogSubsetAndRebasesStartIndex() {
        val localA = "42"
        val catalogA = "${CATALOG_SONG_ID_PREFIX}11111111-1111-4111-8111-111111111111"
        val localB = "43"
        val catalogB = "${CATALOG_SONG_ID_PREFIX}22222222-2222-4222-8222-222222222222"

        val selection = catalogQueueSelectionIndexes(
            listOf(localA, catalogA, localB, catalogB),
            requestedStartIndex = 3,
        )

        assertEquals(listOf(1, 3), selection.sourceIndexes)
        assertEquals(1, selection.startIndex)
    }

    @Test
    fun localOnlyQueueProducesNoPlayableCatalogItems() {
        val selection = catalogQueueSelectionIndexes(listOf("42", "43"), requestedStartIndex = 0)

        assertEquals(emptyList<Int>(), selection.sourceIndexes)
        assertEquals(0, selection.startIndex)
    }

    @Test
    fun catalogFavoriteIdentityDropsTemporaryAssetSuffix() {
        val renditionId = "11111111-1111-4111-8111-111111111111"
        val playbackId = "${CATALOG_SONG_ID_PREFIX}$renditionId:asset:temporary-asset"

        assertEquals("${CATALOG_SONG_ID_PREFIX}$renditionId", playbackId.toStableCatalogSongId())
    }

    @Test
    fun deviceFavoriteIdentityIsUnchanged() {
        assertEquals("42", "42".toStableCatalogSongId())
    }

    private fun score(id: String, head: String?, published: String?) = Score(
        id = id,
        arrangementId = "arrangement",
        label = id,
        origin = "manual",
        derivedFromRevisionId = null,
        headRevisionId = head,
        publishedRevisionId = published,
        revision = 1,
    )

    private fun arrangement(
        preferredScoreId: String? = null,
        scores: List<Score>,
    ) = Arrangement(
        id = "arrangement",
        workId = "work",
        name = "SATB",
        voicing = "SATB",
        keySignature = null,
        basedOnId = null,
        preferredScoreId = preferredScoreId,
        revision = 1,
        parts = emptyList(),
        scores = scores,
        renditions = emptyList(),
    )
}
