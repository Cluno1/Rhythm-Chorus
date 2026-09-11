package io.github.cluno1.sonorus.features.local.presentation.navigation

import io.github.cluno1.sonorus.features.local.data.device.DeviceManualMetadataKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceManualMetadataNavigationTest {
    @Test
    fun `same song and kind is recognized as a duplicate destination`() {
        assertTrue(
            isSameDeviceManualMetadataDestination(
                currentRoute = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                routePattern = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                currentSongId = "42",
                currentKind = "LYRICS",
                currentArtistName = null,
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.LYRICS,
                targetArtistName = null,
            ),
        )
        assertFalse(
            isSameDeviceManualMetadataDestination(
                currentRoute = "player",
                routePattern = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                currentSongId = null,
                currentKind = null,
                currentArtistName = null,
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.LYRICS,
                targetArtistName = null,
            ),
        )
    }

    @Test
    fun `artist destination also compares the target artist`() {
        assertTrue(
            isSameDeviceManualMetadataDestination(
                currentRoute = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                routePattern = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                currentSongId = "42",
                currentKind = "ARTIST_ARTWORK",
                currentArtistName = "Artist A",
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.ARTIST_ARTWORK,
                targetArtistName = "Artist A",
            ),
        )
        assertFalse(
            isSameDeviceManualMetadataDestination(
                currentRoute = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                routePattern = "device_manual_metadata/{songId}?kind={kind}&artistName={artistName}",
                currentSongId = "42",
                currentKind = "ARTIST_ARTWORK",
                currentArtistName = "Artist A",
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.ARTIST_ARTWORK,
                targetArtistName = "Artist B",
            ),
        )
    }

    @Test
    fun `sheet transition hides and dismisses before navigating`() = runBlocking {
        val events = mutableListOf<String>()

        closeSheetThenNavigateToDeviceManualMetadata(
            hideSheet = { events += "hide" },
            dismissSheet = { events += "dismiss" },
            navigate = { events += "navigate" },
        )

        assertEquals(listOf("hide", "dismiss", "navigate"), events)
    }
}
