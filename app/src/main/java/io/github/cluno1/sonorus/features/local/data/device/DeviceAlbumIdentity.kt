/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

import io.github.cluno1.sonorus.shared.data.model.Song
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Produces an identity that is stable locally but never overlaps a Catalog UUID. */
object DeviceAlbumIdentity {
    fun key(song: Song): String? {
        if (!DeviceMetadataPolicy.isEligible(song.id, song.uri.scheme)) return null
        val albumId = song.albumId.trim().takeIf { it.isNotEmpty() && it != "0" && it != "-1" }
        if (albumId != null) {
            val volume = song.uri.pathSegments.firstOrNull().orEmpty().ifBlank { "external" }
            return "mediastore:${normalize(volume)}:$albumId"
        }

        val artist = song.albumArtist?.takeUnless { it.isUnknown() }.orEmpty().ifBlank { song.artist }
        val folderBoundary = song.path?.substringBeforeLast('/', "").orEmpty()
        val fallback = listOf(normalize(artist), normalize(song.album), sha256(folderBoundary)).joinToString("|")
        return "metadata:${sha256(fallback)}"
    }

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private fun String.isUnknown(): Boolean =
        isBlank() || equals("unknown", true) || equals("<unknown>", true) || startsWith("unknown ", true)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
