package chromahub.rhythm.app.features.catalog.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogDtoMapperTest {
    private val workId = "11111111-1111-4111-8111-111111111111"
    private val arrangementId = "22222222-2222-4222-8222-222222222222"
    private val renditionId = "33333333-3333-4333-8333-333333333333"
    private val renditionAssetId = "44444444-4444-4444-8444-444444444444"
    private val assetId = "55555555-5555-4555-8555-555555555555"
    private val hash = "a".repeat(64)

    @Test
    fun mapsAndValidatesPlaybackIdentity() {
        val result = CatalogDtoMapper.playback(
            PlaybackDto(
                renditionId, assetId, "audio/mpeg", 12, "authenticated_url",
                "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "\"sha256:$hash\"", true, null,
            ),
        )
        assertEquals(assetId, result.assetId)
        assertEquals("rhythm:asset:$assetId:$hash", result.cacheKey)
    }

    @Test
    fun rejectsMidiPlaybackDescriptor() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/midi", 12, "authenticated_url",
                    "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "etag", true, null,
                ),
            )
        }
    }

    @Test
    fun rejectsPlaybackCacheKeyForAnotherAsset() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/mpeg", 12, "authenticated_url",
                    "/v2/assets/$assetId/content",
                    "rhythm:asset:66666666-6666-4666-8666-666666666666:$hash", "etag", true, null,
                ),
            )
        }
    }

    @Test
    fun rejectsCrossArrangementRelationships() {
        val work = WorkDto(workId, "Title", null, "active", 1, emptyList(), emptyList(), "now", "now")
        val rendition = RenditionDto(
            renditionId, "77777777-7777-4777-8777-777777777777", "Recording", "audio", null, null, null,
            1000, 1,
            listOf(RenditionAssetDto(renditionAssetId, assetId, "stream", null, null, hash, 12, "audio/mpeg")),
        )
        val arrangement = ArrangementDto(
            arrangementId, workId, "Main", null, null, null, null, 1,
            emptyList(), emptyList(), listOf(rendition),
        )
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.bundle(WorkBundleDto(work, listOf(arrangement), 1))
        }
    }
    @Test
    fun mapsSignedUrlDelivery() {
        val cosUrl =
            "https://bible-1328751369.cos.ap-guangzhou.myqcloud.com/music/221.mp3?q-signature=x"
        val result = CatalogDtoMapper.playback(
            PlaybackDto(
                renditionId, assetId, "audio/mpeg", 12, "signed_url",
                cosUrl, "rhythm:asset:$assetId:$hash", "\"sha256:$hash\"", true,
                "2026-09-04T10:00:00Z",
            ),
        )
        assertEquals("signed_url", result.delivery)
        assertEquals(cosUrl, result.relativeUrl)
    }

    @Test
    fun rejectsUnknownDeliveryMode() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/mpeg", 12, "public_link",
                    "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "etag", true, null,
                ),
            )
        }
    }
}
