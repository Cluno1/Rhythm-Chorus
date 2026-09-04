package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import chromahub.rhythm.app.features.catalog.di.CatalogModule
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
            val resolved = runBlocking {
                CatalogDynamicDelivery.resolve(
                    renditionId = deferredRenditionId,
                    trustedServerUrl = trustedServerUrl,
                    bearerToken = credentials.loadToken(),
                ) { renditionId ->
                    CatalogModule.repository(context).getPlayback(renditionId).getOrElse {
                        throw IOException("Unable to resolve catalog rendition", it)
                    }
                }
            }
            val headers = dataSpec.httpRequestHeaders
                .filterKeys { !it.equals("Authorization", ignoreCase = true) } + resolved.headers
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

    private fun authorize(
        dataSpec: DataSpec,
        resolvedUrl: String,
        credentials: CatalogCredentialsStore,
    ): DataSpec {
        val headers = dataSpec.httpRequestHeaders
            .filterKeys { !it.equals("Authorization", ignoreCase = true) }
            .toMutableMap()
        if (!CatalogPlaybackPolicy.isSignedObjectStoreUrl(resolvedUrl)) {
            credentials.loadToken()?.takeIf { it.isNotBlank() }?.let { headers["Authorization"] = "Bearer $it" }
        }
        return dataSpec.withRequestHeaders(headers)
    }
}
