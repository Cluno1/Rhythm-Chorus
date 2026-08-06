package chromahub.rhythm.app.shared.presentation.components

import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.icons.Icon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import chromahub.rhythm.app.R
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A composable to display and interact with song ratings (0-5 stars)
 * 
 * @param rating Current rating (0-5)
 * @param onRatingChanged Callback when rating is changed
 * @param enabled Whether the rating can be changed
 * @param modifier Modifier for the component
 * @param size Size of each star in dp
 * @param filledColor Color of filled stars
 * @param emptyColor Color of empty stars
 */
@Composable
fun RatingStars(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    filledColor: Color = Color(0xFFFFD700),
    // Gold
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val ratingLabels = listOf(
        stringResource(R.string.rating_not_rated),
        stringResource(R.string.rating_liked),
        stringResource(R.string.rating_good),
        stringResource(R.string.rating_great),
        stringResource(R.string.rating_loved),
        stringResource(R.string.rating_absolute_favorite)
    )
    
    val unknownRatingLabel = stringResource(R.string.rating_unknown)
    val label = ratingLabels.getOrNull(rating) ?: unknownRatingLabel
    val accessibilityDescription = pluralStringResource(R.plurals.rating_stars_desc, rating, label, rating)

    Row(
        modifier = modifier.semantics {
            contentDescription = accessibilityDescription
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            val isFilled = starRating <= rating
            
            val starColor by animateColorAsState(
                targetValue = if (isFilled) filledColor else emptyColor,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "star_color_$index"
            )
            
            val starSize by animateDpAsState(
                targetValue = if (isFilled) size * 1.1f else size,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "star_size_$index"
            )
            
            Icon(
                imageVector = if (isFilled) MaterialSymbolIcon("star", filled = true) else MaterialSymbolIcon("star"),
                contentDescription = pluralStringResource(R.plurals.rating_stars_count, starRating, starRating),
                tint = starColor,
                modifier = Modifier
                    .size(starSize)
                    .graphicsLayer {
                        scaleX = if (isFilled) 1.1f else 1f
                        scaleY = if (isFilled) 1.1f else 1f
                    }
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = ratingLabels.getOrNull(starRating),
                        onClick = {
                            // Toggle: if clicking same rating, unrate it
                            onRatingChanged(if (rating == starRating) 0 else starRating)
                        }
                    )
            )
        }
    }
}

/**
 * Compact version of RatingStars for display only
 */
@Composable
fun RatingStarsDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    filledColor: Color = Color(0xFFFFD700),
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            val isFilled = (index + 1) <= rating
            Icon(
                imageVector = if (isFilled) MaterialSymbolIcon("star", filled = true) else MaterialSymbolIcon("star"),
                contentDescription = null,
                tint = if (isFilled) filledColor else emptyColor,
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Get rating label based on rating value
 */
fun getRatingLabel(context: Context, rating: Int): String {
    return when (rating) {
        0 -> context.getString(R.string.rating_not_rated)
        1 -> context.getString(R.string.rating_liked)
        2 -> context.getString(R.string.rating_good)
        3 -> context.getString(R.string.rating_great)
        4 -> context.getString(R.string.rating_loved)
        5 -> context.getString(R.string.rating_absolute_favorite)
        else -> context.getString(R.string.rating_unknown)
    }
}

/**
 * Get rating description for accessibility
 */
fun getRatingDescription(context: Context, rating: Int): String {
    return when (rating) {
        0 -> context.getString(R.string.rating_desc_not_rated)
        1 -> context.getString(R.string.rating_desc_liked)
        2 -> context.getString(R.string.rating_desc_good)
        3 -> context.getString(R.string.rating_desc_great)
        4 -> context.getString(R.string.rating_desc_loved)
        5 -> context.getString(R.string.rating_desc_absolute_favorite)
        else -> context.getString(R.string.rating_desc_unknown)
    }
}
