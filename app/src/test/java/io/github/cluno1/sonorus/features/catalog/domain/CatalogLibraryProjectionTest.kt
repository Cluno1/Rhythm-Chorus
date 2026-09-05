package io.github.cluno1.sonorus.features.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLibraryProjectionTest {
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
}
