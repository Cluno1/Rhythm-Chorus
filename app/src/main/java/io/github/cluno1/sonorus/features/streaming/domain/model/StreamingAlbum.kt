/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.features.streaming.domain.model

import io.github.cluno1.sonorus.core.domain.model.AlbumItem
import io.github.cluno1.sonorus.core.domain.model.PlayableItem
import io.github.cluno1.sonorus.core.domain.model.SourceType

/**
 * Represents an album from a streaming service.
 */
data class StreamingAlbum(
    override val id: String,
    override val title: String,
    override val artist: String,
    override val artworkUri: String?,
    override val songCount: Int,
    override val year: Int?,
    override val sourceType: SourceType,
    val externalId: String? = null,
    val releaseDate: String? = null,
    val albumType: AlbumType = AlbumType.ALBUM,
    val genres: List<String> = emptyList(),
    val label: String? = null,
    val copyright: String? = null,
    val isExplicit: Boolean = false,
    val tracks: List<StreamingSong> = emptyList()
) : AlbumItem {
    
    override suspend fun getSongs(): List<PlayableItem> = tracks
}

/**
 * Type of album release.
 */
enum class AlbumType {
    ALBUM,
    SINGLE,
    EP,
    COMPILATION
}
