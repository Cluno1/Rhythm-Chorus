package io.github.cluno1.sonorus.features.catalog.data.remote

import android.os.Build
import io.github.cluno1.sonorus.features.catalog.data.CatalogCredentialsStore
import io.github.cluno1.sonorus.features.catalog.data.CatalogDeviceCredentials
import io.github.cluno1.sonorus.features.catalog.data.CatalogDeviceKey
import io.github.cluno1.sonorus.features.catalog.data.CatalogSigner
import io.github.cluno1.sonorus.features.catalog.domain.CatalogFailure
import io.github.cluno1.sonorus.features.catalog.domain.CatalogIssuedInvite
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.TimeUnit

internal data class AdminSessionRequest(val username: String, val password: String)
internal data class AdminSessionDto(val accessToken: String, val expiresIn: Long)
internal data class InviteRequest(
    val userId: String,
    val displayName: String? = null,
    val replaceExistingDevice: Boolean = false,
)
internal data class InviteDto(val inviteCode: String, val userId: String, val expiresAt: String)
internal fun InviteDto.toIssuedInvite(): CatalogIssuedInvite = CatalogIssuedInvite(
    inviteCode = inviteCode,
    userId = userId,
    expiresAt = expiresAt,
)
internal data class InviteChallengeRequest(val inviteCode: String)
internal data class DeviceNonceRequest(val deviceId: String)
internal data class NonceDto(val nonce: String, val expiresAt: String)
internal data class EnrollRequest(
    val inviteCode: String,
    val nonce: String,
    val publicKeySpki: String,
    val signature: String,
    val displayName: String,
)
internal data class RefreshRequest(
    val deviceId: String,
    val sessionId: String,
    val timestamp: Long,
    val nonce: String,
    val signature: String,
)
internal data class DeviceSessionDto(
    val userId: String,
    val deviceId: String,
    val sessionId: String,
    val accessToken: String,
    val accessTokenExpiresIn: Long,
    val sessionExpiresAt: String,
)

internal interface CatalogDeviceAuthApi {
    @POST("v2/admin/session")
    suspend fun adminSession(@Body body: AdminSessionRequest): Response<AdminSessionDto>

    @POST("v2/admin/invites")
    suspend fun createInvite(
        @Header("Authorization") authorization: String,
        @Body body: InviteRequest,
    ): Response<InviteDto>

    @GET("healthz")
    suspend fun health(): Response<HealthDto>

    @POST("v2/device/challenge")
    suspend fun enrollmentChallenge(@Body body: InviteChallengeRequest): Response<NonceDto>

    @POST("v2/device/enroll")
    suspend fun enroll(@Body body: EnrollRequest): Response<DeviceSessionDto>

    @POST("v2/device/nonce")
    fun nonce(
        @Header("Authorization") authorization: String,
        @Body body: DeviceNonceRequest,
    ): Call<NonceDto>

    @POST("v2/device/session/challenge")
    fun refreshChallenge(
        @Header("X-Rhythm-Session-ID") sessionId: String,
        @Body body: DeviceNonceRequest,
    ): Call<NonceDto>

    @POST("v2/device/session/refresh")
    fun refresh(@Body body: RefreshRequest): Call<DeviceSessionDto>
}

internal object CatalogDeviceCanonical {
    val emptySha256: String = sha256Hex(ByteArray(0))

    fun enrollment(nonce: String, inviteCode: String, keyThumbprint: String): ByteArray =
        "RHYTHM-ENROLL-V1\n$nonce\n$inviteCode\n$keyThumbprint".toByteArray()

    fun refresh(deviceId: String, sessionId: String, timestamp: Long, nonce: String): ByteArray =
        "RHYTHM-REFRESH-V1\n$deviceId\n$sessionId\n$timestamp\n$nonce".toByteArray()

    fun request(
        method: String,
        path: String,
        query: String,
        bodySha256: String,
        deviceId: String,
        timestamp: Long,
        nonce: String,
    ): ByteArray = listOf(
        "RHYTHM-DEVICE-V1",
        method.uppercase(),
        path,
        query,
        bodySha256.lowercase(),
        deviceId,
        timestamp.toString(),
        nonce,
    ).joinToString("\n").toByteArray()

