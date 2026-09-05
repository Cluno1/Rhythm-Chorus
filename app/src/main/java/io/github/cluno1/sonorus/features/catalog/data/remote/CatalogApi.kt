package io.github.cluno1.sonorus.features.catalog.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.Streaming

/** Second-phase API surface. Deliberately contains only GET and HEAD operations. */
internal interface CatalogApi {
    @GET("healthz")
    suspend fun health(): Response<HealthDto>

    @GET("v2/works")
    suspend fun works(
        @Query("q") query: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50,
    ): Response<WorkPageDto>

    @GET("v2/library/songs")
    suspend fun librarySongs(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 200,
    ): Response<LibrarySongPageDto>

    @GET("v2/library/albums")
    suspend fun libraryAlbums(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 200,
    ): Response<LibraryAlbumPageDto>

    @GET("v2/library/albums/{id}")
    suspend fun libraryAlbum(@Path("id") albumId: String): Response<LibraryAlbumDetailDto>

    @GET("v2/works/{id}/bundle")
    suspend fun workBundle(
        @Path("id") workId: String,
        @Header("If-None-Match") etag: String? = null,
    ): Response<WorkBundleDto>

    @GET("v2/score-revisions/{id}")
    suspend fun scoreRevision(@Path("id") revisionId: String): Response<ScoreRevisionDto>

    @GET("v2/renditions/{id}/playback")
    suspend fun playback(
        @Path("id") renditionId: String,
        @Query("prefer") prefer: String = "stream",
    ): Response<PlaybackDto>

    @GET("v2/assets/{id}/delivery")
    suspend fun assetDelivery(@Path("id") assetId: String): Response<AssetDeliveryDto>

    @Streaming
    @GET
    suspend fun deliveredAsset(@Url url: String): Response<ResponseBody>

    @Streaming
    @GET("v2/assets/{id}/content")
    suspend fun asset(@Path("id") assetId: String): Response<ResponseBody>

    @HEAD("v2/assets/{id}/content")
    suspend fun headAsset(@Path("id") assetId: String): Response<Void>

    @GET("v2/sync/changes")
    suspend fun changes(
        @Query("after") after: Long,
        @Query("limit") limit: Int = 100,
    ): Response<ChangesDto>
}

internal data class HealthDto(val status: String? = null, val version: String? = null)
