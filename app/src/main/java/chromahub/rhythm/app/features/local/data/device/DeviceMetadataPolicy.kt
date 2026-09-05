/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.features.local.data.device

import java.net.URI

enum class DeviceMetadataSource { USER_SELECTED, EMBEDDED, SIBLING, CACHE, PUBLIC_API }

data class DeviceMetadataRequest(val title: String, val artist: String?, val album: String?, val durationSeconds: Int?) {
    fun publicFields(): Set<String> = buildSet {
        add("title")
        if (artist != null) add("artist")
        if (album != null) add("album")
        if (durationSeconds != null) add("duration")
    }
}

object DeviceMetadataPolicy {
    fun isEligible(songId: String, uriScheme: String?): Boolean =
        !songId.startsWith("rhythm-catalog:") && (uriScheme == "content" || uriScheme == "file")

    fun sourcePriority(hasUserSelection: Boolean): List<DeviceMetadataSource> = buildList {
        if (hasUserSelection) add(DeviceMetadataSource.USER_SELECTED)
        add(DeviceMetadataSource.EMBEDDED)
        add(DeviceMetadataSource.SIBLING)
        add(DeviceMetadataSource.CACHE)
        add(DeviceMetadataSource.PUBLIC_API)
    }

    fun cacheKey(songId: String, artist: String, title: String) = "$songId:$artist:$title".lowercase()
    fun belongsToSong(cacheKey: String, songId: String) = cacheKey.startsWith("$songId:")

    fun safeDeezerArtworkUrl(raw: String): String? = runCatching {
        val promoted = raw.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        val uri = URI(promoted)
        val host = uri.host?.lowercase() ?: return null
        promoted.takeIf { uri.scheme == "https" && uri.userInfo == null && (host == "dzcdn.net" || host.endsWith(".dzcdn.net")) }
    }.getOrNull()

    fun isImageContentType(value: String?): Boolean = value.orEmpty().substringBefore(';').trim().lowercase().startsWith("image/")
}
