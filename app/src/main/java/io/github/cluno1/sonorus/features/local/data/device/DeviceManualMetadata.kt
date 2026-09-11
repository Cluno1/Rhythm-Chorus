/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.data.device

enum class DeviceManualMetadataKind { LYRICS, ARTWORK }

enum class DevicePublicMetadataProvider { LRCLIB, MUSICBRAINZ_CAA, DEEZER }

data class DeviceProviderSearchResult<T>(
    val provider: DevicePublicMetadataProvider,
    val candidates: List<T>,
    val failed: Boolean = false,
)

data class DeviceArtworkCandidate(
    val provider: DevicePublicMetadataProvider,
    val externalId: String,
    val releaseGroupId: String? = null,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Double?,
    val confidence: Double,
    val imageUrl: String,
)

fun DeviceMetadataRequest.normalized(): DeviceMetadataRequest = copy(
    title = title.trim(),
    artist = artist?.trim()?.takeIf(String::isNotEmpty),
    album = album?.trim()?.takeIf(String::isNotEmpty),
    durationSeconds = durationSeconds?.takeIf { it > 0 },
)
