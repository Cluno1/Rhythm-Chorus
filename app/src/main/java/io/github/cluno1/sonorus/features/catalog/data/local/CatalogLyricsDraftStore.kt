package io.github.cluno1.sonorus.features.catalog.data.local

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import io.github.cluno1.sonorus.features.catalog.domain.CatalogLyricsDraft
import java.security.MessageDigest

internal object CatalogLyricsDraftCodec {
    private val gson = Gson()

    fun encode(draft: CatalogLyricsDraft): String = gson.toJson(draft)

    fun decode(value: String): CatalogLyricsDraft? = runCatching {
        gson.fromJson(value, CatalogLyricsDraft::class.java)
    }.getOrNull()?.takeIf { draft ->
        draft.namespace.isNotBlank() &&
            draft.renditionId.isNotBlank() &&
            draft.language.isNotBlank() &&
            draft.lyrics.isNotBlank() &&
            draft.baseRevision > 0 &&
            draft.format in SUPPORTED_FORMATS &&
            draft.status in SUPPORTED_STATUSES
    }

    private val SUPPORTED_FORMATS = setOf(
        "plain",
        "lrc",
        "enhanced_lrc",
        "ttml",
        "word_by_word_json",
    )
    private val SUPPORTED_STATUSES = setOf("pending", "synced", "conflict", "error")
}

class CatalogLyricsDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun load(namespace: String, renditionId: String, language: String): CatalogLyricsDraft? {
        val key = key(namespace, renditionId, language)
        val encoded = preferences.getString(key, null) ?: return null
        return CatalogLyricsDraftCodec.decode(encoded)?.takeIf {
            it.namespace == namespace &&
                it.renditionId == renditionId &&
                it.language.equals(language, ignoreCase = true)
        } ?: run {
            preferences.edit(commit = true) { remove(key) }
            null
        }
    }

    fun save(draft: CatalogLyricsDraft) {
        preferences.edit(commit = true) {
            putString(
                key(draft.namespace, draft.renditionId, draft.language),
                CatalogLyricsDraftCodec.encode(draft),
            )
        }
    }

    fun remove(namespace: String, renditionId: String, language: String) {
        preferences.edit(commit = true) { remove(key(namespace, renditionId, language)) }
    }

    private fun key(namespace: String, renditionId: String, language: String): String {
        val identity = "$namespace\n$renditionId\n${language.lowercase()}"
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val NAME = "rhythm_catalog_lyrics_drafts_v1"
    }
}
