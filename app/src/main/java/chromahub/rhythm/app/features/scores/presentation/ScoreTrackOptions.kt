package chromahub.rhythm.app.features.scores.presentation

internal enum class ScoreStaffMode {
    ALL_STAVES,
    SELECTED_PARTS
}

internal enum class ScoreNotationLayout {
    SEPARATE_PARTS,
    MERGED_STAVES
}

internal enum class ScorePartColorMode {
    DEFAULT,
    ENHANCED
}

internal data class ScoreTrackOption(
    val index: Int,
    val label: String
)

internal fun buildScoreTrackOptions(trackNames: List<String>): List<ScoreTrackOption> {
    val useSatbLabels = trackNames.size == SATB_LABELS.size && trackNames.all {
        it.isGenericTrackName()
    }

    return trackNames.mapIndexed { index, rawName ->
        val name = rawName.trim()
        val label = when {
            useSatbLabels -> SATB_LABELS[index]
            name.isNotEmpty() && !name.isGenericTrackName() -> name
            else -> (index + 1).toString()
        }
        ScoreTrackOption(index = index, label = label)
    }
}

internal fun toggleScoreTrackSelectionMask(
    selectedMask: Int,
    trackIndex: Int,
    trackCount: Int
): Int {
    require(trackIndex in 0 until MAX_MASK_TRACKS)
    require(trackCount in 1..MAX_MASK_TRACKS)

    val normalizedMask = normalizeScoreTrackSelectionMask(selectedMask, trackCount)
    val updatedMask = normalizedMask xor (1 shl trackIndex)
    return if (updatedMask == 0) normalizedMask else updatedMask
}

internal fun normalizeScoreTrackSelectionMask(selectedMask: Int, trackCount: Int): Int {
    require(trackCount in 1..MAX_MASK_TRACKS)

    val availableTracksMask = if (trackCount == MAX_MASK_TRACKS) {
        Int.MAX_VALUE
    } else {
        (1 shl trackCount) - 1
    }
    return (selectedMask and availableTracksMask).takeIf { it != 0 } ?: 1
}

private fun String.isGenericTrackName(): Boolean {
    val normalized = replace('\u00A0', ' ').trim()
    return normalized.isEmpty() ||
        normalized.contains("SmartMusic SoftSynth", ignoreCase = true) ||
        normalized.equals("Piano", ignoreCase = true) ||
        normalized.equals("Pno", ignoreCase = true) ||
        STAFF_NAME.containsMatchIn(normalized)
}

private val STAFF_NAME = Regex("""\[?Staff\s+\d+]?""", RegexOption.IGNORE_CASE)
private val SATB_LABELS = listOf("S", "A", "T", "B")
private const val MAX_MASK_TRACKS = 31
