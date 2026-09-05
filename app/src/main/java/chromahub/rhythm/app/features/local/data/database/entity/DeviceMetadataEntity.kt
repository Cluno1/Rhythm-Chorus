/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Enrichment owned by a DEVICE song. It never changes or uploads the source audio file. */
@Entity(
    tableName = "device_metadata",
    indices = [Index(value = ["songId"], unique = true), Index(value = ["fingerprint"])]
)
data class DeviceMetadataEntity(
    @PrimaryKey val stableId: String,
    val songId: String,
    val contentUri: String,
    val fingerprint: String,
    val titleSource: String = "MEDIASTORE",
    val artistSource: String = "MEDIASTORE",
    val albumSource: String = "MEDIASTORE",
    val lyricsSource: String? = null,
    val lyricsProvider: String? = null,
    val lyricsExternalId: String? = null,
    val lyricsConfidence: Double? = null,
    val lyricsPlain: String? = null,
    val lyricsSynced: String? = null,
    val lyricsCachePath: String? = null,
    val lyricsPinned: Boolean = false,
    val artworkSource: String? = null,
    val artworkProvider: String? = null,
    val artworkExternalId: String? = null,
    val artworkConfidence: Double? = null,
    val artworkCachePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
