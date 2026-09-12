package io.github.cluno1.sonorus.features.catalog.domain

data class CatalogAdminDevice(
    val deviceId: String,
    val userId: String,
    val displayName: String?,
    val applicationId: String,
    val status: String,
    val isAdministrator: Boolean,
    val createdAt: String,
    val lastSeenAt: String?,
)

data class ChorusModerationSettings(
    val automaticApproval: Boolean,
    val updatedBy: String,
    val updatedAt: String?,
)

data class ChorusModerationItem(
    val workId: String,
    val projectTitle: String,
    val track: ChorusTrack,
)

data class CatalogAdminDashboard(
    val devices: List<CatalogAdminDevice>,
    val settings: ChorusModerationSettings,
    val pendingTracks: List<ChorusModerationItem>,
)
