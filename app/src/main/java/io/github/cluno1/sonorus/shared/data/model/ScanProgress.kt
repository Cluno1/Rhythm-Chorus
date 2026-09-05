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
