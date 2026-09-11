package io.github.cluno1.sonorus.features.catalog.domain

import io.github.cluno1.sonorus.features.catalog.data.remote.CatalogEndpoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class CatalogSmartEnrollmentTextTest {
    private val invite = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(24) { it.toByte() })

    @Test
    fun ipv4ProductionShapeRoundTripsWithinLengthLimit() {
        val token = CatalogSmartEnrollmentText.encode(
            normalizedServerUrl = "http://203.0.113.42:8010",
            inviteCode = invite,
            nonce = ByteArray(12) { (it + 1).toByte() },
        )

        assertEquals(83, token.length)
        assertTrue(token.length <= CatalogSmartEnrollmentText.MAX_TOKEN_LENGTH)
        assertTrue("复制整段到 Sonorus 音乐库服务器页：$token".length <= 140)
        assertEquals(
            CatalogSmartEnrollmentPayload("http://203.0.113.42:8010", invite),
            CatalogSmartEnrollmentText.decodeFromText("您好，复制整段：$token\n谢谢"),
        )
    }

    @Test
    fun usesFreshNonceForEveryEncoding() {
        val first = CatalogSmartEnrollmentText.encode("https://203.0.113.42", invite)
        val second = CatalogSmartEnrollmentText.encode("https://203.0.113.42", invite)

        assertNotEquals(first, second)
        assertEquals(
            CatalogSmartEnrollmentText.decodeFromText(first),
            CatalogSmartEnrollmentText.decodeFromText(second),
        )
    }

    @Test
    fun domainAndIpv6RoundTripWhenTheyFit() {
        listOf(
            "https://music.example:8443",
            "http://[2001:db8::1]:8010",
        ).forEach { serverUrl ->
            val token = CatalogSmartEnrollmentText.encode(
                serverUrl,
                invite,
                nonce = ByteArray(12) { 7 },
            )
            val decoded = CatalogSmartEnrollmentText.decodeFromText(token)
            assertTrue(CatalogEndpoint.sameOrigin(serverUrl.toHttpUrl(), decoded.serverUrl.toHttpUrl()))
            assertEquals(invite, decoded.inviteCode)
        }
    }

    @Test
    fun rejectsTamperingAndMalformedInput() {
        val token = CatalogSmartEnrollmentText.encode(
            "http://203.0.113.42:8010",
            invite,
            nonce = ByteArray(12) { 3 },
        )
        val replacement = if (token.last() == 'A') 'B' else 'A'

        assertReason(CatalogSmartEnrollmentError.INVALID_TOKEN) {
            CatalogSmartEnrollmentText.decodeFromText(token.dropLast(1) + replacement)
        }
        assertReason(CatalogSmartEnrollmentError.TOKEN_MISSING) {
            CatalogSmartEnrollmentText.decodeFromText("没有登记文本")
        }
        assertReason(CatalogSmartEnrollmentError.MULTIPLE_TOKENS) {
            CatalogSmartEnrollmentText.decodeFromText("$token $token")
        }
        assertReason(CatalogSmartEnrollmentError.INPUT_TOO_LONG) {
            CatalogSmartEnrollmentText.decodeFromText("x".repeat(4097))
        }
    }

    @Test
    fun rejectsInvalidInvitationAndOversizedDomain() {
        assertReason(CatalogSmartEnrollmentError.INVALID_TOKEN) {
            CatalogSmartEnrollmentText.encode("https://music.example", "not-an-invite")
        }
        assertReason(CatalogSmartEnrollmentError.TOKEN_TOO_LONG) {
            CatalogSmartEnrollmentText.encode("https://${"a".repeat(40)}.example", invite)
        }
    }

    private fun assertReason(
        expected: CatalogSmartEnrollmentError,
        block: () -> Unit,
    ) {
        val error = assertThrows(CatalogSmartEnrollmentException::class.java, block)
        assertEquals(expected, error.reason)
    }
}
