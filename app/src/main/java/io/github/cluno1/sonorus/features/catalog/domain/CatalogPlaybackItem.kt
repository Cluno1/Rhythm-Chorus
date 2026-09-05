package io.github.cluno1.sonorus.features.catalog.domain

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** A playable business identity returned by `GET /v2/renditions/{id}/playback`. */
data class CatalogPlaybackItem(
    val renditionId: String,
    val assetId: String?,
    val title: String,
    val artist: String,
    val arrangementName: String,
    val playbackUrl: String,
    val cacheKey: String?,
    val mediaType: String,
    val durationMs: Long = 0L,
    val albumId: String = "",
    val artworkUrl: String? = null,
) {
    init {
        require(CatalogPlaybackPolicy.isPlayableMediaType(mediaType)) {
            "Catalog playback accepts real audio only"
        }
    }

    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(arrangementName)
        artworkUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?.let { metadata.setArtworkUri(Uri.parse(it)) }
        val builder = MediaItem.Builder()
            .setMediaId(
                assetId?.let { "rhythm-catalog:rendition:$renditionId:asset:$it" }
                    ?: "rhythm-catalog:rendition:$renditionId"
            )
            .setUri(playbackUrl)
            .setMimeType(mediaType)
            .setMediaMetadata(metadata.build())
        if (cacheKey != null) builder.setCustomCacheKey(cacheKey)
        return builder.build()
    }
}
