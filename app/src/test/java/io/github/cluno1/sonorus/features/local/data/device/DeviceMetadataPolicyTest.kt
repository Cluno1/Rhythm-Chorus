package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMetadataPolicyTest {
    @Test fun `source resolver honors pinned selection and strict local priority`() {
        assertEquals(
            listOf(DeviceMetadataSource.USER_SELECTED, DeviceMetadataSource.EMBEDDED, DeviceMetadataSource.SIBLING, DeviceMetadataSource.CACHE, DeviceMetadataSource.PUBLIC_API),
            DeviceMetadataPolicy.sourcePriority(true)
        )
        assertEquals(DeviceMetadataSource.EMBEDDED, DeviceMetadataPolicy.sourcePriority(false).first())
        assertEquals("selected", DeviceMetadataPolicy.resolveArtwork("selected", "embedded", "cache"))
        assertEquals("embedded", DeviceMetadataPolicy.resolveArtwork(null, "embedded", "cache"))
        assertTrue(DeviceMetadataPolicy.shouldPreservePinnedSelection(existingPinned = true, incomingPinned = false))
        assertFalse(DeviceMetadataPolicy.shouldPreservePinnedSelection(existingPinned = true, incomingPinned = true))
    }

    @Test fun `folder artwork uses the audio stem and matching image type`() {
        assertEquals("Track 01.jpg", DeviceArtworkFolderPolicy.fileName("Track 01.flac", "image/jpeg"))
        assertEquals("Track 01.png", DeviceArtworkFolderPolicy.fileName("Track 01.mp3", "image/png; charset=binary"))
        assertEquals("歌曲.webp", DeviceArtworkFolderPolicy.fileName("歌曲.m4a", "image/webp"))
        assertEquals("Track 01.jpg", DeviceArtworkFolderPolicy.fileName("Track 01.ogg", "application/octet-stream"))
    }

    @Test fun `folder artwork never falls back to a shared cover name`() {
        assertEquals("sonorus-artwork.jpg", DeviceArtworkFolderPolicy.fileName(".mp3", "image/jpeg"))
        assertFalse(DeviceArtworkFolderPolicy.fileName("Song.flac", "image/jpeg").startsWith("cover."))
    }

    @Test fun `catalog and arbitrary network songs never enter device enrichment`() {
        assertFalse(DeviceMetadataPolicy.isEligible("rhythm-catalog:rendition:x", "content"))
        assertFalse(DeviceMetadataPolicy.isEligible("42", "https"))
        assertTrue(DeviceMetadataPolicy.isEligible("42", "content"))
    }

    @Test fun `public request contains metadata fields but no uri or path`() {
        val request = DeviceMetadataRequest("Teenagers", "My Chemical Romance", "The Black Parade", 161)
        assertEquals(setOf("title", "artist", "album", "duration"), request.publicFields())
        assertFalse(request.publicFields().contains("uri"))
        assertFalse(request.publicFields().contains("path"))
    }

    @Test fun `manual query normalization does not mutate the song identity`() {
        val request = DeviceMetadataRequest("  Teenagers  ", "  MCR ", "   ", -1).normalized()
        assertEquals("Teenagers", request.title)
        assertEquals("MCR", request.artist)
        assertNull(request.album)
        assertNull(request.durationSeconds)
        assertEquals(setOf("title", "artist"), request.publicFields())
    }

    @Test fun `single song cache matching cannot select another song`() {
        val key = DeviceMetadataPolicy.cacheKey("42", "Artist", "Title")
        assertTrue(DeviceMetadataPolicy.belongsToSong(key, "42"))
        assertFalse(DeviceMetadataPolicy.belongsToSong(key, "43"))
    }

    @Test fun `deezer artwork policy upgrades http and rejects foreign hosts`() {
        assertEquals("https://e-cdns-images.dzcdn.net/images/cover/a.jpg", DeviceMetadataPolicy.safeDeezerArtworkUrl("http://e-cdns-images.dzcdn.net/images/cover/a.jpg"))
        assertNull(DeviceMetadataPolicy.safeDeezerArtworkUrl("https://example.com/a.jpg"))
        assertTrue(DeviceMetadataPolicy.isImageContentType("image/jpeg; charset=binary"))
        assertFalse(DeviceMetadataPolicy.isImageContentType("text/html"))
    }

    @Test fun `cover art archive redirects stay on explicit image hosts`() {
        assertEquals(
            "https://coverartarchive.org/release/abc/front-500",
            DeviceMetadataPolicy.safeCoverArtUrl("https://coverartarchive.org/release/abc/front-500")
        )
        assertEquals(
            "https://archive.org/download/mbid/cover.jpg",
            DeviceMetadataPolicy.safeCoverArtUrl("https://archive.org/download/mbid/cover.jpg")
        )
        assertNull(DeviceMetadataPolicy.safeCoverArtUrl("http://coverartarchive.org/release/abc/front"))
        assertNull(DeviceMetadataPolicy.safeCoverArtUrl("https://coverartarchive.org.example.com/cover.jpg"))
    }
}
