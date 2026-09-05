package chromahub.rhythm.app.features.catalog.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import chromahub.rhythm.app.features.catalog.di.CatalogModule
import chromahub.rhythm.app.features.catalog.data.local.CatalogQueueStore
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackItem
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import chromahub.rhythm.app.features.catalog.domain.CatalogFailure
import chromahub.rhythm.app.features.catalog.domain.PlaybackDescriptor
import chromahub.rhythm.app.features.catalog.domain.ScoreRevision
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
import chromahub.rhythm.app.features.catalog.domain.RhythmQueueEntry
import chromahub.rhythm.app.features.catalog.domain.CatalogLibraryAlbum
import chromahub.rhythm.app.features.catalog.domain.CatalogLibrarySong
import chromahub.rhythm.app.shared.data.model.Song
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val configured: Boolean = false,
    val serverUrl: String = "",
    val works: List<WorkSummary> = emptyList(),
    val songs: List<CatalogLibrarySong> = emptyList(),
    val albums: List<CatalogLibraryAlbum> = emptyList(),
    val selectedBundle: WorkBundle? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val offlineSnapshot: Boolean = false,
    val error: String? = null,
)

data class RestoredUnifiedQueue(
    val songs: List<Song>,
    val catalogEntries: List<RhythmQueueEntry>,
    val currentIndex: Int,
    val positionMs: Long,
)

