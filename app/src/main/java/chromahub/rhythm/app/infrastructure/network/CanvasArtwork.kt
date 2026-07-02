package chromahub.rhythm.app.network

data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
