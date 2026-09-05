package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import androidx.core.content.edit
import chromahub.rhythm.app.features.catalog.domain.RhythmNowPlayingItem
import chromahub.rhythm.app.features.catalog.domain.RhythmQueueEntry
import chromahub.rhythm.app.shared.data.model.Song
import com.google.gson.Gson

data class CatalogQueueRecord(
    val entries: List<CatalogQueueRecordEntry>,
    val currentIndex: Int,
    val positionMs: Long,
)

data class CatalogQueueRecordEntry(
    val source: String? = SOURCE_CATALOG,
    val deviceSongId: String? = null,
    val nowPlaying: RhythmNowPlayingItem? = null,
    val title: String? = null,
    val artist: String? = null,
    val arrangementName: String? = null,
    val assetId: String? = null,
    val cacheKey: String? = null,
    val durationMs: Long = 0L,
    val albumId: String = "",
    val artworkUrl: String? = null,
) {
    fun isDevice(): Boolean = source == SOURCE_DEVICE

    companion object {
        const val SOURCE_CATALOG = "catalog"
        const val SOURCE_DEVICE = "device"
    }
}

class CatalogQueueStore(context: Context, private val gson: Gson = Gson()) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun save(entries: List<RhythmQueueEntry>, currentIndex: Int, positionMs: Long) {
        val record = CatalogQueueRecord(
            entries = entries.map {
                CatalogQueueRecordEntry(
                    source = CatalogQueueRecordEntry.SOURCE_CATALOG,
                    nowPlaying = it.nowPlaying,
                    title = it.playback.title,
                    artist = it.playback.artist,
                    arrangementName = it.playback.arrangementName,
                    assetId = it.playback.assetId,
                    cacheKey = it.playback.cacheKey,
                    durationMs = it.playback.durationMs,
                    albumId = it.playback.albumId,
                    artworkUrl = it.playback.artworkUrl,
                )
            },
            currentIndex = currentIndex.coerceIn(entries.indices),
            positionMs = positionMs.coerceAtLeast(0),
        )
        preferences.edit(commit = true) { putString(KEY_QUEUE, gson.toJson(record)) }
    }

    fun saveUnified(
        songs: List<Song>,
        catalogEntriesByMediaId: Map<String, RhythmQueueEntry>,
        currentIndex: Int,
        positionMs: Long,
    ) {
        if (songs.isEmpty()) {
            clear()
            return
        }
        val record = CatalogQueueRecord(
            entries = songs.map { song ->
                val catalog = catalogEntriesByMediaId[song.id]
                if (catalog == null) {
                    CatalogQueueRecordEntry(
                        source = CatalogQueueRecordEntry.SOURCE_DEVICE,
                        deviceSongId = song.id,
                    )
                } else {
                    CatalogQueueRecordEntry(
                        source = CatalogQueueRecordEntry.SOURCE_CATALOG,
                        nowPlaying = catalog.nowPlaying,
                        title = catalog.playback.title,
                        artist = catalog.playback.artist,
                        arrangementName = catalog.playback.arrangementName,
                        assetId = catalog.playback.assetId,
                        cacheKey = catalog.playback.cacheKey,
                        durationMs = catalog.playback.durationMs,
                        albumId = catalog.playback.albumId,
                        artworkUrl = catalog.playback.artworkUrl,
                    )
                }
            },
            currentIndex = currentIndex.coerceIn(songs.indices),
            positionMs = positionMs.coerceAtLeast(0),
        )
        preferences.edit(commit = true) { putString(KEY_QUEUE, gson.toJson(record)) }
    }

    fun load(): CatalogQueueRecord? = preferences.getString(KEY_QUEUE, null)?.let {
        runCatching { gson.fromJson(it, CatalogQueueRecord::class.java) }
            .getOrNull()
            ?.takeIf { record -> record.entries.isNotEmpty() && record.currentIndex in record.entries.indices }
    }

    fun clear() = preferences.edit(commit = true) { remove(KEY_QUEUE) }

    private companion object {
        const val NAME = "rhythm_catalog_queue_v1"
        const val KEY_QUEUE = "queue"
    }
}
