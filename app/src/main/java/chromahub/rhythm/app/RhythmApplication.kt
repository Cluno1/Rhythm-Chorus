package chromahub.rhythm.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.util.ANRWatchdog
import chromahub.rhythm.app.util.CrashReporter

/**
 * Custom Application class for Rhythm Music Player.
 * Handles initialization of:
 * - AppSettings
 * - CrashReporter
 * - NetworkClient
 * - LeakCanary (debug builds)
 * - ANR Watchdog (debug builds)
 */
class RhythmApplication : Application() {
    
    companion object {
        private const val TAG = "RhythmApplication"

        
        // Static reference to the application instance
        // Using a static reference is safe for Application class
        lateinit var instance: RhythmApplication
            private set
    }
    
    private var anrWatchdog: ANRWatchdog? = null
    
    override fun onCreate() {
        super.onCreate()
        
        instance = this
        
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "RhythmApplication onCreate")
        Log.d(TAG, "Build Type: ${BuildConfig.BUILD_TYPE}")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        Log.d(TAG, "═══════════════════════════════════════════════════")
        
        // Initialize AppSettings early (singleton, uses application context)
        val settings = AppSettings.getInstance(applicationContext)
        Log.d(TAG, "✓ AppSettings initialized")

        // Apply the user's theme preference to the system configuration so the
        // system splash screen and window background match the app theme
        // (light in light mode, dark in dark mode) instead of always being dark.
        val nightMode = if (settings.useSystemTheme.value) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else if (settings.darkMode.value) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        Log.d(TAG, "✓ Applied night mode: $nightMode")
        
        // Initialize CrashReporter
        CrashReporter.init(this)
        Log.d(TAG, "✓ CrashReporter initialized")
        
        // Initialize NetworkClient with AppSettings
        chromahub.rhythm.app.network.NetworkClient.initialize(
            AppSettings.getInstance(applicationContext)
        )
        Log.d(TAG, "✓ NetworkClient initialized")
        
        // Configure LeakCanary for debug builds
        if (BuildConfig.DEBUG) {
            configureLeakCanary()
            startANRWatchdog()
        }
        
        Log.d(TAG, "RhythmApplication initialization complete")
    }
    
    /**
     * Configure LeakCanary for optimal memory leak detection
     */
    private fun configureLeakCanary() {
        try {
            // LeakCanary 2.x auto-configures itself, but we can still apply debug-only tuning.
            // Reflection keeps main source free from debugImplementation class references.
            val debugConfigClass = Class.forName("chromahub.rhythm.app.debug.LeakCanaryDebugConfig")
            val applyMethod = debugConfigClass.getDeclaredMethod("applyKnownReferenceMatchers")
            applyMethod.invoke(null)

            Log.d(TAG, "✓ LeakCanary configured (auto-init + debug matcher tuning)")
        } catch (_: ClassNotFoundException) {
            Log.d(TAG, "✓ LeakCanary configured (auto-init)")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring LeakCanary", e)
        }
    }
    
    /**
     * Start ANR watchdog to monitor UI thread responsiveness
     */
    private fun startANRWatchdog() {
        try {
            // Start with 5 second timeout (standard ANR threshold)
            anrWatchdog = ANRWatchdog(timeoutMs = 5000).apply {
                start()
            }
            Log.d(TAG, "✓ ANR Watchdog started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ANR Watchdog", e)
        }
    }
    
    override fun onTerminate() {
        Log.d(TAG, "RhythmApplication onTerminate")
        
        // Stop ANR watchdog
        anrWatchdog?.stopWatching()
        anrWatchdog = null
        
        super.onTerminate()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "═══════════════════════════════════════════════════")
        Log.w(TAG, "LOW MEMORY WARNING!")
        Log.w(TAG, "═══════════════════════════════════════════════════")
        
        // Notify app components to clear caches
        // This could trigger cleanup in repositories, caches, etc.
        // You can add a broadcast or event here to notify components
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelName = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        
        Log.w(TAG, "onTrimMemory: $levelName")
        
        // Perform cleanup based on memory pressure level
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "Critical memory pressure - performing aggressive cleanup")
                // Trigger aggressive cleanup
                // You can broadcast an event here for repositories to clear caches
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                Log.w(TAG, "Moderate memory pressure - performing standard cleanup")
                // Trigger standard cleanup
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                Log.w(TAG, "App backgrounded - clearing caches")
                // Light cleanup when the app moves to the background
            }
        }
    }
}
