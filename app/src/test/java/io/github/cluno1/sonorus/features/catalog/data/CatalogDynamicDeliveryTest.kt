package io.github.cluno1.sonorus.features.catalog.data

import io.github.cluno1.sonorus.features.catalog.domain.PlaybackDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CatalogDynamicDeliveryTest {
    private val renditionId = "33333333-3333-4333-8333-333333333333"
    private val assetId = "55555555-5555-4555-8555-555555555555"
    private val hash = "a".repeat(64)

    @Test
    fun resolvesAgainForEveryOpenAndNeverSendsBearerToCos() = runBlocking {
        var requestCount = 0
        suspend fun freshDescriptor(): PlaybackDescriptor {
            requestCount += 1
            return descriptor("sig-$requestCount")
        }

        val first = CatalogDynamicDelivery.resolve(renditionId, "https://api.example", "secret") { freshDescriptor() }
        val second = CatalogDynamicDelivery.resolve(renditionId, "https://api.example", "secret") { freshDescriptor() }

        assertEquals(2, requestCount)
        assertFalse(first.url == second.url)
        assertEquals(emptyMap<String, String>(), first.headers)
        assertEquals(emptyMap<String, String>(), second.headers)
    }

    @Test
    fun addsBearerOnlyForAuthenticatedBackendDelivery() = runBlocking {
        val descriptor = descriptor("unused").copy(
            delivery = "authenticated_url",
            relativeUrl = "https://api.example/v2/assets/$assetId/content",
            expiresAt = null,
        )
        val result = CatalogDynamicDelivery.resolve(renditionId, "https://api.example", "secret") { descriptor }
        assertEquals(mapOf("Authorization" to "Bearer secret"), result.headers)
    }

    private fun descriptor(signature: String) = PlaybackDescriptor(
        renditionId = renditionId,
        assetId = assetId,
        mediaType = "audio/mpeg",
        byteSize = 123,
        delivery = "signed_url",
        relativeUrl = "https://bible.cos.ap-guangzhou.myqcloud.com/music/a.mp3" +
            "?q-sign-algorithm=sha1&q-signature=$signature",
        cacheKey = "rhythm:asset:$assetId:$hash",
        etag = "\"sha256:$hash\"",
        supportsRange = true,
        expiresAt = "2026-09-05T12:00:00Z",
    )
}
