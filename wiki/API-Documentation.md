# Rhythm API Documentation

Internal API reference for developers working with Rhythm's codebase. This covers key interfaces, classes, and extension functions.

---

## 📚 Overview

Rhythm's internal APIs are organized into several modules:
- **Player API**: Playback control and state management
- **Repository API**: Data access interfaces
- **ViewModel API**: State management and business logic
- **Service API**: Background services and workers
- **Utility API**: Helper functions and extensions

---

## 🎵 Player API

### MusicPlayer Interface

Core playback control interface.

```kotlin
interface MusicPlayer {
    /**
     * Current playback state
     */
    val playbackState: StateFlow<PlaybackState>
    
    /**
     * Currently playing song
     */
    val currentSong: StateFlow<Song?>
    
    /**
     * Current playback position in milliseconds
     */
    val currentPosition: StateFlow<Long>
    
    /**
     * Play a song
     * @param song Song to play
     * @param startPosition Starting position in milliseconds (default: 0)
     */
    suspend fun playSong(song: Song, startPosition: Long = 0L)
    
    /**
     * Toggle play/pause state
     */
    fun togglePlayPause()
    
    /**
     * Pause playback
     */
    fun pause()
    
    /**
     * Resume playback
     */
    fun resume()
    
    /**
     * Seek to position
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)
    
    /**
     * Skip to next song
     */
    fun skipToNext()
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious()
    
    /**
     * Set repeat mode
     * @param mode RepeatMode.OFF, RepeatMode.ONE, or RepeatMode.ALL
     */
    fun setRepeatMode(mode: RepeatMode)
    
    /**
     * Set shuffle enabled
     * @param enabled True to enable shuffle
     */
    fun setShuffleEnabled(enabled: Boolean)
    
    /**
     * Release resources
     */
    fun release()
}
```

### PlaybackState Sealed Class

```kotlin
sealed class PlaybackState {
    /** Idle, no song loaded */
    object Idle : PlaybackState()
    
    /** Loading song */
    object Loading : PlaybackState()
    
    /** Playing */
    data class Playing(val position: Long, val duration: Long) : PlaybackState()
    
    /** Paused */
    data class Paused(val position: Long, val duration: Long) : PlaybackState()
    
    /** Buffering */
    object Buffering : PlaybackState()
    
    /** Error occurred */
    data class Error(val message: String, val exception: Throwable?) : PlaybackState()
}
```

### RepeatMode Enum

```kotlin
enum class RepeatMode {
    /** No repeat */
    OFF,
    
    /** Repeat current song */
    ONE,
    
    /** Repeat all songs in queue */
    ALL
}
```

---

## 🗄️ Repository API

### MusicRepository Interface

Data access for music library.

```kotlin
interface MusicRepository {
    /**
     * Get all songs
     * @return Flow of song list
     */
    fun getAllSongs(): Flow<List<Song>>
    
    /**
     * Get song by ID
     * @param id Song ID
     * @return Song or null if not found
     */
    suspend fun getSongById(id: Long): Song?
    
    /**
     * Get all albums
     * @return Flow of album list
     */
    fun getAllAlbums(): Flow<List<Album>>
    
    /**
     * Get album by ID
     * @param id Album ID
     * @return Album or null if not found
     */
    suspend fun getAlbumById(id: Long): Album?
    
    /**
     * Get songs for album
     * @param albumId Album ID
     * @return Flow of songs in album
     */
    fun getSongsForAlbum(albumId: Long): Flow<List<Song>>
    
    /**
     * Get all artists
     * @return Flow of artist list
     */
    fun getAllArtists(): Flow<List<Artist>>
    
    /**
     * Get artist by ID
     * @param id Artist ID
     * @return Artist or null if not found
     */
    suspend fun getArtistById(id: Long): Artist?
    
    /**
     * Get songs for artist
     * @param artistId Artist ID
     * @return Flow of songs by artist
     */
    fun getSongsForArtist(artistId: Long): Flow<List<Song>>
    
    /**
     * Update song metadata
     * @param song Song to update
     */
    suspend fun updateSong(song: Song)
    
    /**
     * Refresh media library
     */
    suspend fun refreshLibrary()
}
```

### PlaylistRepository Interface

Playlist management.

```kotlin
interface PlaylistRepository {
    /**
     * Get all playlists
     * @return Flow of playlist list
     */
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    /**
     * Get playlist by ID
     * @param id Playlist ID
     * @return Playlist or null if not found
     */
    suspend fun getPlaylistById(id: Long): Playlist?
    
    /**
     * Create new playlist
     * @param name Playlist name
     * @return Created playlist
     */
    suspend fun createPlaylist(name: String): Playlist
    
    /**
     * Delete playlist
     * @param id Playlist ID
     */
    suspend fun deletePlaylist(id: Long)
    
    /**
     * Add song to playlist
     * @param playlistId Playlist ID
     * @param songId Song ID
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    
    /**
     * Remove song from playlist
     * @param playlistId Playlist ID
     * @param songId Song ID
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    
    /**
     * Get songs in playlist
     * @param playlistId Playlist ID
     * @return Flow of songs in playlist
     */
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
    
    /**
     * Export playlist to M3U file
     * @param playlist Playlist to export
     * @param outputFile Target file
     */
    suspend fun exportPlaylistM3U(playlist: Playlist, outputFile: File)
    
    /**
     * Import playlist from M3U file
     * @param inputFile M3U file
     * @return Imported playlist
     */
    suspend fun importPlaylistM3U(inputFile: File): Playlist
}
```

