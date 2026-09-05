/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

import android.content.Context
import android.net.Uri

/** Validates local artwork by opening and reading it; a syntactically valid URI is not enough. */
class ArtworkUriValidator(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun isReadable(uri: Uri?): Boolean {
        if (uri == null || uri == Uri.EMPTY) return false
        val raw = uri.toString().trim()
        if (raw.isEmpty() || raw == "null" || raw == "content://media/external/audio/albumart/0") return false
        if (uri.scheme == "file" && uri.lastPathSegment?.startsWith("placeholder_") == true) return false
        if (uri.scheme == "http" || uri.scheme == "https") return true
        if (uri.scheme != "content" && uri.scheme != "file" && uri.scheme != null) return false

        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(16)
                val count = input.read(header)
                count > 0 && isSupportedImageHeader(header, count)
            } == true
        }.getOrDefault(false)
    }

    companion object {
        fun isSupportedImageHeader(bytes: ByteArray, count: Int = bytes.size): Boolean {
            if (count >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return true
            if (count >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()))) return true
            return count >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        }
    }
}