    fun publicKeyThumbprint(publicKeySpki: String): String = sha256Hex(
        java.util.Base64.getUrlDecoder().decode(publicKeySpki),
    )

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

internal class CatalogDeviceAuthClient(
    serverUrl: String,
    private val credentials: CatalogCredentialsStore,
    private val signer: CatalogSigner = CatalogDeviceKey,
) {
    private val origin = (CatalogEndpoint.normalize(serverUrl) + "/").toHttpUrl()
    private val api = Retrofit.Builder()
        .baseUrl(origin)
        .client(baseHttpClient())
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(CatalogDeviceAuthApi::class.java)

    suspend fun enroll(inviteCode: String): Unit {
        val normalizedInvite = inviteCode.trim()
        require(normalizedInvite.isNotEmpty()) { "邀请码不能为空" }
        val health = api.health()
        if (!health.isSuccessful) throw IOException("服务器健康检查失败（${health.code()}）")
        val challenge = api.enrollmentChallenge(InviteChallengeRequest(normalizedInvite)).bodyOrThrow()
        val publicKey = signer.publicKeySpki()
        val signature = signer.sign(
            CatalogDeviceCanonical.enrollment(
                challenge.nonce,
                normalizedInvite,
                CatalogDeviceCanonical.publicKeyThumbprint(publicKey),
            ),
        )
        val enrolled = api.enroll(
            EnrollRequest(
                inviteCode = normalizedInvite,
                nonce = challenge.nonce,
                publicKeySpki = publicKey,
                signature = signature,
                displayName = listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Android device" },
            ),
        ).bodyOrThrow()
        credentials.saveDevice(enrolled.toCredentials(origin.toString().trimEnd('/')))
    }

    suspend fun issueInvite(
        username: String,
        password: String,
        userId: String,
        displayName: String?,
        replaceExistingDevice: Boolean,
    ): CatalogIssuedInvite {
        val session = api.adminSession(AdminSessionRequest(username.trim(), password)).bodyOrThrow()
        return api.createInvite(
            "Bearer ${session.accessToken}",
            InviteRequest(
                userId.trim(),
                displayName?.trim()?.takeIf { it.isNotEmpty() },
                replaceExistingDevice,
            ),
        ).bodyOrThrow().toIssuedInvite()
    }

    @Synchronized
    fun proof(request: Request): Map<String, String> {
        require(request.method == "GET" || request.method == "HEAD") {
            "public Catalog only signs GET and HEAD requests"
        }
        val current = accessCredentials()
        val nonce = api.nonce(
            "Device ${current.accessToken}",
            DeviceNonceRequest(current.deviceId),
        ).execute().bodyOrThrow().nonce
        val timestamp = Instant.now().epochSecond
        val canonical = CatalogDeviceCanonical.request(
            request.method,
            request.url.encodedPath,
            request.url.encodedQuery.orEmpty(),
            CatalogDeviceCanonical.emptySha256,
            current.deviceId,
            timestamp,
            nonce,
        )
        return mapOf(
            "Authorization" to "Device ${current.accessToken}",
            "X-Rhythm-Device-ID" to current.deviceId,
            "X-Rhythm-Timestamp" to timestamp.toString(),
            "X-Rhythm-Nonce" to nonce,
            "X-Rhythm-Content-SHA256" to CatalogDeviceCanonical.emptySha256,
            "X-Rhythm-Signature" to signer.sign(canonical),
        )
    }

    fun proofForGet(url: String): Map<String, String> = proof(
        Request.Builder().url(url).get().build(),
    )

    private fun accessCredentials(): CatalogDeviceCredentials {
        var current = credentials.loadDevice() ?: throw IOException("设备尚未登记")
        if (current.accessTokenExpiresAtEpochSeconds > Instant.now().epochSecond + 30) return current
        val challenge = api.refreshChallenge(
            current.sessionId,
            DeviceNonceRequest(current.deviceId),
        ).execute().bodyOrThrow()
        val timestamp = Instant.now().epochSecond
        val refreshed = api.refresh(
            RefreshRequest(
                current.deviceId,
                current.sessionId,
                timestamp,
                challenge.nonce,
                signer.sign(
                    CatalogDeviceCanonical.refresh(
                        current.deviceId,
                        current.sessionId,
                        timestamp,
                        challenge.nonce,
                    ),
                ),
            ),
        ).execute().bodyOrThrow()
        current = refreshed.toCredentials(current.serverUrl)
        credentials.saveDevice(current)
        return current
    }

    private fun DeviceSessionDto.toCredentials(serverUrl: String) = CatalogDeviceCredentials(
        serverUrl = serverUrl,
        userId = userId,
        deviceId = deviceId,
        sessionId = sessionId,
        accessToken = accessToken,
        accessTokenExpiresAtEpochSeconds = Instant.now().epochSecond + accessTokenExpiresIn,
        sessionExpiresAt = sessionExpiresAt,
    )

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) {
            throw when (code()) {
                401 -> CatalogFailure.InvalidCredentials()
                409 -> CatalogFailure.InvalidData("该用户已有登记设备，请生成‘替换已有设备’邀请码")
                429 -> CatalogFailure.InvalidData("请求过于频繁，请稍后再试")
                else -> IOException("服务器拒绝请求（${code()}）")
            }
        }
        return body() ?: throw IOException("服务器返回空响应")
    }

    private fun baseHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
