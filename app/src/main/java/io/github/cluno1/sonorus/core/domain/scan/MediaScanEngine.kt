/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.core.domain.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import io.github.cluno1.sonorus.features.local.data.device.DeviceDocumentPolicy
import io.github.cluno1.sonorus.features.local.data.device.DeviceScanFolderAccess
import io.github.cluno1.sonorus.features.local.data.device.DeviceScanRoot
import io.github.cluno1.sonorus.features.local.data.database.RhythmDatabase
import io.github.cluno1.sonorus.features.local.data.database.entity.SongEntity
import io.github.cluno1.sonorus.shared.data.model.AppSettings
import io.github.cluno1.sonorus.shared.data.model.MediaScanDiagnostics
import io.github.cluno1.sonorus.shared.data.model.MediaScanMode
import io.github.cluno1.sonorus.shared.data.model.ScanPhase
import io.github.cluno1.sonorus.shared.data.model.ScanProgress
import io.github.cluno1.sonorus.shared.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.content.edit

/**
 * Centralized, High-Performance Media Scanning Engine for Rhythm.
 * Features:
 * - Differential scanning (MediaStore vs Room DB DATE_MODIFIED comparison)
 * - O(1) HashMap lookups (eliminates memory leaks, CPU heating, battery drain)
 * - Asynchronous batching & yielding on Dispatchers.IO
 */
