/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.presentation.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLibraryScanGuidanceTest {
    private fun shouldShow(
        acceptedSongs: Int,
        validRoots: Int = 0,
        hasPermission: Boolean = true,
        completed: Boolean = true,
        failed: Boolean = false,
        inProgress: Boolean = false,
        streaming: Boolean = false,
        deviceLibraryEnabled: Boolean = true,
    ) = DeviceLibraryScanGuidance.shouldShow(
        deviceLibraryEnabled = deviceLibraryEnabled,
        isStreamingMode = streaming,
        hasMediaPermission = hasPermission,
        scanCompleted = completed,
        scanFailed = failed,
        scanInProgress = inProgress,
        acceptedDeviceSongs = acceptedSongs,
        validAuthorizedRootCount = validRoots,
    )

    @Test
    fun `zero and nine accepted songs show guidance`() {
        assertTrue(shouldShow(acceptedSongs = 0))
        assertTrue(shouldShow(acceptedSongs = 9))
    }

    @Test
    fun `ten accepted songs does not show guidance`() {
        assertFalse(shouldShow(acceptedSongs = 10))
    }

    @Test
    fun `valid authorized root suppresses guidance`() {
        assertFalse(shouldShow(acceptedSongs = 8, validRoots = 1))
    }

    @Test
    fun `permission scan and product gates suppress guidance`() {
        assertFalse(shouldShow(acceptedSongs = 8, hasPermission = false))
        assertFalse(shouldShow(acceptedSongs = 8, completed = false))
        assertFalse(shouldShow(acceptedSongs = 8, failed = true))
        assertFalse(shouldShow(acceptedSongs = 8, inProgress = true))
        assertFalse(shouldShow(acceptedSongs = 8, streaming = true))
        assertFalse(shouldShow(acceptedSongs = 8, deviceLibraryEnabled = false))
    }
}
