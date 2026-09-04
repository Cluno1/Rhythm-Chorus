package chromahub.rhythm.app.features.catalog.domain

import android.net.Uri
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Song

const val CATALOG_SONG_ID_PREFIX = "rhythm-catalog:rendition:"
private const val UNKNOWN_ARTIST = "未知艺术家"

fun CatalogLibrarySong.toRhythmSong(): Song = Song(
    id = "$CATALOG_SONG_ID_PREFIX$renditionId",
    title = title,
    artist = artist ?: UNKNOWN_ARTIST,
    album = albumTitle,
    albumId = albumId,
    duration = durationMs,
    // This URI is display-only. Playback must exchange renditionId for a fresh descriptor.
    uri = Uri.parse("rhythm-catalog://rendition/$renditionId"),
    artworkUri = coverUrl?.toSafeArtworkUri(),
    trackNumber = trackNo ?: 0,
    albumArtist = artist ?: UNKNOWN_ARTIST,
    codec = "audio/mpeg",
    dateAdded = 0L,
    dateModified = 0L,
    path = null,
)

fun CatalogLibraryAlbum.toRhythmAlbum(): Album {
    require(songs.all { it.albumId == id }) { "catalog album contains a song from another album" }
    val projectedSongs = songs.map { it.toRhythmSong() }
    return Album(
        id = id,
        title = title,
        artist = artist ?: UNKNOWN_ARTIST,
        artworkUri = coverUrl?.toSafeArtworkUri(),
        songs = projectedSongs,
        numberOfSongs = songCount,
        dateModified = 0L,
    )
}

fun Song.isCatalogLibrarySong(): Boolean = id.startsWith(CATALOG_SONG_ID_PREFIX)

private fun String.toSafeArtworkUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
    ?.takeIf { it.scheme.equals("https", ignoreCase = true) }
