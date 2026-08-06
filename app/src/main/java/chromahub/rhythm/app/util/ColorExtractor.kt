package chromahub.rhythm.app.util

import android.annotation.SuppressLint

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.MathUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Data class to store extracted colors from album artwork
 * Stores full Material 3 color scheme for both light and dark themes
 */
data class ExtractedColors(
    val seedColor: Int = 0,
    // Light theme colors
    // Primary colors
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,

    // Secondary colors
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,

    // Tertiary colors
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,

    // Dark theme colors
    // Primary colors
    val darkPrimary: Int,
    val darkOnPrimary: Int,
    val darkPrimaryContainer: Int,
    val darkOnPrimaryContainer: Int,

    // Secondary colors
    val darkSecondary: Int,
    val darkOnSecondary: Int,
    val darkSecondaryContainer: Int,
    val darkOnSecondaryContainer: Int,

    // Tertiary colors
    val darkTertiary: Int,
    val darkOnTertiary: Int,
    val darkTertiaryContainer: Int,
    val darkOnTertiaryContainer: Int,

    // Surface colors (shared or from dark scheme)
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int
)

/**
 * Color scoring configuration for album art extraction
 */
data class ColorScoringConfig(
    val targetChroma: Double = 30.0,
    val weightProportion: Double = 0.7,
    val weightChromaAbove: Double = 0.3,
    val weightChromaBelow: Double = 0.1,
    val cutoffChroma: Double = 5.0,
    val cutoffExcitedProportion: Double = 0.01,
    val maxColorCount: Int = 4,
    val maxHueDifference: Int = 90,
    val minHueDifference: Int = 15
)

/**
 * Color extraction configuration
 */
data class ColorExtractionConfig(
    val downscaleMaxDimension: Int = 128,
    val quantizerMaxColors: Int = 128,
    val scoring: ColorScoringConfig = ColorScoringConfig()
)

/**
 * Scored HCT color for ranking
 */
@SuppressLint("RestrictedApi")
private data class ScoredHct(
    val hct: Hct,
    val score: Double
)

/**
 * Constants for color extraction
 */
private const val GRAYSCALE_CHROMA_THRESHOLD = 12.0
private const val NEUTRAL_PIXEL_CHROMA_THRESHOLD = 8.0
private const val HIGH_CHROMA_THRESHOLD = 18.0
private const val REQUIRED_NEUTRAL_POPULATION = 0.92
private const val MAX_HIGH_CHROMA_POPULATION = 0.03
private const val MAX_WEIGHTED_CHROMA_FOR_NEUTRAL = 9.0
private const val MAX_GRAYSCALE_CHANNEL_DELTA = 10

/**
 * Extracted color cache
 */
private val extractedColorCache = LruCache<Int, Color>(32)

/**
 * Utility object for extracting color palettes from album artwork using Rhythm's palette algorithm
 */
@SuppressLint("RestrictedApi")
object ColorExtractor {

    private const val TAG = "ColorExtractor"
    private val gson = Gson()

