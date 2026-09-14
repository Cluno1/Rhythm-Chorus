package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.github.cluno1.sonorus.features.catalog.domain.CatalogFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAuthJsonContractTest {
    private val gson = GsonBuilder().create()

    @Test
    fun adminSessionUsesStableJsonFieldNames() {
        assertSerializedNames(AdminSessionRequest::class.java, "username", "password")
        assertEquals(
            setOf("username", "password"),
            gson.toJsonTree(AdminSessionRequest("owner", "secret")).asJsonObject.keySet(),
        )

        assertSerializedNames(AdminSessionDto::class.java, "accessToken", "expiresIn")
        val response = gson.fromJson(
            """{"accessToken":"token","expiresIn":900}""",
            AdminSessionDto::class.java,
        )
        assertEquals("token", response.accessToken)
        assertEquals(900L, response.expiresIn)
    }

    @Test
    fun invitationUsesStableJsonFieldNames() {
        assertSerializedNames(
            InviteRequest::class.java,
            "userId",
            "displayName",
            "replaceExistingDevice",
        )
        assertSerializedNames(InviteDto::class.java, "inviteCode", "userId", "expiresAt")
        assertEquals(
            setOf("userId", "displayName", "replaceExistingDevice"),
            gson.toJsonTree(InviteRequest("user", "User", true)).asJsonObject.keySet(),
        )
        assertSerializedNames(InviteChallengeRequest::class.java, "inviteCode")
    }

    @Test
    fun deviceEnrollmentAndRefreshUseStableJsonFieldNames() {
        assertSerializedNames(DeviceNonceRequest::class.java, "deviceId")
        assertSerializedNames(NonceDto::class.java, "nonce", "expiresAt")
        assertSerializedNames(
            EnrollRequest::class.java,
            "inviteCode",
            "nonce",
            "publicKeySpki",
            "signature",
            "displayName",
            "applicationId",
            "signingCertificateSha256",
        )
        assertSerializedNames(
            RefreshRequest::class.java,
            "deviceId",
            "sessionId",
            "timestamp",
            "nonce",
            "signature",
        )
        assertSerializedNames(
            DeviceSessionDto::class.java,
            "userId",
            "deviceId",
            "sessionId",
            "accessToken",
            "accessTokenExpiresIn",
            "sessionExpiresAt",
        )
    }

    @Test
    fun administratorDtosUseStableJsonFieldNames() {
        assertSerializedNames(
            AdminDeviceDto::class.java,
            "deviceId",
            "userId",
            "displayName",
            "applicationId",
            "status",
            "isAdministrator",
            "createdAt",
            "lastSeenAt",
        )
        assertSerializedNames(AdminDeviceListDto::class.java, "items")
        assertSerializedNames(AdministratorChangeDto::class.java, "deviceId", "isAdministrator")
    }

    @Test
    fun validationFailureIsNotReportedAsNetworkOutage() {
        val failure = catalogHttpFailure(422)

        assertTrue(failure is CatalogFailure.InvalidData)
        assertEquals("服务器数据无效：客户端与服务器协议不兼容（422）", failure.message)
    }

    private fun assertSerializedNames(type: Class<*>, vararg expected: String) {
        val actual = type.declaredFields.mapNotNull { field ->
            field.getAnnotation(SerializedName::class.java)?.value
        }.toSet()
        assertEquals(expected.toSet(), actual)
    }
}
