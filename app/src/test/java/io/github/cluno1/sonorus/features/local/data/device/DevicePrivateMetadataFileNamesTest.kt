package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePrivateMetadataFileNamesTest {
    @Test fun `different Chinese artists never collapse to underscore filename`() {
        val jay = DevicePrivateMetadataFileNames.artistArtwork("周杰伦")
        val lin = DevicePrivateMetadataFileNames.artistArtwork("林俊杰")

        assertNotEquals(jay, lin)
        assertTrue(jay.matches(Regex("artist-[0-9a-f]{32}\\.jpg")))
    }

    @Test fun `artist identity is normalized before hashing`() {
        assertEquals(
            DevicePrivateMetadataFileNames.artistArtwork("Beyoncé"),
            DevicePrivateMetadataFileNames.artistArtwork("Ｂｅｙｏｎｃé"),
        )
    }

    @Test fun `lyrics prefer stable song id over lossy metadata`() {
        assertNotEquals(
            DevicePrivateMetadataFileNames.lyrics("101", "周杰伦", "晴天"),
            DevicePrivateMetadataFileNames.lyrics("102", "周杰伦", "晴天"),
        )
        assertFalse(DevicePrivateMetadataFileNames.legacyNameWasLossless("周杰伦", "晴天"))
        assertFalse(DevicePrivateMetadataFileNames.legacyNameWasLossless("Artist Name", "Song-1"))
        assertTrue(DevicePrivateMetadataFileNames.legacyNameWasLossless("Artist_Name", "Song-1"))
    }

    @Test fun `only unambiguous legacy artist filenames can migrate`() {
        assertEquals(
            "Artist_Name.jpg",
            DevicePrivateMetadataFileNames.unambiguousLegacyArtistArtwork("Artist_Name"),
        )
        assertEquals(null, DevicePrivateMetadataFileNames.unambiguousLegacyArtistArtwork("Artist Name"))
        assertEquals(null, DevicePrivateMetadataFileNames.unambiguousLegacyArtistArtwork("周杰伦"))
    }
}
