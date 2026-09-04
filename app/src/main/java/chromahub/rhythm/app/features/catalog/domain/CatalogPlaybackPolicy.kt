package chromahub.rhythm.app.features.catalog.domain

import java.net.URI
import java.util.Locale

/**
 * Hard product boundary for the private Work catalog.
 *
 * Rhythm may play only an Asset selected through a backend Rendition playback response. Device
 * MediaStore entries, arbitrary content/file URIs, and third-party streaming URLs are rejected.
 * Offline playback remains supported by Media3's managed cache: the logical URI and cache key stay
 * the backend Asset identity even when the bytes are served from the local cache.
 *
 * issue 11：新增"后端签发的 COS presigned 直连"通道——playback descriptor 的 delivery=signed_url
 * 时，URL 指向腾讯 COS 对象存储（`*.myqcloud.com`）且自带短时签名。客户端据此绕过后端代理直连
 * 下载音频。此通道只接受来自 descriptor、指向 COS 域名且带签名的 https URL（不接受任意外链），
 * 身份绑定退回 cacheKey（`rhythm:asset:{uuid}:{sha256}`），后端 Bearer 令牌绝不发往 COS。
 */
object CatalogPlaybackPolicy {
    const val DEVICE_LIBRARY_ENABLED = false
    const val EXTERNAL_URI_PLAYBACK_ENABLED = false
    const val THIRD_PARTY_STREAMING_ENABLED = false

    /** issue 11：允许后端签发的 COS presigned 直连 URL。 */
    const val SIGNED_OBJECT_STORE_ENABLED = true

    private const val COS_HOST_SUFFIX = ".myqcloud.com"

