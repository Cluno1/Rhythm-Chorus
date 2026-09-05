package io.github.cluno1.sonorus.features.catalog.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogInvitePresentationTest {
    @Test
    fun `unrecognised expiry remains visible instead of crashing dialog`() {
        assertEquals("server supplied expiry", formatInviteExpiry("server supplied expiry"))
    }
}
