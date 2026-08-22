/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.ui.theme

import androidx.compose.ui.graphics.Color

// Material Design 3 Color System - Light Theme
// Primary palette derived from seed #6750A4 (matches app logo exactly)
val PrimaryLight = Color(0xFF6750A4) // M3 baseline purple — matches logo stroke color
val OnPrimaryLight = Color(0xFFFFFFFF) // White text on primary
val PrimaryContainerLight = Color(0xFFEADDFF) // Light purple container
val OnPrimaryContainerLight = Color(0xFF21005D) // Dark text on primary container

// Secondary color palette — neutral violet
val SecondaryLight = Color(0xFF625B71) // Neutral gray-purple for balance
val OnSecondaryLight = Color(0xFFFFFFFF) // White text on secondary
val SecondaryContainerLight = Color(0xFFE8DEF8) // Light gray-purple container
val OnSecondaryContainerLight = Color(0xFF1D192B) // Dark text on secondary container

// Tertiary color palette — rosy pink (matches logo center pill gradient)
val TertiaryLight = Color(0xFF7D5260) // Rosy pink accent
val OnTertiaryLight = Color(0xFFFFFFFF) // White text on tertiary
val TertiaryContainerLight = Color(0xFFFFD8E4) // Light pink container (matches logo)
val OnTertiaryContainerLight = Color(0xFF31111D) // Dark text on tertiary container

// Error color palette
val ErrorLight = Color(0xFFB3261E) // Standard Material error red
val OnErrorLight = Color(0xFFFFFFFF) // White text on error
val ErrorContainerLight = Color(0xFFF9DEDC) // Light red container
val OnErrorContainerLight = Color(0xFF410E0B) // Dark text on error container

// Background and surface colors
val BackgroundLight = Color(0xFFFAF5FF) // Softened — subtle purple tint, less stark white
val OnBackgroundLight = Color(0xFF1C1B1F) // Dark text on background
val SurfaceLight = Color(0xFFFAF5FF) // Surface same as background
val OnSurfaceLight = Color(0xFF1C1B1F) // Dark text on surface
val SurfaceVariantLight = Color(0xFFE4DCF0) // Slightly richer purple-gray surface variant
val OnSurfaceVariantLight = Color(0xFF49454F) // Medium gray text

// Outline colors for borders and dividers
val OutlineLight = Color(0xFF79747E) // Medium gray outline
val OutlineVariantLight = Color(0xFFCAC4D0) // Light gray outline variant

// Surface containers for different elevation levels
val SurfaceContainerLowestLight = Color(0xFFF5EFFE) // Lowest elevation — faint purple tint
val SurfaceContainerLowLight = Color(0xFFF0E9F9) // Low elevation
val SurfaceContainerLight = Color(0xFFEAE3F3) // Medium elevation
val SurfaceContainerHighLight = Color(0xFFE4DDED) // High elevation
val SurfaceContainerHighestLight = Color(0xFFDED6E7) // Highest elevation

// Inverse colors for special cases
val InverseSurfaceLight = Color(0xFF313033) // Dark surface for light theme
val InverseOnSurfaceLight = Color(0xFFF4EFF4) // Light text on inverse surface
val InversePrimaryLight = Color(0xFFD0BCFF) // Light primary on dark surface

// Material Design 3 Color System - Dark Theme
// Primary palette derived from seed #6750A4 (matches app logo exactly)
val PrimaryDark = Color(0xFFD0BCFF) // Light purple for dark theme (M3 baseline)
val OnPrimaryDark = Color(0xFF381E72) // Dark text on primary
val PrimaryContainerDark = Color(0xFF4F378B) // Medium purple container
val OnPrimaryContainerDark = Color(0xFFEADDFF) // Light text on primary container

// Secondary color palette — neutral violet
val SecondaryDark = Color(0xFFCCC2DC) // Light gray-purple for balance
val OnSecondaryDark = Color(0xFF332D41) // Dark text on secondary
val SecondaryContainerDark = Color(0xFF4A4458) // Medium gray-purple container
val OnSecondaryContainerDark = Color(0xFFE8DEF8) // Light text on secondary container