    private val uuid = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
    private val mediaIdPattern = Regex("^rhythm-catalog:rendition:($uuid):asset:($uuid)$")
    private val deferredMediaIdPattern = Regex("^rhythm-catalog:rendition:($uuid)$")
    private val assetPathPattern = Regex("(?:^|/)v2/assets/($uuid)/content/?$")
    private val cacheKeyPattern = Regex("^rhythm:asset:($uuid):([0-9a-f]{64})$")
    private val playableRoles = setOf("stream", "mix", "master")
    private val playableMediaTypes = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/mp4",
        "audio/m4a",
        "audio/x-m4a",
        "audio/aac",
        "audio/flac",
        "audio/x-flac",
        "audio/ogg",
        "application/ogg",
        "audio/opus",
        "audio/wav",
        "audio/wave",
        "audio/x-wav",
        "audio/vnd.wave",
    )
    private val musicXmlMediaTypes = setOf(
        "application/vnd.recordare.musicxml+xml",
    )

    fun allows(
        mediaId: String,
        uri: String?,
        customCacheKey: String?,
        mediaType: String?,
        trustedServerUrl: String?,
    ): Boolean {
        if (!isPlayableMediaType(mediaType)) return false
        val mediaMatch = mediaIdPattern.matchEntire(mediaId) ?: return false
        val assetId = requestAssetId(uri, customCacheKey, trustedServerUrl) ?: return false
        return assetId.equals(mediaMatch.groupValues[2], ignoreCase = true)
    }

    /** Stable queue identity. It is exchanged for a fresh delivery descriptor at DataSource.open. */
    fun allowsDeferred(mediaId: String, uri: String?, mediaType: String?): Boolean {
        if (!isPlayableMediaType(mediaType)) return false
        val mediaRenditionId = deferredMediaIdPattern.matchEntire(mediaId)?.groupValues?.get(1) ?: return false
        return deferredRenditionId(uri)?.equals(mediaRenditionId, ignoreCase = true) == true
    }

    /** MediaSession admission accepts only a resolved Asset identity or the strict deferred form. */
    fun allowsMediaSessionItem(
        mediaId: String,
        uri: String?,
        customCacheKey: String?,
        mediaType: String?,
        trustedServerUrl: String?,
    ): Boolean = allowsDeferred(mediaId, uri, mediaType) || allows(
        mediaId = mediaId,
        uri = uri,
        customCacheKey = customCacheKey,
        mediaType = mediaType,
        trustedServerUrl = trustedServerUrl,
    )

    fun deferredUri(renditionId: String): String {
        require(Regex("^$uuid$").matches(renditionId)) { "renditionId is not a UUID" }
        return "rhythm-catalog://rendition/${renditionId.lowercase(Locale.ROOT)}"
    }

    fun deferredRenditionId(uri: String?): String? {
        val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
        if (!parsed.scheme.equals("rhythm-catalog", ignoreCase = true)) return null
        if (!parsed.host.equals("rendition", ignoreCase = true)) return null
        val id = parsed.path.orEmpty().removePrefix("/")
        return id.takeIf { Regex("^$uuid$").matches(it) }?.lowercase(Locale.ROOT)
    }

    /** Checks the URI/cache identity available to Media3's DataSpec resolver. */
    fun allowsAssetRequest(
        uri: String?,
        customCacheKey: String?,
        trustedServerUrl: String?,
    ): Boolean = requestAssetId(uri, customCacheKey, trustedServerUrl) != null

    /**
     * issue 11：判断某 URL 是否为后端签发的 COS presigned 直连 URL。用于播放数据源解析层决定
     * **不**给该请求附带后端 Bearer 令牌（签名已在 URL 里，令牌不得泄露给对象存储）。
     */
    fun isSignedObjectStoreUrl(uri: String?): Boolean =
        runCatching { URI(uri) }.getOrNull()?.let(::isSignedObjectStoreUri) ?: false

    /** Explicit real-audio allowlist; a generic audio wildcard is insufficient because of MIDI. */
    fun isPlayableMediaType(mediaType: String?): Boolean =
        mediaType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .let(playableMediaTypes::contains)

    /** Only server-validated MusicXML assets may enter the alphaTab parser. */
    fun isMusicXmlMediaType(mediaType: String?): Boolean =
        mediaType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .let(musicXmlMediaTypes::contains)

    fun isPlayableRenditionAsset(asset: RenditionAsset): Boolean =
        asset.role.lowercase(Locale.ROOT) in playableRoles && isPlayableMediaType(asset.mediaType)

    fun isPlayableRendition(rendition: Rendition): Boolean =
        rendition.assets.any(::isPlayableRenditionAsset)

    private fun requestAssetId(
        uri: String?,
        customCacheKey: String?,
        trustedServerUrl: String?,
    ): String? {
        val cacheMatch = customCacheKey?.let(cacheKeyPattern::matchEntire) ?: return null
        val cacheAssetId = cacheMatch.groupValues[1]
        val parsedUri = runCatching { URI(uri) }.getOrNull() ?: return null
        if (parsedUri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return null
        // 1) 后端受管代理：同源 + /v2/assets/{id}/content，URL 内资产 UUID 必须与 cacheKey 一致。
        val trustedUri = runCatching { URI(trustedServerUrl) }.getOrNull()
        if (trustedUri != null && sameOrigin(parsedUri, trustedUri)) {
            val pathAssetId = assetPathPattern.find(parsedUri.path.orEmpty())?.groupValues?.get(1)
            if (pathAssetId != null && pathAssetId.equals(cacheAssetId, ignoreCase = true)) {
                return cacheAssetId
            }
        }
        // 2) issue 11：COS presigned 直连——身份绑定退回 cacheKey（URL 路径不含资产 UUID）。
        if (isSignedObjectStoreUri(parsedUri)) {
            return cacheAssetId
        }
        return null
    }

    private fun isSignedObjectStoreUri(uri: URI): Boolean {
        if (!SIGNED_OBJECT_STORE_ENABLED) return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        if (!host.endsWith(COS_HOST_SUFFIX)) return false
        val query = uri.rawQuery ?: return false
        return query.contains("q-signature=") && query.contains("q-sign-algorithm=")
    }

    private fun sameOrigin(left: URI, right: URI): Boolean =
        left.scheme.equals(right.scheme, ignoreCase = true) &&
            left.host.equals(right.host, ignoreCase = true) &&
            effectivePort(left) == effectivePort(right)

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
}
