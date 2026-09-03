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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkCatalogScreen(
    state: CatalogUiState,
    onRefresh: (String?) -> Unit,
    onOpenWork: (String) -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    searchInitiallyVisible: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        if (searchInitiallyVisible || query.isNotEmpty()) onRefresh(query)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (searchInitiallyVisible) "搜索作品" else "私有作品库") },
                navigationIcon = { onBack?.let { TextButton(onClick = it) { Text("返回") } } },
                actions = {
                    TextButton(onClick = onConfigure) { Text("服务器") }
                    TextButton(onClick = { onRefresh(query) }, enabled = !state.refreshing) { Text("刷新") }
                },
            )
        },
    ) { padding ->
        if (!state.configured) {
            CatalogEmptyState(onConfigure, Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(LocalMiniPlayerPadding.current),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("按作品名搜索") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.offlineSnapshot) {
                Text(
                    "当前离线，正在显示上次同步的作品",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            when {
                state.loading && state.works.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.works.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (query.isBlank()) "作品库暂时为空" else "没有匹配的作品")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.works, key = { it.id }) { work -> WorkCard(work, onOpenWork) }
                }
            }
        }
    }
}

@Composable
private fun WorkCard(work: WorkSummary, onOpenWork: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onOpenWork(work.id) },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(work.canonicalTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val credits = work.credits.sortedBy { it.position }.joinToString(" · ") { it.displayName }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(credits.ifBlank { "未标注作者" }, style = MaterialTheme.typography.bodyMedium)
                work.language?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("修订 ${work.revision} · ${work.status}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CatalogEmptyState(onConfigure: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("尚未配置私有作品库", style = MaterialTheme.typography.titleLarge)
            Text("连接服务器后即可浏览作品、谱面和受管音源")
            TextButton(onClick = onConfigure) { Text("配置服务器") }
        }
    }
}
