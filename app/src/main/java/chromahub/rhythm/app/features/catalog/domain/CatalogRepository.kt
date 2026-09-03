package chromahub.rhythm.app.features.catalog.domain

sealed class CatalogFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotConfigured : CatalogFailure("私有作品库尚未配置")
    class InvalidCredentials : CatalogFailure("私有作品库凭据已失效")
    class Forbidden : CatalogFailure("当前凭据无权访问此内容")
    class Unreachable(cause: Throwable? = null) : CatalogFailure("无法连接私有作品库", cause)
    class Server(val statusCode: Int) : CatalogFailure("服务器暂时不可用（$statusCode）")
    class InvalidData(detail: String, cause: Throwable? = null) : CatalogFailure("服务器数据无效：$detail", cause)
}

data class CatalogConnection(
    val serverUrl: String,
    val configured: Boolean,
)

interface CatalogRepository {
    fun connection(): CatalogConnection
    fun cachedWorks(): List<WorkSummary>
    fun cachedBundle(workId: String): WorkBundle?
    suspend fun testConnection(serverUrl: String, token: String): Result<Unit>
    suspend fun saveConnection(serverUrl: String, token: String): Result<Unit>
    fun clearConnection()
    suspend fun listWorks(query: String? = null, cursor: String? = null, limit: Int = 50): Result<CatalogPage>
    suspend fun getWorkBundle(workId: String, forceRefresh: Boolean = false): Result<WorkBundle>
    suspend fun getScoreRevision(revisionId: String): Result<ScoreRevision>
    suspend fun getPlayback(renditionId: String, prefer: String = "stream"): Result<PlaybackDescriptor>
    suspend fun downloadAsset(assetId: String, expectedSha256: String, expectedSize: Long): Result<ByteArray>
    suspend fun syncChanges(): Result<CatalogChanges>
}
