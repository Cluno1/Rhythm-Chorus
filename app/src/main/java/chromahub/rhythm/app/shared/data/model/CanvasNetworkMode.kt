package chromahub.rhythm.app.shared.data.model

/**
 * Preference for canvas network playback mode
 */
enum class CanvasNetworkMode(val displayName: String) {
    WIFI_ONLY("Wi-Fi Only"),
    BOTH("Wi-Fi and Cellular");
    
    companion object {
        fun fromOrdinal(ordinal: Int): CanvasNetworkMode {
            return values().getOrElse(ordinal) { BOTH }
        }
    }
}