// Tertiary color palette — rosy pink (matches logo center pill gradient)
val TertiaryDark = Color(0xFFEFB8C8) // Rosy pink for dark theme
val OnTertiaryDark = Color(0xFF492532) // Dark text on tertiary
val TertiaryContainerDark = Color(0xFF633B48) // Medium rose container
val OnTertiaryContainerDark = Color(0xFFFFD8E4) // Light text on tertiary container

// Error color palette
val ErrorDark = Color(0xFFF2B8B5) // Light red for dark theme
val OnErrorDark = Color(0xFF601410) // Dark text on error
val ErrorContainerDark = Color(0xFF8C1D18) // Medium red container
val OnErrorContainerDark = Color(0xFFF9DEDC) // Light text on error container

// Background and surface colors
val BackgroundDark = Color(0xFF141218) // Slightly deeper than M3 baseline for better depth
val OnBackgroundDark = Color(0xFFE6E1E5) // Light text on background
val SurfaceDark = Color(0xFF141218) // Surface same as background
val OnSurfaceDark = Color(0xFFE6E1E5) // Light text on surface
val SurfaceVariantDark = Color(0xFF49454F) // Medium gray surface variant
val OnSurfaceVariantDark = Color(0xFFCAC4D0) // Light gray text

// Outline colors for borders and dividers
val OutlineDark = Color(0xFF938F99) // Light gray outline
val OutlineVariantDark = Color(0xFF49454F) // Medium gray outline variant

// Surface containers for different elevation levels
val SurfaceContainerLowestDark = Color(0xFF0D0B10) // Lowest elevation — slightly deeper
val SurfaceContainerLowDark = Color(0xFF1A1820) // Low elevation
val SurfaceContainerDark = Color(0xFF201E25) // Medium elevation
val SurfaceContainerHighDark = Color(0xFF2A282F) // High elevation
val SurfaceContainerHighestDark = Color(0xFF35333A) // Highest elevation

// Inverse colors for special cases
val InverseSurfaceDark = Color(0xFFE6E1E5) // Light surface for dark theme
val InverseOnSurfaceDark = Color(0xFF313033) // Dark text on inverse surface
val InversePrimaryDark = Color(0xFF6750A4) // Dark primary on light surface (= logo color)

// Legacy music-specific colors (for backward compatibility)
val MusicPrimaryLight = PrimaryLight
val MusicPrimaryVariantLight = PrimaryContainerLight
val MusicSecondaryLight = TertiaryLight

val MusicPrimaryDark = PrimaryDark
val MusicPrimaryVariantDark = PrimaryContainerDark
val MusicSecondaryDark = TertiaryDark

// UI Specific Colors for player components
val PlayerButtonColor = PrimaryLight
val PlayerButtonColorDark = PrimaryDark
val PlayerProgressColor = TertiaryLight
val PlayerProgressBackgroundLight = SurfaceContainerLight
val PlayerProgressBackgroundDark = SurfaceContainerDark
val PlayerBackgroundLight = BackgroundLight
val PlayerBackgroundDark = BackgroundDark

// Status colors for notifications, errors, etc.
val SuccessLight = Color(0xFF2E7D32) // Material green
val SuccessDark = Color(0xFF66BB6A) // Light green for dark theme
val WarningLight = Color(0xFFEF6C00) // Material orange
val WarningDark = Color(0xFFFFB74D) // Light orange for dark theme

// ============================================
// Custom Color Scheme Presets
// ============================================

// Warm Theme - Sunset colors
val WarmPrimaryLight = Color(0xFFFF6B35)
val WarmOnPrimaryLight = Color(0xFFFFFFFF)
val WarmPrimaryContainerLight = Color(0xFFFFDDD2)
val WarmOnPrimaryContainerLight = Color(0xFF3E0400)

val WarmSecondaryLight = Color(0xFFF7931E)
val WarmOnSecondaryLight = Color(0xFFFFFFFF)
val WarmSecondaryContainerLight = Color(0xFFFFDDB6)
val WarmOnSecondaryContainerLight = Color(0xFF2C1600)

