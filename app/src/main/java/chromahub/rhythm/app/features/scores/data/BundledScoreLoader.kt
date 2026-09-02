@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package chromahub.rhythm.app.features.scores.data

import android.content.Context
import alphaTab.PlayerMode
import alphaTab.Settings
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.importer.ScoreLoader
import alphaTab.model.Score
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BundledScoreLoader(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val assets = context.applicationContext.assets
    private val revisionStore = ScoreRevisionStore(context)

    suspend fun loadAll(): Map<BundledScoreVariant, LoadedScore> = withContext(ioDispatcher) {
        BundledScoreVariant.entries.associateWith { variant ->
            val bytes = revisionStore.loadWorkingCopy(variant) ?: ScoreAssetReader.read {
                assets.open(variant.assetName)
            }
            loadVariantNow(variant, bytes)
        }
    }

    suspend fun loadVariant(
        variant: BundledScoreVariant,
        canonicalMusicXml: ByteArray
    ): LoadedScore = withContext(ioDispatcher) {
        loadVariantNow(variant, canonicalMusicXml)
    }

    suspend fun saveWorkingCopy(
        variant: BundledScoreVariant,
        canonicalMusicXml: ByteArray
    ) = withContext(ioDispatcher) {
        // Parsing both projections before writing prevents a malformed draft replacing the last
        // known-good working copy.
        loadVariantNow(variant, canonicalMusicXml)
        revisionStore.saveWorkingCopy(variant, canonicalMusicXml)
    }

    suspend fun loadSoundFont(): ByteArray = withContext(ioDispatcher) {
        ScoreAssetReader.read {
            assets.open(SOUND_FONT_ASSET)
        }
    }

    private fun loadVariantNow(
        variant: BundledScoreVariant,
        canonicalMusicXml: ByteArray
    ): LoadedScore {
        val displayBytes = when (variant) {
            BundledScoreVariant.OCR -> canonicalMusicXml
            BundledScoreVariant.MIDI ->
                ScoreDisplaySanitizer.keepFirstVisibleMetronome(canonicalMusicXml)
        }
        return LoadedScore(
            displayScore = ScoreLoader.loadScoreFromBytes(
                Uint8Array(displayBytes.toUByteArray()),
                scoreSettings()
            ),
            playbackScore = ScoreLoader.loadScoreFromBytes(
                Uint8Array(canonicalMusicXml.toUByteArray()),
                scoreSettings()
            ),
            canonicalMusicXml = canonicalMusicXml,
            displayMusicXml = displayBytes
        )
    }

    private fun scoreSettings() = Settings().apply {
        player.playerMode = PlayerMode.Disabled
        player.enableCursor = false
        player.enableUserInteraction = false
    }

    private companion object {
        const val SOUND_FONT_ASSET = "sonivox.sf2"
    }
}

internal enum class BundledScoreVariant(val assetName: String) {
    OCR("gmusic_321_rev1_ocr.musicxml"),
    MIDI("gmusic_321_midi_20210702.musicxml")
}

internal class LoadedScore(
    val displayScore: Score,
    val playbackScore: Score,
    val canonicalMusicXml: ByteArray,
    private val displayMusicXml: ByteArray
) {
    private val mergedDisplayScores = mutableMapOf<Int, Score>()

    suspend fun loadMergedDisplayScore(selectedTrackIndexes: Set<Int>): Score =
        withContext(Dispatchers.Default) {
            val selectionMask = selectedTrackIndexes.fold(0) { mask, index ->
                mask or (1 shl index)
            }
            synchronized(mergedDisplayScores) {
                mergedDisplayScores[selectionMask]
            } ?: ScoreLoader.loadScoreFromBytes(
                Uint8Array(
                    ChoirScoreDisplayProjector.project(
                        source = displayMusicXml,
                        selectedTrackIndexes = selectedTrackIndexes
                    ).toUByteArray()
                ),
                Settings().apply {
                    player.playerMode = PlayerMode.Disabled
                    player.enableCursor = false
                    player.enableUserInteraction = false
                }
            ).also { score ->
                synchronized(mergedDisplayScores) {
                    mergedDisplayScores[selectionMask] = score
                }
            }
        }
}