---

## 🎨 ViewModel API

### MusicViewModel

Main music player ViewModel.

```kotlin
class MusicViewModel(
    private val musicPlayer: MusicPlayer,
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    /**
     * Current playback state
     */
    val playbackState: StateFlow<PlaybackState> = musicPlayer.playbackState
    
    /**
     * Currently playing song
     */
    val currentSong: StateFlow<Song?> = musicPlayer.currentSong
    
    /**
     * All songs in library
     */
    val allSongs: StateFlow<List<Song>> = musicRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * Play song
     * @param song Song to play
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            musicPlayer.playSong(song)
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
    }
    
    /**
     * Seek to position
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        musicPlayer.seekTo(positionMs)
    }
    
    /**
     * Skip to next song
     */
    fun skipToNext() {
        musicPlayer.skipToNext()
    }
    
    /**
     * Skip to previous song
     */
    fun skipToPrevious() {
        musicPlayer.skipToPrevious()
    }
    
    /**
     * Set repeat mode
     * @param mode Repeat mode
     */
    fun setRepeatMode(mode: RepeatMode) {
        musicPlayer.setRepeatMode(mode)
    }
    
    /**
     * Set shuffle enabled
     * @param enabled Shuffle state
     */
    fun setShuffleEnabled(enabled: Boolean) {
        musicPlayer.setShuffleEnabled(enabled)
    }
}
```

### LibraryViewModel

Music library management.

```kotlin
class LibraryViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    /**
     * Search query
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * Filtered songs based on search
     */
    val filteredSongs: StateFlow<List<Song>> = combine(
        musicRepository.getAllSongs(),
        searchQuery
    ) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * Update search query
     * @param query Search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Refresh library
     */
    fun refreshLibrary() {
        viewModelScope.launch {
            musicRepository.refreshLibrary()
        }
    }
}
```

---

## 🔧 Service API

### MediaPlaybackService

Background playback service.

```kotlin
class MediaPlaybackService : Service() {
    
    /**
     * Service binder for communication
     */
    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }
    
    /**
     * ExoPlayer instance
     */
    lateinit var player: ExoPlayer
        private set
    
    /**
     * MediaSession for media controls
     */
    lateinit var mediaSession: MediaSession
        private set
    
    /**
     * Current song being played
     */
    val currentSong: StateFlow<Song?>
        get() = _currentSong.asStateFlow()
    
    /**
     * Play song
     * @param song Song to play
     */
    fun playSong(song: Song) {
        // Implementation
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        player.pause()
    }
    
    /**
     * Resume playback
     */
    fun resume() {
        player.play()
    }
    
    /**
     * Update notification
     */
    private fun updateNotification() {
        // Implementation
    }
}
```

---

## 📱 Widget API

### WidgetDataProvider

Provides data for widgets.

```kotlin
interface WidgetDataProvider {
    /**
     * Get current playback data for widget
     * @return Widget data
     */
    suspend fun getWidgetData(): WidgetData
}

/**
 * Widget display data
 */
data class WidgetData(
    val songTitle: String,
    val artist: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long
)
```

### GlanceWidget Base Class

```kotlin
abstract class RhythmGlanceWidget : GlanceAppWidget() {
    
    /**
     * Provide Glance content
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }
    
    /**
     * Widget content composable
     */
    @Composable
    abstract fun WidgetContent()
    
    /**
     * Handle widget update
     */
    abstract suspend fun onUpdate(context: Context, glanceId: GlanceId)
}
```

---

## 🛠️ Utility API

### Extension Functions

#### Time Formatting

```kotlin
/**
 * Format milliseconds to MM:SS or HH:MM:SS
 * @return Formatted time string
 */
fun Long.formatTime(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%02d:%02d", minutes, seconds % 60)
    }
}

// Usage: 125000L.formatTime() // "02:05"
```

#### File Size Formatting

```kotlin
/**
 * Format bytes to human-readable size
 * @return Formatted size string (e.g., "4.2 MB")
 */
fun Long.formatFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$this B"
    }
}

// Usage: 4567890L.formatFileSize() // "4.35 MB"
```

#### Bitmap Extensions

```kotlin
/**
 * Load album art from URI
 * @param context Context
 * @param uri Album art URI
 * @return Bitmap or null
 */
suspend fun Uri.loadAlbumArt(context: Context): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this@loadAlbumArt))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this@loadAlbumArt)
        }
    } catch (e: Exception) {
        null
    }
}
```

#### Context Extensions

```kotlin
/**
 * Show toast message
 * @param message Message to display
 * @param length Toast duration (default: LENGTH_SHORT)
 */
fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

/**
 * Check if permission is granted
 * @param permission Permission to check
 * @return True if granted
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
```

