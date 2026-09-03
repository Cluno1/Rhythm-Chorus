package chromahub.rhythm.app.features.catalog.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.catalog.domain.Arrangement as CatalogArrangement
import chromahub.rhythm.app.features.catalog.domain.Rendition
import chromahub.rhythm.app.features.catalog.domain.Score
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    state: CatalogUiState,
    onBack: () -> Unit,
    onPlay: (WorkBundle, CatalogArrangement, Rendition) -> Unit,
    onOpenScore: (WorkBundle, CatalogArrangement, Score) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bundle = state.selectedBundle
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(bundle?.work?.canonicalTitle ?: "作品详情") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        if (bundle == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator() else Text(state.error ?: "无法加载作品")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(LocalMiniPlayerPadding.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(bundle.work.canonicalTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(bundle.work.credits.sortedBy { it.position }.joinToString(" · ") { "${it.role}: ${it.displayName}" }.ifBlank { "暂无作者信息" })
                    Text("${bundle.arrangements.size} 个编配 · bundle ${bundle.bundleVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            items(bundle.arrangements, key = { it.id }) { arrangement ->
                ArrangementCard(bundle, arrangement, onPlay, onOpenScore)
            }
        }
    }
}

@Composable
private fun ArrangementCard(
    bundle: WorkBundle,
    arrangement: CatalogArrangement,
    onPlay: (WorkBundle, CatalogArrangement, Rendition) -> Unit,
    onOpenScore: (WorkBundle, CatalogArrangement, Score) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(arrangement.name, style = MaterialTheme.typography.titleLarge)
            val details = listOfNotNull(arrangement.voicing, arrangement.keySignature).joinToString(" · ")
            if (details.isNotBlank()) Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (arrangement.parts.isNotEmpty()) {
                Text("声部：${arrangement.parts.sortedBy { it.displayOrder }.joinToString(" / ") { it.name }}")
            }
            Text("谱面", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (arrangement.scores.isEmpty()) Text("暂无谱面")
            arrangement.scores.forEach { score ->
                val revisionId = score.publishedRevisionId ?: score.headRevisionId
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = revisionId != null) { onOpenScore(bundle, arrangement, score) }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(score.label)
                        Text("${score.origin} · revision ${score.revision}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (revisionId == null) "无可用修订" else "打开")
                }
            }
            Text("可听版本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (arrangement.renditions.isEmpty()) Text("暂无可听版本")
            arrangement.renditions.forEach { rendition ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(rendition.label)
                        Text(
                            listOfNotNull(rendition.kind, rendition.durationMs?.let(::formatDuration)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { onPlay(bundle, arrangement, rendition) }, enabled = rendition.assets.isNotEmpty()) {
                        Text("播放")
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
