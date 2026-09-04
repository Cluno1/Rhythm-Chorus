package chromahub.rhythm.app.features.catalog.data

import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import chromahub.rhythm.app.features.catalog.domain.PlaybackDescriptor

internal data class CatalogResolvedRequest(
    val url: String,
    val cacheKey: String,
    val headers: Map<String, String>,
)

internal object CatalogDynamicDelivery {
    suspend fun resolve(
        renditionId: String,
        trustedServerUrl: String?,
        bearerToken: String?,
        fetch: suspend (String) -> PlaybackDescriptor,
    ): CatalogResolvedRequest {
        val descriptor = fetch(renditionId)
        require(descriptor.renditionId == renditionId) { "resolved rendition identity changed" }
        require(
            CatalogPlaybackPolicy.allows(
                mediaId = "rhythm-catalog:rendition:${descriptor.renditionId}:asset:${descriptor.assetId}",
                uri = descriptor.relativeUrl,
                customCacheKey = descriptor.cacheKey,
                mediaType = descriptor.mediaType,
                trustedServerUrl = trustedServerUrl,
            )
        ) { "resolved catalog descriptor failed playback policy" }
        val headers = if (CatalogPlaybackPolicy.isSignedObjectStoreUrl(descriptor.relativeUrl)) {
            emptyMap()
        } else {
            bearerToken?.takeIf { it.isNotBlank() }
                ?.let { mapOf("Authorization" to "Bearer $it") }
                .orEmpty()
        }
        return CatalogResolvedRequest(descriptor.relativeUrl, descriptor.cacheKey, headers)
    }
}
