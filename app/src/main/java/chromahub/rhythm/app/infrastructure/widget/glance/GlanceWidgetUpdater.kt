package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import chromahub.rhythm.app.shared.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.withContext
import chromahub.rhythm.app.infrastructure.service.RhythmTileService

/**
 * Utility object for updating the Glance-based widget
 * 
 * This handles updating widget state when playback changes
 */
object GlanceWidgetUpdater {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    /**
     * Update widget with current playback state
     */
    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
        isFavorite: Boolean = false
    ) {
        // Update dynamic launcher shortcuts
        updateAppShortcuts(context, isPlaying)

        // Update SharedPreferences for legacy widget
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (song != null) {
                putString(RhythmMusicWidget.KEY_SONG_ID, song.id)
                putString(RhythmMusicWidget.KEY_SONG_TITLE, song.title)
                putString(RhythmMusicWidget.KEY_ARTIST_NAME, song.artist)
                putString(RhythmMusicWidget.KEY_ALBUM_NAME, song.album)
                putString(RhythmMusicWidget.KEY_ARTWORK_URI, song.artworkUri?.toString())
            } else {
                putString(RhythmMusicWidget.KEY_SONG_ID, "")
                putString(RhythmMusicWidget.KEY_SONG_TITLE, "Rhythm")
                putString(RhythmMusicWidget.KEY_ARTIST_NAME, "")
                putString(RhythmMusicWidget.KEY_ALBUM_NAME, "")
                remove(RhythmMusicWidget.KEY_ARTWORK_URI)
            }
            putBoolean(RhythmMusicWidget.KEY_IS_PLAYING, isPlaying)
            putBoolean(RhythmMusicWidget.KEY_HAS_PREVIOUS, hasPrevious)
            putBoolean(RhythmMusicWidget.KEY_HAS_NEXT, hasNext)
            putBoolean(RhythmMusicWidget.KEY_IS_FAVORITE, isFavorite)
            apply() // Use apply for async write
        }
        
        // Update Glance widget state directly using Glance state system
        scope.launch {
            try {
                // Preload bitmap in background if artworkUri exists
                val artworkUri = song?.artworkUri?.toString()
                if (!artworkUri.isNullOrBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            val imageLoader = ImageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(artworkUri)
                                .size(Size(150, 150))
                                .build()
                            val result = imageLoader.execute(request)
                            val loaded = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                            if (loaded != null) {
                                RhythmMusicWidget.cacheBitmap(artworkUri, loaded)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GlanceWidgetUpdater", "Error preloading bitmap in updater", e)
                    }
                }

                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RhythmMusicWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        if (song != null) {
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID)] = song.id
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] = song.title
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] = song.artist
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] = song.album
                            song.artworkUri?.let {
                                prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI)] = it.toString()
                            }
                        } else {
                            prefs.remove(stringPreferencesKey(RhythmMusicWidget.KEY_SONG_ID))
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_SONG_TITLE)] = "Rhythm"
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ARTIST_NAME)] = ""
                            prefs[stringPreferencesKey(RhythmMusicWidget.KEY_ALBUM_NAME)] = ""
                            prefs.remove(stringPreferencesKey(RhythmMusicWidget.KEY_ARTWORK_URI))
                        }
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_PLAYING)] = isPlaying
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_PREVIOUS)] = hasPrevious
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_HAS_NEXT)] = hasNext
                        prefs[booleanPreferencesKey(RhythmMusicWidget.KEY_IS_FAVORITE)] = isFavorite
                    }
                }
                
                // Update RhythmLyricsWidget as well
                val lyricGlanceIds = manager.getGlanceIds(RhythmLyricsWidget::class.java)
                lyricGlanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        if (song != null) {
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_SONG_TITLE)] = song.title
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTIST_NAME)] = song.artist
                            song.artworkUri?.let {
                                prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI)] = it.toString()
                            } ?: prefs.remove(stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI))
                        } else {
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_SONG_TITLE)] = "Rhythm"
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_ARTIST_NAME)] = ""
                            prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_LYRIC_LINES)] = ""
                            prefs[intPreferencesKey(RhythmLyricsWidget.KEY_ACTIVE_INDEX)] = -1
                            prefs.remove(stringPreferencesKey(RhythmLyricsWidget.KEY_ARTWORK_URI))
                        }
                        prefs[booleanPreferencesKey(RhythmLyricsWidget.KEY_IS_PLAYING)] = isPlaying
                    }
                }
                
                // Force update all widgets
                try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
                try { RhythmLyricsWidget().updateAll(context) } catch (_: Exception) {}

                // Update Quick Settings Tile
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    try {
                        android.service.quicksettings.TileService.requestListeningState(
                            context,
                            android.content.ComponentName(context, RhythmTileService::class.java)
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("GlanceWidgetUpdater", "Error updating tile listening state", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error updating widget", e)
            }
        }
    }
    
    /**
     * Update widget to show "No song playing" state
     */
    fun updateWidgetEmpty(context: Context) {
        updateWidget(
            context = context,
            song = null,
            isPlaying = false,
            hasPrevious = false,
            hasNext = false
        )
    }
    
    /**
     * Force update all widgets
     */
    fun forceUpdateAll(context: Context) {
        scope.launch {
            try {
                RhythmMusicWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error forcing widget update", e)
            }
            try {
                RhythmLyricsWidget().updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error forcing lyrics widget update", e)
            }
        }
        
        // Also trigger worker update
        scheduleWidgetUpdate(context, delayMillis = 0)
    }
    
    /**
     * Schedule a widget update using WorkManager for reliability
     */
    private fun scheduleWidgetUpdate(context: Context, delayMillis: Long = 0) {
        try {
            val updateRequest = OneTimeWorkRequestBuilder<RhythmWidgetWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueue(updateRequest)
        } catch (e: Exception) {
            android.util.Log.e("GlanceWidgetUpdater", "Error scheduling widget update", e)
        }
    }
    
    /**
     * Update lyrics widget with dynamic lyric lines and active index
     */
    fun updateLyrics(
        context: Context,
        lyricTexts: List<String>,
        activeIndex: Int
    ) {
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RhythmLyricsWidget::class.java)
                if (glanceIds.isEmpty()) return@launch
                
                val joined = lyricTexts.joinToString("##LINE##")
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[stringPreferencesKey(RhythmLyricsWidget.KEY_LYRIC_LINES)] = joined
                        prefs[intPreferencesKey(RhythmLyricsWidget.KEY_ACTIVE_INDEX)] = activeIndex
                    }
                }
                try { RhythmLyricsWidget().updateAll(context) } catch (_: Exception) {}
            } catch (e: Exception) {
                android.util.Log.e("GlanceWidgetUpdater", "Error updating lyrics widget", e)
            }
        }
    }

    /**
     * Update launcher app shortcuts dynamically
     */
    private fun updateAppShortcuts(context: Context, isPlaying: Boolean) {
        try {
            val shortcutManager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null) {
                val playPauseShortcut = android.content.pm.ShortcutInfo.Builder(context, "shortcut_play_pause")
                    .setShortLabel(if (isPlaying) "Pause" else "Play")
                    .setLongLabel(if (isPlaying) "Pause Music" else "Play Music")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, if (isPlaying) chromahub.rhythm.app.R.drawable.ic_pause_shortcut else chromahub.rhythm.app.R.drawable.ic_play_shortcut))
                    .setIntent(Intent(context, chromahub.rhythm.app.activities.MainActivity::class.java).apply {
                        action = "chromahub.rhythm.app.action.SHORTCUT_PLAY_PAUSE"
                    })
                    .build()

                val nextShortcut = android.content.pm.ShortcutInfo.Builder(context, "shortcut_next")
                    .setShortLabel("Next")
                    .setLongLabel("Next Track")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, chromahub.rhythm.app.R.drawable.ic_skip_next_shortcut))
                    .setIntent(Intent(context, chromahub.rhythm.app.activities.MainActivity::class.java).apply {
                        action = "chromahub.rhythm.app.action.SHORTCUT_SKIP_NEXT"
                    })
                    .build()

                val prevShortcut = android.content.pm.ShortcutInfo.Builder(context, "shortcut_previous")
                    .setShortLabel("Previous")
                    .setLongLabel("Previous Track")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, chromahub.rhythm.app.R.drawable.ic_skip_previous_shortcut))
                    .setIntent(Intent(context, chromahub.rhythm.app.activities.MainActivity::class.java).apply {
                        action = "chromahub.rhythm.app.action.SHORTCUT_SKIP_PREVIOUS"
                    })
                    .build()

                val openPlayerShortcut = android.content.pm.ShortcutInfo.Builder(context, "shortcut_open_player")
                    .setShortLabel("Open Player")
                    .setLongLabel("Open Music Player")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, chromahub.rhythm.app.R.drawable.ic_music_note_shortcut))
                    .setIntent(Intent(context, chromahub.rhythm.app.activities.MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        putExtra("OPEN_PLAYER", true)
                    })
                    .build()

                shortcutManager.dynamicShortcuts = listOf(playPauseShortcut, nextShortcut, prevShortcut, openPlayerShortcut)
            }
        } catch (e: Exception) {
            android.util.Log.e("GlanceWidgetUpdater", "Error updating dynamic shortcuts", e)
        }
    }
}
