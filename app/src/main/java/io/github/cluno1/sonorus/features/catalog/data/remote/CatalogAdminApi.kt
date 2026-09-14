package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal data class AdminDeviceDto(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("applicationId") val applicationId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("isAdministrator") val isAdministrator: Boolean?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("lastSeenAt") val lastSeenAt: String?,
)

internal data class AdminDeviceListDto(
    @SerializedName("items") val items: List<AdminDeviceDto>?,
)

internal data class AdministratorChangeDto(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("isAdministrator") val isAdministrator: Boolean?,
)

internal data class ChorusModerationSettingsDto(
    @SerializedName("automatic_approval") val automaticApproval: Boolean?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

internal data class ChorusModerationSettingsPatchDto(
    @SerializedName("automatic_approval") val automaticApproval: Boolean,
)

internal data class ChorusModerationItemDto(
    @SerializedName("work_id") val workId: String?,
    @SerializedName("project_title") val projectTitle: String?,
    @SerializedName("track") val track: ChorusTrackDto?,
)

internal data class ChorusModerationQueueDto(
    @SerializedName("items") val items: List<ChorusModerationItemDto>?,
)

internal data class ChorusModerationRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("gain_db") val gainDb: Double = 0.0,
    @SerializedName("pan") val pan: Double = 0.0,
)

internal interface CatalogAdminApi {
    @GET("v2/admin/devices")
    suspend fun devices(): Response<AdminDeviceListDto>

    @POST("v2/admin/devices/{id}/administrator")
    suspend fun grantAdministrator(@Path("id") deviceId: String): Response<AdministratorChangeDto>

    @DELETE("v2/admin/devices/{id}/administrator")
    suspend fun revokeAdministrator(@Path("id") deviceId: String): Response<AdministratorChangeDto>

    @POST("v2/admin/invites")
    suspend fun createInvite(@Body body: InviteRequest): Response<InviteDto>

    @GET("v2/admin/chorus/moderation-settings")
    suspend fun moderationSettings(): Response<ChorusModerationSettingsDto>

    @PATCH("v2/admin/chorus/moderation-settings")
    suspend fun updateModerationSettings(
        @Body body: ChorusModerationSettingsPatchDto,
    ): Response<ChorusModerationSettingsDto>

    @GET("v2/admin/chorus/tracks")
    suspend fun moderationTracks(
        @Query("status") status: String = "pending_review",
        @Query("limit") limit: Int = 100,
    ): Response<ChorusModerationQueueDto>

    @PATCH("v2/admin/chorus/tracks/{id}/moderation")
    suspend fun moderateTrack(
        @Path("id") trackId: String,
        @Header("If-Match") ifMatch: String,
        @Body body: ChorusModerationRequestDto,
    ): Response<ChorusTrackDto>
}
