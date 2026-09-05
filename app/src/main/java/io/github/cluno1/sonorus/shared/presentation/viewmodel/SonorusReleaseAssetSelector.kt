/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.shared.presentation.viewmodel

import io.github.cluno1.sonorus.network.GitHubAsset
import java.util.Locale

/** Selects only first-party Sonorus APKs for this distribution and device ABI. */
object SonorusReleaseAssetSelector {
    private val knownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    fun select(
        assets: List<GitHubAsset>,
        flavor: String,
        supportedAbis: List<String>,
    ): GitHubAsset? {
        val normalizedFlavor = flavor.lowercase(Locale.ROOT)
        val candidates = assets.filter { asset ->
            val name = asset.name.lowercase(Locale.ROOT)
            asset.state == "uploaded" &&
                name.startsWith("sonorus-") &&
                name.endsWith(".apk") &&
                when (normalizedFlavor) {
                    "github" -> name.contains("githubrelease") || name.contains("-github-")
                    "fdroid" -> name.contains("fdroidrelease") || name.contains("-fdroid-")
                    else -> false
                }
        }

        supportedAbis.forEach { abi ->
            candidates.firstOrNull { it.name.contains(abi, ignoreCase = true) }?.let { return it }
        }

        return candidates.firstOrNull { asset ->
            asset.name.contains("universal", ignoreCase = true) ||
                knownAbis.none { abi -> asset.name.contains(abi, ignoreCase = true) }
        }
    }
}