val WarmTertiaryLight = Color(0xFFFFC857)
val WarmOnTertiaryLight = Color(0xFF432A0D)
val WarmTertiaryContainerLight = Color(0xFFFFE8B6)
val WarmOnTertiaryContainerLight = Color(0xFF261900)

val WarmPrimaryDark = Color(0xFFFFB59A)
val WarmOnPrimaryDark = Color(0xFF5F1500)
val WarmPrimaryContainerDark = Color(0xFFC84520)
val WarmOnPrimaryContainerDark = Color(0xFFFFDDD2)

val WarmSecondaryDark = Color(0xFFFFD499)
val WarmOnSecondaryDark = Color(0xFF4A2800)
val WarmSecondaryContainerDark = Color(0xFFD97E00)
val WarmOnSecondaryContainerDark = Color(0xFFFFDDB6)

val WarmTertiaryDark = Color(0xFFFFE099)
val WarmOnTertiaryDark = Color(0xFF442B00)
val WarmTertiaryContainerDark = Color(0xFFFFA91F)
val WarmOnTertiaryContainerDark = Color(0xFFFFE8B6)

// Cool Theme - Ocean colors
val CoolPrimaryLight = Color(0xFF1E88E5)
val CoolOnPrimaryLight = Color(0xFFFFFFFF)
val CoolPrimaryContainerLight = Color(0xFFD1E4FF)
val CoolOnPrimaryContainerLight = Color(0xFF001D36)

val CoolSecondaryLight = Color(0xFF00897B)
val CoolOnSecondaryLight = Color(0xFFFFFFFF)
val CoolSecondaryContainerLight = Color(0xFFB2DFDB)
val CoolOnSecondaryContainerLight = Color(0xFF00201D)

val CoolTertiaryLight = Color(0xFF80DEEA)
val CoolOnTertiaryLight = Color(0xFF003640)
val CoolTertiaryContainerLight = Color(0xFFB2EBF2)
val CoolOnTertiaryContainerLight = Color(0xFF002025)

val CoolPrimaryDark = Color(0xFF90CAF9)
val CoolOnPrimaryDark = Color(0xFF003258)
val CoolPrimaryContainerDark = Color(0xFF004A77)
val CoolOnPrimaryContainerDark = Color(0xFFD1E4FF)

val CoolSecondaryDark = Color(0xFF4DB6AC)
val CoolOnSecondaryDark = Color(0xFF003731)
val CoolSecondaryContainerDark = Color(0xFF005048)
val CoolOnSecondaryContainerDark = Color(0xFFB2DFDB)

val CoolTertiaryDark = Color(0xFF4DD0E1)
val CoolOnTertiaryDark = Color(0xFF00363D)
val CoolTertiaryContainerDark = Color(0xFF004F58)
val CoolOnTertiaryContainerDark = Color(0xFFB2EBF2)

// Forest Theme - Nature green
val ForestPrimaryLight = Color(0xFF2E7D32)
val ForestOnPrimaryLight = Color(0xFFFFFFFF)
val ForestPrimaryContainerLight = Color(0xFFC8E6C9)
val ForestOnPrimaryContainerLight = Color(0xFF0D5016)

val ForestSecondaryLight = Color(0xFF558B2F)
val ForestOnSecondaryLight = Color(0xFFFFFFFF)
val ForestSecondaryContainerLight = Color(0xFFDCEDC8)
val ForestOnSecondaryContainerLight = Color(0xFF1B5E20)

val ForestTertiaryLight = Color(0xFF9CCC65)
val ForestOnTertiaryLight = Color(0xFF2E5016)
val ForestTertiaryContainerLight = Color(0xFFE7F5E1)
val ForestOnTertiaryContainerLight = Color(0xFF223608)

val ForestPrimaryDark = Color(0xFF81C784)
val ForestOnPrimaryDark = Color(0xFF0D5016)
val ForestPrimaryContainerDark = Color(0xFF1B5E20)
val ForestOnPrimaryContainerDark = Color(0xFFC8E6C9)

