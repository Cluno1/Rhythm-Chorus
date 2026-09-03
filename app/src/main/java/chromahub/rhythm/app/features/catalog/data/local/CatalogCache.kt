package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import androidx.core.content.edit
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
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

    private companion object {
        const val NAME = "rhythm_catalog_cache_v1"
        const val KEY_WORKS = "works"
        const val KEY_SYNC_CURSOR = "sync_cursor"
    }
}
