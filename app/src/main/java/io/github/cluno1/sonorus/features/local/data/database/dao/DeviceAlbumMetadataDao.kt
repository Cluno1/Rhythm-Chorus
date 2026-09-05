/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.cluno1.sonorus.features.local.data.database.entity.DeviceAlbumMetadataEntity
import io.github.cluno1.sonorus.features.local.data.database.entity.DeviceSongAlbumEntity

@Dao
interface DeviceAlbumMetadataDao {
    @Query("SELECT * FROM device_album_metadata WHERE albumKey = :albumKey LIMIT 1")
    suspend fun getAlbum(albumKey: String): DeviceAlbumMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(entity: DeviceAlbumMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongAlbum(entity: DeviceSongAlbumEntity)

    @Query("SELECT * FROM device_album_metadata")
    suspend fun getAllAlbums(): List<DeviceAlbumMetadataEntity>

    @Query("UPDATE device_album_metadata SET provider = NULL, externalReleaseId = NULL, externalReleaseGroupId = NULL, confidence = NULL, pinned = 0, artworkSource = NULL, artworkCachePath = NULL, artworkSha256 = NULL, mediaType = NULL, byteSize = NULL, matchedAt = NULL, updatedAt = :updatedAt, negativeUntil = 0 WHERE albumKey = :albumKey")
    suspend fun clearArtwork(albumKey: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM device_album_metadata")
    suspend fun deleteAllAlbums()

    @Query("DELETE FROM device_song_album")
    suspend fun deleteAllSongAlbums()
}
