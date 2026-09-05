package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import chromahub.rhythm.app.features.catalog.data.local.CatalogCache
import chromahub.rhythm.app.features.catalog.data.local.CatalogQueueStore
import chromahub.rhythm.app.features.catalog.data.local.CatalogOfflineCache
import chromahub.rhythm.app.features.catalog.data.remote.CatalogApiClient
import chromahub.rhythm.app.features.catalog.data.remote.CatalogDeviceAuthClient
import chromahub.rhythm.app.features.catalog.data.remote.CatalogDtoMapper
import chromahub.rhythm.app.features.catalog.data.remote.CatalogEndpoint
import chromahub.rhythm.app.features.catalog.domain.CatalogChanges
import chromahub.rhythm.app.features.catalog.domain.CatalogConnection
import chromahub.rhythm.app.features.catalog.domain.CatalogFailure
import chromahub.rhythm.app.features.catalog.domain.CatalogPage
import chromahub.rhythm.app.features.catalog.domain.CatalogLibraryAlbum
import chromahub.rhythm.app.features.catalog.domain.CatalogLibrarySnapshot
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import chromahub.rhythm.app.features.catalog.domain.CatalogRepository
import chromahub.rhythm.app.features.catalog.domain.PlaybackDescriptor
import chromahub.rhythm.app.features.catalog.domain.ScoreRevision
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class CatalogRepositoryImpl(context: Context) : CatalogRepository {
    private val credentials = CatalogCredentialsStore(context)
    private val cache = CatalogCache(context)
    private val queueStore = CatalogQueueStore(context)
    private val offlineCache = CatalogOfflineCache(context)

    override fun connection(): CatalogConnection {
        val server = credentials.loadServerUrl().orEmpty()
        return CatalogConnection(
            server,
            server.isNotEmpty() &&
                (credentials.loadDevice() != null || !credentials.loadToken().isNullOrEmpty()),
        )
    }

    override fun cachedWorks(): List<WorkSummary> = cache.loadWorks()
    override fun cachedBundle(workId: String): WorkBundle? = cache.loadBundle(workId)
    override fun cachedLibrary(): CatalogLibrarySnapshot? = cache.loadLibrary()

    override suspend fun enrollDevice(serverUrl: String, inviteCode: String): Result<Unit> = guarded {
        val normalized = CatalogEndpoint.normalize(serverUrl)
        val oldUrl = credentials.loadServerUrl()
        val oldIdentity = credentials.loadDeviceId() ?: credentials.loadToken()
        CatalogDeviceAuthClient(normalized, credentials).enroll(inviteCode)
        val newIdentity = credentials.loadDeviceId()
        if (oldUrl != null && (oldUrl != normalized || oldIdentity != newIdentity)) {
            cache.clearSession()
            queueStore.clear()
        }
    }

    override suspend fun issueInvite(
        serverUrl: String,
        username: String,
        password: String,
        userId: String,
        displayName: String?,
        replaceExistingDevice: Boolean,
    ): Result<String> = guarded {
        val normalized = CatalogEndpoint.normalize(serverUrl)
        CatalogDeviceAuthClient(normalized, credentials).issueInvite(
            username,
            password,
            userId,
            displayName,
            replaceExistingDevice,
        )
    }

    override fun clearConnection() {
        credentials.clear()
        cache.clearSession()
        queueStore.clear()
    }

    override suspend fun listWorks(query: String?, cursor: String?, limit: Int): Result<CatalogPage> = guarded {
        require(limit in 1..200) { "limit must be in 1..200" }
        val page = CatalogDtoMapper.page(client().api.works(query?.trim()?.takeIf { it.isNotEmpty() }, cursor, limit).bodyOrThrow())
        if (query.isNullOrBlank() && cursor == null) cache.saveWorks(page.items)
        page
    }.recoverCatching { error ->
        if (query.isNullOrBlank() && cursor == null && error is CatalogFailure.Unreachable) {
            val cached = cache.loadWorks()
            if (cached.isNotEmpty()) CatalogPage(cached, null, fromCache = true) else throw error
        } else throw error
    }

    override suspend fun getWorkBundle(workId: String, forceRefresh: Boolean): Result<WorkBundle> = guarded {
        val id = validUuid(workId)
        val etag = if (forceRefresh) null else cache.bundleEtag(id)
        val response = client().api.workBundle(id, etag)
        if (response.code() == 304) {
            cache.loadBundle(id) ?: throw CatalogFailure.InvalidData("服务器返回 304，但本地没有作品详情缓存")
        } else {
            val bundle = CatalogDtoMapper.bundle(response.bodyOrThrow())
            require(bundle.work.id == id) { "bundle work id does not match request" }
            cache.saveBundle(bundle, response.headers()["ETag"])
            bundle
        }
    }.recoverCatching { error ->
        if (error is CatalogFailure.Unreachable) cache.loadBundle(workId) ?: throw error else throw error
    }

    override suspend fun getScoreRevision(revisionId: String): Result<ScoreRevision> = guarded {
        val id = validUuid(revisionId)
        CatalogDtoMapper.scoreRevision(client().api.scoreRevision(id).bodyOrThrow()).also {
            require(it.id == id) { "score revision id does not match request" }
            cache.saveScoreRevision(it)
        }
    }.recoverCatching { error ->
        if (error is CatalogFailure.Unreachable) cache.loadScoreRevision(revisionId) ?: throw error else throw error
    }

    override suspend fun getPlayback(renditionId: String, prefer: String): Result<PlaybackDescriptor> = guarded {
        val id = validUuid(renditionId)
        val apiClient = client()
        val descriptor = CatalogDtoMapper.playback(apiClient.api.playback(id, prefer).bodyOrThrow())
        require(descriptor.renditionId == id) { "playback rendition id does not match request" }
        val absoluteUrl = if (descriptor.delivery == "signed_url") {
            // issue 11：signed_url 已是后端签发的 COS 绝对签名 URL，不做后端同源解析，
            // 由 CatalogPlaybackPolicy 校验其为 *.myqcloud.com 且带签名的受信 URL。
            descriptor.relativeUrl
        } else {
            apiClient.resolveAssetUrl(descriptor.relativeUrl).toString()
        }
        require(
            CatalogPlaybackPolicy.allows(
                mediaId = "rhythm-catalog:rendition:${descriptor.renditionId}:asset:${descriptor.assetId}",
                uri = absoluteUrl,
                customCacheKey = descriptor.cacheKey,
                mediaType = descriptor.mediaType,
                trustedServerUrl = connection().serverUrl,
            ),
        ) { "playback descriptor failed managed asset policy" }
        descriptor.copy(relativeUrl = absoluteUrl)
    }

    override suspend fun getLibrary(forceRefresh: Boolean): Result<CatalogLibrarySnapshot> = guarded {
        val api = client().api
        val songs = mutableListOf<chromahub.rhythm.app.features.catalog.domain.CatalogLibrarySong>()
        var songCursor: String? = null
        val songCursorGuard = CatalogPaginationCursorGuard("library songs")
        do {
            val page = CatalogDtoMapper.librarySongs(api.librarySongs(songCursor).bodyOrThrow())
            songs += page.first
            songCursor = songCursorGuard.advance(page.second)
        } while (songCursor != null)

        val albums = mutableListOf<CatalogLibraryAlbum>()
        var albumCursor: String? = null
        val albumCursorGuard = CatalogPaginationCursorGuard("library albums")
        do {
            val page = CatalogDtoMapper.libraryAlbums(api.libraryAlbums(albumCursor).bodyOrThrow())
            albums += page.first
            albumCursor = albumCursorGuard.advance(page.second)
        } while (albumCursor != null)

        require(songs.distinctBy { it.renditionId }.size == songs.size) { "library contains duplicate rendition_id" }
        require(albums.distinctBy { it.id }.size == albums.size) { "library contains duplicate album id" }
        require(songs.all { song -> albums.any { it.id == song.albumId } }) { "library song refers to an unknown album" }

        val detailedAlbums = albums.map { summary ->
            val detail = CatalogDtoMapper.libraryAlbumDetail(api.libraryAlbum(summary.id).bodyOrThrow())
            require(detail.id == summary.id) { "album detail id does not match request" }
            require(detail.key == summary.key && detail.title == summary.title) {
                "album detail identity does not match album list"
            }
            detail
        }
        val detailSongs = detailedAlbums.flatMap { it.songs }
        require(detailSongs.map { it.renditionId }.toSet() == songs.map { it.renditionId }.toSet()) {
            "album details and songs list do not describe the same renditions"
        }
        val snapshot = CatalogLibrarySnapshot(songs, detailedAlbums)
        cache.saveLibrary(snapshot)
        snapshot
    }.recoverCatching { error ->
        if (error is CatalogFailure.Unreachable) cache.loadLibrary() ?: throw error else throw error
    }

    override suspend fun getLibraryAlbum(albumId: String, forceRefresh: Boolean): Result<CatalogLibraryAlbum> = guarded {
        val id = validUuid(albumId)
        if (!forceRefresh) cache.loadLibraryAlbum(id)?.let { return@guarded it }
        CatalogDtoMapper.libraryAlbumDetail(client().api.libraryAlbum(id).bodyOrThrow()).also {
            require(it.id == id) { "album detail id does not match request" }
            cache.saveLibraryAlbum(it)
        }
    }.recoverCatching { error ->
        if (error is CatalogFailure.Unreachable) cache.loadLibraryAlbum(albumId) ?: throw error else throw error
    }

    override suspend fun downloadAsset(
        assetId: String,
        expectedSha256: String,
        expectedSize: Long,
    ): Result<ByteArray> = downloadScoreAsset(
        assetId = assetId,
        expectedSha256 = expectedSha256,
        expectedSize = expectedSize,
        revisionId = null,
    )

    private suspend fun downloadScoreAsset(
        assetId: String,
        expectedSha256: String,
        expectedSize: Long,
        revisionId: String?,
    ): Result<ByteArray> = guarded {
        val id = validUuid(assetId)
        require(expectedSha256.matches(Regex("^[0-9a-fA-F]{64}$"))) { "expected SHA-256 is invalid" }
        require(expectedSize > 0) { "expected size must be positive" }
        val namespace = cacheNamespace()
        offlineCache.readAsset(namespace, id, expectedSha256, expectedSize)?.let { return@guarded it }
        val apiClient = client()
        val descriptor = CatalogDtoMapper.assetDelivery(apiClient.api.assetDelivery(id).bodyOrThrow())
        require(descriptor.assetId == id) { "asset delivery id does not match request" }
        require(descriptor.sha256.equals(expectedSha256, ignoreCase = true)) { "asset delivery SHA-256 mismatch" }
        require(descriptor.byteSize == expectedSize) { "asset delivery size mismatch" }
        val absoluteUrl = when (descriptor.delivery) {
            "signed_url" -> descriptor.relativeUrl.also {
                require(CatalogPlaybackPolicy.isSignedObjectStoreUrl(it)) { "signed asset delivery is not a trusted COS URL" }
            }
            else -> apiClient.resolveAssetUrl(descriptor.relativeUrl).toString()
        }
        apiClient.api.deliveredAsset(absoluteUrl).bodyOrThrow().use { body ->
            offlineCache.store(
                namespace = namespace,
                kind = CatalogOfflineCache.KIND_SCORE,
                assetId = id,
                sha256 = expectedSha256,
                byteSize = expectedSize,
                mediaType = descriptor.mediaType,
                revisionId = revisionId,
                input = body.byteStream(),
            )
        }
        offlineCache.readAsset(namespace, id, expectedSha256, expectedSize)
            ?: throw IOException("cached score could not be read after download")
    }

    override suspend fun cachePlaybackAndLatestScore(
        workId: String,
        arrangementId: String,
        renditionId: String,
    ): Result<Unit> = guarded {
        val validWorkId = validUuid(workId)
        val validArrangementId = validUuid(arrangementId)
        val validRenditionId = validUuid(renditionId)
        val descriptor = getPlayback(validRenditionId).getOrThrow()
        cachePlayback(descriptor)

        val bundle = getWorkBundle(validWorkId, forceRefresh = true).getOrThrow()
        val arrangement = bundle.arrangements.firstOrNull { it.id == validArrangementId }
            ?: throw CatalogFailure.InvalidData("播放条目对应的编曲不存在")
        val score = arrangement.preferredScoreId
            ?.let { preferred -> arrangement.scores.firstOrNull { it.id == preferred } }
            ?.takeIf { it.headRevisionId != null || it.publishedRevisionId != null }
            ?: arrangement.scores.firstOrNull { it.headRevisionId != null || it.publishedRevisionId != null }
            ?: return@guarded Unit
        val revisionId = score.headRevisionId ?: score.publishedRevisionId ?: return@guarded Unit
        val revision = getScoreRevision(revisionId).getOrThrow()
        val asset = revision.primaryMusicXml
            ?: throw CatalogFailure.InvalidData("最新谱面修订没有 primary_musicxml")
        downloadScoreAsset(
            assetId = asset.assetId,
            expectedSha256 = asset.sha256,
            expectedSize = asset.byteSize,
            revisionId = revision.id,
        ).getOrThrow()
    }

    private suspend fun cachePlayback(descriptor: PlaybackDescriptor) {
        val hash = descriptor.cacheKey.substringAfterLast(':')
        require(hash.matches(Regex("^[0-9a-f]{64}$"))) { "playback cache key hash is invalid" }
        val namespace = cacheNamespace()
        if (offlineCache.findAudio(namespace, descriptor.renditionId)?.let {
                it.assetId == descriptor.assetId &&
                    it.sha256 == hash &&
                    it.byteSize == descriptor.byteSize
            } == true
        ) return
        val apiClient = client()
        apiClient.api.deliveredAsset(descriptor.relativeUrl).bodyOrThrow().use { body ->
            offlineCache.store(
                namespace = namespace,
                kind = CatalogOfflineCache.KIND_AUDIO,
                assetId = descriptor.assetId,
                sha256 = hash,
                byteSize = descriptor.byteSize,
                mediaType = descriptor.mediaType,
                renditionId = descriptor.renditionId,
                input = body.byteStream(),
            )
        }
    }

    override suspend fun syncChanges(): Result<CatalogChanges> = guarded {
        var cursor = cache.syncCursor()
        val all = mutableListOf<chromahub.rhythm.app.features.catalog.domain.CatalogChange>()
        var hasMore: Boolean
        do {
            val page = CatalogDtoMapper.changes(client().api.changes(cursor).bodyOrThrow())
            require(page.nextCursor >= cursor) { "sync cursor moved backwards" }
            all += page.changes
            cursor = page.nextCursor
            hasMore = page.hasMore
        } while (hasMore)
        val tombstonedWorks = all.filter { it.tombstone && it.entityType == "work" }.map { it.entityId }.toSet()
        cache.removeWorks(tombstonedWorks)
        cache.saveSyncCursor(cursor)
        CatalogChanges(all, cursor, false)
    }

    private fun client(): CatalogApiClient {
        val server = credentials.loadServerUrl() ?: throw CatalogFailure.NotConfigured()
        if (credentials.loadDevice() == null && credentials.loadToken() == null) {
            throw CatalogFailure.NotConfigured()
        }
        return CatalogApiClient(server, credentials)
    }

    private fun cacheNamespace(): String {
        val server = credentials.loadServerUrl() ?: throw CatalogFailure.NotConfigured()
        val identity = credentials.loadDeviceId() ?: credentials.loadToken()
            ?: throw CatalogFailure.NotConfigured()
        return CatalogOfflineCache.namespace(server, identity)
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (error: Throwable) {
            when (error) {
                is CatalogFailure -> Result.failure(error)
                is IOException -> Result.failure(CatalogFailure.Unreachable(error))
                is IllegalArgumentException, is IllegalStateException ->
                    Result.failure(CatalogFailure.InvalidData(error.message ?: "未知数据错误", error))
                else -> Result.failure(error)
            }
        }
    }

    private fun validUuid(value: String): String = runCatching { java.util.UUID.fromString(value).toString() }
        .getOrElse { throw CatalogFailure.InvalidData("ID 不是有效 UUID") }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) throw httpFailure(code())
        return body() ?: throw CatalogFailure.InvalidData("服务器返回空响应")
    }

    private fun <T> requireSuccessful(response: Response<T>, allowUnauthenticated: Boolean = false) {
        if (!response.isSuccessful) {
            if (allowUnauthenticated && response.code() == 401) return
            throw httpFailure(response.code())
        }
    }

    private fun httpFailure(code: Int): CatalogFailure = when (code) {
        401 -> CatalogFailure.InvalidCredentials()
        403 -> CatalogFailure.Forbidden()
        else -> CatalogFailure.Server(code)
    }
}
