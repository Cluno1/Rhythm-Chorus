package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import chromahub.rhythm.app.features.catalog.data.local.CatalogCache
import chromahub.rhythm.app.features.catalog.data.local.CatalogQueueStore
import chromahub.rhythm.app.features.catalog.data.local.CatalogAssetCache
import chromahub.rhythm.app.features.catalog.data.remote.CatalogApiClient
import chromahub.rhythm.app.features.catalog.data.remote.CatalogDtoMapper
import chromahub.rhythm.app.features.catalog.data.remote.CatalogEndpoint
import chromahub.rhythm.app.features.catalog.domain.CatalogChanges
import chromahub.rhythm.app.features.catalog.domain.CatalogConnection
import chromahub.rhythm.app.features.catalog.domain.CatalogFailure
import chromahub.rhythm.app.features.catalog.domain.CatalogPage
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
import java.security.MessageDigest

class CatalogRepositoryImpl(context: Context) : CatalogRepository {
    private val credentials = CatalogCredentialsStore(context)
    private val cache = CatalogCache(context)
    private val queueStore = CatalogQueueStore(context)
    private val assetCache = CatalogAssetCache(context)

    override fun connection(): CatalogConnection {
        val server = credentials.loadServerUrl().orEmpty()
        return CatalogConnection(server, server.isNotEmpty() && !credentials.loadToken().isNullOrEmpty())
    }

    override fun cachedWorks(): List<WorkSummary> = cache.loadWorks()
    override fun cachedBundle(workId: String): WorkBundle? = cache.loadBundle(workId)

    override suspend fun testConnection(serverUrl: String, token: String): Result<Unit> = guarded {
        val normalized = CatalogEndpoint.normalize(serverUrl)
        val client = CatalogApiClient(normalized, token)
        requireSuccessful(client.api.health(), allowUnauthenticated = true)
        requireSuccessful(client.api.works(limit = 1))
        return@guarded Unit
    }

    override suspend fun saveConnection(serverUrl: String, token: String): Result<Unit> {
        val normalized = runCatching { CatalogEndpoint.normalize(serverUrl) }
            .getOrElse { return Result.failure(CatalogFailure.InvalidData(it.message ?: "服务器地址无效", it)) }
        val tested = testConnection(normalized, token)
        if (tested.isFailure) return tested
        val oldUrl = credentials.loadServerUrl()
        val oldToken = credentials.loadToken()
        if (oldUrl != null && (oldUrl != normalized || oldToken != token.trim())) {
            cache.clearSession()
            queueStore.clear()
        }
        credentials.save(normalized, token)
        return Result.success(Unit)
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
        }
    }

    override suspend fun getPlayback(renditionId: String, prefer: String): Result<PlaybackDescriptor> = guarded {
        val id = validUuid(renditionId)
        val apiClient = client()
        val descriptor = CatalogDtoMapper.playback(apiClient.api.playback(id, prefer).bodyOrThrow())
        require(descriptor.renditionId == id) { "playback rendition id does not match request" }
        val absoluteUrl = apiClient.resolveAssetUrl(descriptor.relativeUrl).toString()
        require(
            CatalogPlaybackPolicy.allows(
                mediaId = "rhythm-catalog:rendition:${descriptor.renditionId}:asset:${descriptor.assetId}",
                uri = absoluteUrl,
                customCacheKey = descriptor.cacheKey,
                trustedServerUrl = connection().serverUrl,
            ),
        ) { "playback descriptor failed managed asset policy" }
        descriptor.copy(relativeUrl = absoluteUrl)
    }

    override suspend fun downloadAsset(
        assetId: String,
        expectedSha256: String,
        expectedSize: Long,
    ): Result<ByteArray> = guarded {
        val id = validUuid(assetId)
        require(expectedSha256.matches(Regex("^[0-9a-fA-F]{64}$"))) { "expected SHA-256 is invalid" }
        require(expectedSize > 0) { "expected size must be positive" }
        assetCache.read(id, expectedSha256, expectedSize)?.let { return@guarded it }
        val bytes = client().api.asset(id).bodyOrThrow().bytes()
        require(bytes.size.toLong() == expectedSize) { "asset size mismatch" }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(actual.equals(expectedSha256, ignoreCase = true)) { "asset SHA-256 mismatch" }
        assetCache.write(id, expectedSha256, bytes)
        bytes
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
        val token = credentials.loadToken() ?: throw CatalogFailure.NotConfigured()
        return CatalogApiClient(server, token)
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
