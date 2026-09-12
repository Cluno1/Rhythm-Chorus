package io.github.cluno1.sonorus.shared.presentation.components.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNavigationPolicyTest {
    @Test
    fun `only the active player route owns system back`() {
        assertTrue(PlayerNavigationPolicy.handlesSystemBack(isPlayerRouteActive = true))
        assertFalse(PlayerNavigationPolicy.handlesSystemBack(isPlayerRouteActive = false))
    }

    @Test
    fun `collapse animation never grants system back ownership`() {
        val expansionFraction = 0.9f

        assertTrue(
            PlayerNavigationPolicy.keepsExpandedContentComposed(
                isPlayerRouteActive = false,
                expansionFraction = expansionFraction,
            ),
        )
        assertFalse(PlayerNavigationPolicy.handlesSystemBack(isPlayerRouteActive = false))
    }

    @Test
    fun `expanded content is removed once collapse settles`() {
        assertFalse(
            PlayerNavigationPolicy.keepsExpandedContentComposed(
                isPlayerRouteActive = false,
                expansionFraction = 0f,
            ),
        )
    }

    @Test
    fun `collapse is allowed only when player is current destination`() {
        assertTrue(
            PlayerNavigationPolicy.canCollapseCurrentDestination(
                currentRoute = "player",
                playerRoute = "player",
            ),
        )
        assertFalse(
            PlayerNavigationPolicy.canCollapseCurrentDestination(
                currentRoute = "playlist/{playlistId}",
                playerRoute = "player",
            ),
        )
        assertFalse(
            PlayerNavigationPolicy.canCollapseCurrentDestination(
                currentRoute = null,
                playerRoute = "player",
            ),
        )
    }
}
