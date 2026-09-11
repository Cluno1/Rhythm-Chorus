package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceArtistFolderArtworkPolicyTest {
    @Test fun `artist named image is scoped in a mixed folder`() {
        assertTrue(DeviceArtistFolderArtworkPolicy.isArtistSpecific("Beyoncé.jpg", "BEYONCE"))
        assertFalse(DeviceArtistFolderArtworkPolicy.isArtistSpecific("artist.jpg", "Beyoncé"))
    }

    @Test fun `generic image is rejected when directory contains several artists`() {
        assertTrue(DeviceArtistFolderArtworkPolicy.isGeneric("artist.webp"))
        assertFalse(
            DeviceArtistFolderArtworkPolicy.allowsGeneric(
                "Artist A",
                setOf("Artist A", "Artist B"),
            )
        )
    }

    @Test fun `generic image is accepted for a single artist directory`() {
        assertTrue(
            DeviceArtistFolderArtworkPolicy.allowsGeneric(
                "Artist A",
                setOf("artist-a"),
            )
        )
    }
}
