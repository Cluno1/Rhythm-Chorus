package io.github.cluno1.sonorus.features.catalog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.features.catalog.domain.stableArtworkUri
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveCard
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveShapeTarget
import io.github.cluno1.sonorus.shared.presentation.components.common.ExpressiveShapes
import io.github.cluno1.sonorus.shared.presentation.components.common.rememberExpressiveShapeFor
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon
import io.github.cluno1.sonorus.shared.presentation.components.icons.RhythmIcons
import io.github.cluno1.sonorus.util.HapticType
import io.github.cluno1.sonorus.util.HapticUtils

@Composable
fun ScoreWorkArtwork(
    work: CatalogLibraryScoreWork,
    trustedServerUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART),
    iconSize: Dp = 48.dp,
) {
    val context = LocalContext.current
    val artworkUri = remember(work.coverAssetId, work.coverUrl, trustedServerUrl) {
        work.stableArtworkUri(trustedServerUrl)
    }
    val request = remember(artworkUri, context) {
        artworkUri?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(150)
                .memoryCacheKey(it.toString())
                .diskCacheKey(it.toString())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = RhythmIcons.Score,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        request?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

@Composable
fun ScoreWorkGridCard(
    work: CatalogLibraryScoreWork,
    trustedServerUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ExpressiveShapes.SquircleLarge,
    cardHeight: Dp = 260.dp,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val metadata = scoreWorkMetadata(work)
    val description = stringResource(
        R.string.catalog_open_score_description,
        work.title,
        work.artist?.takeIf(String::isNotBlank) ?: stringResource(R.string.unknown_artist),
        metadata,
    )

    ExpressiveCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
            onClick()
        },
        modifier = modifier
            .heightIn(min = cardHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ScoreWorkArtwork(
                work = work,
                trustedServerUrl = trustedServerUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = work.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = work.artist?.takeIf(String::isNotBlank) ?: stringResource(R.string.unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = metadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ScoreWorkListItem(
    work: CatalogLibraryScoreWork,
    trustedServerUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val metadata = scoreWorkMetadata(work)
    val description = stringResource(
        R.string.catalog_open_score_description,
        work.title,
        work.artist?.takeIf(String::isNotBlank) ?: stringResource(R.string.unknown_artist),
        metadata,
    )

    Surface(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreWorkArtwork(
                work = work,
                trustedServerUrl = trustedServerUrl,
                modifier = Modifier.size(68.dp),
                iconSize = 30.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                work.artist?.takeIf(String::isNotBlank)?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = RhythmIcons.Forward,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun scoreWorkMetadata(work: CatalogLibraryScoreWork): String {
    val scoreCount = pluralStringResource(R.plurals.catalog_score_count, work.scoreCount, work.scoreCount)
    val scoreLabels = work.scoreOptions.map { it.scoreLabel }.distinct()
    return (listOf(scoreCount) + scoreLabels).joinToString(" · ")
}
