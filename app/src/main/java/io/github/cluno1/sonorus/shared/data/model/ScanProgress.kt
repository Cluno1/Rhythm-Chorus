/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.shared.data.model

data class ScanProgress(
    val current: Int,
    val total: Int,
    val stage: ScanPhase,
    val estimatedTimeMs: Long = 0
)

data class MediaScanDiagnostics(
    val mediaStoreCandidates: Int = 0,
    val authorizedFolderCandidates: Int = 0,
    val authorizedFolderAccepted: Int = 0,
    val acceptedSongs: Int = 0,
    val filteredByFormat: Int = 0,
    val filteredByDuration: Int = 0,
    val filteredByBitrate: Int = 0,
    val filteredByFolderRule: Int = 0,
    val duplicates: Int = 0,
    val unreadableFiles: Int = 0,
    val failedAuthorizedFolders: Int = 0,
    val missingFolderAuthorizations: Int = 0,
    val preservedPreviousSongs: Int = 0,
    val durationMs: Long = 0L,
    val completedAtMs: Long = 0L,
    val failed: Boolean = false,
)
