package chromahub.rhythm.app.shared.data.model

enum class MediaScanMode(val value: String) {
    BLACKLIST("blacklist"),
    WHITELIST("whitelist");

    companion object {
        fun fromValue(value: String): MediaScanMode =
            entries.firstOrNull { it.value == value } ?: BLACKLIST
    }
}
