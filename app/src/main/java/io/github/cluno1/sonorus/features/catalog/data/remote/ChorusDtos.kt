package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.annotations.SerializedName

internal data class WorkChorusDto(
    @SerializedName("work_id") val workId: String?,
    val projects: List<ChorusProjectDto>?,
)

internal data class ChorusProjectDto(
    val id: String?,
    @SerializedName("work_id") val workId: String?,
    @SerializedName("arrangement_id") val arrangementId: String?,
    @SerializedName("alignment_score_revision_id") val alignmentScoreRevisionId: String?,
    @SerializedName("timeline_hash") val timelineHash: String?,
    val title: String?,
    val status: String?,
    val revision: Int?,
    val parts: List<ChorusPartDto>?,
    val tracks: List<ChorusTrackDto>?,
)

internal data class ChorusPartDto(
    val id: String?,
    val code: String?,
    val name: String?,
    @SerializedName("display_order") val displayOrder: Int?,
)

internal data class ChorusSyncAnchorDto(
    @SerializedName("anchor_order") val anchorOrder: Int,
    @SerializedName("score_tick") val scoreTick: Long,
    @SerializedName("media_ms") val mediaMs: Long,
    val confidence: Double = 1.0,
    val source: String = "in_app_clock",
)

internal data class ChorusTrackDto(
    val id: String?,
    @SerializedName("chorus_project_id") val chorusProjectId: String?,
    @SerializedName("rendition_id") val renditionId: String?,
    @SerializedName("uploader_display_name") val uploaderDisplayName: String?,
    @SerializedName("owned_by_requester") val ownedByRequester: Boolean?,
    @SerializedName("part_id") val partId: String?,
    @SerializedName("contribution_kind") val contributionKind: String?,
    @SerializedName("display_label") val displayLabel: String?,
    @SerializedName("take_no") val takeNo: Int?,
    @SerializedName("alignment_state") val alignmentState: String?,
    @SerializedName("alignment_offset_ms") val alignmentOffsetMs: Long?,
    val status: String?,
    @SerializedName("gain_db") val gainDb: Double?,
    val pan: Double?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("waveform_peaks") val waveformPeaks: List<Float>?,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    val revision: Int?,
    val anchors: List<ChorusSyncAnchorDto>?,
)

internal data class ChorusTrackCreateDto(
    @SerializedName("part_id") val partId: String?,
    @SerializedName("contribution_kind") val contributionKind: String,
    @SerializedName("display_label") val displayLabel: String,
    @SerializedName("take_no") val takeNo: Int,
    val sha256: String,
    @SerializedName("byte_size") val byteSize: Long,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("rights_confirmed") val rightsConfirmed: Boolean = true,
    @SerializedName("initial_anchors") val initialAnchors: List<ChorusSyncAnchorDto>,
)

internal data class ChorusUploadTargetDto(
    val id: String?,
    val method: String?,
    val url: String?,
    @SerializedName("expires_at") val expiresAt: String?,
)

internal data class ChorusTrackCreateResponseDto(
    val track: ChorusTrackDto?,
    @SerializedName("upload_status") val uploadStatus: String?,
    val upload: ChorusUploadTargetDto?,
)

internal data class ChorusAlignmentPatchDto(
    @SerializedName("offset_ms") val offsetMs: Long,
    val anchors: List<ChorusSyncAnchorDto>,
)

internal data class ChorusMixResolveDto(
    @SerializedName("track_ids") val trackIds: List<String>,
)

internal data class ChorusMixDto(
    val id: String?,
    @SerializedName("chorus_project_id") val chorusProjectId: String?,
    @SerializedName("selection_hash") val selectionHash: String?,
    @SerializedName("selected_track_ids") val selectedTrackIds: List<String>?,
    @SerializedName("selected_track_count") val selectedTrackCount: Int?,
    @SerializedName("mix_profile") val mixProfile: String?,
    val state: String?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("error_summary") val errorSummary: String?,
    val delivery: AssetDeliveryDto?,
)
