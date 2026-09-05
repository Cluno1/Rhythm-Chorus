/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.features.streaming.domain.model

/**
 * Represents a browse category/genre from a streaming service.
 * Used for browsing music by category (e.g., "Rock", "Pop", "Workout", etc.).
 */
data class BrowseCategory(
    val id: String,
    val name: String,
    val iconUrl: String? = null
)
