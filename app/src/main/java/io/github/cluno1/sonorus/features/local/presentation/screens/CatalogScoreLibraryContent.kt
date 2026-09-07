package io.github.cluno1.sonorus.features.local.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.cluno1.sonorus.R
import io.github.cluno1.sonorus.features.catalog.domain.CatalogLibraryScoreWork
import io.github.cluno1.sonorus.shared.presentation.components.icons.MaterialSymbolIcon
import io.github.cluno1.sonorus.shared.presentation.components.icons.Icon

@Composable
internal fun CatalogScoreLibraryContent(
    scoreWorks: List<CatalogLibraryScoreWork>,
    onScoreWorkClick: (CatalogLibraryScoreWork) -> Unit,
    bottomPadding: Dp,
) {
    if (scoreWorks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(MaterialSymbolIcon("score"), contentDescription = null, modifier = Modifier.size(48.dp))
                Text(
                    stringResource(R.string.catalog_scores_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(scoreWorks.sortedBy { it.title.lowercase() }, key = { it.workId }) { work ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onScoreWorkClick(work) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                ListItem(
                    headlineContent = { Text(work.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            work.artist?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 1) }
                            Text(stringResource(R.string.catalog_score_count, work.scoreCount))
                        }
                    },
                    leadingContent = {
                        Icon(MaterialSymbolIcon("score"), contentDescription = null, modifier = Modifier.size(32.dp))
                    },
                    trailingContent = { Icon(MaterialSymbolIcon("chevron_right"), contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
    }
}
