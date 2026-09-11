/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.shared.presentation.components.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.shared.data.model.LyricsContribution
import java.util.Locale

@Composable
internal fun localizedLyricsSourceLabel(source: String): String = when {
    source == "DEVICE_EMBEDDED" -> stringResource(R.string.lyrics_source_device_embedded)
    source == "DEVICE_SIBLING" -> stringResource(R.string.lyrics_source_device_sibling)
    source == "DEVICE_SAF_SIBLING" -> stringResource(R.string.lyrics_source_device_saf_sibling)
    source.startsWith("DEVICE_LRCLIB|") || source.startsWith("DEVICE_LRCLIB_SELECTED|") -> {
        val parts = source.split('|')
        stringResource(
            if (source.startsWith("DEVICE_LRCLIB_SELECTED|")) R.string.lyrics_source_lrclib_selected_match else R.string.lyrics_source_lrclib_match,
            parts.getOrNull(1).orEmpty(),
            parts.getOrNull(2).orEmpty(),
            parts.getOrNull(3).orEmpty()
        )
    }
    managedLibraryLanguage(source) != null -> {
        val language = managedLibraryLanguage(source).orEmpty()
        if (language.equals("und", true) || language.isBlank()) {
            stringResource(R.string.lyrics_source_music_library)
        } else {
            val languageName = Locale.forLanguageTag(language)
                .getDisplayName(Locale.getDefault())
                .ifBlank { language }
            stringResource(R.string.lyrics_source_music_library_language, languageName)
        }
    }
    else -> source
}

internal fun managedLibraryLanguage(source: String): String? {
    if (source.startsWith("RHYTHM_LIBRARY|")) return source.substringAfter('|')
    if (source.equals("Catalog", true)) return "und"
    val legacy = Regex("^Catalog\\s*(?:[·:|]\\s*)?(.+)$", RegexOption.IGNORE_CASE)
        .matchEntire(source.trim()) ?: return null
    return legacy.groupValues[1].trim().ifBlank { "und" }
}

@Composable
internal fun LyricsAttributionFooter(
    lyricsSource: String?,
    contributions: List<LyricsContribution>,
    color: Color,
    textStyle: TextStyle,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
) {
    if (lyricsSource.isNullOrBlank() && contributions.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        contributions.forEach { contribution ->
            Text(
                text = stringResource(
                    if (contribution.role.equals("uploader", true)) {
                        R.string.lyrics_contribution_uploader_display
                    } else {
                        R.string.lyrics_contribution_modifier_display
                    },
                    contribution.name,
                    contribution.updatedAt,
                ),
                style = textStyle,
                color = color,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!lyricsSource.isNullOrBlank()) {
            Text(
                text = stringResource(
                    R.string.lyrics_source_display,
                    localizedLyricsSourceLabel(lyricsSource),
                ),
                style = textStyle,
                color = color,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