    /**
    * Extract a Material 3 color palette from album artwork bitmap using Rhythm's palette algorithm
     * Returns null if extraction fails or bitmap is null
     */
    suspend fun extractColorsFromBitmap(
        bitmap: Bitmap?,
        config: ColorExtractionConfig = ColorExtractionConfig()
    ): ExtractedColors? = withContext(Dispatchers.Default) {
        try {
            if (bitmap == null) {
                android.util.Log.w(TAG, "Bitmap is null, cannot extract colors")
                return@withContext null
            }

            val cacheKey = 31 * bitmap.hashCode() + config.hashCode()
            extractedColorCache.get(cacheKey)?.let { seedColor ->
                // Use cached seed color to generate scheme
                return@withContext generateColorSchemeFromSeed(seedColor)
            }

            val workingBitmap = resizeForExtraction(bitmap, config.downscaleMaxDimension)

            val seedColor = runCatching {
                val pixels = IntArray(workingBitmap.width * workingBitmap.height)
                workingBitmap.getPixels(
                    pixels,
                    0,
                    workingBitmap.width,
                    0,
                    0,
                    workingBitmap.width,
                    workingBitmap.height
                )

                val fallbackArgb = averageColorArgb(pixels)
                val quantized = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)

                val rankedSeeds = scoreQuantizedColors(
                    colorsToPopulation = quantized,
                    scoring = config.scoring,
                    fallbackColorArgb = fallbackArgb
                )
                Color(rankedSeeds.firstOrNull() ?: fallbackArgb)
            }.getOrElse {
                android.util.Log.e(TAG, "Failed to extract seed color", it)
                Color(0xFF6750A4) // Material default purple
            }

            // Cache the seed color
            extractedColorCache.put(cacheKey, seedColor)

            // Recycle bitmap if it's not the original
            if (workingBitmap !== bitmap) {
                workingBitmap.recycle()
            }

            // Generate full color scheme from seed
            generateColorSchemeFromSeed(seedColor)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to extract colors from bitmap", e)
            null
        }
    }

    /**
     * Generate a complete Material 3 color scheme from a seed color
     */
    private fun generateColorSchemeFromSeed(seedColor: Color): ExtractedColors? {
        return runCatching {
            val seedArgb = seedColor.toArgb()
            val sourceHct = Hct.fromInt(seedArgb)
            val shouldForceNeutral = shouldUseNeutralArtworkScheme(seedArgb, sourceHct)

            // Choose content scheme for colorful artwork to preserve exact hue and chroma, else tonal spot
            val schemeType = when {
                sourceHct.chroma > 12.0 -> "CONTENT"      // Colorful = CONTENT (keeps exact hue/chroma)
                else -> "TONAL_SPOT"                      // Neutral = TONAL_SPOT
            }

            val lightScheme = createDynamicScheme(sourceHct, schemeType, false)
            val darkScheme = createDynamicScheme(sourceHct, schemeType, true)

            if (shouldForceNeutral) {
                // Convert to grayscale for neutral artwork
                val grayscaleLight = lightScheme.toGrayscaleScheme()
                val grayscaleDark = darkScheme.toGrayscaleScheme()
                ExtractedColors(
                    seedColor = seedArgb,
                    primary = grayscaleLight.primary.toArgb(),
                    onPrimary = grayscaleLight.onPrimary.toArgb(),
                    primaryContainer = grayscaleLight.primaryContainer.toArgb(),
                    onPrimaryContainer = grayscaleLight.onPrimaryContainer.toArgb(),
                    secondary = grayscaleLight.secondary.toArgb(),
                    onSecondary = grayscaleLight.onSecondary.toArgb(),
                    secondaryContainer = grayscaleLight.secondaryContainer.toArgb(),
                    onSecondaryContainer = grayscaleLight.onSecondaryContainer.toArgb(),
                    tertiary = grayscaleLight.tertiary.toArgb(),
                    onTertiary = grayscaleLight.onTertiary.toArgb(),
                    tertiaryContainer = grayscaleLight.tertiaryContainer.toArgb(),
                    onTertiaryContainer = grayscaleLight.onTertiaryContainer.toArgb(),
                    darkPrimary = grayscaleDark.primary.toArgb(),
                    darkOnPrimary = grayscaleDark.onPrimary.toArgb(),
                    darkPrimaryContainer = grayscaleDark.primaryContainer.toArgb(),
                    darkOnPrimaryContainer = grayscaleDark.onPrimaryContainer.toArgb(),
                    darkSecondary = grayscaleDark.secondary.toArgb(),
                    darkOnSecondary = grayscaleDark.onSecondary.toArgb(),
                    darkSecondaryContainer = grayscaleDark.secondaryContainer.toArgb(),
                    darkOnSecondaryContainer = grayscaleDark.onSecondaryContainer.toArgb(),
                    darkTertiary = grayscaleDark.tertiary.toArgb(),
                    darkOnTertiary = grayscaleDark.onTertiary.toArgb(),
                    darkTertiaryContainer = grayscaleDark.tertiaryContainer.toArgb(),
                    darkOnTertiaryContainer = grayscaleDark.onTertiaryContainer.toArgb(),
                    surface = grayscaleDark.surface.toArgb(), // Use dark surface for better contrast
                    onSurface = grayscaleDark.onSurface.toArgb(),
                    surfaceVariant = grayscaleDark.surfaceVariant.toArgb(),
                    onSurfaceVariant = grayscaleDark.onSurfaceVariant.toArgb()
                )
            } else {
                ExtractedColors(
                    seedColor = seedArgb,
                    primary = lightScheme.primary.toArgb(),
                    onPrimary = lightScheme.onPrimary.toArgb(),
                    primaryContainer = lightScheme.primaryContainer.toArgb(),
                    onPrimaryContainer = lightScheme.onPrimaryContainer.toArgb(),
                    secondary = lightScheme.secondary.toArgb(),
                    onSecondary = lightScheme.onSecondary.toArgb(),
                    secondaryContainer = lightScheme.secondaryContainer.toArgb(),
                    onSecondaryContainer = lightScheme.onSecondaryContainer.toArgb(),
                    tertiary = lightScheme.tertiary.toArgb(),
                    onTertiary = lightScheme.onTertiary.toArgb(),
                    tertiaryContainer = lightScheme.tertiaryContainer.toArgb(),
                    onTertiaryContainer = lightScheme.onTertiaryContainer.toArgb(),
                    darkPrimary = darkScheme.primary.toArgb(),
                    darkOnPrimary = darkScheme.onPrimary.toArgb(),
                    darkPrimaryContainer = darkScheme.primaryContainer.toArgb(),
                    darkOnPrimaryContainer = darkScheme.onPrimaryContainer.toArgb(),
                    darkSecondary = darkScheme.secondary.toArgb(),
                    darkOnSecondary = darkScheme.onSecondary.toArgb(),
                    darkSecondaryContainer = darkScheme.secondaryContainer.toArgb(),
                    darkOnSecondaryContainer = darkScheme.onSecondaryContainer.toArgb(),
                    darkTertiary = darkScheme.tertiary.toArgb(),
                    darkOnTertiary = darkScheme.onTertiary.toArgb(),
                    darkTertiaryContainer = darkScheme.tertiaryContainer.toArgb(),
                    darkOnTertiaryContainer = darkScheme.onTertiaryContainer.toArgb(),
                    surface = darkScheme.surface.toArgb(),
                    onSurface = darkScheme.onSurface.toArgb(),
                    surfaceVariant = darkScheme.surfaceVariant.toArgb(),
                    onSurfaceVariant = darkScheme.onSurfaceVariant.toArgb()
                )
            }
        }.getOrElse {
            android.util.Log.e(TAG, "Failed to generate color scheme", it)
            null
        }
    }

    /**
     * Resize bitmap for efficient color extraction
     */
    private fun resizeForExtraction(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (maxDimension <= 0) return bitmap
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return bitmap.scale(newWidth, newHeight, true)
    }

    /**
    * Score quantized colors based on Rhythm's palette algorithm
     */
    private fun scoreQuantizedColors(
        colorsToPopulation: Map<Int, Int>,
        scoring: ColorScoringConfig,
        fallbackColorArgb: Int
    ): List<Int> {
        if (colorsToPopulation.isEmpty()) return listOf(fallbackColorArgb)

        val colorsHct = ArrayList<Hct>(colorsToPopulation.size)
        val huePopulation = IntArray(360)
        var populationSum = 0.0

        // Process colors and build hue population map
        for ((argb, population) in colorsToPopulation) {
            if (population <= 0) continue
            val hct = Hct.fromInt(argb)
            colorsHct.add(hct)
            val hue = MathUtils.sanitizeDegreesInt(hct.hue.roundToInt())
            huePopulation[hue] += population
            populationSum += population.toDouble()
        }

        if (populationSum <= 0.0) return listOf(fallbackColorArgb)

        // Calculate excited proportions for each hue
        val hueExcitedProportions = DoubleArray(360)
        for (hue in 0 until 360) {
            val proportion = huePopulation[hue] / populationSum
            for (neighbor in hue - 14..hue + 15) {
                val wrappedHue = MathUtils.sanitizeDegreesInt(neighbor)
                hueExcitedProportions[wrappedHue] += proportion
            }
        }

        // Score colors based on proportion and chroma
        val scoredColors = ArrayList<ScoredHct>(colorsHct.size)
        for (hct in colorsHct) {
            val hue = MathUtils.sanitizeDegreesInt(hct.hue.roundToInt())
            val excitedProportion = hueExcitedProportions[hue]

            if (hct.chroma < scoring.cutoffChroma || excitedProportion <= scoring.cutoffExcitedProportion) {
                continue
            }

            val proportionScore = excitedProportion * 100.0 * scoring.weightProportion
            val chromaWeight = if (hct.chroma < scoring.targetChroma) scoring.weightChromaBelow else scoring.weightChromaAbove
            val chromaScore = (hct.chroma - scoring.targetChroma) * chromaWeight
            scoredColors.add(ScoredHct(hct, proportionScore + chromaScore))
        }

        if (scoredColors.isEmpty()) return listOf(fallbackColorArgb)

        scoredColors.sortByDescending { it.score }

        // Select diverse colors with minimum hue differences
        val chosen = mutableListOf<Hct>()
        val maxHueDifference = scoring.maxHueDifference.coerceAtLeast(scoring.minHueDifference)
        val minHueDifference = scoring.minHueDifference.coerceAtLeast(1)
        val desiredColorCount = scoring.maxColorCount.coerceAtLeast(1)

        for (differenceDegrees in maxHueDifference downTo minHueDifference) {
            chosen.clear()
            for (candidate in scoredColors) {
                val isDuplicateHue = chosen.any {
                    MathUtils.differenceDegrees(candidate.hct.hue, it.hue) < differenceDegrees.toDouble()
                }
                if (!isDuplicateHue) {
                    chosen.add(candidate.hct)
                }
                if (chosen.size >= desiredColorCount) break
            }
            if (chosen.size >= desiredColorCount) break
        }

        return if (chosen.isEmpty()) listOf(fallbackColorArgb) else chosen.map { it.toInt() }
    }

    /**
     * Create dynamic color scheme using Material Design utilities
     */
    fun createDynamicScheme(
        sourceHct: Hct,
        paletteStyle: String,
        isDark: Boolean
    ): androidx.compose.material3.ColorScheme {
        val scheme = when (paletteStyle) {
            "TONAL_SPOT" -> SchemeTonalSpot(sourceHct, isDark, 0.0)
            "VIBRANT" -> SchemeVibrant(sourceHct, isDark, 0.0)
            "EXPRESSIVE" -> SchemeExpressive(sourceHct, isDark, 0.0)
            "FRUIT_SALAD" -> SchemeFruitSalad(sourceHct, isDark, 0.0)
            "CONTENT" -> SchemeContent(sourceHct, isDark, 0.0)
            else -> SchemeTonalSpot(sourceHct, isDark, 0.0)
        }

        return androidx.compose.material3.ColorScheme(
            primary = Color(scheme.primary),
            onPrimary = Color(scheme.onPrimary),
            primaryContainer = Color(scheme.primaryContainer),
            onPrimaryContainer = Color(scheme.onPrimaryContainer),
            inversePrimary = Color(scheme.inversePrimary),
            secondary = Color(scheme.secondary),
            onSecondary = Color(scheme.onSecondary),
            secondaryContainer = Color(scheme.secondaryContainer),
            onSecondaryContainer = Color(scheme.onSecondaryContainer),
            tertiary = Color(scheme.tertiary),
            onTertiary = Color(scheme.onTertiary),
            tertiaryContainer = Color(scheme.tertiaryContainer),
            onTertiaryContainer = Color(scheme.onTertiaryContainer),
            background = Color(scheme.background),
            onBackground = Color(scheme.onBackground),
            surface = Color(scheme.surface),
            onSurface = Color(scheme.onSurface),
            surfaceVariant = Color(scheme.surfaceVariant),
            onSurfaceVariant = Color(scheme.onSurfaceVariant),
            surfaceTint = Color(scheme.primary), // Use primary color as surface tint
            inverseSurface = Color(scheme.inverseSurface),
            inverseOnSurface = Color(scheme.inverseOnSurface),
            error = Color(scheme.error),
            onError = Color(scheme.onError),
            errorContainer = Color(scheme.errorContainer),
            onErrorContainer = Color(scheme.onErrorContainer),
            outline = Color(scheme.outline),
            outlineVariant = Color(scheme.outlineVariant),
            scrim = Color(scheme.scrim),
            surfaceBright = Color(scheme.surfaceBright),
            surfaceDim = Color(scheme.surfaceDim),
            surfaceContainer = Color(scheme.surfaceContainer),
            surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
            surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
            surfaceContainerLow = Color(scheme.surfaceContainerLow),
            surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
            primaryFixed = Color(scheme.primaryFixed),
            primaryFixedDim = Color(scheme.primaryFixedDim),
            onPrimaryFixed = Color(scheme.onPrimaryFixed),
            onPrimaryFixedVariant = Color(scheme.onPrimaryFixedVariant),
            secondaryFixed = Color(scheme.secondaryFixed),
            secondaryFixedDim = Color(scheme.secondaryFixedDim),
            onSecondaryFixed = Color(scheme.onSecondaryFixed),
            onSecondaryFixedVariant = Color(scheme.onSecondaryFixedVariant),
            tertiaryFixed = Color(scheme.tertiaryFixed),
            tertiaryFixedDim = Color(scheme.tertiaryFixedDim),
            onTertiaryFixed = Color(scheme.onTertiaryFixed),
            onTertiaryFixedVariant = Color(scheme.onTertiaryFixedVariant)
        )
    }

    /**
     * Convert color scheme to grayscale
     */
    private fun androidx.compose.material3.ColorScheme.toGrayscaleScheme(): androidx.compose.material3.ColorScheme {
        fun Color.toGrayscale(): Color {
            val gray = (red * 0.299f + green * 0.587f + blue * 0.114f)
            return Color(gray, gray, gray, alpha)
        }

        return copy(
            primary = primary.toGrayscale(),
            onPrimary = onPrimary.toGrayscale(),
            primaryContainer = primaryContainer.toGrayscale(),
            onPrimaryContainer = onPrimaryContainer.toGrayscale(),
            secondary = secondary.toGrayscale(),
            onSecondary = onSecondary.toGrayscale(),
            secondaryContainer = secondaryContainer.toGrayscale(),
            onSecondaryContainer = onSecondaryContainer.toGrayscale(),
            tertiary = tertiary.toGrayscale(),
            onTertiary = onTertiary.toGrayscale(),
            tertiaryContainer = tertiaryContainer.toGrayscale(),
            onTertiaryContainer = onTertiaryContainer.toGrayscale(),
            surface = surface.toGrayscale(),
            onSurface = onSurface.toGrayscale(),
            surfaceVariant = surfaceVariant.toGrayscale(),
            onSurfaceVariant = onSurfaceVariant.toGrayscale()
        )
    }

    /**
     * Calculate average color from pixels
     */
    private fun averageColorArgb(pixels: IntArray): Int {
        if (pixels.isEmpty()) return 0xFF6750A4.toInt()

        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L

        for (argb in pixels) {
            totalRed += (argb ushr 16) and 0xFF
            totalGreen += (argb ushr 8) and 0xFF
            totalBlue += argb and 0xFF
        }

        val size = pixels.size.toLong()
        val r = (totalRed / size).toInt().coerceIn(0, 255)
        val g = (totalGreen / size).toInt().coerceIn(0, 255)
        val b = (totalBlue / size).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Check if artwork is mostly neutral (low chroma)
     */
    private fun isMostlyNeutralArtwork(colorsToPopulation: Map<Int, Int>): Boolean {
        if (colorsToPopulation.isEmpty()) return false

        var totalPopulation = 0.0
        var neutralPopulation = 0.0
        var highChromaPopulation = 0.0
        var weightedChroma = 0.0

        for ((argb, populationInt) in colorsToPopulation) {
            if (populationInt <= 0) continue
            val population = populationInt.toDouble()
            val chroma = Hct.fromInt(argb).chroma

            totalPopulation += population
            weightedChroma += chroma * population

            if (chroma <= NEUTRAL_PIXEL_CHROMA_THRESHOLD) {
                neutralPopulation += population
            }
            if (chroma >= HIGH_CHROMA_THRESHOLD) {
                highChromaPopulation += population
            }
        }

        if (totalPopulation <= 0.0) return false

        val neutralRatio = neutralPopulation / totalPopulation
        val highChromaRatio = highChromaPopulation / totalPopulation
        val meanChroma = weightedChroma / totalPopulation

        return neutralRatio >= REQUIRED_NEUTRAL_POPULATION &&
            highChromaRatio <= MAX_HIGH_CHROMA_POPULATION &&
            meanChroma <= MAX_WEIGHTED_CHROMA_FOR_NEUTRAL
    }

    /**
     * Check if color should use neutral scheme
     */
    private fun shouldUseNeutralArtworkScheme(argb: Int, sourceHct: Hct): Boolean {
        return sourceHct.chroma <= GRAYSCALE_CHROMA_THRESHOLD && isArgbNearGrayscale(argb)
    }

    /**
     * Check if ARGB color is near grayscale
     */
    private fun isArgbNearGrayscale(argb: Int): Boolean {
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        return maxOf(abs(red - green), abs(green - blue), abs(red - blue)) <= MAX_GRAYSCALE_CHANNEL_DELTA
    }

    /**
     * Clear the extracted color cache
     */
    fun clearExtractedColorCache() {
        extractedColorCache.evictAll()
    }

    /**
     * Convert ExtractedColors to JSON string for storage
     */
    fun colorsToJson(colors: ExtractedColors): String {
        return gson.toJson(colors)
    }

    /**
     * Convert JSON string back to ExtractedColors
     * Returns null if parsing fails
     */
    fun jsonToColors(json: String?): ExtractedColors? {
        if (json == null) return null
        return try {
            gson.fromJson(json, ExtractedColors::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse colors JSON", e)
            null
        }
    }
}
