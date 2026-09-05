package chromahub.rhythm.app.core

import chromahub.rhythm.app.BuildConfig

/** Product-wide capability boundary for the customized Catalog client. */
object ProductCapabilities {
    val catalogOnly: Boolean
        get() = BuildConfig.CATALOG_ONLY

    val thirdPartyMusicServices: Boolean
        get() = !catalogOnly

    val inAppUpdates: Boolean
        get() = !catalogOnly
}
