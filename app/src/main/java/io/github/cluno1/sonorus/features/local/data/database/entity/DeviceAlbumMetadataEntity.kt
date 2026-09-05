/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Album-scoped artwork metadata owned only by DEVICE music. */
@Entity(tableName = "device_album_metadata")
data class DeviceAlbumMetadataEntity(
    @PrimaryKey val albumKey: String,
    val localTitle: String,
    val localArtist: String,
    val provider: String? = null,
    val externalReleaseId: String? = null,
    val externalReleaseGroupId: String? = null,
    val confidence: Double? = null,
    val pinned: Boolean = false,
    val artworkSource: String? = null,
    val artworkCachePath: String? = null,
    val artworkSha256: String? = null,
    val mediaType: String? = null,
    val byteSize: Long? = null,
    val matchedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val negativeUntil: Long = 0L
)

/** Stable DEVICE song-to-album relation; it never points into Catalog storage. */
@Entity(
    tableName = "device_song_album",
    indices = [Index(value = ["albumKey"])]
)
data class DeviceSongAlbumEntity(
    @PrimaryKey val songStableId: String,
    val albumKey: String
)
