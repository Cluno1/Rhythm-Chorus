package chromahub.rhythm.app.features.catalog.domain

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** A playable business identity returned by `GET /v2/renditions/{id}/playback`. */
data class CatalogPlaybackItem(
    val renditionId: String,
    val assetId: String,
    val title: String,
    val artist: String,
    val arrangementName: String,
    val playbackUrl: String,
    val cacheKey: String,
    val mediaType: String,
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("rhythm-catalog:rendition:$renditionId:asset:$assetId")
        .setUri(playbackUrl)
        .setMimeType(mediaType)
        .setCustomCacheKey(cacheKey)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(arrangementName)
                .build(),
        )
        .build()
}
