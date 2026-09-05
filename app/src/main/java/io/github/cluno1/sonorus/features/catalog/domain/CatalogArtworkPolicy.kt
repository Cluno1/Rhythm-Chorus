package io.github.cluno1.sonorus.features.catalog.domain

import java.net.URI
import java.util.Locale

/** Stable Catalog artwork identity; short-lived COS URLs never enter UI or persisted library data. */
object CatalogArtworkPolicy {
    private val uuid = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-" +
            "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
    )

    fun uri(assetId: String): String {
        require(uuid.matches(assetId)) { "assetId is not a UUID" }
        return "rhythm-catalog://asset/${assetId.lowercase(Locale.ROOT)}"
    }

    fun assetId(uri: String?): String? {
        val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
        if (!parsed.scheme.equals("rhythm-catalog", ignoreCase = true)) return null
        if (!parsed.host.equals("asset", ignoreCase = true)) return null
        if (parsed.userInfo != null || parsed.rawQuery != null || parsed.fragment != null) return null
        val id = parsed.path.orEmpty().removePrefix("/")
        return id.takeIf(uuid::matches)?.lowercase(Locale.ROOT)
    }
}
