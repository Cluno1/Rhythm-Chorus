package io.github.cluno1.sonorus.core

import io.github.cluno1.sonorus.BuildConfig

/** Product-wide capability boundary for the customized Catalog client. */
object ProductCapabilities {
    val catalogOnly: Boolean
        get() = BuildConfig.CATALOG_ONLY

    val thirdPartyMusicServices: Boolean
        get() = !catalogOnly

    val firstPartyUpdates: Boolean
        get() = BuildConfig.FIRST_PARTY_UPDATES

    val inAppUpdates: Boolean
        get() = firstPartyUpdates

    val devicePublicMetadata: Boolean
        get() = BuildConfig.DEVICE_PUBLIC_METADATA

    val metadataNetworkClient: Boolean
        get() = shouldInitializeMetadataNetworkClient(thirdPartyMusicServices, devicePublicMetadata)

    internal fun shouldInitializeMetadataNetworkClient(
        thirdPartyMusicServices: Boolean,
        devicePublicMetadata: Boolean
    ): Boolean = thirdPartyMusicServices || devicePublicMetadata
}
