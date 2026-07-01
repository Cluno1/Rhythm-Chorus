/*
 *     Copyright (C) 2025 nift4
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package chromahub.rhythm.app.util

import androidx.annotation.VisibleForTesting
import androidx.media3.common.Metadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.google.gson.Gson
import chromahub.rhythm.app.util.SemanticLyrics.SyncedLyrics
import java.io.File
import java.nio.charset.Charset

object LrcUtils {

    private const val TAG = "LrcUtils"

    enum class LyricFormat {
        LRC,
        TTML,
        SRT
    }

    data class LrcParserOptions(
        val trim: Boolean,
        val multiLine: Boolean,
        val errorText: String?,
        val autoWordSync: Boolean = false
    )

    @VisibleForTesting
    fun parseLyrics(
        lyrics: String,
        audioMimeType: String?,
        parserOptions: LrcParserOptions,
        format: LyricFormat?
    ): SemanticLyrics? {
        for (i in listOf({
            if (format == null || format == LyricFormat.TTML)
                parseTtml(audioMimeType, lyrics)
            else null
        }, {
            if (format == null || format == LyricFormat.SRT)
                parseSrt(lyrics, parserOptions.trim)
            else null
        }, {
            if (format == null || format == LyricFormat.LRC)
                parseLrc(lyrics, parserOptions.trim, parserOptions.multiLine)
            else null
        })) {
            return try {
                var ret = i() ?: continue
                if (parserOptions.autoWordSync && ret is SyncedLyrics) {
                    applyAutoWordSync(ret)
                    splitBidirectionalWords(ret)
                }
                if (ret is SyncedLyrics && Flags.HIDE_SAME_TRANSLATIONS)
                    ret = ret.copy(text = ret.text.filterIndexed { i, it ->
                        !it.isTranslated || it.text != ret.text.subList(0, i)
                            .last { !it.isTranslated }.text
                    })
                ret
            } catch (e: Exception) {
                if (parserOptions.errorText == null)
                    throw e
                Log.e(TAG, Log.getThrowableString(e)!!)
                Log.e(TAG, "The lyrics are:\n$lyrics")
                SemanticLyrics.UnsyncedLyrics(
                    listOf(
                        parserOptions.errorText + "\n\n${
                            Log.getThrowableString(
                                e
                            )!!
                        }\n\n$lyrics" to null
                    )
                )
            }
        }
        return null
    }

    // returns best lyrics first
    fun extractAndParseLyrics(
        sampleRate: Int,
        audioMimeType: String?,
        metadata: Metadata,
        parserOptions: LrcParserOptions
    ): List<SemanticLyrics> {
        val out = mutableListOf<SemanticLyrics>()
        for (i in 0..<metadata.length()) {
            val meta = metadata.get(i)
            if (meta is BinaryFrame && (meta.id == "SYLT" || meta.id == "SLT")) {
                val syltData = UsltFrameDecoder.decodeSylt(sampleRate, ParsableByteArray(meta.data))
                if (syltData != null) {
                    if (syltData.contentType == 1 || syltData.contentType == 2) {
                        out.add(syltData.toSyncedLyrics(parserOptions.trim))
                    }
                    continue
                }
            }
            val plainTextData =
                if (meta is VorbisComment && meta.key == "LYRICS") // vorbis comments
                    meta.value
                else if (meta is BinaryFrame && (meta.id == "USLT" || meta.id == "ULT"
                            || meta.id == "SLT" /* out-of-spec */
                            || meta.id == "SYLT" /* out-of-spec */)
                ) // ID3
                    UsltFrameDecoder.decode(ParsableByteArray(meta.data))?.text
                else if (meta is TextInformationFrame && meta.id == "USLT") // mp4
                    meta.values.joinToString("\n")
                else null
            if (plainTextData != null) {
                parseLyrics(plainTextData, audioMimeType, parserOptions, null)?.let {
                    out.add(it)
                    continue
                }
            }
        }
        out.sortBy {
            if (it !is SyncedLyrics) {
                return@sortBy -10
            }
            val hasWords = it.text.find { it.words != null } != null
            val hasTl = it.text.find { it.isTranslated } != null
            if (hasWords) 10 else 0 + if (hasTl) 1 else 0
        }
        return out
    }

    fun loadAndParseLyricsFile(
        musicFile: File?,
        audioMimeType: String?,
        parserOptions: LrcParserOptions
    ): SemanticLyrics? {
        return loadTextFile(
            musicFile?.let { File(it.parentFile, it.nameWithoutExtension + ".ttml") },
            parserOptions.errorText
        )?.let { parseLyrics(it, audioMimeType, parserOptions, LyricFormat.TTML) }
            ?: loadTextFile(
                musicFile?.let { File(it.parentFile, it.nameWithoutExtension + ".srt") },
                parserOptions.errorText
            )?.let { parseLyrics(it, audioMimeType, parserOptions, LyricFormat.SRT) }
            ?: loadTextFile(
                musicFile?.let { File(it.parentFile, it.nameWithoutExtension + ".lrc") },
                parserOptions.errorText
            )?.let { parseLyrics(it, audioMimeType, parserOptions, LyricFormat.LRC) }
    }

    private fun loadTextFile(lrcFile: File?, errorText: String?): String? {
        return try {
            if (lrcFile?.exists() == true)
                lrcFile.readBytes().toString(Charset.defaultCharset())
            else null
        } catch (e: Exception) {
            Log.e(TAG, Log.getThrowableString(e)!!)
            return errorText
        }
    }

    fun isInstrumentalLine(text: String): Boolean {
        val clean = text.trim().lowercase().removeSurrounding("[", "]").removeSurrounding("(", ")").trim()
        return clean == "instrumental" ||
               clean == "no vocals" ||
               clean == "music" ||
               clean == "instrumental break" ||
               clean == "instrumental track" ||
               clean == "pure instrumental" ||
               clean == "no lyrics" ||
               clean == "♪" ||
               clean == "♫" ||
               clean == "🎶"
    }

    fun convertSemanticLyricsToWordByWord(syncedLyrics: SyncedLyrics): String? {
        val rhythmWordLines = syncedLyrics.text.mapNotNull { line ->
            // Skip translated/romanization lines — only process primary lines
            if (line.isTranslated) return@mapNotNull null

            // Skip instrumental / gap lines — leave them as timing gaps
            if (isInstrumentalLine(line.text)) return@mapNotNull null

            val words = line.words
            val wordMaps: List<Map<String, Any>> = if (words != null) {
                // Real word-by-word data from the parser
                words.mapNotNull { word ->
                    val text = try {
                        line.text.substring(word.charRange)
                    } catch (e: Exception) { "" }
                    if (text.isBlank() || isInstrumentalLine(text)) return@mapNotNull null
                    mapOf(
                        "text" to text,
                        "part" to false,
                        "timestamp" to word.begin.toLong(),
                        "endtime" to (word.endInclusive ?: word.begin).toLong()
                    )
                }
            } else {
                // auto-sync skipped this line (e.g. single-word line or no duration).
                // Synthesise a single word entry spanning the whole line so it still renders.
                val trimmed = line.text.trim()
                if (trimmed.isBlank()) return@mapNotNull null
                listOf(
                    mapOf(
                        "text" to trimmed,
                        "part" to false,
                        "timestamp" to line.start.toLong(),
                        "endtime" to if (line.end > line.start) (line.end - 1uL).toLong()
                                     else line.start.toLong()
                    )
                )
            }

            if (wordMaps.isEmpty()) return@mapNotNull null

            // Use the actual last word's endtime as line endtime so WordByWordLyricsView's
            // gap detection measures singing-end → next-line-start, not implicit-next-start.
            val lineEndtime = wordMaps.maxOfOrNull {
                (it["endtime"] as? Long) ?: 0L
            } ?: line.end.toLong()

            mutableMapOf<String, Any>(
                "text" to wordMaps,
                "background" to false,
                "timestamp" to line.start.toLong(),
                "endtime" to lineEndtime
            )
        }
        return if (rhythmWordLines.isNotEmpty()) Gson().toJson(rhythmWordLines) else null
    }
}
