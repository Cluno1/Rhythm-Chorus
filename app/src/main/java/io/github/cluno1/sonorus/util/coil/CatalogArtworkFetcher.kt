package io.github.cluno1.sonorus.util.coil

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import io.github.cluno1.sonorus.features.catalog.di.CatalogModule
import io.github.cluno1.sonorus.features.catalog.domain.CatalogArtworkPolicy
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException

/** Exchanges stable Catalog Asset URIs for fresh delivery descriptors only on a cache miss. */
class CatalogArtworkFetcher(
    private val context: Context,
    private val uri: Uri,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val assetId = CatalogArtworkPolicy.assetId(uri.toString())
            ?: throw IOException("Invalid Catalog artwork URI")
        val artwork = CatalogModule.repository(context).downloadArtwork(assetId).getOrElse {
            throw IOException("Unable to download Catalog artwork", it)
        }
        val source = ByteArrayInputStream(artwork.bytes).source().buffer()
        return SourceResult(
            source = ImageSource(source = source, context = context),
            mimeType = artwork.mediaType,
            dataSource = DataSource.NETWORK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            CatalogArtworkPolicy.assetId(data.toString())?.let { CatalogArtworkFetcher(context, data) }
    }
}

/** Asset IDs are immutable content identities, so this key remains valid across signed URL expiry. */
class CatalogArtworkKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String? =
        CatalogArtworkPolicy.assetId(data.toString())?.let { "catalog_artwork_$it" }
}
