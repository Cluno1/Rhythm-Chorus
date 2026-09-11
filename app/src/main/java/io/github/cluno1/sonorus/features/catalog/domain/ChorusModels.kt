package io.github.cluno1.sonorus.features.catalog.domain

import java.io.File

data class ChorusCatalog(
    val workId: String,
    val projects: List<ChorusProject>,
)

data class ChorusProject(
    val id: String,
    val workId: String,
    val arrangementId: String,
    val alignmentScoreRevisionId: String,
    val timelineHash: String,
    val title: String,
    val status: String,
    val revision: Int,
    val parts: List<ChorusPart>,
    val tracks: List<ChorusTrack>,
)

data class ChorusPart(
    val id: String,
    val code: String,
    val name: String,
    val displayOrder: Int,
)

data class ChorusSyncAnchor(
    val anchorOrder: Int,
    val scoreTick: Long,
    val mediaMs: Long,
    val confidence: Double = 1.0,
    val source: String = "in_app_clock",
)

data class ChorusTrack(
    val id: String,
    val chorusProjectId: String,
    val renditionId: String,
    val uploaderDisplayName: String,
    val ownedByRequester: Boolean,
    val partId: String?,
    val contributionKind: String,
    val displayLabel: String,
    val takeNo: Int,
    val alignmentState: String,
    val alignmentOffsetMs: Long,
    val status: String,
    val gainDb: Double,
    val pan: Double,
    val durationMs: Long?,
    val waveformPeaks: List<Float>,
    val rejectionReason: String?,
    val revision: Int,
    val anchors: List<ChorusSyncAnchor>,
)

data class ChorusTrackUpload(
    val file: File,
    val mediaType: String,
    val sha256: String,
    val durationMs: Long,
    val partId: String?,
    val contributionKind: String,
    val displayLabel: String,
    val takeNo: Int = 1,
    val initialAnchors: List<ChorusSyncAnchor> = emptyList(),
)

data class ChorusMix(
    val id: String,
    val chorusProjectId: String,
    val selectionHash: String,
    val selectedTrackIds: List<String>,
    val state: String,
    val durationMs: Long?,
    val errorSummary: String?,
    val playback: ChorusPlayback?,
)

data class ChorusPlayback(
    val assetId: String,
    val mediaType: String,
    val byteSize: Long,
    val sha256: String,
    val url: String,
    val cacheKey: String,
    val expiresAt: String?,
)
