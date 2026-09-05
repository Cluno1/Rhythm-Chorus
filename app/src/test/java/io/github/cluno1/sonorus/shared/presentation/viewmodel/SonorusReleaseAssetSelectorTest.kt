/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.shared.presentation.viewmodel

import io.github.cluno1.sonorus.network.GitHubAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SonorusReleaseAssetSelectorTest {
    private fun asset(name: String, state: String = "uploaded") = GitHubAsset(
        id = 1,
        name = name,
        browser_download_url = "https://example.invalid/$name",
        content_type = "application/vnd.android.package-archive",
        size = 1,
        state = state,
    )

    @Test
    fun `prefers first supported ABI before universal`() {
        val result = SonorusReleaseAssetSelector.select(
            assets = listOf(
                asset("Sonorus-1.1.0-githubRelease.apk"),
                asset("Sonorus-1.1.0-githubRelease-arm64-v8a.apk"),
            ),
            flavor = "github",
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )
        assertEquals("Sonorus-1.1.0-githubRelease-arm64-v8a.apk", result?.name)
    }

    @Test
    fun `falls back to universal Sonorus APK`() {
        val result = SonorusReleaseAssetSelector.select(
            assets = listOf(asset("Sonorus-1.1.0-githubRelease.apk")),
            flavor = "github",
            supportedAbis = listOf("arm64-v8a"),
        )
        assertEquals("Sonorus-1.1.0-githubRelease.apk", result?.name)
    }

    @Test
    fun `rejects upstream and wrong flavor assets`() {
        val result = SonorusReleaseAssetSelector.select(
            assets = listOf(
                asset("Rhythm-9.9.9-githubRelease-arm64-v8a.apk"),
                asset("Sonorus-9.9.9-fdroidRelease-arm64-v8a.apk"),
            ),
            flavor = "github",
            supportedAbis = listOf("arm64-v8a"),
        )
        assertNull(result)
    }
}
