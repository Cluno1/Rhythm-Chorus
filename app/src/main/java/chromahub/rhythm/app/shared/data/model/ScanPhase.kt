package chromahub.rhythm.app.shared.data.model

sealed class ScanPhase(val displayName: String) {
    data object Idle : ScanPhase("Idle")
    data object Songs : ScanPhase("Songs")
    data object Incremental : ScanPhase("Incremental")
    data object SavingDb : ScanPhase("Saving Database")
    data object Complete : ScanPhase("Complete")
    data object Error : ScanPhase("Error")
    data object PermissionDenied : ScanPhase("Permission Denied")
}
