package io.github.cluno1.sonorus.features.catalog.data

import io.github.cluno1.sonorus.features.catalog.domain.PlaybackDescriptor

/** Identity retained by the app-private cache independently of a short-lived delivery URL. */
internal data class CatalogCachedAudioIdentity(
    val assetId: String,
    val sha256: String,
    val byteSize: Long,
)

/** Result of exchanging one stable rendition URI at the start of a data-source open. */
internal sealed interface CatalogOpenDecision {
    data object UseCachedAudio : CatalogOpenDecision
    data class UseRemote(val request: CatalogResolvedRequest) : CatalogOpenDecision
    data class Unavailable(val cause: Throwable) : CatalogOpenDecision
}

/**
 * Pure orchestration for [CatalogDataSpecResolver]. Keeping this free of Android types lets the
 * presigned-URL expiry path be tested deterministically without depending on a production TTL.
 */
internal object CatalogOpenResolver {
    suspend fun resolve(
        renditionId: String,
        trustedServerUrl: String?,
        bearerToken: String?,
        cached: CatalogCachedAudioIdentity?,
        fetch: suspend (String) -> Result<PlaybackDescriptor>,
    ): CatalogOpenDecision {
        val descriptor = fetch(renditionId).getOrElse { error ->
            if (cached != null) return CatalogOpenDecision.UseCachedAudio
            return CatalogOpenDecision.Unavailable(error)
        }
        if (cached?.matches(descriptor) == true) return CatalogOpenDecision.UseCachedAudio

        return CatalogOpenDecision.UseRemote(
            CatalogDynamicDelivery.resolve(
                renditionId = renditionId,
                trustedServerUrl = trustedServerUrl,
                bearerToken = bearerToken,
            ) { descriptor },
        )
    }

    fun mergeHeaders(
        original: Map<String, String>,
        resolved: Map<String, String>,
        deviceProof: Map<String, String>,
    ): Map<String, String> = original
        .filterKeys { key ->
            !key.equals("Authorization", ignoreCase = true) &&
                !key.startsWith("X-Rhythm-", ignoreCase = true)
        } + resolved + deviceProof

    private fun CatalogCachedAudioIdentity.matches(descriptor: PlaybackDescriptor): Boolean =
        assetId == descriptor.assetId &&
            sha256.equals(descriptor.cacheKey.substringAfterLast(':'), ignoreCase = true) &&
            byteSize == descriptor.byteSize
}
