package chromahub.rhythm.app.features.catalog.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogDeviceCanonicalTest {
    @Test
    fun requestCanonicalMatchesBackendContractExactly() {
        assertEquals(
            listOf(
                "RHYTHM-DEVICE-V1",
                "GET",
                "/v2/library/songs",
                "cursor=a%2Fb&limit=200",
                CatalogDeviceCanonical.emptySha256,
                "11111111-1111-4111-8111-111111111111",
                "1788595200",
                "server-nonce",
            ).joinToString("\n"),
            CatalogDeviceCanonical.request(
                method = "get",
                path = "/v2/library/songs",
                query = "cursor=a%2Fb&limit=200",
                bodySha256 = CatalogDeviceCanonical.emptySha256.uppercase(),
                deviceId = "11111111-1111-4111-8111-111111111111",
                timestamp = 1788595200,
                nonce = "server-nonce",
            ).decodeToString(),
        )
    }

    @Test
    fun enrollmentCanonicalMatchesBackendContractExactly() {
        assertEquals(
            "RHYTHM-ENROLL-V1\nnonce\ninvite\nthumbprint",
            CatalogDeviceCanonical.enrollment("nonce", "invite", "thumbprint").decodeToString(),
        )
    }
}
