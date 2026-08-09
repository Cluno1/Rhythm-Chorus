package chromahub.rhythm.app.debug

import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import kotlin.jvm.JvmStatic

/**
 * Debug-only LeakCanary tuning, loaded reflectively by
 * [chromahub.rhythm.app.RhythmApplication.configureLeakCanary].
 *
 * Suppresses known false positives so only real app-side leaks get reported.
 */
object LeakCanaryDebugConfig {

    @JvmStatic
    fun applyKnownReferenceMatchers() {
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = AndroidReferenceMatchers.appDefaults +
                // Framework quirk: on Android 12+ (and especially newer SDKs like 36)
                // TileService keeps its anonymous IQSTileService$Stub binder (TileService$2)
                // alive after Service#onDestroy(), which holds the service via `this$0`.
                // Nothing app-side retains the tile service, so treat it as a known
                // framework leak instead of a real one.
                AndroidReferenceMatchers.instanceFieldLeak(
                    className = "android.service.quicksettings.TileService\$2",
                    fieldName = "this\$0",
                    description = "TileService retained by framework TileService\$2 binder stub after onDestroy (system quirk, not an app leak)."
                )
        )
    }
}
