/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.shared.data.model

import java.util.Locale

/** Pure policy shared by MediaStore and user-authorized document-tree discovery. */
object LocalAudioScanPolicy {
    val commonFormats = listOf(
        "mp3", "mp2", "m4a", "m4r", "aac", "alac", "flac", "ogg", "opus", "oga", "opa",
        "wav", "aiff", "aif", "aifc", "wma", "amr",
    )
    val losslessFormats = listOf("ape", "wv", "tta", "tak", "dsf", "dff", "dsd")
    val surroundFormats = listOf("ac3", "ac4", "eac", "eac3", "dts", "dtshd", "dtsx", "truehd")
    val containerFormats = listOf("mka", "m4b", "adts", "mp4", "mkv", "webm", "3gp", "3gpp")
    val legacyFormats = listOf("mid", "midi", "mhm", "mhm1")
    val knownFormats: List<String> =
        commonFormats + losslessFormats + surroundFormats + containerFormats + legacyFormats

    // Video-capable containers stay opt-in so an ordinary scan does not turn videos into songs.
    private val optInContainerFormats = setOf("mp4", "mkv", "webm", "3gp", "3gpp")
    val defaultAllowedFormats: Set<String> = knownFormats.toSet() - optInContainerFormats

    private val legacyDefaultAllowedFormats = setOf(
        "mp3", "flac", "ogg", "m4a", "opus", "opa", "wav", "aac", "alac", "aiff", "aif", "wma",
        "mka", "ac3", "ac4", "oga", "mid", "midi", "adts", "m4b", "eac", "eac3", "mhm", "mhm1",
        "dts", "dtshd", "dtsx", "truehd", "ape", "wv", "tta", "tak", "dsf", "dff", "dsd",
    )

    /** Adds newly supported defaults only when the user had never customized the old defaults. */
    fun upgradeLegacyDefaultFormats(formats: Set<String>): Set<String> {
        val normalized = formats.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        return if (normalized == legacyDefaultAllowedFormats) defaultAllowedFormats else normalized
    }

    /**
     * Resolves an enabled format from the file name first, then from MIME when the provider omits
     * or invents an extension. A known but disabled extension is never re-enabled through MIME.
     */
    fun resolveEnabledFormat(
        displayName: String?,
        mimeType: String?,
        allowedFormats: Set<String>,
    ): String? {
        val normalizedAllowed = allowedFormats.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        val extension = displayName.orEmpty()
            .substringAfterLast('.', "")
            .trim()
            .lowercase(Locale.ROOT)

        if (extension in knownFormats) return extension.takeIf(normalizedAllowed::contains)
        return mimeCandidates(mimeType).firstOrNull(normalizedAllowed::contains)
    }

    fun normalizedPathKey(path: String): String {
        val normalized = path.trim().replace('\\', '/').replace(Regex("/{2,}"), "/")
        return (if (normalized.length > 1) normalized.trimEnd('/') else normalized)
            .lowercase(Locale.ROOT)
    }

    fun isWithinFolder(path: String, folder: String): Boolean {
        val normalizedPath = normalizedPathKey(path)
        val normalizedFolder = normalizedPathKey(folder)
        if (normalizedPath.isBlank() || normalizedFolder.isBlank()) return false
        return normalizedPath == normalizedFolder || normalizedPath.startsWith("$normalizedFolder/")
    }

    fun pathsOverlap(first: String, second: String): Boolean =
        isWithinFolder(first, second) || isWithinFolder(second, first)

    fun authorizedRootApplies(
        mode: MediaScanMode,
        rootDisplayPath: String,
        whitelistedFolders: Collection<String>,
    ): Boolean = mode != MediaScanMode.WHITELIST ||
        whitelistedFolders.isEmpty() ||
        whitelistedFolders.any { pathsOverlap(rootDisplayPath, it) }

    fun includesPath(
        mode: MediaScanMode,
        path: String,
        whitelistedFolders: Collection<String>,
        blacklistedFolders: Collection<String>,
    ): Boolean = when (mode) {
        MediaScanMode.WHITELIST ->
            whitelistedFolders.isEmpty() || whitelistedFolders.any { isWithinFolder(path, it) }
        MediaScanMode.BLACKLIST ->
            blacklistedFolders.none { isWithinFolder(path, it) }
    }

    fun missingAuthorizationCount(
        whitelistedFolders: Collection<String>,
        authorizedRootDisplayPaths: Collection<String>,
    ): Int = whitelistedFolders.count { folder ->
        authorizedRootDisplayPaths.none { root -> pathsOverlap(root, folder) }
    }

    private fun mimeCandidates(mimeType: String?): List<String> = when (
        mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
    ) {
        "audio/mpeg", "audio/mp3", "audio/x-mpeg" -> listOf("mp3", "mp2")
        "audio/mp4", "audio/x-m4a", "audio/m4a" -> listOf("m4a", "m4r", "mp4")
        "audio/aac", "audio/aacp" -> listOf("aac", "adts")
        "audio/flac", "audio/x-flac" -> listOf("flac")
        "audio/ogg", "application/ogg" -> listOf("ogg", "opus", "oga", "opa")
        "audio/opus" -> listOf("opus", "ogg")
        "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> listOf("wav")
        "audio/aiff", "audio/x-aiff" -> listOf("aiff", "aif", "aifc")
        "audio/x-ms-wma" -> listOf("wma")
        "audio/amr", "audio/amr-wb" -> listOf("amr")
        "audio/x-ape" -> listOf("ape")
        "audio/x-wavpack" -> listOf("wv")
        "audio/x-matroska" -> listOf("mka", "mkv")
        "audio/webm" -> listOf("webm")
        "audio/3gpp", "video/3gpp" -> listOf("3gp", "3gpp")
        "audio/midi", "audio/x-midi", "audio/sp-midi" -> listOf("mid", "midi")
        "audio/ac3" -> listOf("ac3")
        "audio/eac3" -> listOf("eac3", "eac")
        "audio/vnd.dts" -> listOf("dts")
        "audio/vnd.dts.hd" -> listOf("dtshd", "dtsx")
        else -> emptyList()
    }
}