val ForestSecondaryDark = Color(0xFFAED581)
val ForestOnSecondaryDark = Color(0xFF1B5E20)
val ForestSecondaryContainerDark = Color(0xFF33691E)
val ForestOnSecondaryContainerDark = Color(0xFFDCEDC8)

val ForestTertiaryDark = Color(0xFFDCE775)
val ForestOnTertiaryDark = Color(0xFF3F5100)
val ForestTertiaryContainerDark = Color(0xFF5A7700)
val ForestOnTertiaryContainerDark = Color(0xFFE7F5E1)

// Rose Theme - Pink elegance
val RosePrimaryLight = Color(0xFFE91E63)
val RoseOnPrimaryLight = Color(0xFFFFFFFF)
val RosePrimaryContainerLight = Color(0xFFF8BBD0)
val RoseOnPrimaryContainerLight = Color(0xFF3E001D)

val RoseSecondaryLight = Color(0xFFC2185B)
val RoseOnSecondaryLight = Color(0xFFFFFFFF)
val RoseSecondaryContainerLight = Color(0xFFFFCDD2)
val RoseOnSecondaryContainerLight = Color(0xFF300016)

val RoseTertiaryLight = Color(0xFFF8BBD0)
val RoseTertiaryLight2 = Color(0xFFFF80AB)
val RoseOnTertiaryLight = Color(0xFF5C002E)
val RoseTertiaryContainerLight = Color(0xFFFFE0EC)
val RoseOnTertiaryContainerLight = Color(0xFF31000F)

val RosePrimaryDark = Color(0xFFF48FB1)
val RoseOnPrimaryDark = Color(0xFF560027)
val RosePrimaryContainerDark = Color(0xFFC2185B)
val RoseOnPrimaryContainerDark = Color(0xFFF8BBD0)

val RoseSecondaryDark = Color(0xFFFFAB91)
val RoseOnSecondaryDark = Color(0xFF5F000A)
val RoseSecondaryContainerDark = Color(0xFFAD1457)
val RoseOnSecondaryContainerDark = Color(0xFFFFCDD2)

val RoseTertiaryDark = Color(0xFFFF80AB)
val RoseOnTertiaryDark = Color(0xFF5C002E)
val RoseTertiaryContainerDark = Color(0xFFD81B60)
val RoseOnTertiaryContainerDark = Color(0xFFFFE0EC)

// Monochrome Theme - Minimalist grayscale
val MonoPrimaryLight = Color(0xFF424242)
val MonoOnPrimaryLight = Color(0xFFFFFFFF)
val MonoPrimaryContainerLight = Color(0xFFE0E0E0)
val MonoOnPrimaryContainerLight = Color(0xFF1C1C1C)

val MonoSecondaryLight = Color(0xFF616161)
val MonoOnSecondaryLight = Color(0xFFFFFFFF)
val MonoSecondaryContainerLight = Color(0xFFEEEEEE)
val MonoOnSecondaryContainerLight = Color(0xFF2C2C2C)

val MonoTertiaryLight = Color(0xFF9E9E9E)
val MonoOnTertiaryLight = Color(0xFF1C1C1C)
val MonoTertiaryContainerLight = Color(0xFFF5F5F5)
val MonoOnTertiaryContainerLight = Color(0xFF1C1C1C)

val MonoPrimaryDark = Color(0xFFBDBDBD)
val MonoOnPrimaryDark = Color(0xFF1C1C1C)
val MonoPrimaryContainerDark = Color(0xFF424242)
val MonoOnPrimaryContainerDark = Color(0xFFE0E0E0)

val MonoSecondaryDark = Color(0xFF9E9E9E)
val MonoOnSecondaryDark = Color(0xFF2C2C2C)
val MonoSecondaryContainerDark = Color(0xFF616161)
val MonoOnSecondaryContainerDark = Color(0xFFEEEEEE)

val MonoTertiaryDark = Color(0xFF757575)
val MonoOnTertiaryDark = Color(0xFFEEEEEE)
val MonoTertiaryContainerDark = Color(0xFF424242)
val MonoOnTertiaryContainerDark = Color(0xFFF5F5F5)
