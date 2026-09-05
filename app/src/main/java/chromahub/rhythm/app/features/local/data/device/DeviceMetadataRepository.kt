/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.features.local.data.device

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import chromahub.rhythm.app.features.local.data.database.RhythmDatabase
import chromahub.rhythm.app.features.local.data.database.entity.DeviceMetadataEntity
import chromahub.rhythm.app.network.LrcLibLyrics
import chromahub.rhythm.app.network.NetworkClient
import chromahub.rhythm.app.shared.data.model.LyricsData
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.util.MediaUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

data class DeviceLyricsCandidate(
    val externalId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Double?,
    val confidence: Double,
    val lyrics: LyricsData
)

class DeviceMetadataRepository(private val context: Context) {
    private val dao = RhythmDatabase.getInstance(context).deviceMetadataDao()
    val folders = DeviceFolderAccess(context)

    fun stableId(song: Song): String = sha256("${song.uri}|${song.id}")
    fun fingerprint(song: Song): String = sha256(
        "${song.uri}|${song.duration}|${song.dateModified}|${sourceSize(song)}|${song.path.orEmpty()}"
    )

    suspend fun cachedLyrics(song: Song): LyricsData? {
        val row = dao.getBySongId(song.id) ?: return null
        if (row.fingerprint != fingerprint(song)) return null
        if (row.lyricsPlain.isNullOrBlank() && row.lyricsSynced.isNullOrBlank()) return null
        return LyricsData(row.lyricsPlain, row.lyricsSynced, source = row.lyricsSource ?: "Device cache")
    }

    suspend fun pinnedLyrics(song: Song): LyricsData? {
        val row = dao.getBySongId(song.id) ?: return null
        if (!row.lyricsPinned || row.fingerprint != fingerprint(song)) return null
        if (row.lyricsPlain.isNullOrBlank() && row.lyricsSynced.isNullOrBlank()) return null
        return LyricsData(row.lyricsPlain, row.lyricsSynced, source = row.lyricsSource)
    }

    suspend fun saveLyrics(song: Song, value: LyricsData, provider: String?, externalId: String?, confidence: Double?, cachePath: String? = null, pinned: Boolean = false) {
        val old = dao.getBySongId(song.id)
        val resolvedCachePath = cachePath ?: provider?.let {
            metadataDir().resolve("lyrics-${stableId(song)}.json").also { file -> file.writeText(Gson().toJson(value)) }.absolutePath
        }
        dao.upsert(base(song, old).copy(
            lyricsSource = value.source,
            lyricsProvider = provider,
            lyricsExternalId = externalId,
            lyricsConfidence = confidence,
            lyricsPlain = value.plainLyrics,
            lyricsSynced = value.syncedLyrics,
            lyricsCachePath = resolvedCachePath,
            lyricsPinned = pinned,
            updatedAt = System.currentTimeMillis()
        ))
    }

    suspend fun clearLyrics(song: Song) {
        dao.getBySongId(song.id)?.lyricsCachePath?.let { runCatching { File(it).delete() } }
        dao.clearLyrics(song.id)
    }

    suspend fun searchLyrics(song: Song): List<DeviceLyricsCandidate> = withContext(Dispatchers.IO) {
        if (!song.isDeviceSong()) return@withContext emptyList()
        if (!NetworkClient.isDevicePublicMetadataEnabled()) return@withContext emptyList()
        val service = NetworkClient.lrclibApiService ?: return@withContext emptyList()
        val input = DeviceMatchInput(song.title, song.artist, song.album, song.duration)
        val request = DeviceMetadataRequest(
            title = song.title,
            artist = song.artist.takeUnless { it.isUnknown() },
            album = song.album.takeUnless { it.isUnknown() },
            durationSeconds = (song.duration / 1000L).toInt().takeIf { it > 0 }
        )
        val precise = runCatching {
            service.searchLyrics(
                trackName = request.title,
                artistName = request.artist,
                albumName = request.album,
                duration = request.durationSeconds
            )
        }.getOrDefault(emptyList())
        val broad = runCatching {
            service.searchLyrics(query = "${song.artist} ${song.title}".trim())
        }.getOrDefault(emptyList())
        val results = (precise + broad).distinctBy(LrcLibLyrics::id)
        results.asSequence().filter(LrcLibLyrics::hasLyrics).map { item ->
            val confidence = DeviceMetadataMatcher.score(input, item.trackName ?: item.name, item.artistName, item.albumName, item.duration)
            DeviceLyricsCandidate(
                externalId = item.id.toString(), title = item.trackName ?: item.name.orEmpty(),
                artist = item.artistName.orEmpty(), album = item.albumName.orEmpty(),
                durationSeconds = item.duration, confidence = confidence,
                lyrics = LyricsData(item.plainLyrics, item.syncedLyrics, source = "LRCLIB")
            )
        }.sortedByDescending(DeviceLyricsCandidate::confidence).take(12).toList()
    }

