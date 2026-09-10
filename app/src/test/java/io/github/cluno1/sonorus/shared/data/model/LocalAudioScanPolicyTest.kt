package io.github.cluno1.sonorus.shared.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioScanPolicyTest {
    @Test
    fun defaultFormatsCoverCommonAudioWithoutAutoIncludingVideoContainers() {
        assertTrue(LocalAudioScanPolicy.defaultAllowedFormats.containsAll(setOf("mp2", "m4r", "amr", "aifc")))
        assertFalse(LocalAudioScanPolicy.defaultAllowedFormats.contains("mp4"))
        assertFalse(LocalAudioScanPolicy.defaultAllowedFormats.contains("mkv"))
        assertFalse(LocalAudioScanPolicy.defaultAllowedFormats.contains("3gp"))
    }

    @Test
    fun oldUnmodifiedDefaultsGainNewCommonFormatsOnUpgrade() {
        val oldDefaults = LocalAudioScanPolicy.defaultAllowedFormats - setOf("mp2", "m4r", "amr", "aifc")

        assertEquals(
            LocalAudioScanPolicy.defaultAllowedFormats,
            LocalAudioScanPolicy.upgradeLegacyDefaultFormats(oldDefaults),
        )
    }

    @Test
    fun customizedLegacyFormatsRemainCustomizedOnUpgrade() {
        val customized = LocalAudioScanPolicy.defaultAllowedFormats - setOf("mp2", "m4r", "amr", "aifc", "wma")

        assertEquals(customized, LocalAudioScanPolicy.upgradeLegacyDefaultFormats(customized))
    }

    @Test
    fun providerMimeCanIdentifyAudioWhenNameHasNoUsefulExtension() {
        assertEquals(
            "flac",
            LocalAudioScanPolicy.resolveEnabledFormat(
                displayName = "recording",
                mimeType = "audio/flac",
                allowedFormats = setOf("flac"),
            ),
        )
        assertEquals(
            "mp3",
            LocalAudioScanPolicy.resolveEnabledFormat(
                displayName = "download.bin",
                mimeType = "audio/mpeg; charset=binary",
                allowedFormats = setOf("mp3"),
            ),
        )
    }

    @Test
    fun disabledKnownExtensionCannotBeReenabledByMime() {
        assertNull(
            LocalAudioScanPolicy.resolveEnabledFormat(
                displayName = "concert.MP4",
                mimeType = "audio/mp4",
                allowedFormats = setOf("m4a"),
            ),
        )
    }

    @Test
    fun folderMatchingHonorsDirectoryBoundariesAndCase() {
        assertTrue(LocalAudioScanPolicy.isWithinFolder("/Storage/Emulated/0/Music/A.mp3", "/storage/emulated/0/music"))
        assertFalse(LocalAudioScanPolicy.isWithinFolder("/storage/emulated/0/Music-old/A.mp3", "/storage/emulated/0/Music"))
    }

    @Test
    fun authorizedRootsSupplementBlacklistModeAndRemainScopedInWhitelistMode() {
        assertTrue(
            LocalAudioScanPolicy.authorizedRootApplies(
                MediaScanMode.BLACKLIST,
                "/storage/emulated/0/Music",
                emptyList(),
            ),
        )
        assertTrue(
            LocalAudioScanPolicy.authorizedRootApplies(
                MediaScanMode.WHITELIST,
                "/storage/emulated/0/Music",
                listOf("/storage/emulated/0/Music/Choir"),
            ),
        )
        assertFalse(
            LocalAudioScanPolicy.authorizedRootApplies(
                MediaScanMode.WHITELIST,
                "/storage/emulated/0/Podcasts",
                listOf("/storage/emulated/0/Music"),
            ),
        )
    }

    @Test
    fun blacklistAndWhitelistUseTheSamePathPolicyForAuthorizedDocuments() {
        assertFalse(
            LocalAudioScanPolicy.includesPath(
                MediaScanMode.BLACKLIST,
                "/storage/emulated/0/Music/Voice Notes/a.m4a",
                emptyList(),
                listOf("/storage/emulated/0/Music/Voice Notes"),
            ),
        )
        assertTrue(
            LocalAudioScanPolicy.includesPath(
                MediaScanMode.WHITELIST,
                "/storage/emulated/0/Music/Choir/a.flac",
                listOf("/storage/emulated/0/Music/Choir"),
                emptyList(),
            ),
        )
    }

    @Test
    fun legacyWhitelistPathsReportMissingPersistedAuthorization() {
        assertEquals(
            1,
            LocalAudioScanPolicy.missingAuthorizationCount(
                whitelistedFolders = listOf(
                    "/storage/emulated/0/Music",
                    "/storage/emulated/0/Recordings",
                ),
                authorizedRootDisplayPaths = listOf("/storage/emulated/0/Music/Choir"),
            ),
        )
    }
}
