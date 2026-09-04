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
                "audio/mpeg",
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
                "audio/mpeg",
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "file:///sdcard/song.mp3",
                cacheKey,
                "audio/mpeg",
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://unrelated.example/song.mp3",
                cacheKey,
                "audio/mpeg",
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
                "audio/mpeg",
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                "local-song-id",
                "https://music.example/v2/assets/$assetId/content",
                cacheKey,
                "audio/mpeg",
                "https://music.example",
            ),
        )
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://evil.example/v2/assets/$assetId/content",
                cacheKey,
                "audio/mpeg",
                "https://music.example",
            ),
        )
    }

    @Test
    fun acceptsExplicitRealAudioFormatsAndParameters() {
        assertTrue(CatalogPlaybackPolicy.isPlayableMediaType("audio/mpeg"))
        assertTrue(CatalogPlaybackPolicy.isPlayableMediaType("audio/mp4; codecs=mp4a.40.2"))
        assertTrue(CatalogPlaybackPolicy.isPlayableMediaType("audio/flac"))
        assertTrue(CatalogPlaybackPolicy.isPlayableMediaType("audio/ogg; codecs=opus"))
        assertTrue(CatalogPlaybackPolicy.isPlayableMediaType("audio/wav"))
    }

    @Test
    fun rejectsMidiAndGenericOrUnknownAudioTypes() {
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType("audio/midi"))
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType("audio/x-midi"))
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType("application/x-midi"))
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType("audio/*"))
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType("audio/unknown"))
        assertFalse(CatalogPlaybackPolicy.isPlayableMediaType(null))
        assertFalse(
            CatalogPlaybackPolicy.allows(
                mediaId,
                "https://music.example/v2/assets/$assetId/content",
                cacheKey,
                "audio/midi",
                "https://music.example",
            ),
        )
    }

    @Test
    fun renditionRequiresPlayableRoleAndRealAudioMime() {
        val mp3Master = RenditionAsset(
            id = "33333333-3333-4333-8333-333333333333",
            assetId = assetId,
            role = "master",
            partId = null,
            codecProfile = "mp3",
            sha256 = "a".repeat(64),
            byteSize = 1024,
            mediaType = "audio/mpeg",
        )
        assertTrue(CatalogPlaybackPolicy.isPlayableRenditionAsset(mp3Master))
        assertFalse(CatalogPlaybackPolicy.isPlayableRenditionAsset(mp3Master.copy(role = "source")))
        assertFalse(CatalogPlaybackPolicy.isPlayableRenditionAsset(mp3Master.copy(mediaType = "audio/midi")))
    }
}
