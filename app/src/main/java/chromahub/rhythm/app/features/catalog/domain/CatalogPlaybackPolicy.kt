package chromahub.rhythm.app.features.catalog.domain

import java.net.URI

/**
 * Hard product boundary for the private Work catalog.
 *
 * Rhythm may play only an Asset selected through a backend Rendition playback response. Device
 * MediaStore entries, arbitrary content/file URIs, and third-party streaming URLs are rejected.
 * Offline playback remains supported by Media3's managed cache: the logical URI and cache key stay
 * the backend Asset identity even when the bytes are served from the local cache.
 */
object CatalogPlaybackPolicy {
    const val DEVICE_LIBRARY_ENABLED = false
    const val EXTERNAL_URI_PLAYBACK_ENABLED = false
    const val THIRD_PARTY_STREAMING_ENABLED = false

    private val uuid = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
    private val mediaIdPattern = Regex("^rhythm-catalog:rendition:($uuid):asset:($uuid)$")
    private val assetPathPattern = Regex("(?:^|/)v2/assets/($uuid)/content/?$")
    private val cacheKeyPattern = Regex("^rhythm:asset:($uuid):([0-9a-f]{64})$")

    fun allows(
        mediaId: String,
        uri: String?,
        customCacheKey: String?,
        trustedServerUrl: String?,
    ): Boolean {
        val mediaMatch = mediaIdPattern.matchEntire(mediaId) ?: return false
        val assetId = requestAssetId(uri, customCacheKey, trustedServerUrl) ?: return false
        return assetId.equals(mediaMatch.groupValues[2], ignoreCase = true)
    }

    /** Checks the URI/cache identity available to Media3's DataSpec resolver. */
    fun allowsAssetRequest(
        uri: String?,
        customCacheKey: String?,
        trustedServerUrl: String?,
    ): Boolean = requestAssetId(uri, customCacheKey, trustedServerUrl) != null

    private fun requestAssetId(
        uri: String?,
        customCacheKey: String?,
        trustedServerUrl: String?,
    ): String? {
        val cacheMatch = customCacheKey?.let(cacheKeyPattern::matchEntire) ?: return null
        val parsedUri = runCatching { URI(uri) }.getOrNull() ?: return null
        val trustedUri = runCatching { URI(trustedServerUrl) }.getOrNull() ?: return null
        if (parsedUri.scheme !in setOf("http", "https")) return null
        if (!sameOrigin(parsedUri, trustedUri)) return null
        val pathAssetId = assetPathPattern.find(parsedUri.path.orEmpty())?.groupValues?.get(1)
            ?: return null
        val cacheAssetId = cacheMatch.groupValues[1]
        return pathAssetId.takeIf { it.equals(cacheAssetId, ignoreCase = true) }
    }

    private fun sameOrigin(left: URI, right: URI): Boolean =
        left.scheme.equals(right.scheme, ignoreCase = true) &&
            left.host.equals(right.host, ignoreCase = true) &&
            effectivePort(left) == effectivePort(right)

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
}
