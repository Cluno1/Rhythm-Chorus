package io.github.cluno1.sonorus.features.catalog.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class CatalogOfflineCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun productionLimitIsExactlyTenGibibytes() {
        assertEquals(10L * 1024L * 1024L * 1024L, CatalogOfflineCache.MAX_BYTES)
    }

    @Test
    fun evictsLeastRecentlyUsedAssetBeforeCrossingLimit() {
        val cache = CatalogOfflineCache(temporaryFolder.newFolder("offline"), maxBytes = 7)
        val namespace = "a".repeat(64)
        val first = "first".toByteArray().copyOf(4)
        val second = "second".toByteArray().copyOf(4)
        val firstRendition = "11111111-1111-4111-8111-111111111111"
        val secondRendition = "22222222-2222-4222-8222-222222222222"

        cache.store(
            namespace,
            CatalogOfflineCache.KIND_AUDIO,
            "33333333-3333-4333-8333-333333333333",
            sha256(first),
            first.size.toLong(),
            "audio/mpeg",
            renditionId = firstRendition,
            input = first.inputStream(),
        )
        cache.store(
            namespace,
            CatalogOfflineCache.KIND_AUDIO,
            "44444444-4444-4444-8444-444444444444",
            sha256(second),
            second.size.toLong(),
            "audio/mpeg",
            renditionId = secondRendition,
            input = second.inputStream(),
        )

        assertNull(cache.findAudio(namespace, firstRendition))
        assertNotNull(cache.findAudio(namespace, secondRendition))
        assertEquals(4L, cache.usedBytes())
    }

    @Test
    fun namespaceDoesNotExposeServerOrToken() {
        val namespace = CatalogOfflineCache.namespace("https://music.example", "secret-token")
        assertEquals(64, namespace.length)
        assertFalse(namespace.contains("music"))
        assertFalse(namespace.contains("secret"))
    }

    @Test
    fun newerAssetReplacesOlderBytesForTheSameRendition() {
        val cache = CatalogOfflineCache(temporaryFolder.newFolder("replacement"), maxBytes = 20)
        val namespace = "b".repeat(64)
        val renditionId = "11111111-1111-4111-8111-111111111111"
        val oldBytes = byteArrayOf(1, 2, 3, 4)
        val newBytes = byteArrayOf(5, 6, 7, 8, 9)

        cache.store(
            namespace,
            CatalogOfflineCache.KIND_AUDIO,
            "33333333-3333-4333-8333-333333333333",
            sha256(oldBytes),
            oldBytes.size.toLong(),
            "audio/mpeg",
            renditionId = renditionId,
            input = oldBytes.inputStream(),
        )
        cache.store(
            namespace,
            CatalogOfflineCache.KIND_AUDIO,
            "44444444-4444-4444-8444-444444444444",
            sha256(newBytes),
            newBytes.size.toLong(),
            "audio/mpeg",
            renditionId = renditionId,
            input = newBytes.inputStream(),
        )

        val current = cache.findAudio(namespace, renditionId)
        assertEquals("44444444-4444-4444-8444-444444444444", current?.assetId)
        assertEquals(5L, cache.usedBytes())
    }

    @Test(expected = java.io.IOException::class)
    fun rejectsBytesThatDoNotMatchDescriptorHash() {
        val cache = CatalogOfflineCache(temporaryFolder.newFolder("integrity"), maxBytes = 20)
        val bytes = byteArrayOf(1, 2, 3)
        cache.store(
            "c".repeat(64),
            CatalogOfflineCache.KIND_SCORE,
            "33333333-3333-4333-8333-333333333333",
            "d".repeat(64),
            bytes.size.toLong(),
            "application/vnd.recordare.musicxml+xml",
            revisionId = "55555555-5555-4555-8555-555555555555",
            input = bytes.inputStream(),
        )
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
