package io.github.cluno1.sonorus.features.catalog.data.remote

import io.github.cluno1.sonorus.features.catalog.domain.CatalogIssuedInvite
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogInviteDtoTest {
    @Test
    fun `invite response keeps code user and expiry for confirmation dialog`() {
        val actual = InviteDto(
            inviteCode = "single-use-code",
            userId = "listener-42",
            expiresAt = "2026-09-05T05:30:00Z",
        ).toIssuedInvite()

        assertEquals(
            CatalogIssuedInvite(
                inviteCode = "single-use-code",
                userId = "listener-42",
                expiresAt = "2026-09-05T05:30:00Z",
            ),
            actual,
        )
    }
}
