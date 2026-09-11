/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.presentation.navigation

import io.github.cluno1.sonorus.features.local.data.device.DeviceManualMetadataKind
import kotlinx.coroutines.CancellationException

internal fun isSameDeviceManualMetadataDestination(
    currentRoute: String?,
    routePattern: String,
    currentSongId: String?,
    currentKind: String?,
    currentArtistName: String?,
    targetSongId: String,
    targetKind: DeviceManualMetadataKind,
    targetArtistName: String?,
): Boolean = currentRoute == routePattern &&
    currentSongId == targetSongId &&
    currentKind == targetKind.name &&
    currentArtistName.orEmpty() == targetArtistName.orEmpty()

internal suspend fun closeSheetThenNavigateToDeviceManualMetadata(
    hideSheet: suspend () -> Unit,
    dismissSheet: () -> Unit,
    navigate: () -> Unit,
) {
    try {
        hideSheet()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // A sheet can already be hidden during a configuration/layout transition.
    }
    dismissSheet()
    navigate()
}
