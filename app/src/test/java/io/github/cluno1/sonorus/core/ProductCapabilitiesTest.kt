package io.github.cluno1.sonorus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCapabilitiesTest {
    @Test
    fun `device public metadata initializes metadata network client in catalog-only builds`() {
        assertTrue(
            ProductCapabilities.shouldInitializeMetadataNetworkClient(
                thirdPartyMusicServices = false,
                devicePublicMetadata = true
            )
        )
    }

    @Test
    fun `metadata network client stays disabled when no network metadata capability exists`() {
        assertFalse(
            ProductCapabilities.shouldInitializeMetadataNetworkClient(
                thirdPartyMusicServices = false,
                devicePublicMetadata = false
            )
        )
    }
}
