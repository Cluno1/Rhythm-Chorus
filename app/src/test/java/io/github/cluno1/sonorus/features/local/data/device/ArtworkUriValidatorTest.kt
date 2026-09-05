package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkUriValidatorTest {
    @Test fun `recognizes supported image headers`() {
        assertTrue(ArtworkUriValidator.isSupportedImageHeader(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertTrue(ArtworkUriValidator.isSupportedImageHeader(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())))
        assertTrue(ArtworkUriValidator.isSupportedImageHeader("RIFF1234WEBP".toByteArray()))
    }

    @Test fun `rejects empty and non-image payloads`() {
        assertFalse(ArtworkUriValidator.isSupportedImageHeader(byteArrayOf()))
        assertFalse(ArtworkUriValidator.isSupportedImageHeader("not an image".toByteArray()))
    }
}
