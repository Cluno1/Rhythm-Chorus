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
                currentRoute = "device_manual_metadata/{songId}?kind={kind}",
                routePattern = "device_manual_metadata/{songId}?kind={kind}",
                currentSongId = "42",
                currentKind = "LYRICS",
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.LYRICS,
            ),
        )
        assertFalse(
            isSameDeviceManualMetadataDestination(
                currentRoute = "player",
                routePattern = "device_manual_metadata/{songId}?kind={kind}",
                currentSongId = null,
                currentKind = null,
                targetSongId = "42",
                targetKind = DeviceManualMetadataKind.LYRICS,
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
