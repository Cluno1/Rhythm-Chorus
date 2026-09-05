package chromahub.rhythm.app.features.catalog.data

import chromahub.rhythm.app.features.catalog.data.local.CatalogOfflineCache
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import chromahub.rhythm.app.features.catalog.domain.PlaybackDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class CatalogPresignedExpiryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val renditionId = "33333333-3333-4333-8333-333333333333"
    private val nextRenditionId = "44444444-4444-4444-8444-444444444444"
    private val assetId = "55555555-5555-4555-8555-555555555555"
    private val nextAssetId = "66666666-6666-4666-8666-666666666666"
    private val hash = "a".repeat(64)

    @Test
    fun rejectedFirstSignatureIsReplacedWhenMedia3ReopensStableUri() = runBlocking {
        val clock = FakeClock(1_000)
        val catalog = FakeCatalog(clock, mutableListOf(-1, 60))
        val stableUri = CatalogPlaybackPolicy.deferredUri(renditionId)

        val rejected = resolveOpen(stableUri, catalog)
        assertFalse(FakeCos.accepts(rejected.url, clock.nowSeconds))

        // Models Media3 retrying DataSource.open with the unchanged queue URI after COS rejects 403.
        val retry = resolveOpen(stableUri, catalog)
        assertTrue(FakeCos.accepts(retry.url, clock.nowSeconds))
        assertEquals(listOf(renditionId, renditionId), catalog.requestedRenditions)
        assertNotEquals(rejected.url, retry.url)
        assertEquals(rejected.cacheKey, retry.cacheKey)
        assertEquals("rhythm:asset:$assetId:$hash", retry.cacheKey)
    }

    @Test
    fun reopeningAfterExpiryFetchesFreshDescriptorWithoutChangingAssetIdentity() = runBlocking {
        val clock = FakeClock(2_000)
        val catalog = FakeCatalog(clock, mutableListOf(5, 5))
        val stableUri = CatalogPlaybackPolicy.deferredUri(renditionId)

        val first = resolveOpen(stableUri, catalog)
        assertTrue(FakeCos.accepts(first.url, clock.nowSeconds))
        clock.nowSeconds += 6
        assertFalse(FakeCos.accepts(first.url, clock.nowSeconds))

        val reopened = resolveOpen(stableUri, catalog)
        assertTrue(FakeCos.accepts(reopened.url, clock.nowSeconds))
        assertEquals(2, catalog.requestedRenditions.size)
        assertNotEquals(first.url, reopened.url)
        assertEquals(first.cacheKey, reopened.cacheKey)
    }

    @Test
    fun switchingTracksResolvesEachStableRenditionAndKeepsCosFreeOfCredentials() = runBlocking {
        val clock = FakeClock(3_000)
        val catalog = FakeCatalog(clock, mutableListOf(60, 60))

        val first = resolveOpen(CatalogPlaybackPolicy.deferredUri(renditionId), catalog)
        val next = resolveOpen(CatalogPlaybackPolicy.deferredUri(nextRenditionId), catalog)

        assertEquals(listOf(renditionId, nextRenditionId), catalog.requestedRenditions)
        assertEquals("rhythm:asset:$assetId:$hash", first.cacheKey)
        assertEquals("rhythm:asset:$nextAssetId:$hash", next.cacheKey)
        assertEquals(emptyMap<String, String>(), first.headers)
        assertEquals(emptyMap<String, String>(), next.headers)

        val headers = CatalogOpenResolver.mergeHeaders(
            original = mapOf(
                "Authorization" to "Bearer must-not-reach-cos",
                "x-rhythm-device" to "must-not-reach-cos",
                "User-Agent" to "Rhythm-test",
            ),
            resolved = next.headers,
            deviceProof = emptyMap(),
        )
        assertEquals(mapOf("User-Agent" to "Rhythm-test"), headers)
    }

    @Test
    fun networkFailureFallsBackToVerifiedOfflineBytesWithoutMutatingCacheIdentity() = runBlocking {
        val bytes = "cached catalog audio".toByteArray()
        val cachedHash = sha256(bytes)
        val cache = CatalogOfflineCache(temporaryFolder.newFolder("offline-expiry"), maxBytes = 1024)
        val namespace = "b".repeat(64)
        cache.store(
            namespace = namespace,
            kind = CatalogOfflineCache.KIND_AUDIO,
            assetId = assetId,
            sha256 = cachedHash,
            byteSize = bytes.size.toLong(),
            mediaType = "audio/mpeg",
            renditionId = renditionId,
            input = bytes.inputStream(),
        )
        val cached = requireNotNull(cache.findAudio(namespace, renditionId))

        val decision = CatalogOpenResolver.resolve(
            renditionId = renditionId,
            trustedServerUrl = "https://api.example",
            bearerToken = "secret",
            cached = CatalogCachedAudioIdentity(cached.assetId, cached.sha256, cached.byteSize),
        ) { Result.failure(IOException("offline")) }

        assertEquals(CatalogOpenDecision.UseCachedAudio, decision)
        assertArrayEquals(bytes, cache.readAsset(namespace, assetId, cachedHash, bytes.size.toLong()))
        val after = requireNotNull(cache.findAudio(namespace, renditionId))
        assertEquals(assetId, after.assetId)
        assertEquals(cachedHash, after.sha256)
        assertEquals(bytes.size.toLong(), after.byteSize)
    }

    @Test
    fun networkFailureWithoutOfflineBytesRemainsUnavailable() = runBlocking {
        val failure = IOException("offline")
        val decision = CatalogOpenResolver.resolve(
            renditionId = renditionId,
            trustedServerUrl = "https://api.example",
            bearerToken = "secret",
            cached = null,
        ) { Result.failure(failure) }

        assertTrue(decision is CatalogOpenDecision.Unavailable)
        assertEquals(failure, (decision as CatalogOpenDecision.Unavailable).cause)
    }

    private suspend fun resolveOpen(stableUri: String, catalog: FakeCatalog): CatalogResolvedRequest {
        val id = requireNotNull(CatalogPlaybackPolicy.deferredRenditionId(stableUri))
        val decision = CatalogOpenResolver.resolve(
            renditionId = id,
            trustedServerUrl = "https://api.example",
            bearerToken = "secret",
            cached = null,
        ) { Result.success(catalog.fetch(it)) }
        return (decision as CatalogOpenDecision.UseRemote).request
    }

    private inner class FakeCatalog(
        private val clock: FakeClock,
        private val expiryOffsets: MutableList<Long>,
    ) {
        val requestedRenditions = mutableListOf<String>()

        fun fetch(requestedRenditionId: String): PlaybackDescriptor {
            requestedRenditions += requestedRenditionId
            val sequence = requestedRenditions.size
            val requestedAssetId = if (requestedRenditionId == renditionId) assetId else nextAssetId
            val expiry = clock.nowSeconds + expiryOffsets.removeAt(0)
            return PlaybackDescriptor(
                renditionId = requestedRenditionId,
                assetId = requestedAssetId,
                mediaType = "audio/mpeg",
                byteSize = 123,
                delivery = "signed_url",
                relativeUrl = "https://bucket.cos.ap-guangzhou.myqcloud.com/music/$requestedAssetId.mp3" +
                    "?q-sign-algorithm=sha1&q-signature=sig-$sequence&q-expiry=$expiry",
                cacheKey = "rhythm:asset:$requestedAssetId:$hash",
                etag = "\"sha256:$hash\"",
                supportsRange = true,
                expiresAt = "fixture-$expiry",
            )
        }
    }

    private data class FakeClock(var nowSeconds: Long)

    private object FakeCos {
        fun accepts(url: String, nowSeconds: Long): Boolean {
            val expiry = url.substringAfter("q-expiry=").substringBefore('&').toLong()
            return nowSeconds <= expiry
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
