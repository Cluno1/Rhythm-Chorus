package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import chromahub.rhythm.app.features.catalog.di.CatalogModule
import chromahub.rhythm.app.features.catalog.data.local.CatalogOfflineCache
import chromahub.rhythm.app.features.catalog.data.remote.CatalogDeviceAuthClient
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import kotlinx.coroutines.runBlocking
import java.io.IOException

/** Resolves stable catalog queue URIs only when Media3 is about to open that track. */
@UnstableApi
object CatalogDataSpecResolver {
    fun resolve(context: Context, dataSpec: DataSpec): DataSpec {
        val credentials = CatalogCredentialsStore(context)
        val trustedServerUrl = credentials.loadServerUrl()
        val deferredRenditionId = CatalogPlaybackPolicy.deferredRenditionId(dataSpec.uri.toString())
        if (deferredRenditionId != null) {
            val token = credentials.loadToken()
            val cached = if (!trustedServerUrl.isNullOrBlank() && !token.isNullOrBlank()) {
                val namespace = CatalogOfflineCache.namespace(trustedServerUrl, token)
                CatalogOfflineCache(context).findAudio(namespace, deferredRenditionId)
            } else {
                null
            }
            val descriptor = runBlocking {
                CatalogModule.repository(context).getPlayback(deferredRenditionId)
            }.getOrElse { error ->
                cached?.let { return cachedDataSpec(dataSpec, it) }
                throw IOException("Unable to resolve catalog rendition", error)
            }
            val descriptorHash = descriptor.cacheKey.substringAfterLast(':')
            if (
                cached != null &&
                cached.assetId == descriptor.assetId &&
                cached.sha256.equals(descriptorHash, ignoreCase = true) &&
                cached.byteSize == descriptor.byteSize
            ) {
                return cachedDataSpec(dataSpec, cached)
            }
            val resolved = runBlocking {
                CatalogDynamicDelivery.resolve(
                    renditionId = deferredRenditionId,
                    trustedServerUrl = trustedServerUrl,
                    bearerToken = token,
                ) { _ -> descriptor }
            }
            val deviceHeaders = if (
                !CatalogPlaybackPolicy.isSignedObjectStoreUrl(resolved.url) &&
                credentials.loadDevice() != null &&
                !trustedServerUrl.isNullOrBlank()
            ) {
                CatalogDeviceAuthClient(trustedServerUrl, credentials).proofForGet(resolved.url)
            } else {
                emptyMap()
            }
            val headers = dataSpec.httpRequestHeaders
                .filterKeys { key ->
                    !key.equals("Authorization", ignoreCase = true) &&
                        !key.startsWith("X-Rhythm-", ignoreCase = true)
                } + resolved.headers + deviceHeaders
            return dataSpec.buildUpon()
                .setUri(resolved.url)
                .setKey(resolved.cacheKey)
                .setHttpRequestHeaders(headers)
                .build()
        }

        if (CatalogPlaybackPolicy.allowsAssetRequest(dataSpec.uri.toString(), dataSpec.key, trustedServerUrl)) {
            return authorize(dataSpec, dataSpec.uri.toString(), credentials)
        }
        return dataSpec
    }

    private fun cachedDataSpec(
        dataSpec: DataSpec,
        cached: CatalogOfflineCache.CachedAsset,
    ): DataSpec = dataSpec.buildUpon()
        .setUri(cached.uri)
        .setKey("rhythm:asset:${cached.assetId}:${cached.sha256}")
        .setHttpRequestHeaders(emptyMap())
        .build()

    private fun authorize(
        dataSpec: DataSpec,
        resolvedUrl: String,
        credentials: CatalogCredentialsStore,
    ): DataSpec {
        val headers = dataSpec.httpRequestHeaders
            .filterKeys { !it.equals("Authorization", ignoreCase = true) }
            .toMutableMap()
        if (!CatalogPlaybackPolicy.isSignedObjectStoreUrl(resolvedUrl)) {
            val device = credentials.loadDevice()
            if (device != null) {
                CatalogDeviceAuthClient(device.serverUrl, credentials)
                    .proofForGet(resolvedUrl)
                    .forEach(headers::put)
            } else {
                credentials.loadToken()?.takeIf { it.isNotBlank() }
                    ?.let { headers["Authorization"] = "Bearer $it" }
            }
        }
        return dataSpec.withRequestHeaders(headers)
    }
}
