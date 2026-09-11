package io.github.cluno1.sonorus.features.catalog.data.remote

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface CatalogChorusApi {
    @GET("v2/works/{id}/chorus")
    suspend fun workChorus(@Path("id") workId: String): Response<WorkChorusDto>

    @GET("v2/chorus-projects/{id}")
    suspend fun project(@Path("id") projectId: String): Response<ChorusProjectDto>

    @POST("v2/chorus-projects/{id}/tracks")
    suspend fun createTrack(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ChorusTrackCreateDto,
    ): Response<ChorusTrackCreateResponseDto>

    @PUT("v2/chorus-tracks/{id}/content")
    suspend fun uploadTrackContent(
        @Path("id") trackId: String,
        @Header("X-Sonorus-Content-SHA256") sha256: String,
        @Body body: RequestBody,
    ): Response<ChorusTrackDto>

    @POST("v2/chorus-tracks/{id}/complete")
    suspend fun completeTrack(
        @Path("id") trackId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): Response<ChorusTrackDto>

    @PATCH("v2/chorus-tracks/{id}/alignment")
    suspend fun updateAlignment(
        @Path("id") trackId: String,
        @Header("If-Match") ifMatch: String,
        @Body body: ChorusAlignmentPatchDto,
    ): Response<ChorusTrackDto>

    @POST("v2/chorus-tracks/{id}/submit")
    suspend fun submitTrack(
        @Path("id") trackId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): Response<ChorusTrackDto>

    @DELETE("v2/chorus-tracks/{id}")
    suspend fun withdrawTrack(@Path("id") trackId: String): Response<ChorusTrackDto>

    @POST("v2/chorus-projects/{id}/mixes:resolve")
    suspend fun resolveMix(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ChorusMixResolveDto,
    ): Response<ChorusMixDto>

    @GET("v2/chorus-mixes/{id}")
    suspend fun mix(@Path("id") mixId: String): Response<ChorusMixDto>
}
