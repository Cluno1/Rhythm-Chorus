/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

/** Strict automatic artist selection; manual search may still show every candidate. */
object DeviceArtistMatchPolicy {
    private const val MINIMUM_CONFIDENCE = 0.90
    private const val MINIMUM_MARGIN = 0.08

    fun bestAutomaticIndex(query: String, candidateNames: List<String>): Int? {
        if (query.isBlank() || candidateNames.isEmpty()) return null
        val ranked = candidateNames.mapIndexed { index, name ->
            index to DeviceMetadataMatcher.artistNameScore(query, name)
        }.sortedByDescending { it.second }
        val best = ranked.first()
        return best.first.takeIf {
            DeviceMetadataMatcher.isAutomaticMatch(
                best = best.second,
                runnerUp = ranked.getOrNull(1)?.second,
                minimum = MINIMUM_CONFIDENCE,
                margin = MINIMUM_MARGIN,
                unconditionalThreshold = DeviceMetadataRepository.EXACT_AUTO_CONFIDENCE,
            )
        }
    }
}
