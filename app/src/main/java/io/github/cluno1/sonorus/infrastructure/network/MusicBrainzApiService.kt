/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApiService {
    @GET("ws/2/recording/")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 25
    ): MusicBrainzRecordingSearchResponse
}

data class MusicBrainzRecordingSearchResponse(
    val recordings: List<MusicBrainzRecording> = emptyList()
)

data class MusicBrainzRecording(
    val id: String,
    val title: String,
    val length: Long? = null,
    val score: Int? = null,
    @SerializedName("first-release-date") val firstReleaseDate: String? = null,
    val isrcs: List<String> = emptyList(),
    @SerializedName("artist-credit") val artistCredit: List<MusicBrainzArtistCredit> = emptyList(),
    val releases: List<MusicBrainzRelease> = emptyList()
)

data class MusicBrainzArtistCredit(
    val name: String? = null,
    val artist: MusicBrainzArtist? = null
)

data class MusicBrainzArtist(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("sort-name") val sortName: String? = null,
    val aliases: List<MusicBrainzAlias> = emptyList()
)

data class MusicBrainzAlias(
    val name: String? = null,
    @SerializedName("sort-name") val sortName: String? = null
)

data class MusicBrainzRelease(
    val id: String,
    val title: String,
    val status: String? = null,
    val date: String? = null,
    val country: String? = null,
    @SerializedName("track-count") val trackCount: Int? = null,
    @SerializedName("label-info") val labelInfo: List<MusicBrainzLabelInfo> = emptyList(),
    @SerializedName("release-group") val releaseGroup: MusicBrainzReleaseGroup? = null
)

data class MusicBrainzReleaseGroup(
    val id: String,
    val title: String? = null,
    @SerializedName("primary-type") val primaryType: String? = null,
    @SerializedName("secondary-types") val secondaryTypes: List<String> = emptyList(),
)

data class MusicBrainzLabelInfo(
    val label: MusicBrainzLabel? = null,
)

data class MusicBrainzLabel(
    val id: String? = null,
    val name: String? = null,
)
