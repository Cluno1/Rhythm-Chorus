package io.github.cluno1.sonorus.features.catalog.data.local

import io.github.cluno1.sonorus.features.catalog.domain.CatalogLyricsDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogLyricsDraftStoreTest {
    @Test
    fun codecRoundTripsStableCatalogIdentityAndSyncMetadata() {
        val draft = CatalogLyricsDraft(
            namespace = "https://catalog.example|device-1",
            renditionId = "33333333-3333-4333-8333-333333333333",
            language = "zh-Hans",
            lyrics = "[00:01.000]歌词",
            format = "lrc",
            baseRevision = 7,
            status = "pending",
            updatedAtEpochMs = 1234L,
        )

        assertEquals(draft, CatalogLyricsDraftCodec.decode(CatalogLyricsDraftCodec.encode(draft)))
    }

    @Test
    fun codecRejectsCorruptOrUnsupportedDrafts() {
        assertNull(CatalogLyricsDraftCodec.decode("not json"))
        assertNull(
            CatalogLyricsDraftCodec.decode(
                """{"namespace":"n","renditionId":"r","language":"en","lyrics":"x","format":"bad","baseRevision":1,"status":"pending","updatedAtEpochMs":1}""",
            ),
        )
    }
}
