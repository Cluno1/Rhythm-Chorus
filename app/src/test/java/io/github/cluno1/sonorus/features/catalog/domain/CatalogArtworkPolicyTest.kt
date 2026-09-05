package io.github.cluno1.sonorus.features.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogArtworkPolicyTest {
    private val assetId = "22222222-2222-4222-8222-222222222222"

    @Test
    fun roundTripsStableCatalogAssetUri() {
        val uri = CatalogArtworkPolicy.uri(assetId)
        assertEquals("rhythm-catalog://asset/$assetId", uri)
        assertEquals(assetId, CatalogArtworkPolicy.assetId(uri))
    }

    @Test
    fun rejectsUnmanagedOrDecoratedUris() {
        assertNull(CatalogArtworkPolicy.assetId("https://example.com/$assetId"))
        assertNull(CatalogArtworkPolicy.assetId("rhythm-catalog://rendition/$assetId"))
        assertNull(CatalogArtworkPolicy.assetId("rhythm-catalog://asset/$assetId?url=https://evil.example"))
        assertNull(CatalogArtworkPolicy.assetId("rhythm-catalog://asset/not-a-uuid"))
    }
}
