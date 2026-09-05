package io.github.cluno1.sonorus.features.scores.data

import java.io.InputStream

internal object ScoreAssetReader {
    fun read(openAsset: () -> InputStream): ByteArray =
        openAsset().use(InputStream::readBytes).also { bytes ->
            require(bytes.isNotEmpty()) { "Score asset is empty" }
        }
}