    suspend fun applyLyrics(song: Song, candidate: DeviceLyricsCandidate, userSelected: Boolean = false): LyricsData {
        val prefix = if (userSelected) "DEVICE_LRCLIB_SELECTED" else "DEVICE_LRCLIB"
        val labelled = candidate.lyrics.copy(source = "$prefix|${candidate.title}|${candidate.artist}|${(candidate.confidence * 100).toInt()}%")
        saveLyrics(song, labelled, "LRCLIB", candidate.externalId, candidate.confidence, pinned = userSelected)
        return labelled
    }

    suspend fun cachedArtwork(song: Song): Uri? {
        val row = dao.getBySongId(song.id) ?: return null
        if (row.fingerprint != fingerprint(song)) return null
        return row.artworkCachePath?.let(::File)?.takeIf(File::isFile)?.toUri()
    }

    suspend fun restoreLocalArtwork(song: Song): Uri? = withContext(Dispatchers.IO) {
        if (!song.isDeviceSong()) return@withContext null
        dao.getBySongId(song.id)?.artworkCachePath?.let { path ->
            if (path.startsWith(metadataDir().absolutePath)) runCatching { File(path).delete() }
        }
        dao.clearArtwork(song.id)
        MediaUtils.extractEmbeddedAlbumArt(context, song.uri, context.filesDir, false, song.path)?.let { uri ->
            uri.path?.let(::File)?.takeIf(File::isFile)?.let { saveArtwork(song, it, "EMBEDDED", null, null, 1.0) }
            return@withContext uri
        }
        val physical = findPhysicalSiblingArtwork(song)?.let { source ->
            metadataDir().resolve("sibling-${stableId(song)}.${source.extension.ifBlank { "img" }}").also { source.copyTo(it, overwrite = true) }
        }
        val sibling = physical ?: folders.findSibling(
            song.uri, setOf("jpg", "jpeg", "png", "webp"), setOf("cover", "folder", "album", "front")
        )?.let { copyContent(it.uri, "sibling-${stableId(song)}.img") }
        sibling?.let {
            saveArtwork(song, it, "SIBLING", null, null, 1.0)
            it.toUri()
        }
    }

    suspend fun findOrFetchArtwork(song: Song, forceOnline: Boolean = false): Uri? = withContext(Dispatchers.IO) {
        if (!song.isDeviceSong()) return@withContext null
        if (!forceOnline) {
            MediaUtils.extractEmbeddedAlbumArt(context, song.uri, context.filesDir, false, song.path)?.let { uri ->
                uri.path?.let(::File)?.takeIf(File::isFile)?.let { file ->
                    saveArtwork(song, file, "EMBEDDED", null, null, 1.0)
                    return@withContext uri
                }
            }
        }
        val physicalSibling = findPhysicalSiblingArtwork(song)?.let { source ->
            metadataDir().resolve("sibling-${stableId(song)}.${source.extension.ifBlank { "img" }}").also { source.copyTo(it, overwrite = true) }
        }
        val sibling = physicalSibling ?: folders.findSibling(
            song.uri, setOf("jpg", "jpeg", "png", "webp"), setOf("cover", "folder", "album", "front")
        )?.let { siblingFile -> copyContent(siblingFile.uri, "sibling-${stableId(song)}.img") }
        if (sibling != null) {
            saveArtwork(song, sibling, "SIBLING", null, null, 1.0)
            return@withContext sibling.toUri()
        }
        if (!forceOnline) cachedArtwork(song)?.let { return@withContext it }
        if (!NetworkClient.isDevicePublicMetadataEnabled()) return@withContext null
        val result = runCatching {
            NetworkClient.deezerApiService?.searchTracks("track:\"${song.title}\" artist:\"${song.artist}\"", 25)
        }.getOrNull()?.data.orEmpty().map { track ->
            val score = DeviceMetadataMatcher.score(
                DeviceMatchInput(song.title, song.artist, song.album, song.duration),
                track.title, track.artist?.name, track.album?.title, track.duration?.toDouble()
            )
            track to score
        }.maxByOrNull { it.second } ?: return@withContext null
        if (result.second < MIN_AUTO_CONFIDENCE) return@withContext null
        val albumResponse = runCatching { NetworkClient.deezerApiService?.searchAlbums("album:\"${result.first.album?.title ?: song.album}\" artist:\"${result.first.artist?.name ?: song.artist}\"", 10) }.getOrNull()
        val album = albumResponse?.data?.firstOrNull { it.id == result.first.album?.id } ?: albumResponse?.data?.firstOrNull()
        val url = album?.coverXl ?: album?.coverBig ?: album?.coverMedium ?: return@withContext null
        val file = downloadArtwork(url, "deezer-${stableId(song)}.jpg") ?: return@withContext null
        saveArtwork(song, file, "PUBLIC_API", "DEEZER", result.first.id.toString(), result.second)
        file.toUri()
    }

