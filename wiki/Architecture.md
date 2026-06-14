# Rhythm Architecture Guide

Technical documentation of Rhythm's app structure, design patterns, and architectural decisions.

## 🔄 Dual-Mode Architecture

Rhythm employs a unique dual-mode architecture to support both local and streaming playback experiences while sharing core infrastructure.

### Local Mode (`features/local`)
Focuses on device-based media using the Android `MediaStore` API. It handles local file indexing, metadata extraction from files, and local playback state.

### Streaming Mode (`features/streaming`)
Provides a completely separate pipeline for streaming servers. It includes its own data repositories and presentation layer, allowing the app to function as a streaming client without interfering with the local library.

### Shared Core
Both modes leverage the `shared` and `infrastructure` layers:
- **Shared Data**: Common domain models (Song, Album, Artist) ensure consistency.
- **Playback Service**: A unified `MediaPlaybackService` handles the actual audio output via ExoPlayer, regardless of whether the source is local or streaming.
- **Infrastructure**: Common utilities for networking, permissions, and background workers are used by both modes.


### StateFlow Pattern

Reactive state updates using Kotlin Flow.

```kotlin
class PlayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    fun updateState(newState: PlayerState) {
        _state.value = newState
    }
}

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        is PlayerState.Playing -> ShowPlayingUI()
        is PlayerState.Paused -> ShowPausedUI()
        is PlayerState.Idle -> ShowIdleUI()
    }
}
```

---

## 🌐 Network Layer

### API Integration

```kotlin
interface LyricsApi {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String
    ): LyricsResponse
}

class LyricsRepository(private val api: LyricsApi) {
    suspend fun fetchLyrics(song: Song): Result<Lyrics> {
        return try {
            val response = api.getLyrics(song.title, song.artist)
            Result.success(response.toLyrics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🧪 Testing Architecture

### Unit Tests

```kotlin
class MusicViewModelTest {
    @Test
    fun `playSong updates state correctly`() = runTest {
        val viewModel = MusicViewModel()
        val testSong = Song(/* test data */)
        
        viewModel.playSong(testSong)
        
        assertEquals(testSong, viewModel.currentSong.value)
        assertEquals(PlaybackState.Playing, viewModel.playbackState.value)
    }
}
```

### UI Tests

```kotlin
@Test
fun playerScreen_showsCorrectSongInfo() {
    composeTestRule.setContent {
        PlayerScreen(song = testSong)
    }
    
    composeTestRule
        .onNodeWithText(testSong.title)
        .assertIsDisplayed()
}
```

---

## 🔐 Security & Privacy

### Data Privacy

- No analytics or tracking code
- All data stored locally
- No server communication except optional features
- Encrypted backups (optional)

### Permissions

```kotlin
object PermissionManager {
    fun requestStoragePermission(activity: Activity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Request READ_MEDIA_AUDIO
            }
            else -> {
                // Request READ_EXTERNAL_STORAGE
            }
        }
    }
}
```

---

## ⚡ Performance Optimization

### Lazy Loading

```kotlin
@Composable
fun SongList(songs: List<Song>) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            SongItem(song)
        }
    }
}
```

### Image Caching

```kotlin
// Coil for efficient image loading
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(song.albumArtUri)
        .crossfade(true)
        .build(),
    contentDescription = "Album Art"
)
```

### Background Processing

```kotlin
class MediaScanWorker : CoroutineWorker() {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Heavy processing off main thread
        scanMediaLibrary()
        Result.success()
    }
}
```

---

## 📊 Dependency Injection

Currently using manual DI. Future migration to Hilt planned.

```kotlin
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MusicRepository = MusicRepositoryImpl(
        MediaStoreDataSource(application)
    )
}
```

---

## 🔄 Build System

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 514081066
        versionName = "5.1.408.1066"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Version Catalog

```toml
[versions]
kotlin = "2.3.21"
composeBom = "2026.05.01"
media3 = "1.10.1"

[libraries]
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

---

## 🎯 Design Patterns

### Repository Pattern
- Abstraction over data sources
- Testable business logic
- Single source of truth

### Observer Pattern
- StateFlow for reactive updates
- LiveData alternative
- Lifecycle-aware

### Factory Pattern
- ViewModel creation
- Widget instantiation

### Singleton Pattern
- AppSettings
- Repository instances

---

## 📚 Further Reading

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)

---

**Questions?** Check [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing) or ask in [Telegram](https://t.me/RhythmSupport) or [Discord](https://discord.gg/XjPyUYPQYc)!
