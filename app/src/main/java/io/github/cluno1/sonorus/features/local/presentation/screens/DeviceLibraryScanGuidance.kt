/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.presentation.screens

/** Product gate for the post-scan SAF folder guidance shown in the DEVICE library. */
object DeviceLibraryScanGuidance {
    const val LOW_RESULT_EXCLUSIVE_LIMIT = 10

    fun shouldShow(
        deviceLibraryEnabled: Boolean,
        isStreamingMode: Boolean,
        hasMediaPermission: Boolean,
        scanCompleted: Boolean,
        scanFailed: Boolean,
        scanInProgress: Boolean,
        acceptedDeviceSongs: Int,
        validAuthorizedRootCount: Int,
    ): Boolean =
        deviceLibraryEnabled &&
            !isStreamingMode &&
            hasMediaPermission &&
            scanCompleted &&
            !scanFailed &&
            !scanInProgress &&
            acceptedDeviceSongs in 0 until LOW_RESULT_EXCLUSIVE_LIMIT &&
            validAuthorizedRootCount == 0
}
