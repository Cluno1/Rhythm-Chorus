package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import androidx.core.content.edit
import chromahub.rhythm.app.features.catalog.domain.RhythmNowPlayingItem
import chromahub.rhythm.app.features.catalog.domain.RhythmQueueEntry
import com.google.gson.Gson

data class CatalogQueueRecord(
    val entries: List<CatalogQueueRecordEntry>,
    val currentIndex: Int,
    val positionMs: Long,
)

data class CatalogQueueRecordEntry(
    val nowPlaying: RhythmNowPlayingItem,
    val title: String,
    val artist: String,
    val arrangementName: String,
    val assetId: String?,
    val cacheKey: String?,
    val durationMs: Long = 0L,
    val albumId: String = "",
    val artworkUrl: String? = null,
)

class CatalogQueueStore(context: Context, private val gson: Gson = Gson()) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun save(entries: List<RhythmQueueEntry>, currentIndex: Int, positionMs: Long) {
        val record = CatalogQueueRecord(
            entries = entries.map {
                CatalogQueueRecordEntry(
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
