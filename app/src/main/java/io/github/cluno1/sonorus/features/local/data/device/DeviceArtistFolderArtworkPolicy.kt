/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

import java.text.Normalizer
import java.util.Locale

/** Prevents folder-wide artist images from leaking across artists in mixed music folders. */
object DeviceArtistFolderArtworkPolicy {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val genericNames = setOf("artist", "band")

    fun isSupportedImage(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in imageExtensions

    fun isArtistSpecific(fileName: String, artistName: String): Boolean {
        if (!isSupportedImage(fileName)) return false
        val stem = fileName.substringBeforeLast('.')
        return normalize(stem) == normalize(artistName)
    }

    fun isGeneric(fileName: String): Boolean =
        isSupportedImage(fileName) &&
            fileName.substringBeforeLast('.').lowercase(Locale.ROOT) in genericNames

    fun allowsGeneric(artistName: String, artistsUnderDirectory: Set<String>): Boolean {
        val target = normalize(artistName)
        val distinct = artistsUnderDirectory.map(::normalize).filter(String::isNotEmpty).toSet()
        return target.isNotEmpty() && distinct == setOf(target)
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), "")
}
