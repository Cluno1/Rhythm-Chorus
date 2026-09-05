/* SPDX-License-Identifier: GPL-3.0-or-later */
package chromahub.rhythm.app.shared.presentation.components.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import chromahub.rhythm.app.R

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
    else -> source
}
