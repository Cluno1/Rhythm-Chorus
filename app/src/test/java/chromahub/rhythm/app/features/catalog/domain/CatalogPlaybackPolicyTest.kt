package chromahub.rhythm.app.features.catalog.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPlaybackPolicyTest {
    private val renditionId = "11111111-1111-4111-8111-111111111111"
    private val assetId = "22222222-2222-4222-8222-222222222222"
    private val mediaId = "rhythm-catalog:rendition:$renditionId:asset:$assetId"
    private val cacheKey = "rhythm:asset:$assetId:${"a".repeat(64)}"

    @Test
    fun acceptsBackendManagedRenditionAsset() {
        assertTrue(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://music.example/v2/assets/$assetId/content",
                cacheKey,
                "https://music.example",
            ),
        )
    }

    @Test
    fun rejectsDeviceAndArbitraryNetworkSources() {
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "content://media/audio/42",
                cacheKey,
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "file:///sdcard/song.mp3",
                cacheKey,
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://unrelated.example/song.mp3",
                cacheKey,
                "https://music.example",
            ),
        )
    }

    @Test
    fun rejectsMismatchedOrUnmanagedAssetIdentity() {
        val otherAsset = "33333333-3333-4333-8333-333333333333"
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://music.example/v2/assets/$otherAsset/content",
                cacheKey,
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                "local-song-id",
                "https://music.example/v2/assets/$assetId/content",
                cacheKey,
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://evil.example/v2/assets/$assetId/content",
                cacheKey,
                "https://music.example",
            ),
        )
    }
}