private data class RestoredQueueItem(
    val sourceIndex: Int,
    val song: Song,
    val catalogEntry: RhythmQueueEntry?,
)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogModule.repository(application)
    private val queueStore = CatalogQueueStore(application)
    private val _state = MutableStateFlow(
        repository.connection().let {
            CatalogUiState(
                configured = it.configured,
                serverUrl = it.serverUrl,
                works = repository.cachedWorks(),
                songs = repository.cachedLibrary()?.songs.orEmpty(),
                albums = repository.cachedLibrary()?.albums.orEmpty(),
            )
        },
    )
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var libraryRefreshJob: Job? = null

    init {
        if (_state.value.configured) refreshLibrary()
    }

    fun saveConnection(serverUrl: String, token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.saveConnection(serverUrl, token).fold(
                onSuccess = {
                    val connection = repository.connection()
                    _state.value = _state.value.copy(
                        configured = true,
                        serverUrl = connection.serverUrl,
                        loading = false,
                    )
                    refreshLibrary()
                },
                onFailure = { _state.value = _state.value.copy(loading = false, error = message(it)) },
            )
        }
    }

    fun clearConnection() {
        repository.clearConnection()
        queueStore.clear()
        _state.value = CatalogUiState()
    }

    fun refreshWorks(query: String? = null) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!query.isNullOrBlank()) delay(300)
            val hadItems = _state.value.works.isNotEmpty()
            _state.value = _state.value.copy(
                loading = !hadItems,
                refreshing = hadItems,
                error = null,
            )
            repository.listWorks(query).fold(
                onSuccess = { page ->
                    _state.value = _state.value.copy(
                        works = page.items,
                        loading = false,
                        refreshing = false,
                        offlineSnapshot = page.fromCache,
                    )
                    if (!page.fromCache && query.isNullOrBlank()) repository.syncChanges()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        offlineSnapshot = hadItems && error is CatalogFailure.Unreachable,
                        error = message(error),
                    )
                },
            )
        }
    }

    fun openWork(workId: String) {
        viewModelScope.launch {
            val cached = repository.cachedBundle(workId)
            _state.value = _state.value.copy(selectedBundle = cached, loading = cached == null, error = null)
            repository.getWorkBundle(workId).fold(
                onSuccess = { _state.value = _state.value.copy(selectedBundle = it, loading = false) },
                onFailure = { _state.value = _state.value.copy(loading = false, error = message(it)) },
            )
        }
    }

    suspend fun loadWork(workId: String): Result<WorkBundle> {
        val cached = repository.cachedBundle(workId)
        if (cached != null) _state.value = _state.value.copy(selectedBundle = cached)
        return repository.getWorkBundle(workId).onSuccess {
            _state.value = _state.value.copy(selectedBundle = it, error = null)
        }
    }

    fun refreshLibrary() {
        libraryRefreshJob?.cancel()
        libraryRefreshJob = viewModelScope.launch {
            val hadItems = _state.value.songs.isNotEmpty() || _state.value.albums.isNotEmpty()
            _state.value = _state.value.copy(
                loading = !hadItems,
                refreshing = hadItems,
                error = null,
            )
            repository.getLibrary(forceRefresh = true).fold(
                onSuccess = { snapshot ->
                    _state.value = _state.value.copy(
                        songs = snapshot.songs,
                        albums = snapshot.albums,
                        loading = false,
                        refreshing = false,
                        offlineSnapshot = snapshot.fromCache,
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        offlineSnapshot = hadItems && error is CatalogFailure.Unreachable,
                        error = message(error),
                    )
                },
            )
        }
    }

    fun closeWork() {
        _state.value = _state.value.copy(selectedBundle = null, error = null)
    }

    suspend fun playback(renditionId: String): Result<PlaybackDescriptor> = repository.getPlayback(renditionId)

    suspend fun scoreRevision(revisionId: String): Result<ScoreRevision> = repository.getScoreRevision(revisionId)

    suspend fun scoreHistory(headRevisionId: String): Result<List<ScoreRevision>> = runCatching {
        val revisions = mutableListOf<ScoreRevision>()
        val seen = mutableSetOf<String>()
        var next: String? = headRevisionId
        while (next != null) {
            check(seen.add(next)) { "谱面修订链出现循环" }
            val revision = repository.getScoreRevision(next).getOrElse { error ->
                if (revisions.isNotEmpty()) return@runCatching revisions
                throw error
            }
            revisions += revision
            next = revision.basedOnRevisionId
        }
        revisions
    }

    suspend fun scoreBytes(revision: ScoreRevision): Result<ByteArray> {
        val asset = revision.primaryMusicXml
            ?: return Result.failure(CatalogFailure.InvalidData("谱面修订没有 primary_musicxml"))
        return repository.downloadAsset(asset.assetId, asset.sha256, asset.byteSize)
    }

    suspend fun restoreQueue(deviceSongs: List<Song> = emptyList()): Result<RestoredUnifiedQueue?> {
        val record = queueStore.load() ?: return Result.success(null)
        if (!repository.connection().configured) return Result.success(null)
        return runCatching {
            val deviceSongsById = deviceSongs.associateBy(Song::id)
            if (deviceSongsById.isEmpty() && record.entries.any { it.isDevice() }) {
                // Most commonly the runtime audio permission has not been granted yet. Keep the
                // complete record intact and retry after MediaStore initialization.
                return@runCatching null
            }
            val restored = record.entries.mapIndexedNotNull { sourceIndex, saved ->
                if (saved.isDevice()) {
                    saved.deviceSongId?.let(deviceSongsById::get)?.let {
                        RestoredQueueItem(sourceIndex, it, null)
                    }
                } else {
                    val nowPlaying = saved.nowPlaying ?: return@mapIndexedNotNull null
                    val playback = CatalogPlaybackItem(
                        renditionId = nowPlaying.renditionId,
                        assetId = null,
                        title = saved.title ?: nowPlaying.title,
                        artist = saved.artist ?: nowPlaying.subtitle,
                        arrangementName = saved.arrangementName.orEmpty(),
                        playbackUrl = CatalogPlaybackPolicy.deferredUri(nowPlaying.renditionId),
                        cacheKey = null,
                        mediaType = "audio/mpeg",
                        durationMs = saved.durationMs,
                        albumId = saved.albumId,
                        artworkUrl = CatalogPlaybackPolicy.resolveAutomaticArtworkUrl(
                            saved.artworkUrl,
                            repository.connection().serverUrl,
                        ),
                    )
                    val entry = RhythmQueueEntry(
                        nowPlaying = nowPlaying.copy(assetId = null),
                        playback = playback,
                    )
                    val song = Song(
                        id = playback.toMediaItem().mediaId,
                        title = nowPlaying.title,
                        artist = nowPlaying.subtitle,
                        album = playback.arrangementName,
                        albumId = playback.albumId.ifBlank { nowPlaying.arrangementId },
                        duration = playback.durationMs,
                        uri = Uri.parse(playback.playbackUrl),
                        artworkUri = playback.artworkUrl?.let(Uri::parse),
                        codec = playback.mediaType,
                    )
                    RestoredQueueItem(sourceIndex, song, entry)
                }
            }
            if (restored.isEmpty()) return@runCatching null
            val currentIndex = restored.indexOfFirst { it.sourceIndex == record.currentIndex }
                .takeIf { it >= 0 }
                ?: restored.indexOfLast { it.sourceIndex < record.currentIndex }.coerceAtLeast(0)
            RestoredUnifiedQueue(
                songs = restored.map { it.song },
                catalogEntries = restored.mapNotNull { it.catalogEntry },
                currentIndex = currentIndex,
                positionMs = record.positionMs,
            )
        }
    }

    private fun message(error: Throwable): String = error.message ?: "发生未知错误"
}