class MediaScanEngine(
    private val context: Context,
    private val database: RhythmDatabase,
    private val appSettings: AppSettings
) {
    companion object {
        private const val TAG = "MediaScanEngine"
        private const val BATCH_SIZE = 100
        private const val MAX_SAF_SCAN_DEPTH = 64
        private val SUPPORTED_AUDIO_EXTENSIONS =
            AppSettings.defaultAllowedFormats() + setOf("mp4", "mkv")

        fun mediaScanSelection(minimumDuration: Long = 0L): String {
            val baseSelection = "(${MediaStore.Audio.Media.IS_MUSIC} = 1 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR ${MediaStore.Audio.Media.MIME_TYPE} = 'video/mp4' OR ${MediaStore.Audio.Media.MIME_TYPE} = 'video/x-matroska' OR ${MediaStore.Audio.Media.MIME_TYPE} = 'application/x-matroska')"
            return if (minimumDuration > 0L) {
                "$baseSelection AND ${MediaStore.Audio.Media.DURATION} >= $minimumDuration"
            } else {
                baseSelection
            }
        }
    }

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, ScanPhase.Idle))
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()
    private val _scanDiagnostics = MutableStateFlow(MediaScanDiagnostics())
    val scanDiagnostics: StateFlow<MediaScanDiagnostics> = _scanDiagnostics.asStateFlow()

    /**
     * Performs a high-performance differential or full media scan.
     */
    suspend fun performScan(
        forceRefresh: Boolean = false,
        allowedFormats: Set<String>? = null,
        minimumBitrate: Int = 0,
        minimumDuration: Long = 0L,
        includeMediaStore: Boolean = true,
    ): List<Song> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Starting media scan (forceRefresh=$forceRefresh, minimumDuration=${minimumDuration}ms)")
        _scanProgress.value = ScanProgress(0, 0, ScanPhase.Songs, 0)

        val persistedSongs = database.songDao().getAllSongs()
        // Query existing DB entries into an O(1) Map by ID for differential scans.
        val existingDbSongs = if (!forceRefresh) persistedSongs.associateBy { it.id } else emptyMap()

        val mediaScanMode = appSettings.mediaScanMode.value
        val whitelistedFolders = appSettings.whitelistedFolders.value
        val blacklistedFolders = appSettings.blacklistedFolders.value
        val blacklistedSongs = appSettings.blacklistedSongs.value
        val includeHiddenWhitelistedMedia = appSettings.includeHiddenWhitelistedMedia.value
        val authorizedRoots = if (mediaScanMode == MediaScanMode.WHITELIST) {
            val configuredFolders = whitelistedFolders.map(::normalizePath).toSet()
            DeviceScanFolderAccess(context).roots().filter { root ->
                normalizePath(root.displayPath) in configuredFolders
            }
        } else {
            emptyList()
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
                add(MediaStore.Audio.Media.DISC_NUMBER)
                add(MediaStore.Audio.Media.CD_TRACK_NUMBER)
            }
        }.toTypedArray()

        val selection = mediaScanSelection(minimumDuration)
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val scannedSongs = mutableListOf<SongEntity>()
        val seenIds = mutableSetOf<String>()
        val seenPaths = mutableSetOf<String>()

        var rawMediaStoreCount = 0
        var authorizedFolderCandidates = 0
        var authorizedFolderAccepted = 0
        var filteredByFormat = 0
        var filteredByDuration = 0
        var filteredByBitrate = 0
        var filteredByFolderRule = 0
        var duplicates = 0
        var unreadableFiles = 0
        var failedAuthorizedFolders = 0
        var preservedPreviousSongs = 0
        var mediaStoreSucceeded = false
        var mediaStoreFailed = false
        val failedRoots = mutableListOf<DeviceScanRoot>()

        try {
            if (includeMediaStore) {
                val cursor = try {
                    context.contentResolver.query(collection, projection, selection, null, sortOrder)
                } catch (e: Exception) {
                    mediaStoreFailed = true
                    Log.e(TAG, "MediaStore query failed; retaining its previous rows", e)
                    null
                }
                if (cursor == null) {
                    mediaStoreFailed = true
                } else cursor.use { cursor ->
                    mediaStoreSucceeded = true
                val totalCount = cursor.count
                rawMediaStoreCount = totalCount
                Log.d(TAG, "MediaStore query found $totalCount candidates")
                _scanProgress.value = ScanProgress(0, totalCount, ScanPhase.Songs, 0)

                val colId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val colDisplayName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val colTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val colArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val colAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val colAlbumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val colDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val colTrack = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val colYear = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val colDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val colDateModified = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val colData = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val colGenre = cursor.getColumnIndex("genre") // MediaStore.Audio.AudioColumns.GENRE (API 30+)
                val colAlbumArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val colDiscNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER) else -1
                val colCdTrackNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cursor.getColumnIndex(MediaStore.Audio.Media.CD_TRACK_NUMBER) else -1

            var processed = 0
            var lastProgressEmitTime = 0L

                while (cursor.moveToNext()) {
                    processed++
                    val id = cursor.getLong(colId).toString()
                    if (seenIds.contains(id)) {
                        duplicates++
                        continue
                    }
                    if (blacklistedSongs.contains(id)) {
                        filteredByFolderRule++
                        continue
                    }

                    val path = if (colData >= 0) cursor.getString(colData) else null
                    if (allowedFormats != null) {
                        val candidateName = path?.takeIf(String::isNotBlank)
                            ?: if (colDisplayName >= 0) cursor.getString(colDisplayName).orEmpty() else ""
                        val extension = candidateName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        if (extension.isBlank() || extension !in allowedFormats) {
                            filteredByFormat++
                            continue
                        }
                    }
                    if (path != null) {
                        val normPath = normalizePath(path)
                        if (seenPaths.contains(normPath)) {
                            duplicates++
                            continue
                        }

                        if (mediaScanMode == MediaScanMode.WHITELIST && whitelistedFolders.isNotEmpty()) {
                            val isWhitelisted = whitelistedFolders.any { isWithinFolder(normPath, it) }
                            if (!isWhitelisted) {
                                filteredByFolderRule++
                                continue
                            }
                        }

                        if (mediaScanMode == MediaScanMode.BLACKLIST && blacklistedFolders.isNotEmpty()) {
                            val isBlacklisted = blacklistedFolders.any { isWithinFolder(normPath, it) }
                            if (isBlacklisted) {
                                filteredByFolderRule++
                                continue
                            }
                        }

                        seenPaths.add(normPath)
                    }

                    val duration = cursor.getLong(colDuration)
                    if (minimumDuration > 0 && duration < minimumDuration) {
                        filteredByDuration++
                        continue
                    }

                    val contentUri = Uri.withAppendedPath(collection, id)
                    val bitrate = if (minimumBitrate > 0) readBitrate(contentUri) else null
                    if (minimumBitrate > 0 && bitrate != null && bitrate < minimumBitrate * 1000) {
                        filteredByBitrate++
                        continue
                    }

                    val rawDateModified = cursor.getLong(colDateModified)
                    val dateModified = if (rawDateModified in 1..99_999_999_999L) rawDateModified * 1000L else rawDateModified

                    val preferSongArtwork = appSettings.preferSongArtwork.value
                    val losslessArtwork = appSettings.isLosslessArtworkActive.value

                    // Differential check: reuse existing DB record if unmodified and timestamps are in ms
                    val existing = existingDbSongs[id]

                    if (existing != null && existing.dateModified == dateModified && existing.dateAdded >= 100_000_000_000L) {
                        val existingArt = if (preferSongArtwork) {
                            io.github.cluno1.sonorus.util.MediaUtils.getCachedEmbeddedAlbumArtUri(
                                cacheDir = context.filesDir,
                                songUri = (existing.uri).toUri(),
                                lossless = losslessArtwork,
                                exactMatchOnly = false
                            )?.toString() ?: (existing.artworkUri ?: Uri.withAppendedPath(
                                ("content://media/external/audio/albumart").toUri(),
                                existing.albumId
                            ).toString())
                        } else {
                            existing.artworkUri ?: Uri.withAppendedPath(
                                ("content://media/external/audio/albumart").toUri(),
                                existing.albumId
                            ).toString()
                        }
                        scannedSongs.add(existing.copy(artworkUri = existingArt))
                        seenIds.add(id)
                    } else {
                        val rawTitle = cursor.getString(colTitle) ?: "Unknown Title"
                        val rawArtist = cursor.getString(colArtist) ?: "<unknown>"
                        val rawAlbum = cursor.getString(colAlbum) ?: "Unknown Album"
                        val albumId = cursor.getLong(colAlbumId).toString()
                        val rawTrack = cursor.getInt(colTrack)
                        val cdTrack = if (colCdTrackNumber >= 0) cursor.getInt(colCdTrackNumber) else 0
                        val discFromStore = if (colDiscNumber >= 0) cursor.getInt(colDiscNumber) else 0
                        val rawYear = cursor.getInt(colYear)
                        val rawDateAdded = cursor.getLong(colDateAdded)
                        val dateAdded = if (rawDateAdded in 1..99_999_999_999L) {
                            rawDateAdded * 1000L
                        } else if (rawDateAdded > 0L) {
                            rawDateAdded
                        } else {
                            System.currentTimeMillis()
                        }
                        val finalDateModified = dateModified.takeIf { it > 0L } ?: dateAdded
                        val rawGenre = if (colGenre >= 0) cursor.getString(colGenre) else null
                        val rawAlbumArtist = if (colAlbumArtist >= 0) cursor.getString(colAlbumArtist) else null

                        var title = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(rawTitle) ?: rawTitle
                        var artist = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(rawArtist) ?: rawArtist
                        var album = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(rawAlbum) ?: rawAlbum
                        var genre = rawGenre?.let { io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(it) }
                        var albumArtist = rawAlbumArtist?.let { io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(it) }

                        var discNumber = when {
                            discFromStore > 0 -> discFromStore
                            rawTrack >= 1000 -> rawTrack / 1000
                            else -> 1
                        }

                        var trackNumber = when {
                            rawTrack >= 1000 -> rawTrack % 1000
                            rawTrack > 0 -> rawTrack
                            cdTrack > 0 -> cdTrack
                            else -> 0
                        }

                        var year = rawYear

                        // Fallback tag extraction for missing year or FLAC/audio files where MediaStore failed
                        if ((year == 0 || trackNumber == 0 || path?.lowercase()?.endsWith(".flac") == true) && !path.isNullOrBlank()) {
                            try {
                                val file = File(path)
                                if (file.exists() && file.canRead()) {
                                    android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                                        val metadata = com.kyant.taglib.TagLib.getMetadata(fd.detachFd())
                                        val propertyMap = metadata?.propertyMap ?: emptyMap()
                                        if (year == 0) {
                                            val dateValues = propertyMap["DATE"] ?: propertyMap["YEAR"]
                                            if (!dateValues.isNullOrEmpty() && dateValues[0].isNotBlank()) {
                                                year = io.github.cluno1.sonorus.util.MetadataHeuristics.parseYear(dateValues[0])
                                            }
                                        }
                                        if (trackNumber == 0) {
                                            val trackValues = propertyMap["TRACKNUMBER"]
                                            if (!trackValues.isNullOrEmpty() && trackValues[0].isNotBlank()) {
                                                val parsedTrackStr = trackValues[0].substringBefore('/')
                                                val parsedInt = parsedTrackStr.toIntOrNull() ?: 0
                                                if (parsedInt >= 1000) {
                                                    discNumber = parsedInt / 1000
                                                    trackNumber = parsedInt % 1000
                                                } else if (parsedInt > 0) {
                                                    trackNumber = parsedInt
                                                }
                                            }
                                        }
                                        if (discNumber <= 1) {
                                            val discValues = propertyMap["DISCNUMBER"]
                                            if (!discValues.isNullOrEmpty() && discValues[0].isNotBlank()) {
                                                val parsedDisc = discValues[0].substringBefore('/').toIntOrNull() ?: 1
                                                if (parsedDisc > 0) discNumber = parsedDisc
                                            }
                                        }
                                        val tagTitle = propertyMap["TITLE"]?.firstOrNull()?.trim()
                                        if (!tagTitle.isNullOrBlank() && (title == "Unknown Title" || io.github.cluno1.sonorus.util.MetadataHeuristics.isLikelyCorruptedMetadata(title))) {
                                            title = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(tagTitle) ?: tagTitle
                                        }
                                        val tagArtist = propertyMap["ARTIST"]?.firstOrNull()?.trim()
                                        if (!tagArtist.isNullOrBlank() && (artist == "<unknown>" || io.github.cluno1.sonorus.util.MetadataHeuristics.isLikelyCorruptedMetadata(artist))) {
                                            artist = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(tagArtist) ?: tagArtist
                                        }
                                        val tagAlbum = propertyMap["ALBUM"]?.firstOrNull()?.trim()
                                        if (!tagAlbum.isNullOrBlank() && (album == "Unknown Album" || io.github.cluno1.sonorus.util.MetadataHeuristics.isLikelyCorruptedMetadata(album))) {
                                            album = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(tagAlbum) ?: tagAlbum
                                        }
                                        val tagGenre = propertyMap["GENRE"]?.firstOrNull()?.trim()
                                        if (!tagGenre.isNullOrBlank() && (genre.isNullOrBlank() || io.github.cluno1.sonorus.util.MetadataHeuristics.isLikelyCorruptedMetadata(genre))) {
                                            genre = io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(tagGenre) ?: tagGenre
                                        }
                                    }
                                }
                            } catch (_: Throwable) {
                                // TagLib fallback non-fatal
                            }
                        }

                        val contentUriString = contentUri.toString()
                        val defaultArtworkUri = Uri.withAppendedPath(
                            ("content://media/external/audio/albumart").toUri(),
                            albumId
                        ).toString()

                        val initialArtworkUri = if (preferSongArtwork) {
                            io.github.cluno1.sonorus.util.MediaUtils.getCachedEmbeddedAlbumArtUri(
                                cacheDir = context.filesDir,
                                songUri = contentUri,
                                lossless = losslessArtwork,
                                exactMatchOnly = false
                            )?.toString() ?: defaultArtworkUri
                        } else {
                            defaultArtworkUri
                        }

                        val entity = SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            duration = duration,
                            uri = contentUriString,
                            artworkUri = initialArtworkUri,
                            trackNumber = trackNumber,
                            year = year,
                            genre = genre,
                            dateAdded = dateAdded,
                            dateModified = finalDateModified,
                            albumArtist = albumArtist,
                            bitrate = bitrate,
                            sampleRate = null,
                            channels = null,
                            codec = null,
                            discNumber = discNumber,
                            path = path
                        )
                        scannedSongs.add(entity)
                        seenIds.add(id)
                    }

                    val nowTime = System.currentTimeMillis()
                    if (nowTime - lastProgressEmitTime >= 150 || processed == totalCount) {
                        _scanProgress.value = ScanProgress(processed, totalCount, ScanPhase.Songs, 0)
                        lastProgressEmitTime = nowTime
                        yield()
                    }
                }
            }
            }

            if (authorizedRoots.isNotEmpty()) {
                val safResult = scanAuthorizedRoots(
                    roots = authorizedRoots,
                    includeHidden = includeHiddenWhitelistedMedia,
                    allowedFormats = allowedFormats,
                    minimumBitrate = minimumBitrate,
                    minimumDuration = minimumDuration,
                    blacklistedSongs = blacklistedSongs,
                    seenIds = seenIds,
                    seenPaths = seenPaths,
                    existingSongs = persistedSongs.associateBy(SongEntity::id),
                )
                scannedSongs += safResult.songs
                authorizedFolderCandidates += safResult.candidates
                authorizedFolderAccepted += safResult.songs.size
                filteredByFormat += safResult.filteredByFormat
                filteredByDuration += safResult.filteredByDuration
                filteredByBitrate += safResult.filteredByBitrate
                filteredByFolderRule += safResult.filteredByFolderRule
                duplicates += safResult.duplicates
                unreadableFiles += safResult.unreadable
                failedAuthorizedFolders += safResult.failedRoots.size
                preservedPreviousSongs += safResult.preservedPrevious
                failedRoots += safResult.failedRoots
            }

            val authorizedFolderSucceeded =
                authorizedRoots.isNotEmpty() && failedRoots.size < authorizedRoots.size
            if (!mediaStoreSucceeded && !authorizedFolderSucceeded) {
                _scanProgress.value = ScanProgress(0, 0, ScanPhase.Error, 0)
                _scanDiagnostics.value = MediaScanDiagnostics(
                    mediaStoreCandidates = rawMediaStoreCount,
                    authorizedFolderCandidates = authorizedFolderCandidates,
                    acceptedSongs = persistedSongs.size,
                    failedAuthorizedFolders = failedAuthorizedFolders,
                    preservedPreviousSongs = persistedSongs.size,
                    durationMs = System.currentTimeMillis() - startTime,
                    completedAtMs = System.currentTimeMillis(),
                    failed = true,
                )
                return@withContext persistedSongs.map { it.toSongModel() }
            }

            if (mediaStoreFailed) {
                persistedSongs.filterNot { it.id.startsWith(DeviceDocumentPolicy.MEDIA_ID_PREFIX) }
                    .forEach { entity ->
                        if (seenIds.add(entity.id)) {
                            scannedSongs += entity
                            entity.path?.let { seenPaths += normalizePath(it) }
                            preservedPreviousSongs++
                        }
                    }
            }
            failedRoots.forEach { failedRoot ->
                persistedSongs.filter { belongsToRoot(it, failedRoot) }.forEach { entity ->
                    if (seenIds.add(entity.id)) {
                        scannedSongs += entity
                        entity.path?.let { seenPaths += normalizePath(it) }
                        preservedPreviousSongs++
                    }
                }
            }

            // Sync with Room DB atomically
            _scanProgress.value = ScanProgress(scannedSongs.size, scannedSongs.size, ScanPhase.SavingDb, 0)
            database.withTransaction {
                if (forceRefresh) {
                    database.songDao().replaceAll(scannedSongs)
                } else {
                    val staleSongIds = existingDbSongs.keys - seenIds
                    if (staleSongIds.isNotEmpty()) {
                        database.songDao().deleteByIds(staleSongIds.toList())
                    }
                    database.songDao().upsertAll(scannedSongs)
                }
            }

            appSettings.setLastScanTimestamp(System.currentTimeMillis())
            try {
                context.getSharedPreferences("library_scan_metadata", Context.MODE_PRIVATE)
                    .edit { putInt("last_scan_mediastore_count", rawMediaStoreCount) }
                Log.d(TAG, "Saved MediaStore count ($rawMediaStoreCount) to library_scan_metadata")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save MediaStore count", e)
            }

            val totalDuration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Scan completed: ${scannedSongs.size} songs processed in ${totalDuration}ms")
            _scanProgress.value = ScanProgress(scannedSongs.size, scannedSongs.size, ScanPhase.Complete, totalDuration)
            _scanDiagnostics.value = MediaScanDiagnostics(
                mediaStoreCandidates = rawMediaStoreCount,
                authorizedFolderCandidates = authorizedFolderCandidates,
                authorizedFolderAccepted = authorizedFolderAccepted,
                acceptedSongs = scannedSongs.size,
                filteredByFormat = filteredByFormat,
                filteredByDuration = filteredByDuration,
                filteredByBitrate = filteredByBitrate,
                filteredByFolderRule = filteredByFolderRule,
                duplicates = duplicates,
                unreadableFiles = unreadableFiles,
                failedAuthorizedFolders = failedAuthorizedFolders,
                preservedPreviousSongs = preservedPreviousSongs,
                durationMs = totalDuration,
                completedAtMs = System.currentTimeMillis(),
            )

            scannedSongs.map { it.toSongModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Error during media scan", e)
            _scanProgress.value = ScanProgress(0, 0, ScanPhase.Error, 0)
            val totalDuration = System.currentTimeMillis() - startTime
            _scanDiagnostics.value = MediaScanDiagnostics(
                mediaStoreCandidates = rawMediaStoreCount,
                authorizedFolderCandidates = authorizedFolderCandidates,
                acceptedSongs = persistedSongs.size,
                filteredByFormat = filteredByFormat,
                filteredByDuration = filteredByDuration,
                filteredByBitrate = filteredByBitrate,
                filteredByFolderRule = filteredByFolderRule,
                duplicates = duplicates,
                unreadableFiles = unreadableFiles,
                failedAuthorizedFolders = failedAuthorizedFolders,
                preservedPreviousSongs = persistedSongs.size,
                durationMs = totalDuration,
                completedAtMs = System.currentTimeMillis(),
                failed = true,
            )
            persistedSongs.map { it.toSongModel() }
        }
    }

    private data class SafScanResult(
        val songs: List<SongEntity>,
        val candidates: Int,
        val filteredByFormat: Int,
        val filteredByDuration: Int,
        val filteredByBitrate: Int,
        val filteredByFolderRule: Int,
        val duplicates: Int,
        val unreadable: Int,
        val preservedPrevious: Int,
        val failedRoots: List<DeviceScanRoot>,
    )

    private data class MutableSafCounters(
        var candidates: Int = 0,
        var filteredByFormat: Int = 0,
        var filteredByDuration: Int = 0,
        var filteredByBitrate: Int = 0,
        var filteredByFolderRule: Int = 0,
        var duplicates: Int = 0,
        var unreadable: Int = 0,
        var preservedPrevious: Int = 0,
    )

    private data class DocumentQueueEntry(
        val document: DocumentFile,
        val depth: Int,
    )

    private fun scanAuthorizedRoots(
        roots: List<DeviceScanRoot>,
        includeHidden: Boolean,
        allowedFormats: Set<String>?,
        minimumBitrate: Int,
        minimumDuration: Long,
        blacklistedSongs: List<String>,
        seenIds: MutableSet<String>,
        seenPaths: MutableSet<String>,
        existingSongs: Map<String, SongEntity>,
    ): SafScanResult {
        val songs = mutableListOf<SongEntity>()
        val failedRoots = mutableListOf<DeviceScanRoot>()
        val totals = MutableSafCounters()

        roots.distinctBy(DeviceScanRoot::treeUri).forEach { root ->
            val rootSongs = mutableListOf<SongEntity>()
            val rootCounters = MutableSafCounters()
            val rootSeenIds = (seenIds + songs.map(SongEntity::id)).toMutableSet()
            val rootSeenPaths = (seenPaths + songs.mapNotNull(SongEntity::path)).toMutableSet()

            val succeeded = runCatching {
                val treeUri = Uri.parse(root.treeUri)
                val tree = DocumentFile.fromTreeUri(context, treeUri)
                    ?: error("Unable to open authorized document tree")
                check(tree.exists() && tree.isDirectory && tree.canRead()) {
                    "Authorized document tree is not readable"
                }

                val queue = ArrayDeque<DocumentQueueEntry>()
                val visited = mutableSetOf<String>()
                queue.addLast(DocumentQueueEntry(tree, 0))

                while (queue.isNotEmpty()) {
                    val (directory, depth) = queue.removeFirst()
                    if (depth > MAX_SAF_SCAN_DEPTH || !visited.add(directory.uri.toString())) continue
                    if (!includeHidden && directory.name.orEmpty().startsWith('.')) {
                        rootCounters.filteredByFolderRule++
                        continue
                    }

                    val children = directory.listFiles()
                    if (!includeHidden && children.any { it.name.equals(".nomedia", ignoreCase = true) }) {
                        rootCounters.filteredByFolderRule++
                        continue
                    }

                    children.forEach { child ->
                        if (child.isDirectory) {
                            if (!includeHidden && child.name.orEmpty().startsWith('.')) {
                                rootCounters.filteredByFolderRule++
                            } else {
                                queue.addLast(DocumentQueueEntry(child, depth + 1))
                            }
                            return@forEach
                        }
                        if (!child.isFile) return@forEach

                        rootCounters.candidates++
                        val name = child.name.orEmpty()
                        if (!includeHidden && name.startsWith('.')) {
                            rootCounters.filteredByFolderRule++
                            return@forEach
                        }
                        val enabledFormats = allowedFormats ?: SUPPORTED_AUDIO_EXTENSIONS
                        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        if (extension.isBlank() || extension !in enabledFormats) {
                            rootCounters.filteredByFormat++
                            return@forEach
                        }
                        val uri = child.uri
                        val id = DeviceDocumentPolicy.mediaId(uri)
                        if (id in rootSeenIds) {
                            rootCounters.duplicates++
                            return@forEach
                        }
                        if (id in blacklistedSongs) {
                            rootCounters.filteredByFolderRule++
                            return@forEach
                        }

                        val rawPath = DeviceScanFolderAccess.rawPath(uri)?.let(::normalizePath)
                        if (rawPath != null && rawPath in rootSeenPaths) {
                            rootCounters.duplicates++
                            return@forEach
                        }

                        if (!canOpenDocument(uri)) {
                            rootCounters.unreadable++
                            existingSongs[id]?.let { previous ->
                                rootSongs += previous
                                rootSeenIds += id
                                previous.path?.let(rootSeenPaths::add)
                                rootCounters.preservedPrevious++
                            }
                            return@forEach
                        }

                        val metadata = readDocumentMetadata(child, extension)
                        if (minimumDuration > 0 && metadata.durationMs < minimumDuration) {
                            rootCounters.filteredByDuration++
                            return@forEach
                        }
                        if (
                            minimumBitrate > 0 &&
                            metadata.bitrate != null &&
                            metadata.bitrate < minimumBitrate * 1000
                        ) {
                            rootCounters.filteredByBitrate++
                            return@forEach
                        }

                        val previous = existingSongs[id]
                        val modifiedAt = child.lastModified().takeIf { it > 0L }
                            ?: previous?.dateModified?.takeIf { it > 0L }
                            ?: System.currentTimeMillis()
                        val entity = SongEntity(
                            id = id,
                            title = metadata.title,
                            artist = metadata.artist,
                            album = metadata.album,
                            albumId = "document_album_${(metadata.album.lowercase(Locale.ROOT) + "|" + metadata.artist.lowercase(Locale.ROOT)).hashCode()}",
                            duration = metadata.durationMs,
                            uri = uri.toString(),
                            artworkUri = previous?.artworkUri,
                            trackNumber = metadata.trackNumber,
                            year = metadata.year,
                            genre = metadata.genre ?: previous?.genre,
                            dateAdded = previous?.dateAdded?.takeIf { it > 0L } ?: modifiedAt,
                            dateModified = modifiedAt,
                            albumArtist = metadata.albumArtist,
                            bitrate = metadata.bitrate,
                            sampleRate = metadata.sampleRate,
                            channels = metadata.channels,
                            codec = metadata.codec,
                            discNumber = metadata.discNumber,
                            path = rawPath,
                        )
                        rootSongs += entity
                        rootSeenIds += id
                        rawPath?.let(rootSeenPaths::add)
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "Authorized folder scan failed for provider ${Uri.parse(root.treeUri).authority}", error)
            }.isSuccess

            if (succeeded) {
                songs += rootSongs
                seenIds += rootSongs.map(SongEntity::id)
                seenPaths += rootSongs.mapNotNull(SongEntity::path)
                totals.add(rootCounters)
            } else {
                failedRoots += root
            }
        }

        return SafScanResult(
            songs = songs,
            candidates = totals.candidates,
            filteredByFormat = totals.filteredByFormat,
            filteredByDuration = totals.filteredByDuration,
            filteredByBitrate = totals.filteredByBitrate,
            filteredByFolderRule = totals.filteredByFolderRule,
            duplicates = totals.duplicates,
            unreadable = totals.unreadable,
            preservedPrevious = totals.preservedPrevious,
            failedRoots = failedRoots,
        )
    }

    private fun MutableSafCounters.add(other: MutableSafCounters) {
        candidates += other.candidates
        filteredByFormat += other.filteredByFormat
        filteredByDuration += other.filteredByDuration
        filteredByBitrate += other.filteredByBitrate
        filteredByFolderRule += other.filteredByFolderRule
        duplicates += other.duplicates
        unreadable += other.unreadable
        preservedPrevious += other.preservedPrevious
    }

    private data class DocumentMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val albumArtist: String?,
        val durationMs: Long,
        val bitrate: Int?,
        val sampleRate: Int?,
        val channels: Int?,
        val codec: String,
        val year: Int,
        val genre: String?,
        val trackNumber: Int,
        val discNumber: Int,
    )

    private fun readDocumentMetadata(file: DocumentFile, extension: String): DocumentMetadata {
        val retriever = MediaMetadataRetriever()
        val fallbackTitle = file.name.orEmpty().substringBeforeLast('.', file.name.orEmpty())
            .ifBlank { "Unknown" }
        return try {
            retriever.setDataSource(context, file.uri)
            val rawTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 0
            val title = normalizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            )?.takeUnless { it.equals("<unknown>", ignoreCase = true) } ?: fallbackTitle
            val artist = normalizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            )?.takeUnless { it.equals("<unknown>", ignoreCase = true) } ?: "Unknown Artist"
            val album = normalizeMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            )?.takeUnless { it.equals("<unknown>", ignoreCase = true) } ?: "Unknown Album"
            DocumentMetadata(
                title = title,
                artist = artist,
                album = album,
                albumArtist = normalizeMetadata(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                ),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()?.takeIf { it > 0 },
                sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                        ?.toIntOrNull()?.takeIf { it > 0 }
                } else null,
                channels = null,
                codec = extension.uppercase(Locale.ROOT),
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.take(4)?.toIntOrNull() ?: 0,
                genre = normalizeMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)),
                trackNumber = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack,
                discNumber = if (rawTrack >= 1000) {
                    rawTrack / 1000
                } else {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                        ?.substringBefore('/')?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                },
            )
        } catch (e: Exception) {
            Log.d(TAG, "Using fallback metadata for an authorized audio document", e)
            DocumentMetadata(
                title = fallbackTitle,
                artist = "Unknown Artist",
                album = "Unknown Album",
                albumArtist = null,
                durationMs = 0L,
                bitrate = null,
                sampleRate = null,
                channels = null,
                codec = extension.uppercase(Locale.ROOT),
                year = 0,
                genre = null,
                trackNumber = 0,
                discNumber = 1,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun canOpenDocument(uri: Uri): Boolean = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun readBitrate(uri: Uri): Int? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun normalizeMetadata(value: String?): String? =
        value?.let { io.github.cluno1.sonorus.util.MetadataHeuristics.normalizeMetadataText(it) }
            ?.trim()?.takeIf(String::isNotBlank)

    private fun normalizePath(path: String): String =
        DeviceScanFolderAccess.normalizePath(appSettings.normalizeStoragePath(path)).lowercase(Locale.ROOT)

    private fun isWithinFolder(path: String, folder: String): Boolean {
        if (!folder.trim().startsWith('/')) return false
        val normalizedFolder = normalizePath(folder)
        return path == normalizedFolder || path.startsWith("$normalizedFolder/")
    }

    private fun belongsToRoot(entity: SongEntity, root: DeviceScanRoot): Boolean {
        if (!entity.id.startsWith(DeviceDocumentPolicy.MEDIA_ID_PREFIX)) return false
        val itemUri = runCatching { Uri.parse(entity.uri) }.getOrNull() ?: return false
        val rootUri = runCatching { Uri.parse(root.treeUri) }.getOrNull() ?: return false
        if (itemUri.authority != rootUri.authority) return false
        val itemTreeId = runCatching { DocumentsContract.getTreeDocumentId(itemUri) }.getOrNull()
        val rootTreeId = runCatching { DocumentsContract.getTreeDocumentId(rootUri) }.getOrNull()
        return itemTreeId != null && itemTreeId == rootTreeId
    }

    private fun SongEntity.toSongModel(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        uri = uri.toUri(),
        artworkUri = artworkUri?.toUri(),
        trackNumber = trackNumber,
        year = year,
        genre = genre,
        dateAdded = dateAdded,
        dateModified = dateModified,
        albumArtist = albumArtist,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codec = codec,
        discNumber = discNumber,
        path = path,
    )

}
