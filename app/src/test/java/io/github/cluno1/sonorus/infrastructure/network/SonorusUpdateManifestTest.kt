/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.network

import java.util.Base64
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SonorusUpdateManifestTest {
    private val signer = "ab".repeat(32)
    private val manifest = SonorusUpdateManifest(
        schemaVersion = 1,
        channel = "debug",
        applicationId = "io.github.cluno1.sonorus.debug",
        signingCertificateSha256 = signer,
        versionCode = 2_001_001,
        versionName = "1.1.0-debug.2001001",
        publishedAt = "2026-09-06T12:00:00Z",
        minimumAndroidSdk = 26,
        mandatory = false,
        releaseNotes = listOf("安全更新"),
        assets = listOf(
            SonorusUpdateAsset(
                abi = "arm64-v8a",
                fileName = "Sonorus-1.1.0-debug.2001001-arm64-v8a.apk",
                url = "/v2/app-updates/files/2001001/Sonorus-1.1.0-debug.2001001-arm64-v8a.apk",
                sizeBytes = 123,
                sha256 = "cd".repeat(32),
            ),
        ),
        signatureAlgorithm = "Ed25519",
        manifestSignature = Base64.getEncoder().encodeToString(ByteArray(64)),
    )

    @Test
    fun `canonical payload excludes signature and sorts object keys`() {
        val json = SonorusManifestCanonicalizer.payload(manifest).decodeToString()
        assertEquals('{', json.first())
        assertEquals("applicationId", json.substringAfter('"').substringBefore('"'))
        assertEquals(false, json.contains("manifestSignature"))
        assertEquals(true, json.contains("安全更新"))
    }

    @Test
    fun `accepts exact build identity and verified signature`() {
        SonorusUpdateValidator.validate(
            manifest,
            expectedChannel = "debug",
            expectedApplicationId = "io.github.cluno1.sonorus.debug",
            expectedSigningCertificateSha256 = signer.uppercase().chunked(2).joinToString(":"),
            publicKeyBase64 = Base64.getEncoder().encodeToString(ByteArray(32)),
            verifier = ManifestSignatureVerifier { _, _, _ -> true },
        )
    }

    @Test
    fun `rejects cross-channel manifest before signature verification`() {
        assertThrows(IllegalArgumentException::class.java) {
            SonorusUpdateValidator.validate(
                manifest,
                expectedChannel = "stable",
                expectedApplicationId = manifest.applicationId,
                expectedSigningCertificateSha256 = signer,
                publicKeyBase64 = Base64.getEncoder().encodeToString(ByteArray(32)),
                verifier = ManifestSignatureVerifier { _, _, _ -> true },
            )
        }
    }

    @Test
    fun `selects supported ABI then universal and never a foreign ABI`() {
        val universal = manifest.assets.first().copy(abi = "universal", fileName = "Sonorus-universal.apk")
        val x86 = manifest.assets.first().copy(abi = "x86", fileName = "Sonorus-x86.apk")
        assertEquals(
            manifest.assets.first(),
            SonorusUpdateAssetSelector.select(listOf(x86, universal) + manifest.assets, listOf("arm64-v8a")),
        )
        assertEquals(universal, SonorusUpdateAssetSelector.select(listOf(x86, universal), listOf("arm64-v8a")))
        assertNull(SonorusUpdateAssetSelector.select(listOf(x86), listOf("arm64-v8a")))
    }

    @Test
    fun `accepts only the exact signed update COS object`() {
        val path = "/debug/releases/2001001/${"cd".repeat(32)}"
        val signed = (
            "https://sonorus-updates-1328751369.cos.ap-guangzhou.myqcloud.com$path" +
                "?q-sign-algorithm=sha1&q-ak=AKIDtest&q-sign-time=1%3B2&q-key-time=1%3B2" +
                "&q-header-list=host&q-url-param-list=&q-signature=deadbeef"
            ).toHttpUrl()
        SonorusUpdateCosRedirectPolicy.validate(signed, path)

        assertThrows(IllegalArgumentException::class.java) {
            SonorusUpdateCosRedirectPolicy.validate(
                signed.newBuilder().host("evil.example").build(),
                path,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonorusUpdateCosRedirectPolicy.validate(signed, "/debug/releases/2001002/other.apk")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SonorusUpdateCosRedirectPolicy.validate(
                signed.newBuilder().removeAllQueryParameters("q-signature").build(),
                path,
            )
        }
    }

    @Test
    fun `removes backend credentials and validators before COS request`() {
        val request = Request.Builder()
            .url("https://sonorus-updates-1328751369.cos.ap-guangzhou.myqcloud.com/a.apk")
            .header("Authorization", "Device private-token")
            .header("X-Rhythm-Signature", "private-signature")
            .header("X-Sonorus-Update-Channel", "debug")
            .header("If-Match", "backend-sha256")
            .header("Range", "bytes=100-")
            .build()

        val sanitized = SonorusUpdateCosRedirectPolicy.sanitize(request)
        assertFalse(sanitized.headers.names().any { it.startsWith("X-Rhythm-", ignoreCase = true) })
        assertFalse(sanitized.headers.names().any { it.startsWith("X-Sonorus-", ignoreCase = true) })
        assertNull(sanitized.header("Authorization"))
        assertNull(sanitized.header("If-Match"))
        assertTrue(sanitized.header("Range") == "bytes=100-")
    }
}