### Audio Utilities

#### AudioDeviceManager

```kotlin
object AudioDeviceManager {
    /**
     * Get available audio devices
     * @param context Context
     * @return List of audio devices
     */
    fun getAudioDevices(context: Context): List<AudioDevice> {
        // Implementation
    }
    
    /**
     * Check if Bluetooth audio connected
     * @param context Context
     * @return True if Bluetooth audio device connected
     */
    fun isBluetoothAudioConnected(context: Context): Boolean {
        val audioManager = context.getSystemService<AudioManager>()
        return audioManager?.isBluetoothA2dpOn == true
    }
}
```

#### AutoEQManager

```kotlin
object AutoEQManager {
    /**
     * Load AutoEQ preset for headphone model
     * @param model Headphone model name
     * @return EQ preset or null if not found
     */
    suspend fun loadPreset(model: String): EQPreset? {
        // Implementation
    }
    
    /**
     * Get all available presets
     * @return List of preset names
     */
    suspend fun getAllPresets(): List<String> {
        // Implementation
    }
    
    /**
     * Apply preset to equalizer
     * @param equalizer Equalizer instance
     * @param preset Preset to apply
     */
    fun applyPreset(equalizer: Equalizer, preset: EQPreset) {
        // Implementation
    }
}
```

### Lyrics API

#### LyricsProvider

```kotlin
interface LyricsProvider {
    /**
     * Fetch lyrics for song
     * @param song Song to fetch lyrics for
     * @return Lyrics or null if not found
     */
    suspend fun fetchLyrics(song: Song): Lyrics?
}

/**
 * Lyrics data class
 */
data class Lyrics(
    val text: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>? = null
)

/**
 * Synced lyric line
 */
data class LyricLine(
    val text: String,
    val timestampMs: Long
)
```

#### LRCLib Integration

```kotlin
class LRCLibProvider(private val api: LRCLibApi) : LyricsProvider {
    
    override suspend fun fetchLyrics(song: Song): Lyrics? {
        return try {
            val response = api.getLyrics(song.title, song.artist)
            Lyrics(
                text = response.plainLyrics,
                isSynced = response.syncedLyrics != null,
                lines = parseLRC(response.syncedLyrics)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseLRC(lrc: String?): List<LyricLine>? {
        // LRC parsing implementation
    }
}
```

---

## 🎛️ Equalizer API

### EqualizerController

```kotlin
interface EqualizerController {
    /**
     * Equalizer enabled state
     */
    val isEnabled: StateFlow<Boolean>
    
    /**
     * Band levels (-1500 to 1500)
     */
    val bandLevels: StateFlow<IntArray>
    
    /**
     * Enable/disable equalizer
     * @param enabled True to enable
     */
    fun setEnabled(enabled: Boolean)
    
    /**
     * Set band level
     * @param band Band index (0-9 for 10-band EQ)
     * @param level Level in millibels (-1500 to 1500)
     */
    fun setBandLevel(band: Int, level: Int)
    
    /**
     * Get preset names
     * @return List of preset names
     */
    fun getPresetNames(): List<String>
    
    /**
     * Use preset
     * @param presetIndex Preset index
     */
    fun usePreset(presetIndex: Int)
    
    /**
     * Reset to flat
     */
    fun reset()
}
```

---

## 📊 Data Models

### Song

```kotlin
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val trackNumber: Int,
    val year: Int,
    val genre: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L
)
```

### Album

```kotlin
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri?,
    val year: Int,
    val songCount: Int
)
```

### Artist

```kotlin
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int
)
```

### Playlist

```kotlin
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdDate: Long,
    val modifiedDate: Long
)
```

---

## 🔄 Flow Utilities

### StateFlow Helpers

```kotlin
/**
 * Combine multiple StateFlows
 */
fun <T1, T2, R> combineStates(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    transform: (T1, T2) -> R
): StateFlow<R> = combine(flow1, flow2, transform)
    .stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, transform(flow1.value, flow2.value))
```

---

## 🧪 Testing Utilities

### Fake Implementations

```kotlin
class FakeMusicRepository : MusicRepository {
    private val songs = MutableStateFlow<List<Song>>(emptyList())
    
    override fun getAllSongs(): Flow<List<Song>> = songs.asStateFlow()
    
    override suspend fun getSongById(id: Long): Song? = songs.value.find { it.id == id }
    
    fun addSong(song: Song) {
        songs.value = songs.value + song
    }
    
    // Other methods...
}
```

---

## 📚 Further Documentation

- [Architecture Guide](https://github.com/cromaguy/Rhythm/wiki/Architecture) - App structure and patterns
- [Technology Stack](https://github.com/cromaguy/Rhythm/wiki/Technology-Stack) - Technologies used
- [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing) - Development guidelines

---

**Questions?** Check the [FAQ](https://github.com/cromaguy/Rhythm/wiki/FAQ) or ask in [Telegram](https://t.me/RhythmSupport) or [Discord](https://discord.gg/XjPyUYPQYc)!
