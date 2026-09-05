package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import androidx.core.content.edit
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
import chromahub.rhythm.app.features.catalog.domain.CatalogLibraryAlbum
import chromahub.rhythm.app.features.catalog.domain.CatalogLibrarySnapshot
import chromahub.rhythm.app.features.catalog.domain.CatalogLibrarySong
import chromahub.rhythm.app.features.catalog.domain.ScoreRevision
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class CatalogCache(context: Context, private val gson: Gson = Gson()) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun loadWorks(): List<WorkSummary> =
        read<List<WorkSummary>>(KEY_WORKS, object : TypeToken<List<WorkSummary>>() {}.type).orEmpty()
    fun saveWorks(works: List<WorkSummary>) = write(KEY_WORKS, works)

    fun loadBundle(workId: String): WorkBundle? = read(bundleKey(workId), WorkBundle::class.java)
    fun saveBundle(bundle: WorkBundle, etag: String?) {
        write(bundleKey(bundle.work.id), bundle)
        preferences.edit { etag?.let { putString(etagKey(bundle.work.id), it) } ?: remove(etagKey(bundle.work.id)) }
    }
    fun bundleEtag(workId: String): String? = preferences.getString(etagKey(workId), null)

    fun loadLibrary(): CatalogLibrarySnapshot? {
        val songs = read<List<CatalogLibrarySong>>(KEY_LIBRARY_SONGS, object : TypeToken<List<CatalogLibrarySong>>() {}.type)
            ?: return null
        val albums = read<List<CatalogLibraryAlbum>>(KEY_LIBRARY_ALBUMS, object : TypeToken<List<CatalogLibraryAlbum>>() {}.type)
            ?: return null
        return CatalogLibrarySnapshot(songs, albums, fromCache = true)
    }

    fun saveLibrary(snapshot: CatalogLibrarySnapshot) {
        // Songs, album summaries and detail projections describe one server snapshot. A single
        // SharedPreferences transaction prevents readers from observing a torn refresh.
        preferences.edit(commit = true) {
            putString(KEY_LIBRARY_SONGS, gson.toJson(snapshot.songs))
            putString(KEY_LIBRARY_ALBUMS, gson.toJson(snapshot.albums))
            snapshot.albums.forEach { album ->
                putString(libraryAlbumKey(album.id), gson.toJson(album))
            }
        }
    }

    fun loadLibraryAlbum(albumId: String): CatalogLibraryAlbum? =
        read(libraryAlbumKey(albumId), CatalogLibraryAlbum::class.java)

    fun saveLibraryAlbum(album: CatalogLibraryAlbum) = write(libraryAlbumKey(album.id), album)

    fun loadScoreRevision(revisionId: String): ScoreRevision? =
        read(scoreRevisionKey(revisionId), ScoreRevision::class.java)

    fun saveScoreRevision(revision: ScoreRevision) = write(scoreRevisionKey(revision.id), revision)

    fun syncCursor(): Long = preferences.getLong(KEY_SYNC_CURSOR, 0L)
    fun saveSyncCursor(cursor: Long) = preferences.edit(commit = true) { putLong(KEY_SYNC_CURSOR, cursor) }

    fun removeWorks(ids: Set<String>) {
        if (ids.isEmpty()) return
        saveWorks(loadWorks().filterNot { it.id in ids })
        preferences.edit(commit = true) {
            ids.forEach { remove(bundleKey(it)); remove(etagKey(it)) }
        }
    }

    fun clearSession() = preferences.edit(commit = true) { clear() }

    private fun <T> read(key: String, type: java.lang.reflect.Type): T? =
        preferences.getString(key, null)?.let { runCatching { gson.fromJson<T>(it, type) }.getOrNull() }
    private fun write(key: String, value: Any) = preferences.edit(commit = true) { putString(key, gson.toJson(value)) }
    private fun bundleKey(id: String) = "bundle:$id"
    private fun etagKey(id: String) = "etag:$id"
    private fun libraryAlbumKey(id: String) = "library_album:$id"
    private fun scoreRevisionKey(id: String) = "score_revision:$id"

    private companion object {
        const val NAME = "rhythm_catalog_cache_v1"
        const val KEY_WORKS = "works"
        const val KEY_SYNC_CURSOR = "sync_cursor"
        const val KEY_LIBRARY_SONGS = "library_songs"
        const val KEY_LIBRARY_ALBUMS = "library_albums"
    }
}
