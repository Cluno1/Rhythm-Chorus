/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.shared.presentation.components.player

/**
 * Keeps player navigation decisions independent from sheet animation progress.
 *
 * The player route is the source of truth for system Back ownership. Animation progress may keep
 * the expanded surface composed long enough to finish a transition, but it must never grant that
 * surface permission to consume Back while another destination is active.
 */
internal object PlayerNavigationPolicy {
    private const val CONTENT_VISIBILITY_THRESHOLD = 0.001f

    fun handlesSystemBack(isPlayerRouteActive: Boolean): Boolean = isPlayerRouteActive

    fun keepsExpandedContentComposed(
        isPlayerRouteActive: Boolean,
        expansionFraction: Float,
    ): Boolean = isPlayerRouteActive || expansionFraction > CONTENT_VISIBILITY_THRESHOLD

    fun canCollapseCurrentDestination(
        currentRoute: String?,
        playerRoute: String,
    ): Boolean = currentRoute == playerRoute
}
