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
}
