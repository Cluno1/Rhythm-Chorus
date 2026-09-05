/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.features.local.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import chromahub.rhythm.app.features.local.data.database.entity.DeviceMetadataEntity

@Dao
interface DeviceMetadataDao {
    @Query("SELECT * FROM device_metadata WHERE songId = :songId LIMIT 1")
    suspend fun getBySongId(songId: String): DeviceMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceMetadataEntity)

    @Query("DELETE FROM device_metadata WHERE songId = :songId")
    suspend fun deleteForSong(songId: String)

    @Query("UPDATE device_metadata SET lyricsSource = NULL, lyricsProvider = NULL, lyricsExternalId = NULL, lyricsConfidence = NULL, lyricsPlain = NULL, lyricsSynced = NULL, lyricsCachePath = NULL, lyricsPinned = 0, updatedAt = :updatedAt WHERE songId = :songId")
    suspend fun clearLyrics(songId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE device_metadata SET artworkSource = NULL, artworkProvider = NULL, artworkExternalId = NULL, artworkConfidence = NULL, artworkCachePath = NULL, updatedAt = :updatedAt WHERE songId = :songId")
    suspend fun clearArtwork(songId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE device_metadata SET lyricsSource = NULL, lyricsProvider = NULL, lyricsExternalId = NULL, lyricsConfidence = NULL, lyricsPlain = NULL, lyricsSynced = NULL, lyricsCachePath = NULL, lyricsPinned = 0, updatedAt = :updatedAt")
    suspend fun clearAllLyrics(updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM device_metadata")
    suspend fun deleteAll()
}
