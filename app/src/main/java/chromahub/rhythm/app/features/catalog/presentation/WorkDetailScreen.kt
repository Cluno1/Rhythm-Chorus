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
import chromahub.rhythm.app.features.catalog.domain.CatalogPlaybackPolicy
import chromahub.rhythm.app.features.catalog.domain.Rendition
import chromahub.rhythm.app.features.catalog.domain.Score
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding

/**
 * Work 详情 = 专辑页。
 *
 * 一个 work-id 聚合它全部编配下的：
 *  - 谱面（乐谱）→ 打开自实现的乐谱页（alphaTab 渲染并可直接合成发声）；
 *  - 歌曲（真实音频 Rendition，如 MP3）→ 当作歌曲交给 Rhythm 播放器（Media3）。
 * MIDI-only 等无真实音频的 Rendition 不作为“歌曲”出现（MIDI 只作来源）。
 */
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
                title = { Text(bundle?.work?.canonicalTitle ?: "专辑") },
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

        val scores: List<Pair<CatalogArrangement, Score>> =
            bundle.arrangements.flatMap { arr -> arr.scores.map { arr to it } }
        val songs: List<Pair<CatalogArrangement, Rendition>> =
            bundle.arrangements.flatMap { arr ->
                arr.renditions
                    .filter { CatalogPlaybackPolicy.isPlayableRendition(it) }
                    .map { arr to it }
            }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(LocalMiniPlayerPadding.current),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        bundle.work.canonicalTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        bundle.work.credits.sortedBy { it.position }
                            .joinToString(" · ") { it.displayName }
                            .ifBlank { "暂无作者信息" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "专辑 · ${scores.size} 个谱面 · ${songs.size} 首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }

            item { SectionHeader("谱面（乐谱）", scores.size) }
            if (scores.isEmpty()) {
                item { EmptyHint("暂无谱面") }
            } else {
                items(scores, key = { it.second.id }) { (arrangement, score) ->
                    ScoreRow(bundle, arrangement, score, onOpenScore)
                }
            }

            item { SectionHeader("歌曲", songs.size) }
            if (songs.isEmpty()) {
                item { EmptyHint("暂无歌曲（真实音频）") }
            } else {
                items(songs, key = { it.second.id }) { (arrangement, rendition) ->
                    SongRow(bundle, arrangement, rendition, onPlay)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        "$title · $count",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun ScoreRow(
    bundle: WorkBundle,
    arrangement: CatalogArrangement,
    score: Score,
    onOpenScore: (WorkBundle, CatalogArrangement, Score) -> Unit,
) {
    val revisionId = score.publishedRevisionId ?: score.headRevisionId
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clickable(enabled = revisionId != null) { onOpenScore(bundle, arrangement, score) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(score.label, fontWeight = FontWeight.Medium)
                Text(
                    listOf(arrangement.name, score.origin, "修订 ${score.revision}").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(if (revisionId == null) "无可用修订" else "打开乐谱")
        }
    }
}

@Composable
private fun SongRow(
    bundle: WorkBundle,
    arrangement: CatalogArrangement,
    rendition: Rendition,
    onPlay: (WorkBundle, CatalogArrangement, Rendition) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(rendition.label, fontWeight = FontWeight.Medium)
                Text(
                    listOfNotNull(
                        arrangement.name,
                        rendition.kind,
                        rendition.durationMs?.let(::formatDuration),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { onPlay(bundle, arrangement, rendition) }) { Text("播放") }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
