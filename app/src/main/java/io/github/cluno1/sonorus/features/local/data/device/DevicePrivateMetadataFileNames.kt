/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Collision-resistant app-private names for downloaded or edited DEVICE metadata. */
object DevicePrivateMetadataFileNames {
    fun artistArtwork(artistName: String): String =
        "artist-${digest("artist:${normalize(artistName)}").take(32)}.jpg"

    fun lyrics(songId: String?, artist: String, title: String): String {
        val identity = songId?.trim()?.takeIf(String::isNotEmpty)?.let { "song:$it" }
            ?: "metadata:${normalize(artist)}\u0000${normalize(title)}"
        return "lyrics-${digest(identity).take(32)}.json"
    }

    /** Returns the old artist filename only when sanitizing did not alter its identity. */
    fun unambiguousLegacyArtistArtwork(artistName: String): String? = artistName
        .takeIf { value -> value.isNotEmpty() && value.all(::isLegacySafeCharacter) }
        ?.let { "$it.jpg" }

    fun legacyNameWasLossless(vararg values: String): Boolean = values.all { value ->
        value.isNotEmpty() && value.all(::isLegacySafeCharacter)
    }

    private fun isLegacySafeCharacter(character: Char): Boolean =
        (character.isLetterOrDigit() && character.code < 128) || character in "._-"

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT)

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
