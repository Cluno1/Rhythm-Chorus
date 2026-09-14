package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.annotations.SerializedName

internal data class WorkChorusDto(
    @SerializedName("work_id") val workId: String?,
    @SerializedName("projects") val projects: List<ChorusProjectDto>?,
)

internal data class ChorusProjectDto(
    @SerializedName("id") val id: String?,
    @SerializedName("work_id") val workId: String?,
    @SerializedName("arrangement_id") val arrangementId: String?,
    @SerializedName("score_id") val scoreId: String?,
    @SerializedName("alignment_score_revision_id") val alignmentScoreRevisionId: String?,
    @SerializedName("timeline_hash") val timelineHash: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("revision") val revision: Int?,
    @SerializedName("parts") val parts: List<ChorusPartDto>?,
    @SerializedName("timelines") val timelines: List<ChorusTimelineDto>?,
    @SerializedName("tracks") val tracks: List<ChorusTrackDto>?,
)

internal data class ChorusTimelineDto(
    @SerializedName("id") val id: String?,
    @SerializedName("chorus_project_id") val chorusProjectId: String?,
    @SerializedName("score_revision_id") val scoreRevisionId: String?,
    @SerializedName("timeline_hash") val timelineHash: String?,
    @SerializedName("revision") val revision: Int?,
)

internal data class ChorusPartDto(
    @SerializedName("id") val id: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("display_order") val displayOrder: Int?,
)

internal data class ChorusSyncAnchorDto(
    @SerializedName("anchor_order") val anchorOrder: Int,
    @SerializedName("score_tick") val scoreTick: Long,
    @SerializedName("media_ms") val mediaMs: Long,
    @SerializedName("confidence") val confidence: Double = 1.0,
    @SerializedName("source") val source: String = "in_app_clock",
)

internal data class ChorusTrackDto(
    @SerializedName("id") val id: String?,
    @SerializedName("chorus_project_id") val chorusProjectId: String?,
    @SerializedName("chorus_timeline_id") val chorusTimelineId: String?,
    @SerializedName("rendition_id") val renditionId: String?,
    @SerializedName("uploader_display_name") val uploaderDisplayName: String?,
    @SerializedName("owned_by_requester") val ownedByRequester: Boolean?,
    @SerializedName("part_id") val partId: String?,
    @SerializedName("contribution_kind") val contributionKind: String?,
    @SerializedName("display_label") val displayLabel: String?,
    @SerializedName("take_no") val takeNo: Int?,
    @SerializedName("alignment_state") val alignmentState: String?,
    @SerializedName("alignment_offset_ms") val alignmentOffsetMs: Long?,
    @SerializedName("status") val status: String?,
    @SerializedName("gain_db") val gainDb: Double?,
    @SerializedName("pan") val pan: Double?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("waveform_peaks") val waveformPeaks: List<Float>?,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    @SerializedName("revision") val revision: Int?,
    @SerializedName("anchors") val anchors: List<ChorusSyncAnchorDto>?,
)

internal data class ChorusTrackCreateDto(
    @SerializedName("chorus_timeline_id") val chorusTimelineId: String,
    @SerializedName("part_id") val partId: String?,
    @SerializedName("contribution_kind") val contributionKind: String,
    @SerializedName("display_label") val displayLabel: String,
    @SerializedName("take_no") val takeNo: Int,
    @SerializedName("sha256") val sha256: String,
    @SerializedName("byte_size") val byteSize: Long,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("rights_confirmed") val rightsConfirmed: Boolean = true,
    @SerializedName("initial_anchors") val initialAnchors: List<ChorusSyncAnchorDto>,
)

internal data class ChorusUploadTargetDto(
    @SerializedName("id") val id: String?,
    @SerializedName("method") val method: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("expires_at") val expiresAt: String?,
)

internal data class ChorusTrackCreateResponseDto(
    @SerializedName("track") val track: ChorusTrackDto?,
    @SerializedName("upload_status") val uploadStatus: String?,
    @SerializedName("upload") val upload: ChorusUploadTargetDto?,
)

internal data class ChorusAlignmentPatchDto(
    @SerializedName("offset_ms") val offsetMs: Long,
    @SerializedName("anchors") val anchors: List<ChorusSyncAnchorDto>,
)

internal data class ChorusMixResolveDto(
    @SerializedName("chorus_timeline_id") val chorusTimelineId: String,
    @SerializedName("track_ids") val trackIds: List<String>,
)

internal data class ChorusMixDto(
    @SerializedName("id") val id: String?,
    @SerializedName("chorus_project_id") val chorusProjectId: String?,
    @SerializedName("chorus_timeline_id") val chorusTimelineId: String?,
    @SerializedName("selection_hash") val selectionHash: String?,
    @SerializedName("selected_track_ids") val selectedTrackIds: List<String>?,
    @SerializedName("selected_track_count") val selectedTrackCount: Int?,
    @SerializedName("mix_profile") val mixProfile: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("error_summary") val errorSummary: String?,
    @SerializedName("delivery") val delivery: AssetDeliveryDto?,
)