    private suspend fun saveArtwork(song: Song, file: File, source: String, provider: String?, externalId: String?, confidence: Double?) {
        val old = dao.getBySongId(song.id)
        dao.upsert(base(song, old).copy(
            artworkSource = source, artworkProvider = provider, artworkExternalId = externalId,
            artworkConfidence = confidence, artworkCachePath = file.absolutePath, updatedAt = System.currentTimeMillis()
        ))
    }

    private fun base(song: Song, old: DeviceMetadataEntity?) = (old ?: DeviceMetadataEntity(
        stableId = stableId(song), songId = song.id, contentUri = song.uri.toString(), fingerprint = fingerprint(song)
    )).copy(contentUri = song.uri.toString(), fingerprint = fingerprint(song))

    private fun findPhysicalSiblingArtwork(song: Song): File? {
        val audio = song.path?.let(::File)?.takeIf(File::isFile) ?: return null
        val siblings = audio.parentFile?.listFiles().orEmpty()
        val extensions = setOf("jpg", "jpeg", "png", "webp")
        return siblings.firstOrNull { it.isFile && it.nameWithoutExtension.equals(audio.nameWithoutExtension, true) && it.extension.lowercase() in extensions }
            ?: siblings.firstOrNull { it.isFile && it.nameWithoutExtension.lowercase() in setOf("cover", "folder", "album", "front") && it.extension.lowercase() in extensions }
    }

    private fun copyContent(uri: Uri, name: String): File? = runCatching {
        val target = metadataDir().resolve(name)
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } ?: return null
        target
    }.getOrNull()

    private fun downloadArtwork(url: String, name: String): File? = runCatching {
        val httpsUrl = DeviceMetadataPolicy.safeDeezerArtworkUrl(url) ?: return null
        NetworkClient.genericHttpClient.newCall(Request.Builder().url(httpsUrl).get().build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val finalUrl = response.request.url
            if (finalUrl.scheme != "https" || (finalUrl.host != "dzcdn.net" && !finalUrl.host.endsWith(".dzcdn.net"))) return null
            if (!DeviceMetadataPolicy.isImageContentType(response.header("Content-Type"))) return null
            val declaredLength = response.body.contentLength()
            if (declaredLength > MAX_ARTWORK_BYTES) return null
            val target = metadataDir().resolve(name)
            response.body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_ARTWORK_BYTES) {
                            target.delete()
                            return null
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            target
        }
    }.getOrNull()

    private fun metadataDir() = File(context.filesDir, "device_metadata").apply { mkdirs() }
    suspend fun clearAllCachedMetadata() = withContext(Dispatchers.IO) {
        File(context.filesDir, "device_metadata").listFiles()?.forEach(File::delete)
        dao.deleteAll()
    }
    private fun sourceSize(song: Song): Long = song.path?.let(::File)?.takeIf(File::isFile)?.length() ?: runCatching {
        context.contentResolver.query(song.uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun String.isUnknown() = isBlank() || equals("unknown", true) || equals("<unknown>", true) || startsWith("unknown ", true)
    private fun Song.isDeviceSong() = DeviceMetadataPolicy.isEligible(id, uri.scheme)

    companion object {
        const val MIN_AUTO_CONFIDENCE = 0.72
        const val MAX_ARTWORK_BYTES = 8L * 1024L * 1024L
    }
}
